package rip.sunrise.injectapi.transformers

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.hooks.redirect.method.MethodRedirectHook
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.getCapturedDescriptor
import rip.sunrise.injectapi.utils.getLocalRunningHookArray
import rip.sunrise.injectapi.utils.isHookRunning
import rip.sunrise.injectapi.utils.setHookRunning
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class MethodRedirectTransformer {
    fun transform(node: ClassNode) {
        node.methods.forEach { method ->
            HookManager.getHooks().filterIsInstance<MethodRedirectHook>()
                .filter { Type.getType(it.clazz).internalName == node.name }
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .forEach { hook ->
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