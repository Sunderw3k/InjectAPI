package rip.sunrise.injectapi.hooks.inject.modes

import rip.sunrise.injectapi.hooks.inject.InjectionMode

/**
 * Injects at the entry point of the method
 */
class HeadInjection : InjectionMode {
    override val typePriority = 2
}