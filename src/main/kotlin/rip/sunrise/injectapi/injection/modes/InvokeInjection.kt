package rip.sunrise.injectapi.injection.modes

import rip.sunrise.injectapi.injection.InjectionMode
import rip.sunrise.injectapi.injection.TargetMethod

/**
 * Injects [offset] instructions after (or before) all INVOKE nodes matching [method].
 *
 * For the offset of 0 it injects right after the call.
 */
class InvokeInjection(val method: TargetMethod, val offset: Int) : InjectionMode {
    override val typePriority = 1
}