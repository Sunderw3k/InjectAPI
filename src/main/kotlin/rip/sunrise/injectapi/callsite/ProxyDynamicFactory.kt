package rip.sunrise.injectapi.callsite

import rip.sunrise.injectapi.hooks.inject.Context
import java.lang.invoke.*

/**
 * Class containing a bootstrap method for all the hooks.
 *
 * NOTE: Loaded on ALL hooked ClassLoaders.
 */
object ProxyDynamicFactory {
    /**
     * This method is called as a bootstrap for invokedynamic in hooked code. The returned [CallSite] points to the hooks [MethodHandle].
     */
    @JvmStatic
    fun bootstrap(caller: MethodHandles.Lookup, name: String, type: MethodType, hookId: Int): CallSite {
        try {
            println("Got invokedynamic call from $caller for hookId $hookId")

            val cl = ClassLoader.getSystemClassLoader()
                .loadClass("rip.sunrise.injectapi.callsite.DataTransport")
                .getDeclaredMethod("getClassLoader")
                .invoke(null) as ClassLoader

            val clazz = cl.loadClass("rip.sunrise.injectapi.managers.HookManager")

            // Note: DO NOT CAST. This is most likely not the correct classloader. current != cl
            val instance = clazz.getDeclaredField("INSTANCE").get(null)

            val hook = clazz.getDeclaredMethod("getHook", Int::class.java).invoke(instance, hookId)
            val handle = (hook::class.java.getDeclaredMethod("getHandle").invoke(hook) as MethodHandle).let {
                return@let when (name) {
                    "INJECT" -> {
                        transformInjectHandle(it)
                    }
                    else -> error("Unknown hook name `$name`")
                }
            }

            // TODO: Kinda forced, it won't fail. Only happens because types are lost when calling unreflect.
            return ConstantCallSite(handle.asType(type))
        } catch (e: Exception) {
            e.printStackTrace()
            error(e)
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
    private fun transformInjectHandle(handle: MethodHandle): MethodHandle {
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

        val invokeHandle = handle.let {
            // Set return type to Void and first parameter to Context
            it.asType(MethodType.methodType(
                Void.TYPE,
                Context::class.java,
                *it.type().parameterList().drop(1).toTypedArray()
            ))
        }

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
}