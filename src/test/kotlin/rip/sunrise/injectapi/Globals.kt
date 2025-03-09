package rip.sunrise.injectapi

import net.bytebuddy.agent.ByteBuddyAgent
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.managers.HookManager
import java.io.File
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader

val instrumentation: Instrumentation = ByteBuddyAgent.install()
val classLoader: URLClassLoader = URLClassLoader.newInstance(
    arrayOf(File("build/libs/test-targets.jar").toURI().toURL())
)

fun testHookedMethodInvocation(
    className: String,
    methodName: String,
    vararg args: Any,
    getHooks: (Class<*>, Method) -> Array<Hook>
) {
    val clazz = classLoader.loadClass(className)
    val method = clazz.declaredMethods.first { it.name == methodName }

    val hooks = getHooks(clazz, method)
    hooks.forEach {
        HookManager.addHook(it)
    }
    InjectApi.transform(instrumentation)

    // TODO: assertDoesNotThrow is cleaner, but this unwraps the message
    val result = runCatching {
        val isVirtual = method.modifiers and Modifier.STATIC == 0
        if (isVirtual) {
            val instance = clazz.getDeclaredConstructor().newInstance()
            method.invoke(instance, *args)
        } else {
            method.invoke(null, *args)
        }
    }

    assert(result.isSuccess) {
        val cause = result.exceptionOrNull()!!.cause!!
        cause.message!!
    }
}
