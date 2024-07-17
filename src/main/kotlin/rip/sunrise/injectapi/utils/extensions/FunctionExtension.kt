package rip.sunrise.injectapi.utils.extensions

import rip.sunrise.injectapi.hooks.inject.Context
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * Transforms a [Function] into a [MethodHandle].
 *
 * Assumes there's a method called `invoke`.
 */
fun Function<*>.toMethodHandle(): MethodHandle {
    // TODO: Less assumptions, prefer also getting the types.
    val invoke = this::class.java.declaredMethods.first { it.name == "invoke" }.also {
        it.isAccessible = true
    }

    val invokeHandle = MethodHandles.lookup().unreflect(invoke).bindTo(this)

    return invokeHandle
}