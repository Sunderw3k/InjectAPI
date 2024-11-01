package rip.sunrise.injectapi.transformers

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.hooks.redirect.field.FieldRedirectHook
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.getCapturedDescriptor
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
                .filter { it.className.replace(".", "/") == node.name }
                .filter { it.method.name == method.name && it.method.desc == method.desc }
                .forEach { hook ->
                    method.instructions
                        .filterIsInstance<FieldInsnNode>()
                        .filter { it.name == hook.targetField.name && it.desc == hook.targetField.type }
                        .filter { it.opcode in hook.type.allowedOpcodes }
                        .forEach {
                            val hookCode = generateHookCode(hook, it, method)

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

    private fun generateHookCode(hook: FieldRedirectHook, field: FieldInsnNode, method: MethodNode): InsnList {
        val hookId = HookManager.getHookId(hook)

        return InsnList().apply {
            val endLabel = LabelNode()

            isHookRunning(hookId)
            add(JumpInsnNode(Opcodes.IFNE, endLabel))

            setHookRunning(hookId, true)

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
            val capturedDescriptor = getCapturedDescriptor(method.localVariables, hook.arguments)
            add(InvokeDynamicInsnNode(
                "REDIRECT_FIELD",
                "($fieldType$capturedDescriptor)$fieldType",
                hookHandle,
                hookId
            ))
            setHookRunning(hookId, false)

            add(endLabel)
        }
    }
}