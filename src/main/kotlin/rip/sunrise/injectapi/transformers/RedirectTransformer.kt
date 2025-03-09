package rip.sunrise.injectapi.transformers

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.hooks.redirect.field.FieldRedirectHook
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.getCapturedDescriptor
import rip.sunrise.injectapi.utils.getLocalRunningHookArray
import rip.sunrise.injectapi.utils.isHookRunning
import rip.sunrise.injectapi.utils.setHookRunning
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class RedirectTransformer {
    fun transform(node: ClassNode) {
        node.methods.forEach { method ->
            HookManager.getHookMap().values
                .filterIsInstance<FieldRedirectHook>()
                .filter { Type.getType(it.clazz).internalName == node.name }
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .forEach { hook ->
                    method.instructions
                        .filterIsInstance<FieldInsnNode>()
                        .filter { it.name == hook.targetField.name && it.desc == hook.targetField.type }
                        .filter { it.opcode in hook.type.allowedOpcodes }
                        .forEach {
                            val hookCode = generateHookCode(hook, it, method, node)

                            when (it.opcode) {
                                in listOf(Opcodes.GETFIELD, Opcodes.GETSTATIC) -> {
                                    method.instructions.insert(it, hookCode)
                                }

                                in listOf(Opcodes.PUTFIELD, Opcodes.PUTSTATIC) -> {
                                    method.instructions.insertBefore(it, hookCode)
                                }

                                else -> error("Disallowed opcode ${it.opcode} while redirecting fields")
                            }
                        }
                }
        }
    }

    private fun generateHookCode(hook: FieldRedirectHook, field: FieldInsnNode, method: MethodNode, clazz: ClassNode): InsnList {
        val hookId = HookManager.getHookId(hook)

        return InsnList().apply {
            val endLabel = LabelNode()

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

            if (hook.targetField.type == "J" || hook.targetField.type == "D") {
                add(InsnNode(Opcodes.DUP_X2))
                add(InsnNode(Opcodes.POP))
            } else {
                add(InsnNode(Opcodes.SWAP))
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

            val fieldType = field.desc
            val capturedDescriptor = getCapturedDescriptor(hook.arguments, method, clazz.name)
            add(InvokeDynamicInsnNode(
                "REDIRECT_FIELD",
                "($fieldType$capturedDescriptor)$fieldType",
                hookHandle,
                hookId
            ))

            if (hook.targetField.type == "J" || hook.targetField.type == "D") {
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