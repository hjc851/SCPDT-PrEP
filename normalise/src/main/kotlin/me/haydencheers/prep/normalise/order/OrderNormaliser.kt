package me.haydencheers.prep.normalise.order

import me.haydencheers.prep.normalise.Forker
import me.haydencheers.prep.normalise.Normaliser
import me.haydencheers.prep.normalise.Parser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OrderNormaliser: Normaliser {
    override fun normalise(paths: List<Path>) {
        val ret = Forker.exec(
            Orderer.javaClass,
            paths.map { it.toAbsolutePath().toString() }
                .toTypedArray()
        )

        if (ret != 0) System.err.println("Orderer returned $ret")
    }
}