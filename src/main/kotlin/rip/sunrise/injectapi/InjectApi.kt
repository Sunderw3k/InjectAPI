package rip.sunrise.injectapi

import org.objectweb.asm.Type
import rip.sunrise.injectapi.access.AccessWidener
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.global.ProxyDynamicFactory
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.defineClass
import rip.sunrise.injectapi.utils.setAccessibleUnsafe
import java.lang.instrument.Instrumentation

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
                defineClass(getClassBytes(Type.getInternalName(Context::class.java)), it, null)
                defineClass(getClassBytes(Type.getInternalName(ProxyDynamicFactory::class.java)), it, null)
            }

            val dynamicFactory = if (it == null) {
                findBootstrapClassOrNull.invoke(null, ProxyDynamicFactory::class.java.name) as Class<*>
            } else {
                it.loadClass(ProxyDynamicFactory::class.java.name)
            }

            // Set the InjectAPI ClassLoader
            dynamicFactory.getDeclaredField("classLoader").set(null, InjectApi::class.java.classLoader)

            // Resize the running hooks array
            // TODO: This doesn't copy the previous values, hooks may go into single depth recursion.
            dynamicFactory.getDeclaredField("runningHooks")
                .set(null, ThreadLocal.withInitial { BooleanArray(HookManager.getHookMap().size) })
        }

        // Retransform
        inst.retransformClasses(*HookManager.getTargetClasses().toTypedArray())
    }

    private val findBootstrapClassOrNull = ClassLoader::class.java.getDeclaredMethod("findBootstrapClassOrNull", String::class.java).also {
        it.setAccessibleUnsafe(true)
    }

    private fun getClassBytes(name: String): ByteArray {
        return InjectApi::class.java.classLoader.getResourceAsStream("$name.class")?.readAllBytes()
            ?: error("Couldn't find bytes for $name")
    }
}