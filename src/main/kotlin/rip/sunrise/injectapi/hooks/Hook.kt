package rip.sunrise.injectapi.hooks

import java.lang.invoke.MethodHandle
import java.util.*

abstract class Hook(
    val className: String,
    val classLoader: Optional<ClassLoader>,
    val method: TargetMethod,
    val handle: MethodHandle
)