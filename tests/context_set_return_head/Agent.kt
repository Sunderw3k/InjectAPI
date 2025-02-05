import rip.sunrise.injectapi.*
import rip.sunrise.injectapi.hooks.*
import rip.sunrise.injectapi.hooks.inject.*
import rip.sunrise.injectapi.hooks.inject.modes.*
import rip.sunrise.injectapi.global.*

import rip.sunrise.injectapi.debug.*;

import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.managers.HookManager

import org.objectweb.asm.Opcodes

import java.lang.instrument.Instrumentation
import App

fun premain(args: String?, instrumentation: Instrumentation) {
    HookManager.addHook(InjectHook(
        HeadInjection(),
        App::class.java,
        TargetMethod("getReturnCode", "()I"),
        emptyList(),
    ) { ctx: Context ->
        ctx.setReturnValue(0)
    })

    GlobalTransformer(FileSystemDumper(java.nio.file.Path.of("/home/sun3k/test"))).register(instrumentation)
    InjectApi.transform(instrumentation)
}
