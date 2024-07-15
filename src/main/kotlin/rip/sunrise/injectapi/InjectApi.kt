package rip.sunrise.injectapi

import org.objectweb.asm.Type
import rip.sunrise.injectapi.callsite.DataTransport
import rip.sunrise.injectapi.callsite.ProxyDynamicFactory
import rip.sunrise.injectapi.injection.Context
import rip.sunrise.injectapi.managers.HookManager
import java.lang.instrument.Instrumentation
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private external fun nativeDefineClass(loader: ClassLoader, classBytes: ByteArray)

/**
 * The main class containing all the initialization logic.
 *
 * IMPORTANT: Functioning of the hooks depends on where this class is loaded. Make sure this is the same ClassLoader that loads HookManager.
 */
object InjectApi {
    init {
        // TODO: Windows/Mac support.
        val stream = InjectApi::class.java.getResourceAsStream("/ForceClassLoaderDefine.so") ?: error("Couldn't find native")
        Files.createTempFile("forcecldefine", null).also {
            it.toFile().deleteOnExit()

            Files.copy(stream, it, StandardCopyOption.REPLACE_EXISTING)
            System.load(it.toFile().path)
        }
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
            nativeDefineClass(it, getClassBytes(Type.getInternalName(Context.Companion::class.java)))
        }

        // Retransform
        inst.retransformClasses(*HookManager.getTargetClasses().toTypedArray())
    }

    private fun setupDataTransport() {
        // Load DataTransport into System CL
        // Note: Don't use ::class.java because it loads. This should be exclusively on the System CL
        nativeDefineClass(ClassLoader.getSystemClassLoader(), getClassBytes("rip/sunrise/injectapi/callsite/DataTransport"))

        // Set up the injection CL.
        ClassLoader.getSystemClassLoader()
            .loadClass("rip.sunrise.injectapi.callsite.DataTransport")
            .getDeclaredMethod("setClassLoader", ClassLoader::class.java)
            .invoke(null, InjectApi::class.java.classLoader)
    }

    private fun getClassBytes(name: String): ByteArray {
        return InjectApi::class.java.classLoader.getResourceAsStream("$name.class")?.readAllBytes()
            ?: error("Couldn't find bytes for $name")
    }
}