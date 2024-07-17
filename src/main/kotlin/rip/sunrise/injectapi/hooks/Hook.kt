package rip.sunrise.injectapi.hooks

import java.lang.invoke.MethodHandle

abstract class Hook(
    val clazz: Class<*>,
    val method: TargetMethod,
    val handle: MethodHandle
)