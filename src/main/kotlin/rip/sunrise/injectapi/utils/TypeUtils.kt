package rip.sunrise.injectapi.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import rip.sunrise.injectapi.injection.CapturedArgument

/**
 * Returns a [InsnList] containing the required opcodes to return a [type] while unwrapping from [Object]
 */
fun getCheckCastReturnBytecode(type: Type): InsnList {
    return InsnList().apply {
        when (type.sort) {
            Type.VOID -> {}
            Type.BOOLEAN -> {
                add(TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"))
                add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false))
            }
            Type.CHAR -> {
                add(TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"))
                add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false))
            }
            Type.BYTE, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE -> {
                val methodName = "${type.className}Value" // eg. intValue, doubleValue
                val descriptor = "()${type.descriptor}"   // eg. ()I, ()D

                add(TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"))
                add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", methodName, descriptor, false))
            }
            Type.ARRAY, Type.OBJECT -> {
                add(TypeInsnNode(Opcodes.CHECKCAST, type.internalName))
            }
            Type.METHOD -> {
                error("Can't return a method from a method.")
            }
        }

        add(InsnNode(type.getOpcode(Opcodes.IRETURN)))
    }
}

/**
 * Returns the descriptor of the parameters from known method [variables] and requested [capturedArguments].
 */
fun getCapturedDescriptor(variables: List<LocalVariableNode>, capturedArguments: List<CapturedArgument>): String {
    return capturedArguments.joinToString("") { variables.first { variable -> variable.index == it.index }.desc }
}