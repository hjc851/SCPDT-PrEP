package me.haydencheers.prep

import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Paths

fun main() {
    val archive = Paths.get("/media/haydencheers/Data/PrEP/out-all-but-sim-and-sherlock-sydney/scores-Naive Program Dependence Graph.strf")
    val bean = STRFSerialiser.deserialise(archive, BatchEvaluationResult::class)
    val matches = bean.comparisons
        .sortedByDescending { it.similarity }

    println()
}