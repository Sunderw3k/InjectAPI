package rip.sunrise.injectapi.debug

/**
 * Used for the purpose of debugging classed modified by hooks.
 *
 * @see rip.sunrise.injectapi.transformers.GlobalTransformer
 */
interface ClassDumper {
    /**
     * Called on-load of a hooked class.
     *
     * @param name The internal class name. For example, `"java/util/List"`.
     * @param bytes The class file bytes.
     */
    fun dump(name: String, bytes: ByteArray)
}