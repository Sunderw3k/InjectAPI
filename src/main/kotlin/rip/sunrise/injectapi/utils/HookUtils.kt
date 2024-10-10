package rip.sunrise.injectapi.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

fun InsnList.isHookRunning(hookId: Int) {
    add(FieldInsnNode(Opcodes.GETSTATIC, "@BOOTSTRAP@", "runningHooks", "Ljava/util/BitSet;"))
    add(LdcInsnNode(hookId))
    add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "get", "(I)Z", false))
}

fun InsnList.setHookRunning(hookId: Int, state: Boolean) {
    add(FieldInsnNode(Opcodes.GETSTATIC, "@BOOTSTRAP@", "runningHooks", "Ljava/util/BitSet;"))
    add(LdcInsnNode(hookId))

    if (state) {
        add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "set", "(I)V", false))
    } else {
        add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "clear", "(I)V", false))
    }
}