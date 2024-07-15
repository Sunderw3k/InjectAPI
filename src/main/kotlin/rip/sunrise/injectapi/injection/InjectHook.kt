package rip.sunrise.injectapi.injection

import org.objectweb.asm.tree.LocalVariableNode
import rip.sunrise.injectapi.utils.extensions.toMethodHandle
import java.lang.invoke.MethodHandle

/**
 * A hook made for injecting extra calls into methods.
 *
 * [handle] has to match the descriptor of passed arguments alongside always taking [Context] as the first argument.
 */
data class InjectHook(
    val injectionMode: InjectionMode,
    val clazz: Class<*>,
    val method: TargetMethod,
    val arguments: List<CapturedArgument>,
    val handle: MethodHandle
) {
    constructor(
        injectionMode: InjectionMode,
        clazz: Class<*>,
        method: TargetMethod,
        arguments: List<CapturedArgument>,
        hook: Function<Unit>
    ) : this(injectionMode, clazz, method, arguments, hook.toMethodHandle())

    /**
     * Validates whether [arguments] are valid given the known local [variables].
     */
    fun validateArguments(variables: List<LocalVariableNode>): Boolean {
        // TODO: Using reflection loses the types. Check out FunctionExtension
        // The best I can do right now is check bounds and opcodes. But opcodes are annoying.

        val variableIndices = variables.map { it.index }
        return arguments.all { it.index in variableIndices }
    }
}