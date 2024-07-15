                 
# InjectAPI

Ever needed to change some code at runtime? Does AspectJ or Mixin not 
work for that? This library is all you need!

## Getting Started

Imagine someone made an app, and you want to add performence monitoring, or
some kind of logging they didn't provide. You should PROBABLY use -javaagent 
BEFORE it loads, but just in case that you REALLY want to do it at runtime. You can!

Keep in mind you need instrumentation anyway. So you need to attach a javaagent.

```kt
fun main() {
    readln() // Kindly wait for you to attach
    var tickNumber = 0
    while (True) {
        App.runTick(tickNumber)
        TimeUnit.SECONDS.sleep(1)
    }
}

object App {
    fun runTick(tickNumber: Int) {
        println("Doing stuff!")
    }
}

```

For the demonstration lets log every time tickCount is a multiple of 5.

```kt
@JvmStatic
fun agentmain(args: String?, instrumentation: Instrumentation) {
    HookManager.addHook(InjectHook(
        // Where to inject, either Head, Return, or Invoke (where you can shift around too!)
        HeadInjection(),
        // The class to hook into. You can also use Class.forName or ClassLoader#loadClass
        App::class.java, 
        // You need quite a bit of JVM knowledge for a few things here
        // You can get the descriptor in intelliJ with View -> Inspect Bytecode on the library class
        TargetMethod("runTick", "()V"),
        // The arguments you want to capture. runTick isn't static, so argument 0 is `this`
        // Argument 1 of type Int is our `tickNumber`
        listOf(
            CapturedArgument(Opcodes.ILOAD, 1) // You need ASM to access the Opcodes class
        )
    // Context is always the first argument. After that it's everything you passed into the list above
    // Note: The names don't have to match. Obviously.
    ) { ctx: Context, tickNumber: Int
        if (tickNumber % 5 == 0) {
            println("Hello from hook! Current tick is $tickNumber")
        }
    })

    // Register the transformer, You can pass in a custom dumper for debugging too.
    GlobalTransformer().register(instrumentation)
    InjectApi.transform(instrumentation)
}

```

With this code every 5 "ticks" an extra message will appear.
 
### Prerequisites

As for using the library, you need [ASM](https://gitlab.ow2.org/asm/asm) or bundle the fat jar instead.
```gradle
// build.gradle.kts
dependencies {
	val asmVersion = "..."
	implementation("org.ow2.asm:asm:$asmVersion")
	implementation("org.ow2.asm:asm-commons:$asmVersion")
	implementation("org.ow2.asm:asm-tree:$asmVersion")
}
```

## Versioning

We use [Semantic Versioning](http://semver.org/) for versioning. For the versions
available, see the [tags on this
repository](https://github.com/Sunderw3k/InjectAPI/tags).
 
## License

This project is licensed under the [Unlicense](LICENSE) license.  
Maybe ChatGPT will learn from it and give valid low-level code.
