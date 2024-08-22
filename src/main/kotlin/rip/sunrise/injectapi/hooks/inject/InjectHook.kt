package rip.sunrise.injectapi.hooks.inject

import org.objectweb.asm.tree.LocalVariableNode
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.utils.extensions.toMethodHandle
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

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
) : Hook(clazz, method, handle.transformInjectHandle()) {
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

/**
 * Transforms a [MethodHandle] for InjectHooks.
 *
 * The [MethodHandle] is expected to take [Context] as the first argument and return [Void].
 * The original descriptor is `(Context, ...)V` and is transformed into `(Map, ...)Map`
 *
 * Equivalent of:
 * ```
 * val ctx = Context.deserialize(map)
 * hook(ctx, arg1, arg2, ...)
 * return ctx.serialize()
 * ```
 */
fun MethodHandle.transformInjectHandle(): MethodHandle {
    val serializeHandle = MethodHandles.lookup().findVirtual(
        Context::class.java,
        "serialize",
        MethodType.methodType(Map::class.java)
    )

    val deserializeHandle = MethodHandles.lookup().findStatic(
        Context::class.java,
        "deserialize",
        MethodType.methodType(Context::class.java, Map::class.java)
    )

    val invokeHandle = this.asType(MethodType.methodType(
        Void.TYPE,
        Context::class.java,
        *this.type().parameterList().drop(1).toTypedArray()
    ))

    return MethodHandles.filterArguments(
        MethodHandles.foldArguments(
            MethodHandles.dropArguments(
                serializeHandle,
                1,
                invokeHandle.type().parameterList().drop(1)
            ), invokeHandle
        ), 0, deserializeHandle
    )
}