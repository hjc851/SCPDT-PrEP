package me.haydencheers.prep.beans

import me.haydencheers.prep.util.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

object ListingsFactory {
    fun produceForDirectory(dir: Path, mutable: Boolean): MutableList<SubmissionListing> {
        if (!Files.isDirectory(dir)) throw IllegalArgumentException("Listing directory is invalid ${dir}")

        return Files.list(dir)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }
            .map { produceForSubmission(it, mutable) }
            .toMutableList()
    }

    fun produceForSubmission(dir: Path, mutable: Boolean): SubmissionListing {
        if (!Files.isDirectory(dir)) throw IllegalArgumentException("Submission directory is invalid ${dir}")

        val files = FileUtils.listFiles(dir, ".java")

        if (files.firstOrNull { it.fileName.toString().startsWith("._") } != null)
            Unit

        return SubmissionListing(
            dir.fileName.toString(),
            dir,
            files,
            mutable
        )
    }
}