package rip.sunrise.injectapi.injection

/**
 * Specifies the point of injection
 */
interface InjectionMode {
    /**
     * Defines the order of injections. Lower goes first.
     */
    val typePriority: Int
}