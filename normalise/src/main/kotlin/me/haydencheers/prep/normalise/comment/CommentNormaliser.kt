package me.haydencheers.prep.normalise.comment

import me.haydencheers.prep.normalise.Forker
import me.haydencheers.prep.normalise.Normaliser
import java.nio.file.Path

class CommentNormaliser: Normaliser {
    override fun normalise(paths: List<Path>) {
        val ret = Forker.exec(
            CommentRemover.javaClass,
            paths.map { it.toAbsolutePath().toString() }
                .toTypedArray()
        )

        if (ret != 0) System.err.println("CommentRemover returned $ret")
    }
}