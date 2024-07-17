package rip.sunrise.injectapi.hooks.inject

import org.objectweb.asm.tree.LocalVariableNode
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.utils.extensions.toMethodHandle
import java.lang.invoke.MethodHandle

/**
 * A hook made for injecting extra calls into methods.
 *
 * [handle] has to match the descriptor of passed arguments alongside always taking [Context] as the first argument.
 */
class InjectHook(
    val injectionMode: InjectionMode,
    clazz: Class<*>,
    method: TargetMethod,
    val arguments: List<CapturedArgument>,
    handle: MethodHandle
) : Hook(clazz, method, handle) {
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
        // TODO: Breaks when LocalVariableTable is not there.

        // TODO: Using reflection loses the types. Check out FunctionExtension
        // The best I can do right now is check bounds and opcodes. But opcodes are annoying.

        val variableIndices = variables.map { it.index }
        return arguments.all { it.index in variableIndices }
    }
}