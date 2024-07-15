package rip.sunrise.injectapi.debug

import java.nio.file.Path

/**
 * A ClassDumper made to dump into the filesystem [rootDirectory].
 */
class FileSystemDumper(private val rootDirectory: Path) : ClassDumper {
    override fun dump(name: String, bytes: ByteArray) {
        val targetFile = rootDirectory.resolve("$name.class").toFile()

        targetFile.parentFile.mkdirs()
        targetFile.writeBytes(bytes)
    }
}