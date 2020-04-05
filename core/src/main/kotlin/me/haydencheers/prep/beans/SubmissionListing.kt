package me.haydencheers.prep.beans

import java.nio.file.Path

class SubmissionListing (
    var name: String,
    var root: Path,
    var files: List<Path>,
    var isMutable: Boolean
)