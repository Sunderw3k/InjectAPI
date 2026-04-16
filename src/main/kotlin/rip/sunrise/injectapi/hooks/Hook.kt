package rip.sunrise.injectapi.hooks

import rip.sunrise.injectapi.annotations.ReferencedByName
import java.lang.invoke.MethodHandle

abstract class Hook(
    val clazz: Class<*>,
    val method: TargetMethod,
    @get:ReferencedByName(reason = "Called from ProxyDynamicFactory")
    val handle: MethodHandle
)