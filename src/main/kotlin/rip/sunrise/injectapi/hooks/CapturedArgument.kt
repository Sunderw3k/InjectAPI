package rip.sunrise.injectapi.hooks

/**
 *  An argument to capture. The variable is loaded using [opcode] where [index] is the variable offset.
 *
 *  If [opcode] is -1, it will autodetect the correct one. Note that this only works on method parameters, not locals.
 */
data class CapturedArgument(val opcode: Int = -1, val index: Int)