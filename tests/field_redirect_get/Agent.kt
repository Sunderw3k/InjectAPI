import rip.sunrise.injectapi.*
import rip.sunrise.injectapi.hooks.*
import rip.sunrise.injectapi.hooks.redirect.field.*

import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.managers.HookManager

import org.objectweb.asm.Opcodes

import java.lang.instrument.Instrumentation
import App

fun premain(args: String?, instrumentation: Instrumentation) {
    HookManager.addHook(FieldRedirectHook(
        FieldRedirectHook.Type.GET,
        App::class.java,
        TargetMethod("main", "([Ljava/lang/String;)V"),
        TargetField("returnCode", "App", "I"),
        emptyList()
    ) { value: Int ->
        return@FieldRedirectHook 0
    })

    GlobalTransformer().register(instrumentation)
    InjectApi.transform(instrumentation)
}
