package rip.sunrise.injectapi.injection.modes

import rip.sunrise.injectapi.injection.InjectionMode

/**
 * Injects before any return from the method.
 */
class ReturnInjection : InjectionMode {
    override val typePriority = 0
}