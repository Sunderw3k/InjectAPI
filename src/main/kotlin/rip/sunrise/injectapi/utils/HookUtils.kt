package rip.sunrise.injectapi.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

fun InsnList.getLocalRunningHookArray() {
    add(FieldInsnNode(Opcodes.GETSTATIC, "@BOOTSTRAP@", "runningHooks", "Ljava/lang/ThreadLocal;"))
    add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;"))
    add(TypeInsnNode(Opcodes.CHECKCAST, "[Z"))
}

fun InsnList.isHookRunning(hookId: Int) {
    add(LdcInsnNode(hookId))
    add(InsnNode(Opcodes.BALOAD))
}

fun InsnList.setHookRunning(hookId: Int, state: Boolean) {
    add(LdcInsnNode(hookId))
    add(InsnNode(if (state) Opcodes.ICONST_1 else Opcodes.ICONST_0))
    add(InsnNode(Opcodes.BASTORE))
}