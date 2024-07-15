package rip.sunrise.injectapi.callsite

/**
 * Because [ProxyDynamicFactory.bootstrap] needs a [ClassLoader] to use when loading HookManager, we need to somehow specify it.
 *
 * This class is loaded in the System CL and is accessed so that we can properly load HookManager.
 */
@Suppress("unused")
object DataTransport {
    @JvmStatic
    lateinit var classLoader: ClassLoader
}