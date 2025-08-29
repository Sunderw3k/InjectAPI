package rip.sunrise.injectapi.backends

/**
 * Handled by [TransformationBackend], defines the contract for modifying class bytecode.
 */
interface Transformer {
    fun transform(loader: ClassLoader?, className: String, classfileBuffer: ByteArray): ByteArray?
}