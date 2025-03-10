package rip.sunrise.injectapi.transformers

import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.hooks.inject.modes.InvokeInjection
import rip.sunrise.injectapi.hooks.inject.modes.ReturnInjection
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.*
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class InjectTransformer {
    fun transform(node: ClassNode) {
        node.methods.forEach { method ->
            HookManager.getHooks()
                .filterIsInstance<InjectHook>()
                .filter { Type.getType(it.clazz).internalName == node.name }
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .sortedBy { it.injectionMode.typePriority }
                .forEach { hook ->
                    val hookCode = generateHookCode(hook, method, node)
                    when (hook.injectionMode) {
                        is HeadInjection -> {
                            method.instructions.insert(hookCode)
                        }

                        is ReturnInjection -> {
                            method.instructions
                                .iterator()
                                .asSequence()
                                .filterIsInstance<InsnNode>()
                                .filter {
                                    it.opcode in arrayOf(
                                        Opcodes.RETURN,
                                        Opcodes.ARETURN,
                                        Opcodes.IRETURN,
                                        Opcodes.LRETURN,
                                        Opcodes.FRETURN,
                                        Opcodes.DRETURN
                                    )
                                }.forEach {
                                    method.instructions.insertBefore(it, hookCode)
                                }
                        }

                        is InvokeInjection -> {
                            method.instructions
                                .iterator()
                                .asSequence()
                                .filterIsInstance<MethodInsnNode>()
                                .filter { it.name == hook.injectionMode.method.name && it.desc == hook.injectionMode.method.desc }
                                .forEach {
                                    val index = method.instructions.indexOf(it)
                                    val offset = method.instructions.get(index + hook.injectionMode.offset)

                                    method.instructions.insert(offset, hookCode)
                                }
                        }
                    }
                }
        }
    }

    /**
     * Generates the [hook] bytecode for [method].
     *
     * The generated code replicates the following code.
     * The hookId is obtained using [HookManager.getCachedHookId].
     * ```
     * val output = Context().serialize() // Map<String, Any>
     * val input = #invokedynamic hook(output, arg1, arg2, ...) // returns Map<String, Any>
     * val context = Context.deserialize(input)
     *
     * if (context.returnValue.isPresent()) {
     *      return context.returnValue.get()
     * }
     * ```
     *
     * Note: The output currently holds nothing.
     *
     * @see InjectHook.arguments
     * @see HookManager.getCachedHookId
     * @see rip.sunrise.injectapi.global.ProxyDynamicFactory.bootstrap
     */
    @OptIn(InjectApi.Internal::class)
    private fun generateHookCode(hook: InjectHook, method: MethodNode, clazz: ClassNode): InsnList {
        val contextClass = "@CONTEXT@"
        val hookId = HookManager.getCachedHookId(hook)

        return InsnList().apply {
            val endLabel = LabelNode()

            // Check and set running state
            getLocalRunningHookArray()

            add(InsnNode(Opcodes.DUP))
            isHookRunning(hookId)

            val ifNotRunningLabel = LabelNode()
            add(JumpInsnNode(Opcodes.IFEQ, ifNotRunningLabel))

            add(InsnNode(Opcodes.POP))
            add(JumpInsnNode(Opcodes.GOTO, endLabel))

            add(ifNotRunningLabel)

            add(InsnNode(Opcodes.DUP))
            setHookRunning(hookId, true)
            // Actual bootstrap entry. Stack: [Hook Array]

            // Initialize Context
            add(TypeInsnNode(Opcodes.NEW, contextClass))
            add(InsnNode(Opcodes.DUP))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, contextClass, "<init>", "()V", false))

            // Serialize
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, contextClass, "serialize", "()Ljava/util/Map;", false))

            // Load args
            hook.arguments.forEach {
                add(VarInsnNode(it.opcode, it.index))
            }

            // Hook InvokeDynamic
            val hookHandle = Handle(
                Opcodes.H_INVOKESTATIC,
                "@BOOTSTRAP@",
                "bootstrap",
                MethodType.methodType(
                    CallSite::class.java,
                    MethodHandles.Lookup::class.java,
                    String::class.java,
                    MethodType::class.java,
                    Int::class.java
                ).toMethodDescriptorString(),
                false
            )

            val capturedDescriptor = getCapturedDescriptor(hook.arguments, method, clazz.name)
            add(
                InvokeDynamicInsnNode(
                    "INJECT",
                    "(Ljava/util/Map;$capturedDescriptor)Ljava/util/Map;",
                    hookHandle,
                    hookId
                )
            )

            // Deserialize
            add(
                MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    contextClass,
                    "deserialize",
                    "(Ljava/util/Map;)L$contextClass;",
                    false
                )
            )

            // Get optional
            add(InsnNode(Opcodes.DUP))
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, contextClass, "hasReturnValue", "()Z"))

            // If Context has value, return value
            val falseLabel = LabelNode(Label())
            add(JumpInsnNode(Opcodes.IFEQ, falseLabel))

            // True Branch
            if (Type.getReturnType(method.desc) != Type.VOID_TYPE) {
                // Get the optional value
                add(FieldInsnNode(Opcodes.GETFIELD, contextClass, "returnValue", "Ljava/lang/Object;"))
                add(InsnNode(Opcodes.SWAP))
            } else {
                // Pop the optional
                add(InsnNode(Opcodes.POP))
            }

            setHookRunning(hookId, false)
            add(getCheckCastReturnBytecode(Type.getReturnType(method.desc)))

            // False Branch
            add(falseLabel) // Stack: [Hook Array, Context]
            add(InsnNode(Opcodes.POP))

            setHookRunning(hookId, false)
            add(endLabel)
        }
    }
}