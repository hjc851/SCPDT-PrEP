package me.haydencheers.prep.clustering.diag

import me.haydencheers.clustering.loadVariantScoresForTool
import me.haydencheers.clustering.partitionVariantIds
import me.haydencheers.prep.scripts.Config

object VariantDistributionChecker {
    @JvmStatic
    fun main(args: Array<String>) {

        val tool = "Sherlock-Sydney"

        for ((pname, pvalue) in Config.VARIANT_LEVELS) {
            println(pname)

            val scores = loadVariantScoresForTool(tool, "SENG1110_A1_2017", pname)
            val partitions = partitionVariantIds(scores)

            val g0g1scores = scores.filter { it.isIn(partitions.g0g1links) }
            val g0g1avg = g0g1scores.map { it.score }.average()

            val g1g2scores = scores.filter { it.isIn(partitions.g1g2links) }
            val g1g2avg = g1g2scores.map { it.score }.average()

//            val g0g2scores = scores.filter { it.isIn(partitions.g0g2links) }
//            val g0g2avg = g0g2scores.map { it.score }.average()

            var rawScores = (g0g1scores + g1g2scores)
                .map { it.score }
                .sorted()
//
//            var rawScores = scores.map { it.score }.sorted()

            var lastSize = rawScores.size
            for (i in -1 .. 99) {
                rawScores = rawScores.dropWhile { it > i && it <= (i+1) }
                val diff = lastSize - rawScores.size
                lastSize = rawScores.size

                println("${diff}")
            }

            println()
            println("---------------------------------------------------------")
            println()
        }
    }
}