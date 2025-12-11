package rip.sunrise.injectapi.transformers

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import rip.sunrise.injectapi.backends.Transformer
import rip.sunrise.injectapi.debug.ClassDumper
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.utils.CustomClassWriter
import rip.sunrise.injectapi.backends.TransformationBackend

/**
 * The main transformer which adds hooks into classes.
 *
 * NOTE: Excepted to run last, otherwise it might break during a retransform,
 * where a transformer registered after us left.
 * @see HookManager.getTargetClasses
 * @see TransformationBackend.retransform
 */
internal object GlobalTransformer : Transformer {
    private val transformedClasses = mutableMapOf<String, ByteArray>()

    var dumper: ClassDumper? = null

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classfileBuffer: ByteArray
    ): ByteArray? {
        // TODO: https://stackoverflow.com/questions/78421704/java-classfiletransformer-fails-to-throw-exception
        return runCatching {
            if (className == null) return null

            // Check whether any hook applies to this class
            if (HookManager.getHooks().none { it.clazz.name.replace(".", "/") == className }) return null

            // Load cached bytes, so that all previous hooks are removed
            val classBytes = transformedClasses.getOrPut(className) { classfileBuffer }
            val node = ClassReader(classBytes).let {
                val node = ClassNode(Opcodes.ASM9)
                it.accept(node, 0)
                node
            }

            val supportsInvokedynamic = node.version and 0xFF >= 51

            // Run transformers
            InjectTransformer().transform(node, supportsInvokedynamic)
            FieldRedirectTransformer().transform(node, supportsInvokedynamic)
            MethodRedirectTransformer().transform(node, supportsInvokedynamic)

            val bytes = CustomClassWriter(ClassWriter.COMPUTE_FRAMES, loader).let {
                node.accept(it)
                it.toByteArray()
            }

            // Dump the class, see ClassDumper
            dumper?.dump(className, bytes)

            return@runCatching bytes
        }.getOrElse { it.printStackTrace(); null }
    }
}