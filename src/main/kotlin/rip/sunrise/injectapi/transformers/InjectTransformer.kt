package rip.sunrise.injectapi.transformers

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
import rip.sunrise.injectapi.utils.extensions.getLoadBytecodes

class InjectTransformer {
    fun transform(node: ClassNode, supportsInvokedynamic: Boolean) {
        node.methods.forEach { method ->
            HookManager.getHooks()
                .filterIsInstance<InjectHook>()
                .filter { Type.getType(it.clazz).internalName == node.name }
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .sortedBy { it.injectionMode.typePriority }
                .forEach { hook ->
                    // NOTE: Keep the generateHookCode inlined, ASM doesn't like repeating labels.
                    when (hook.injectionMode) {
                        is HeadInjection -> {
                            method.instructions.insert(generateHookCode(hook, method, node, supportsInvokedynamic))
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
                                    method.instructions.insertBefore(
                                        it,
                                        generateHookCode(hook, method, node, supportsInvokedynamic)
                                    )
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

                                    method.instructions.insert(
                                        offset,
                                        generateHookCode(hook, method, node, supportsInvokedynamic)
                                    )
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
    private fun generateHookCode(
        hook: InjectHook,
        method: MethodNode,
        clazz: ClassNode,
        supportsInvokedynamic: Boolean
    ): InsnList {
        val contextClass = "@CONTEXT@"
        val hookId = HookManager.getCachedHookId(hook)

        val exStartLabel = LabelNode()
        val exEndLabel = LabelNode()
        val exHandlerLabel = LabelNode()
        val tryCatchBlock = TryCatchBlockNode(exStartLabel, exEndLabel, exHandlerLabel, null)

        // Insert at the front so that real exception handlers don't catch it instead of us
        method.tryCatchBlocks.add(0, tryCatchBlock)

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

            if (!supportsInvokedynamic) {
                add(LdcInsnNode(hookId))
                add(
                    MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "@BOOTSTRAP@",
                        "getHandle",
                        "(I)Ljava/lang/invoke/MethodHandle;",
                        false
                    )
                )
            }

            // Initialize Context
            add(TypeInsnNode(Opcodes.NEW, contextClass))
            add(InsnNode(Opcodes.DUP))
            add(MethodInsnNode(Opcodes.INVOKESPECIAL, contextClass, "<init>", "()V", false))

            // Serialize
            add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, contextClass, "serialize", "()Ljava/util/Map;", false))

            // Load args
            val resolved = resolveOpcodes(hook.arguments, method)
            add(resolved.getLoadBytecodes())

            val capturedDescriptor = getCapturedDescriptor(hook.arguments, method, clazz.name)

            add(exStartLabel)
            if (supportsInvokedynamic) {
                // Hook InvokeDynamic
                val hookHandle = getBootstrapHandle()

                add(
                    InvokeDynamicInsnNode(
                        "INJECT",
                        "(Ljava/util/Map;$capturedDescriptor)Ljava/util/Map;",
                        hookHandle,
                        hookId
                    )
                )
            } else {
                add(
                    MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandle",
                        "invoke",
                        "(Ljava/util/Map;$capturedDescriptor)Ljava/util/Map;",
                        false
                    )
                )
            }
            add(exEndLabel)

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
            add(JumpInsnNode(Opcodes.GOTO, endLabel))

            // Exception Handler code, it only catches user code.
            // Rethrows the exception after clearing the running flag
            // Stack: [Exception]

            add(exHandlerLabel)
            getLocalRunningHookArray()
            setHookRunning(hookId, false)
            add(InsnNode(Opcodes.ATHROW))

            add(endLabel)
        }
    }
}