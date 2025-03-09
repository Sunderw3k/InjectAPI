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
 * Note: Already loaded classes require a reload.
 * @see HookManager.getTargetClasses
 * @see Instrumentation.retransformClasses
 */
class GlobalTransformer(private val dumper: ClassDumper? = null) : ClassFileTransformer {
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
            if (HookManager.getHookMap().values.none { it.clazz.name.replace(".", "/") == className }) return classfileBuffer

            val node = ClassReader(classfileBuffer).let {
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

    /**
     * Used for registering the global transformer.
     *
     * This call is equal to
     * ```
     * inst.addTransformer(transformer, true)
     * ```
     */
    fun register(inst: Instrumentation) {
        inst.addTransformer(this, true)
    }
}