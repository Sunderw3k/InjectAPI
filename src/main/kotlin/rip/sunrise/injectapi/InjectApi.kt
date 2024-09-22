package rip.sunrise.injectapi

import org.objectweb.asm.Type
import rip.sunrise.injectapi.access.AccessWidener
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.global.ProxyDynamicFactory
import rip.sunrise.injectapi.managers.HookManager
import java.lang.instrument.Instrumentation
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/**
 * The main class containing all the initialization logic.
 *
 * IMPORTANT: Functioning of the hooks depends on where this class is loaded. Make sure this is the same ClassLoader that loads HookManager.
 */
@Suppress("unused")
object InjectApi {
    /**
     * Call this when you're ready to apply the hooks.
     *
     * IMPORTANT: Call from the same ClassLoader which loaded HookManager, or any CL that resolves both of classes to the same one.
     */
    fun transform(inst: Instrumentation) {
        AccessWidener.initialize(inst)
        ClassLoader::class.java.module.addOpens("java.lang", InjectApi::class.java.module)

        HookManager.getTargetClasses().map { it.classLoader }.distinct().forEach {
            // In case the classes are defined already, we don't want to load them again.
            if (runCatching { it.loadClass(Context::class.java.name) }.isFailure) {
                // Load Context and ProxyDynamicFactory in the hooked classloaders
                defineClass(it, getClassBytes(Type.getInternalName(Context::class.java)))
                defineClass(it, getClassBytes(Type.getInternalName(ProxyDynamicFactory::class.java)))
            }

            it.loadClass(ProxyDynamicFactory::class.java.name)
                .getDeclaredField("classLoader")
                .set(null, InjectApi::class.java.classLoader)
        }

        // Retransform
        inst.retransformClasses(*HookManager.getTargetClasses().toTypedArray())
    }

    // NOTE: Needs to be lazy to not crash before transforming
    private val defineClass: MethodHandle by lazy {
        MethodHandles.lookup()
            .unreflect(
                ClassLoader::class.java.getDeclaredMethod(
                    "defineClass",
                    String::class.java,
                    ByteArray::class.java,
                    Int::class.java,
                    Int::class.java
                ).also { it.isAccessible = true }
            )
    }
    private fun defineClass(classLoader: ClassLoader, bytes: ByteArray) {
        defineClass.invoke(classLoader, null, bytes, 0, bytes.size)
    }

    private fun getClassBytes(name: String): ByteArray {
        return InjectApi::class.java.classLoader.getResourceAsStream("$name.class")?.readAllBytes()
            ?: error("Couldn't find bytes for $name")
    }
}