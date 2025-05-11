package rip.sunrise.injectapi.hooks.redirect.method

import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.utils.extensions.toMethodHandle
import java.lang.invoke.MethodHandle

class MethodRedirectHook(
    clazz: Class<*>,
    method: TargetMethod,
    val targetMethod: TargetMethod,
    val arguments: List<CapturedArgument>,
    handle: MethodHandle
) : Hook(clazz, method, handle) {
    constructor(
        clazz: Class<*>,
        method: TargetMethod,
        targetMethod: TargetMethod,
        arguments: List<CapturedArgument> = emptyList(),
        hook: Function<*>
    ) : this(clazz, method, targetMethod, arguments, hook.toMethodHandle())
}
