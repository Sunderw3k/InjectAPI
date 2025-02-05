package rip.sunrise.injectapi.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.hooks.CapturedArgument

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
 * Returns the descriptor of the parameters from requested [capturedArguments].
 */
fun getCapturedDescriptor(
    capturedArguments: List<CapturedArgument>,
    method: MethodNode,
    classType: String
): String {
    // TODO: Some sanity checking if the args are valid

    val arguments = Type.getArgumentTypes(method.desc)
    val isVirtual = method.access and Opcodes.ACC_STATIC == 0

    return capturedArguments.joinToString("") {
        if (isVirtual && it.index == 0) {
            "L$classType;" // this
        } else if (isVirtual) {
            arguments[it.index - 1].descriptor // virtual 1 is arguments[0]
        } else arguments[it.index].descriptor // static 1 is arguments[1]
    }
}