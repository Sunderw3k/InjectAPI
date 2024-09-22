import rip.sunrise.injectapi.*
import rip.sunrise.injectapi.hooks.*
import rip.sunrise.injectapi.hooks.inject.*
import rip.sunrise.injectapi.hooks.inject.modes.*
import rip.sunrise.injectapi.global.*

import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.managers.HookManager

import org.objectweb.asm.Opcodes

import java.lang.instrument.Instrumentation
import App

fun premain(args: String?, instrumentation: Instrumentation) {
    HookManager.addHook(InjectHook(
        HeadInjection(),
        App::class.java,
        TargetMethod("main", "([Ljava/lang/String;)V"),
        emptyList(),
    ) { _: Context ->
        System.exit(0)
    })

    GlobalTransformer().register(instrumentation)
    InjectApi.transform(instrumentation)
}
