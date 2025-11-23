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

private fun getTypeFromLoadOpcode(opcode: Int): Type {
    return when (opcode) {
        Opcodes.ILOAD -> Type.INT_TYPE
        Opcodes.LLOAD -> Type.LONG_TYPE
        Opcodes.FLOAD -> Type.FLOAT_TYPE
        Opcodes.DLOAD -> Type.DOUBLE_TYPE
        Opcodes.ALOAD -> Type.getType("Ljava/lang/Object;")
        else -> error("Unknown load opcode $opcode")
    }
}

/**
 * Returns the first local index that isn't a method param (a pure local).
 */
fun getFirstPureLocalIndex(argumentTypes: Array<Type>, isVirtual: Boolean): Int {
    return argumentTypes.fold(0) { acc, type ->
        acc + type.size
    } + if (isVirtual) 1 else 0
}

/**
 * Resolves a `method index` from the [localsIndex].
 *
 * The [localsIndex] accounts for sizes, whereas the `method index` doesn't.
 *
 * @throws IllegalStateException If the [localsIndex] doesn't map to any `method index`.
 */
fun getMethodParamIndexFromLocalsIndex(argumentTypes: Array<Type>, localsIndex: Int, isVirtual: Boolean): Int {
    check(getFirstPureLocalIndex(argumentTypes, isVirtual) > localsIndex)

    val methodIndices = argumentTypes.runningFold(0) { acc, type ->
        acc + type.size
    }.dropLast(1)

    val index = if (isVirtual) {
        methodIndices.indexOf(localsIndex - 1) // virtual 1 is arguments[0]
    } else methodIndices.indexOf(localsIndex) // static 1 is arguments[1]

    check(index != -1) { "No known method index for locals index $localsIndex" }
    return index
}

/**
 * Resolves and verifies opcodes from [capturedArguments], returning a list of [CapturedArgument] with no -1 entries.
 *
 * @throws IllegalStateException If any, non-autodetected opcode is incorrect, or any autodetection fails.
 */
fun resolveOpcodes(capturedArguments: List<CapturedArgument>, method: MethodNode): List<CapturedArgument> {
    val argumentTypes = Type.getArgumentTypes(method.desc)
    val isVirtual = method.access and Opcodes.ACC_STATIC == 0

    val firstPureLocal = getFirstPureLocalIndex(argumentTypes, isVirtual)

    return capturedArguments.map { captured ->
        if (captured.index == 0 && isVirtual) {
            check(captured.opcode == Opcodes.ALOAD || captured.opcode == -1) {
                "Bad opcode for `this`. Expected: ${Opcodes.ALOAD} Actual: ${captured.opcode}"
            }
            return@map CapturedArgument(Opcodes.ALOAD, 0)
        }

        if (captured.index >= firstPureLocal) {
            // It's a pure local, can't verify or autodetect.
            check(captured.opcode != -1) { "Can't autodetect pure local ${captured.index}. Please specify the opcode explicitly." }
            return@map captured
        }

        val index = getMethodParamIndexFromLocalsIndex(argumentTypes, captured.index, isVirtual)
        val type = argumentTypes[index]

        val opcode = if (captured.opcode == -1) {
            // Autodetect
            type.getOpcode(Opcodes.ILOAD)
        } else {
            // Verify
            val expectedLoadOpcode = type.getOpcode(Opcodes.ILOAD)
            check(expectedLoadOpcode == captured.opcode) {
                "Bad opcode for local $index (locals: ${captured.index}). Expected: $expectedLoadOpcode Actual: ${captured.opcode}"
            }

            captured.opcode
        }
        return@map CapturedArgument(opcode, captured.index)
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
    val argumentTypes = Type.getArgumentTypes(method.desc)
    val isVirtual = method.access and Opcodes.ACC_STATIC == 0

    val firstPureLocal = getFirstPureLocalIndex(argumentTypes, isVirtual)

    return capturedArguments.joinToString("") {
        if (isVirtual && it.index == 0) {
            return@joinToString "L$classType;" // this
        }

        if (it.index >= firstPureLocal) {
            // TODO: We can't really verify this unless the bytecode is valid.
            //  We can technically use StackMap frames to look at the last xSTORE for this index.
            //  Also returning java/lang/Object seems a little cursed?

            check(it.opcode != -1)
            return@joinToString getTypeFromLoadOpcode(it.opcode).descriptor
        }

        val index = getMethodParamIndexFromLocalsIndex(argumentTypes, it.index, isVirtual)
        return@joinToString argumentTypes[index].descriptor
    }
}