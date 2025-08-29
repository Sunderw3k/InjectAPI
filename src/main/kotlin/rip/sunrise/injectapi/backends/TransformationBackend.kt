package rip.sunrise.injectapi.backends

/**
 * Describes the contract for applying transformations. For an example see [InstrumentationBackend].
 */
interface TransformationBackend {
    /**
     * Register a [transformer]. The transformer is expected to be registered as retransformation capable.
     */
    fun addTransformer(transformer: Transformer)

    /**
     * Unregisters a [transformer]. No-op if the transformer isn't registered.
     */
    fun removeTransformer(transformer: Transformer)

    /**
     * Returns the number of registered transformers.
     */
    fun transformerCount(): Int

    /**
     * Returns the index of [transformer]. Sometimes running last is preferred, and this is used for assertions.
     */
    fun indexOf(transformer: Transformer): Int

    /**
     * Makes the JVM follow the retransformation process for given [classes].
     */
    fun retransform(vararg classes: Class<*>)
}