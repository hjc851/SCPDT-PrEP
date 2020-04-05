package me.haydencheers.prep.normalise.format

import me.haydencheers.prep.normalise.Forker
import me.haydencheers.prep.normalise.Normaliser
import java.nio.file.Path

class FormattingNormaliser: Normaliser {
    override fun normalise(paths: List<Path>) {
        val ret = Forker.exec(
            Formatter.javaClass,
            paths.map { it.toAbsolutePath().toString() }
                .toTypedArray()
        )

        if (ret != 0) System.err.println("Formatter returned $ret")
    }
}