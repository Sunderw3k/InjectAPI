package rip.sunrise.injectapi.backends

import rip.sunrise.injectapi.utils.extensions.getRegisteredTransformers
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class InstrumentationBackend(val inst: Instrumentation) : TransformationBackend {
    val transformers = mutableMapOf<Transformer, ClassFileTransformer>()

    private fun makeJavaTransformer(transformer: Transformer): ClassFileTransformer = object : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray
        ): ByteArray? {
            return transformer.transform(loader, className, classfileBuffer)
        }
    }

    override fun addTransformer(transformer: Transformer) {
        makeJavaTransformer(transformer).also {
            transformers[transformer] = it
            inst.addTransformer(it, true)
        }
    }

    override fun removeTransformer(transformer: Transformer) {
        inst.removeTransformer(transformers.remove(transformer) ?: return)
    }

    override fun transformerCount(): Int {
        return inst.getRegisteredTransformers().size
    }

    override fun indexOf(transformer: Transformer): Int {
        val javaTransformer = transformers[transformer] ?: return -1
        return inst.getRegisteredTransformers().indexOf(javaTransformer)
    }

    override fun retransform(vararg classes: Class<*>) {
        inst.retransformClasses(*classes)
    }
}