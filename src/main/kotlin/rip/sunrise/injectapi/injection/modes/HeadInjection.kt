package rip.sunrise.injectapi.injection.modes

import rip.sunrise.injectapi.injection.InjectionMode

/**
 * Injects at the entry point of the method
 */
class HeadInjection : InjectionMode {
    override val typePriority = 2
}