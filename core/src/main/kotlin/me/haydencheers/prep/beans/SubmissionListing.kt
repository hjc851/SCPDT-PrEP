package me.haydencheers.prep.beans

import java.nio.file.Path

class SubmissionListing (
    val name: String,
    var root: Path,
    val files: List<Path>
)