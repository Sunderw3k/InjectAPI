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

    val serializeHandle = MethodHandles.lookup().findVirtual(Context::class.java, "serialize", MethodType.methodType(Map::class.java))
    val deserializeHandle = MethodHandles.lookup().findStatic(
        Context::class.java, "deserialize", MethodType.methodType(
            Context::class.java, Map::class.java))
    val invokeHandle = MethodHandles.lookup().unreflect(invoke).bindTo(this).let {
        // Set return type to Void and first parameter to Context
        it.asType(MethodType.methodType(Void.TYPE, Context::class.java, *it.type().parameterList().drop(1).toTypedArray()))
    }

    /*
    Takes in (Map, arg1, arg2) and returns Map
    Transformed from (Context, arg1, arg2) -> Void

    Equivalent of:
    val ctx = deserializeHandle(map)
    invokeHandle(ctx, arg1, arg2)
    return serializeHandle(ctx)
     */
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