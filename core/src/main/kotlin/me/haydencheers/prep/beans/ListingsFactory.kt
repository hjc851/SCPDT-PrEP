package me.haydencheers.prep.beans

import me.haydencheers.prep.util.FileUtils
import java.nio.file.Files
import java.nio.file.Path

object ListingsFactory {
    fun produceForDirectory(dir: Path): MutableList<SubmissionListing> {
        if (!Files.isDirectory(dir)) throw IllegalArgumentException("Listing directory is invalid ${dir}")

        return FileUtils.listDirectories(dir)
            .map { produceForSubmission(it) }
            .toMutableList()
    }

    fun produceForSubmission(dir: Path): SubmissionListing {
        if (!Files.isDirectory(dir)) throw IllegalArgumentException("Submission directory is invalid ${dir}")

        val files = FileUtils.listFiles(dir, ".java")

        return SubmissionListing(
            dir.fileName.toString(),
            dir,
            files
        )
    }
}