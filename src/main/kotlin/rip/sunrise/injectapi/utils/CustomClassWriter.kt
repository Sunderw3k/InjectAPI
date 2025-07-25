package rip.sunrise.injectapi.utils

import org.objectweb.asm.ClassWriter

class CustomClassWriter(flags: Int, private val cl: ClassLoader?) : ClassWriter(flags) {
    override fun getClassLoader(): ClassLoader = cl ?: super.getClassLoader()
}