package rip.sunrise.injectapi.transformers

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.hooks.redirect.method.MethodRedirectHook
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.extensions.getLoadBytecodes
import rip.sunrise.injectapi.utils.getBootstrapHandle
import rip.sunrise.injectapi.utils.getCapturedDescriptor
import rip.sunrise.injectapi.utils.getLocalRunningHookArray
import rip.sunrise.injectapi.utils.isHookRunning
import rip.sunrise.injectapi.utils.setHookRunning

class MethodRedirectTransformer {
    fun transform(node: ClassNode, supportsInvokedynamic: Boolean) {
        node.methods.forEach { method ->
            HookManager.getHooks().filterIsInstance<MethodRedirectHook>()
                .filter { Type.getType(it.clazz).internalName == node.name }
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .forEach { hook ->
                    if (!supportsInvokedynamic) {
                        TODO("Implement java 6 for FieldRedirect")
                    }

                    method.instructions
                        .filterIsInstance<MethodInsnNode>()
                        .filter { it.name == hook.targetMethod.name && it.desc == hook.targetMethod.desc }
                        .forEach {
                            val code = generateHookCode(hook, it, method, node)

                            method.instructions.insert(it, code)
                            method.instructions.remove(it)
                        }
                }
        }
    }

    @OptIn(InjectApi.Internal::class)
    private fun generateHookCode(
        hook: MethodRedirectHook,
        targetMethod: MethodInsnNode,
        method: MethodNode,
        clazz: ClassNode
    ): InsnList {
        val hookId = HookManager.getCachedHookId(hook)

        return InsnList().apply {
            val endLabel = LabelNode()

            getLocalRunningHookArray()

            add(InsnNode(Opcodes.DUP))
            isHookRunning(hookId)

            val ifNotRunningLabel = LabelNode()
            add(JumpInsnNode(Opcodes.IFEQ, ifNotRunningLabel))

            // Clear stack and call original method
            add(InsnNode(Opcodes.POP))
            add(targetMethod.clone(null))
            add(JumpInsnNode(Opcodes.GOTO, endLabel))

            add(ifNotRunningLabel)

            add(InsnNode(Opcodes.DUP))
            setHookRunning(hookId, true)

            // Pop `this` which is before a virtual call
            if (targetMethod.opcode == Opcodes.INVOKEVIRTUAL) {
                add(InsnNode(Opcodes.SWAP))
                add(InsnNode(Opcodes.POP))
            }

            // Load args
            add(hook.arguments.getLoadBytecodes())

            // Hook InvokeDynamic
            val hookHandle = getBootstrapHandle()

            val methodType = Type.getMethodType(targetMethod.desc)
            val methodArgDescriptor = methodType.argumentTypes.joinToString("") { it.descriptor }
            val capturedDescriptor = getCapturedDescriptor(hook.arguments, method, clazz.name)
            val returnDesc = methodType.returnType

            add(
                InvokeDynamicInsnNode(
                    "REDIRECT_METHOD",
                    "($methodArgDescriptor$capturedDescriptor)${methodType.returnType.descriptor}",
                    hookHandle,
                    hookId
                )
            )

            if (returnDesc == Type.LONG_TYPE || returnDesc == Type.DOUBLE_TYPE) {
                add(InsnNode(Opcodes.DUP2_X1))
                add(InsnNode(Opcodes.POP2))
            } else {
                add(InsnNode(Opcodes.SWAP))
            }

            setHookRunning(hookId, false)
            add(endLabel)
        }
    }
}