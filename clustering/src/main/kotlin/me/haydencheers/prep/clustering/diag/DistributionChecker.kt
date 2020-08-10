package me.haydencheers.prep.clustering.diag

import me.haydencheers.clustering.BUCKET_WIDTH
import me.haydencheers.clustering.loadToolScores
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import org.apache.commons.math3.random.EmpiricalDistribution
import java.nio.file.Paths
import kotlin.math.roundToInt

object DistributionChecker {
    @JvmStatic
    fun main(args: Array<String>) {
        val tool = "Sherlock-Sydney"

        val scores = loadToolScores(tool, "SENG4400_A1_2018")

        var rawScores = scores.map { it.score }
            .sorted()

        var lastSize = rawScores.size
        for (i in -1 .. 99 step 2) {
            rawScores = rawScores.dropWhile { it > i && it <= (i+2) }
            val diff = lastSize - rawScores.size
            lastSize = rawScores.size

            println("${i+1}\t${diff}")
        }

//        val ed = EmpiricalDistribution((100 / BUCKET_WIDTH).toInt())
//        ed.load(rawScores)

//        val bs = ed.binStats
//        for (s in bs) {
//            println("${s.min}\t${s.max}\t${s.n}")
//        }

//        for (i in 0..99) {
//            val prob = ed.probability(i.toDouble(), i+1.0)
//            val perc = (prob * scores.size)
//
//            val noScores = if (perc.isNaN()) 0 else (perc+1).toInt()
//
//            println("${i+1}\t${noScores}")
//        }
//
//        Unit
    }
}