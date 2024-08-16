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

    private var initialized = false

    /**
     * Call this when you're ready to apply the hooks.
     *
     * IMPORTANT: Call from the same ClassLoader which loaded HookManager, or any CL that resolves both of classes to the same one.
     */
    fun transform(inst: Instrumentation) {
        // Set up DataTransport
        if (!initialized) {
            setupDataTransport()
            initialized = true
        }

        // Load ProxyDynamicFactory and Context in all CLs
        HookManager.getTargetClasses().map { it.classLoader }.filter { it != InjectApi::class.java.classLoader }.distinct().forEach {
            nativeDefineClass(it, getClassBytes(Type.getInternalName(ProxyDynamicFactory::class.java)))

            nativeDefineClass(it, getClassBytes(Type.getInternalName(Context::class.java)))
        }

        // Retransform
        inst.retransformClasses(*HookManager.getTargetClasses().toTypedArray())
    }

    private fun setupDataTransport() {
        // Load DataTransport into System CL
        // Note: Don't use ::class.java because it loads. This should be exclusively on the System CL
        nativeDefineClass(ClassLoader.getSystemClassLoader(), getClassBytes(DATA_TRANSPORT_CLASS))

        // Set up the injection CL.
        ClassLoader.getSystemClassLoader()
            .loadClass(DATA_TRANSPORT_CLASS.replace("/", "."))
            .getDeclaredField("classLoader")
            .set(null, InjectApi::class.java.classLoader)
    }

    private fun getClassBytes(name: String): ByteArray {
        return InjectApi::class.java.classLoader.getResourceAsStream("$name.class")?.readAllBytes()
            ?: error("Couldn't find bytes for $name")
    }
}