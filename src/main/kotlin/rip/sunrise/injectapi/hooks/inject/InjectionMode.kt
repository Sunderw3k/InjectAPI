package rip.sunrise.injectapi.hooks.inject

/**
 * Specifies the point of injection
 */
interface InjectionMode {
    /**
     * Defines the order of injections. Lower goes first.
     */
    val typePriority: Int
}