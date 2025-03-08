package rip.sunrise.injectapi.utils

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

object SanityTransformer : ClassFileTransformer {
    val classMap = mutableMapOf<String, ByteArray>()

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray? {
        return classMap.getOrPut(className) { classfileBuffer }
    }
}