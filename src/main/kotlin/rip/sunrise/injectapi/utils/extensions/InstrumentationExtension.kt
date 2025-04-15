package rip.sunrise.injectapi.utils.extensions

import rip.sunrise.injectapi.utils.setAccessibleUnsafe
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation

fun Instrumentation.getRegisteredTransformers(): List<ClassFileTransformer> {
    val transformerManager = this::class.java.getDeclaredField("mRetransfomableTransformerManager").also {
        it.setAccessibleUnsafe(true)
    }.get(this)!!

    val transformerListField = transformerManager::class.java.getDeclaredField("mTransformerList").also {
        it.setAccessibleUnsafe(true)
    }

    return (transformerListField.get(transformerManager) as Array<Any>).map {
        it::class.java.getDeclaredMethod("transformer").also {
            it.setAccessibleUnsafe(true)
        }(it) as ClassFileTransformer
    }
}