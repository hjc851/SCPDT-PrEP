package me.haydencheers.prep.clustering

import me.haydencheers.clustering.datasetNames
import me.haydencheers.clustering.loadToolScores
import me.haydencheers.clustering.scoreRoot
import me.haydencheers.clustering.toolNames
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser

object PercOver50 {
    @JvmStatic
    fun main(args: Array<String>) {
        val THRESHOLD = 75

        println("DS\tCnt\t> ${THRESHOLD}\t%")
        for (tool in toolNames) {
            println(tool)
            for (ds in datasetNames) {
                val scores = loadToolScores(tool, ds)

                val subIds = scores.flatMap { listOf(it.lhs, it.rhs) }.toSet()
                val above50 = scores.filter { it.score > THRESHOLD }
                val idsAbove50 = above50.flatMap { listOf(it.lhs, it.rhs) }.toSet()

                val perc = idsAbove50.size
                    .toDouble()
                    .div(subIds.size)
                    .times(100.0)

                println("$ds\t${subIds.size}\t${idsAbove50.size}\t$perc")
            }
            println()
        }
    }
}