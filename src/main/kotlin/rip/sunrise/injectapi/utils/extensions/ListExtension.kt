package rip.sunrise.injectapi.utils.extensions

import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.VarInsnNode
import rip.sunrise.injectapi.hooks.CapturedArgument

fun List<CapturedArgument>.getLoadBytecodes(): InsnList = InsnList().also { list ->
    forEach {
        list.add(VarInsnNode(it.opcode, it.index))
    }
}