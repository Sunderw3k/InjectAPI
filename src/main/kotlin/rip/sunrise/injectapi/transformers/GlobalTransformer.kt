package rip.sunrise.injectapi.transformers

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import rip.sunrise.injectapi.debug.ClassDumper
import rip.sunrise.injectapi.managers.HookManager
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

/**
 * The main transformer which adds hooks into classes.
 *
 * NOTE: Excepted to run last, otherwise it might break during a retransform,
 * where a transformer registered after us left.
 * @see HookManager.getTargetClasses
 * @see Instrumentation.retransformClasses
 */
internal object GlobalTransformer : ClassFileTransformer {
    private val transformedClasses = mutableMapOf<String, ByteArray>()

    var dumper: ClassDumper? = null

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray? {
        // TODO: https://stackoverflow.com/questions/78421704/java-classfiletransformer-fails-to-throw-exception
        return runCatching {
            // Check whether any hook applies to this class
            if (HookManager.getHooks().none { it.clazz.name.replace(".", "/") == className }) return classfileBuffer

            // Load cached bytes, so that all previous hooks are removed
            val classBytes = transformedClasses.getOrPut(className) { classfileBuffer }
            val node = ClassReader(classBytes).let {
                val node = ClassNode(Opcodes.ASM5)
                it.accept(node, 0)
                node
            }

            // Run transformers
            InjectTransformer().transform(node)
            RedirectTransformer().transform(node)

            val bytes = ClassWriter(ClassWriter.COMPUTE_FRAMES).let {
                node.accept(it)
                it.toByteArray()
            }

            // Dump the class, see ClassDumper
            dumper?.dump(className, bytes)

            return@runCatching bytes
        }.getOrElse { it.printStackTrace(); null }
    }
}