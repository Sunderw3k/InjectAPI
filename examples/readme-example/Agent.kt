import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.managers.HookManager

import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.global.Context

import java.lang.instrument.Instrumentation
import org.objectweb.asm.Opcodes
import App

@JvmStatic
fun premain(args: String?, instrumentation: Instrumentation) {
     HookManager.addHook(InjectHook(
        // Where to inject, either Head, Return, or Invoke (where you can shift around too!)
        HeadInjection(),
        // The class to hook into. You can also use Class.forName or ClassLoader#loadClass
        App::class.java,
        // You need quite a bit of JVM knowledge for a few things here
        // You can get the descriptor in intelliJ with View -> Inspect Bytecode on the library class
        TargetMethod("runTick", "(I)V"),
        // The arguments you want to capture. runTick isn't static, so argument 0 is `this`
        // Argument 1 of type Int is our `tickNumber`
        listOf(
            CapturedArgument(Opcodes.ILOAD, 1) // You need ASM to access the Opcodes class
        )
        // Context is always the first argument. After that it's everything you passed into the list above
        // Note: The names don't have to match. Obviously.
    ) { ctx: Context, tickNumber: Int ->
        if (tickNumber % 5 == 0) {
            println("Hello from hook! Current tick is $tickNumber")
        }
    })

    // Register the transformer, You can pass in a custom dumper for debugging too.
    GlobalTransformer().register(instrumentation)
    InjectApi.transform(instrumentation)
}
