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
    private val findBootstrapClassOrNull =
        ClassLoader::class.java.getDeclaredMethod("findBootstrapClassOrNull", String::class.java).also {
            it.setAccessibleUnsafe(true)
        }

    /**
     * Call this when you're ready to apply the hooks.
     *
     * IMPORTANT: Call from the same ClassLoader which loaded HookManager, or any CL that resolves both of classes to the same one.
     */
    fun transform(inst: Instrumentation) {
        AccessWidener.initialize(inst)
        ClassLoader::class.java.module.addOpens("java.lang", InjectApi::class.java.module)

        // Get loaded classes by checking all loaded classes for matching class names and class loaders
        val loaded = HookManager.getHookMap().values.map { hook -> inst.allLoadedClasses.filter {
            it.name == hook.className && (hook.classLoader.isEmpty || hook.classLoader.get() == it.classLoader)
        } }

        inst.retransformClasses(*loaded.flatten().toTypedArray())
    }

    /**
     * Used to inject Context and ProxyDynamicFactory to a given [ClassLoader] and set them up.
     * The code checks whether the class is defined before defining it again.
     */
    fun setupClassLoader(classLoader: ClassLoader?) {
        if (!isClassDefined(classLoader, Context::class.java.name)) {
            // Load Context and ProxyDynamicFactory in the hooked classloaders
            defineClass(getClassBytes(Type.getInternalName(Context::class.java)), classLoader, null)
            defineClass(getClassBytes(Type.getInternalName(ProxyDynamicFactory::class.java)), classLoader, null)
        }

        // Set the hook ClassLoader reference to our ClassLoader
        if (classLoader == null) {
            findBootstrapClassOrNull.invoke(null, ProxyDynamicFactory::class.java.name) as Class<*>
        } else {
            classLoader.loadClass(ProxyDynamicFactory::class.java.name)
        }.getDeclaredField("classLoader").set(null, InjectApi::class.java.classLoader)
    }

    /**
     * Check whether a class is defined on a classloader (or the parent).
     * Works for bootstrap CL.
     */
    private fun isClassDefined(classLoader: ClassLoader?, name: String): Boolean {
        try {
            val clazz = classLoader?.loadClass(name) ?: findBootstrapClassOrNull(null, name)
            return clazz != null
        } catch (_: ClassNotFoundException) {
            return false
        }
    }

    private fun getClassBytes(name: String): ByteArray {
        return InjectApi::class.java.classLoader.getResourceAsStream("$name.class")?.readAllBytes()
            ?: error("Couldn't find bytes for $name")
    }
}