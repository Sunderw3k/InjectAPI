package rip.sunrise.injectapi.injection

/**
 *  An argument to capture. The variable is loaded using [opcode] where [index] is the variable offset.
 */
data class CapturedArgument(val opcode: Int, val index: Int)