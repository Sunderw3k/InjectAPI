import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.managers.HookManager

import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.inject.Context
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.CapturedArgument

import rip.sunrise.injectapi.hooks.redirect.field.FieldRedirectHook
import rip.sunrise.injectapi.hooks.TargetField

import java.lang.instrument.Instrumentation
import org.objectweb.asm.Opcodes
import App

@JvmStatic
fun premain(args: String?, instrumentation: Instrumentation) {
    HookManager.addHook(FieldRedirectHook(
        FieldRedirectHook.Type.SET,
        App::class.java,
        TargetMethod("main", "([Ljava/lang/String;)V"),
        TargetField("num", "App", "I"),
        emptyList()
    ) { value: Int ->
        return@FieldRedirectHook if (value % 5 == 0) {
            println("Current value is $value. Adding 2 for fun")
            value + 2
        } else value
    })

    // Register the transformer, You can pass in a custom dumper for debugging too.
    GlobalTransformer().register(instrumentation)
    InjectApi.transform(instrumentation)
}