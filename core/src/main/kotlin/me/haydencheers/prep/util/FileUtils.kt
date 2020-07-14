package me.haydencheers.prep.util

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.streams.toList

object FileUtils {
    /**
     * Copies all files in @from to @to
     */
    fun copyDir(from: Path, to: Path) {
        if (!Files.isDirectory(from)) throw IllegalArgumentException("File $from does not exist")

        Files.walk(from)
//            .filter {
//                (Files.isRegularFile(it) && !Files.isHidden(it) && !it.fileName.toString().startsWith(".")) ||
//                        Files.isRegularFile()
//            }
            .forEachOrdered { file ->
                Files.copy(file, to.resolve(from.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
            }
    }

    fun copyDirOnlyJava(from: Path, to: Path) {
        if (!Files.isDirectory(from)) throw IllegalArgumentException("File $from does not exist")

        Files.walk(from)
            .filter { it.fileName.toString().endsWith(".java") }
            .forEachOrdered { file ->
                val toPath = to.resolve(from.relativize(file).toString())
                val parent = toPath.parent
                if (!Files.exists(parent))
                    Files.createDirectories(parent)

                Files.copy(file, toPath, StandardCopyOption.REPLACE_EXISTING)
            }
    }

    fun listFiles(path: Path, extension: String): List<Path> {
        return Files.walk(path)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && !it.fileName.toString().startsWith(".") }
            .use { it.toList() }
    }
}

