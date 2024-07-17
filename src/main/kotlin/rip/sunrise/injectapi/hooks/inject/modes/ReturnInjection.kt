package rip.sunrise.injectapi.hooks.inject.modes

import rip.sunrise.injectapi.hooks.inject.InjectionMode

/**
 * Injects before any return from the method.
 */
class ReturnInjection : InjectionMode {
    override val typePriority = 0
}