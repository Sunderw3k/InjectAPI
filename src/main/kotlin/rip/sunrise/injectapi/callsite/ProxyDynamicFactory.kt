package rip.sunrise.injectapi.callsite

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
            val handle = hook::class.java.getDeclaredMethod("getHandle").invoke(hook) as MethodHandle

            // TODO: Kinda forced, it won't fail. Only happens because types are lost when calling unreflect.
            return ConstantCallSite(handle.asType(type))
        } catch (e: Exception) {
            e.printStackTrace()
            error(e)
        }
    }
}