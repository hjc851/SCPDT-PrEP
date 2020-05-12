package me.haydencheers.prep

import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Paths

object STRFPrinter {
    @JvmStatic
    fun main(args: Array<String>) {
        val archive = Paths.get(args[0])
        val bean = STRFSerialiser.deserialise(archive, BatchEvaluationResult::class)
        val matches = bean.comparisons.sortedByDescending { it.similarity }

        for (match in matches) {
            println("${match.lhs} vs ${match.rhs} @ ${match.similarity}")
        }
    }
}