package rip.sunrise.injectapi

import org.objectweb.asm.Type
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.global.ProxyDynamicFactory
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.Native
import java.lang.instrument.Instrumentation

private external fun nativeDefineClass(loader: ClassLoader, classBytes: ByteArray)

/**
 * The main class containing all the initialization logic.
 *
 * IMPORTANT: Functioning of the hooks depends on where this class is loaded. Make sure this is the same ClassLoader that loads HookManager.
 */
@Suppress("unused")
object InjectApi {
    // TODO: This should be some kind of AP for compile-time inlining.
    // I know const is inlining it, but I'm not sure if it works on java. Its better for me to write it myself.
    const val CONTEXT_CLASS = "rip/sunrise/injectapi/global/Context"
    const val BOOTSTRAP_CLASS = "rip/sunrise/injectapi/global/ProxyDynamicFactory"
    const val DATA_TRANSPORT_CLASS = "rip/sunrise/injectapi/global/DataTransport"

    init {
        Native.loadNatives()
    }

    /**
     * Call this when you're ready to apply the hooks.
     *
     * IMPORTANT: Call from the same ClassLoader which loaded HookManager, or any CL that resolves both of classes to the same one.
     */
    fun transform(inst: Instrumentation) {
        // TODO: This is pretty sketchy. nativeDefineClass kills the jvm. It should throw a java error instead.
        HookManager.getTargetClasses().map { it.classLoader }.distinct().forEach {
            // In case the classes are defined already, we don't want to load them again.
            if (runCatching { it.loadClass(Context::class.java.name) }.isFailure) {
                // Load Context and ProxyDynamicFactory in the hooked classloaders
                nativeDefineClass(it, getClassBytes(Type.getInternalName(Context::class.java)))
                nativeDefineClass(it, getClassBytes(Type.getInternalName(ProxyDynamicFactory::class.java)))
            }

            // Set the classloader
            it.loadClass(ProxyDynamicFactory::class.java.name)
                .getDeclaredField("classLoader")
                .set(null, InjectApi::class.java.classLoader)
        }

        // Retransform
        inst.retransformClasses(*HookManager.getTargetClasses().toTypedArray())
    }

    private fun getClassBytes(name: String): ByteArray {
        return InjectApi::class.java.classLoader.getResourceAsStream("$name.class")?.readAllBytes()
            ?: error("Couldn't find bytes for $name")
    }
}