package rip.sunrise.injectapi.transformers

import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.hooks.inject.modes.InvokeInjection
import rip.sunrise.injectapi.hooks.inject.modes.ReturnInjection
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.getCapturedDescriptor
import rip.sunrise.injectapi.utils.getCheckCastReturnBytecode
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class InjectTransformer {
    fun transform(node: ClassNode) {
        node.methods.forEach { method ->
            method as MethodNode

            val methodHooks = HookManager.getHookMap().values
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .sortedBy { it.injectionMode.typePriority }
            if (methodHooks.isEmpty()) return@forEach

            methodHooks.forEach { hook ->
                if (!hook.validateArguments(method.localVariables as List<LocalVariableNode>)) {
                    // TODO: Error doesnt work. This is wild
                    println("Hook '$hook' attempts to capture invalid arguments")
                }

                val hookCode = generateHookCode(hook, method)

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
     * The hookId is obtained using [HookManager.getHookId].
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
     * @see HookManager.getHookId
     * @see rip.sunrise.injectapi.callsite.ProxyDynamicFactory.bootstrap
     */
    private fun generateHookCode(hook: InjectHook, method: MethodNode): InsnList {
        return InsnList().apply {
            // Initialize Context
            add(TypeInsnNode(Opcodes.NEW, "rip/sunrise/injectapi/hooks/inject/Context"))
            add(InsnNode(Opcodes.DUP))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, "rip/sunrise/injectapi/hooks/inject/Context", "<init>", "()V"))

            // Serialize
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "rip/sunrise/injectapi/hooks/inject/Context", "serialize", "()Ljava/util/Map;"))

            // Load args
            hook.arguments.forEach {
                add(VarInsnNode(it.opcode, it.index))
            }

            // Hook InvokeDynamic
            val hookHandle = Handle(
                Opcodes.H_INVOKESTATIC,
                "rip/sunrise/injectapi/callsite/ProxyDynamicFactory",
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
            val capturedDescriptor = getCapturedDescriptor(method.localVariables as List<LocalVariableNode>, hook.arguments)
            add(InvokeDynamicInsnNode(
                "INJECT",
                "(Ljava/util/Map;$capturedDescriptor)Ljava/util/Map;",
                hookHandle,
                HookManager.getHookId(hook)
            ))

            // Deserialize
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "rip/sunrise/injectapi/hooks/inject/Context", "deserialize", "(Ljava/util/Map;)Lrip/sunrise/injectapi/hooks/inject/Context;"))

            // Get optional
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "rip/sunrise/injectapi/hooks/inject/Context", "getReturnValue", "()Ljava/util/Optional;"))
            add(InsnNode(Opcodes.DUP))

            // Get optional value and check if its present
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z", false))

            // If optional has value return value
            val falseLabel = LabelNode(Label())
            add(JumpInsnNode(Opcodes.IFEQ, falseLabel))

            // True Branch
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;", false))
            add(getCheckCastReturnBytecode(Type.getReturnType(method.desc)))

            // IF FALSE
            add(falseLabel)
            add(InsnNode(Opcodes.POP))
        }
    }
}