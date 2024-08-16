package rip.sunrise.injectapi.utils

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object Native {
    private val OS_NAME = System.getProperty("os.name").lowercase()

    fun isWindows(): Boolean {
        return OS_NAME.contains("win")
    }
    fun isLinux(): Boolean {
        return OS_NAME.contains("linux")
    }

    fun loadNatives() {
        val extension = if (isWindows()) ".dll" else if (isLinux()) ".so" else error("Unknown OS")

        val name = "ForceClassLoaderDefine$extension"
        val stream = this.javaClass.getResourceAsStream("/$name") ?: error("Couldn't find resource $name")
        loadNative(name.removeSuffix(extension), stream)
    }

    private fun loadNative(name: String, stream: InputStream) {
        Files.createTempFile(name, null).also {
            it.toFile().deleteOnExit()

            Files.copy(stream, it, StandardCopyOption.REPLACE_EXISTING)
            System.load(it.toFile().path)
        }
    }
}