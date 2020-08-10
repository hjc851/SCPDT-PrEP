package me.haydencheers.prep.clustering.diag

import me.haydencheers.clustering.Score
import me.haydencheers.clustering.datasetNames
import me.haydencheers.clustering.loadToolScores
import me.haydencheers.clustering.toolNames
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

object AllAvgSimStdDev {
    @JvmStatic
    fun main(args: Array<String>) {
        for (tool in toolNames) {
            val scores = mutableListOf<Score>()
            for (ds in datasetNames) {
                val dsscores = loadToolScores(tool, ds)
                scores.addAll(dsscores)
            }

            val values = scores.map { it.score }.toDoubleArray()

            val avg = values.average()
            val stddev = 10 // StandardDeviation().evaluate(values)

            val withinstd = values.filter { it >= (avg-stddev) && it <= (avg+stddev) }
                .count()
                .div(values.size.toDouble())
                .times(100)

            println(tool)
            println("Avg\t${avg}")
            println("StdDev\t${stddev}")
            println("InStd\t${withinstd}")
            println()
        }
    }
}