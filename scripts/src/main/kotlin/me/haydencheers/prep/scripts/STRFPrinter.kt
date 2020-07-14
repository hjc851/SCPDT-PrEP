package me.haydencheers.prep.scripts

import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Paths

object STRFPrinter {
    @JvmStatic
    fun main(args: Array<String>) {
        val strfPath = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV1B/SENG1110_A1_2017/p1/out/scores-JPlag.strf")
        val strf = STRFSerialiser.deserialise(strfPath, BatchEvaluationResult::class)

        for (comparison in strf.comparisons) {
            println("${comparison.lhs} ${comparison.rhs} ${comparison.similarity}")
        }
    }
}