import rip.sunrise.injectapi.*
import rip.sunrise.injectapi.hooks.*
import rip.sunrise.injectapi.hooks.inject.*
import rip.sunrise.injectapi.hooks.inject.modes.*
import rip.sunrise.injectapi.global.*
import rip.sunrise.injectapi.access.*

import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.managers.HookManager

import org.objectweb.asm.Opcodes

import java.lang.instrument.Instrumentation
import App

fun premain(args: String?, instrumentation: Instrumentation) {
    InjectApi.transform(instrumentation)

    if (add(App(), 2, 3) == 5) {
        System.exit(0)
    }
    System.exit(1)
}

fun add(instance: App, a: Int, b: Int): Int {
    return AccessWidener.accessMethod<App, Int>("add", Int::class.java, Int::class.java)(instance, a, b)
}
