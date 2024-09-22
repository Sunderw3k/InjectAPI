package rip.sunrise.injectapi.access

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import rip.sunrise.injectapi.access.AccessWidener.FunctionWrapper
import rip.sunrise.injectapi.utils.extensions.addOpensAll
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.lang.invoke.MethodHandles
import java.security.ProtectionDomain

/**
 * Used for calling private/protected methods, getting/setting private/protected fields.
 *
 * NOTE: Unstable
 */
object AccessWidener {
    /**
     * Used for modifying addOpens to allow for calling private methods.
     */
    internal fun initialize(inst: Instrumentation) {
        val transformer = Transformer()
        inst.addTransformer(transformer, true)
        inst.retransformClasses(Module::class.java)
        inst.removeTransformer(transformer)
    }

    inline fun <reified T, R> accessMethod(name: String, vararg types: Class<*>): FunctionWrapper<R> {
        T::class.java.module.addOpensAll(AccessWidener::class.java.module)

        return FunctionWrapper { args ->
            val method = T::class.java.getDeclaredMethod(name, *types).also { it.isAccessible = true }
            val handle = MethodHandles.lookup().unreflect(method)

            handle.invokeWithArguments(args.toList()) as R
        }
    }

    fun interface FunctionWrapper<R> {
        operator fun invoke(vararg args: Any?): R
    }

    private class Transformer : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray
        ): ByteArray {
            if (className != "java/lang/Module") return classfileBuffer

            val node = ClassReader(classfileBuffer).let {
                val node = ClassNode(Opcodes.ASM5)
                it.accept(node, 0)
                node
            }

            val implAddExportsOrOpens = node.methods.first { m -> m.name == "addOpens" && m.desc == "(Ljava/lang/String;Ljava/lang/Module;)Ljava/lang/Module;" }

            implAddExportsOrOpens.instructions.insert(InsnList().apply {
                val endLabel = LabelNode()

                // Check if the package is null
                add(VarInsnNode(Opcodes.ALOAD, 1))
                add(JumpInsnNode(Opcodes.IFNULL, endLabel))

                // Check if the module is null
                add(VarInsnNode(Opcodes.ALOAD, 2))
                add(JumpInsnNode(Opcodes.IFNULL, endLabel))

                // Check if is named
                add(VarInsnNode(Opcodes.ALOAD, 0))
                add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Module", "isNamed", "()Z", false))
                add(JumpInsnNode(Opcodes.IFEQ, endLabel))

                // Call implAddExportsOrOpens
                add(VarInsnNode(Opcodes.ALOAD, 0))
                add(VarInsnNode(Opcodes.ALOAD, 1))
                add(VarInsnNode(Opcodes.ALOAD, 2))
                add(InsnNode(Opcodes.ICONST_1))
                add(InsnNode(Opcodes.ICONST_1))
                add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Module", "implAddExportsOrOpens", "(Ljava/lang/String;Ljava/lang/Module;ZZ)V", false))

                // Return
                add(VarInsnNode(Opcodes.ALOAD, 0))
                add(InsnNode(Opcodes.ARETURN))

                add(endLabel)
            })

            val bytes = ClassWriter(0).let {
                node.accept(it)
                it.toByteArray()
            }

            return bytes
        }
    }
}