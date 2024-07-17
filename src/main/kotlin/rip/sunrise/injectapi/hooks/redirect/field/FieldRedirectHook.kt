package rip.sunrise.injectapi.hooks.redirect.field

import org.objectweb.asm.Opcodes
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.hooks.TargetField
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.utils.extensions.toMethodHandle
import java.lang.invoke.MethodHandle

class FieldRedirectHook(
    val type: Type,
    clazz: Class<*>,
    method: TargetMethod,
    val targetField: TargetField,
    val arguments: List<CapturedArgument>,
    handle: MethodHandle
) : Hook(clazz, method, handle) {
    constructor(
        type: Type,
        clazz: Class<*>,
        method: TargetMethod,
        targetField: TargetField,
        arguments: List<CapturedArgument> = emptyList(),
        hook: Function<*>
    ) : this(type, clazz, method, targetField, arguments, hook.toMethodHandle())

    enum class Type(val allowedOpcodes: List<Int>) {
        GET(listOf(Opcodes.GETFIELD, Opcodes.GETSTATIC)),
        SET(listOf(Opcodes.PUTFIELD, Opcodes.PUTSTATIC)),
        GET_SET(GET.allowedOpcodes + SET.allowedOpcodes)
    }
}