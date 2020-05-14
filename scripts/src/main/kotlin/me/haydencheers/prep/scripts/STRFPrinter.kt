package me.haydencheers.prep.scripts

import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Paths

object STRFPrinter {
    @JvmStatic
    fun main(args: Array<String>) {
        val strfPath = Paths.get(args[0])
        val strf = STRFSerialiser.deserialise(strfPath, BatchEvaluationResult::class)

        for (comparison in strf.comparisons) {
            println("${comparison.lhs} ${comparison.rhs} ${comparison.similarity}")
        }
    }
}