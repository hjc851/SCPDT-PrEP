package me.haydencheers.prep.clustering

import me.haydencheers.clustering.*
import me.haydencheers.prep.clustering.util.ClusterGraph
import me.haydencheers.prep.scripts.Config

object StaticSeededTopNDetection {
    @JvmStatic
    fun main(args: Array<String>) {
        val n = 50

        for ((pname, pvalue) in Config.VARIANT_LEVELS) {
            for (ds in datasetNames) {
                // Get dataset Suspicious Scores
                val rawSuspicious = loadSuspiciousScores(ds)

                println("${ds} - ${pname} - Top ${n}")

                for (tool in toolNames) {
                    // Load scores for the detection tool for this data set
                    val rawScores = loadToolScores(tool, ds)

                    val variantScores = loadVariantScoresForTool(tool, ds, pname)
                    val variantSuspicious = getSuspiciousVariantPairs(variantScores)

                    val _vs = variantScores.filter { it.isIn(variantSuspicious) }

                    val scores = (rawScores + _vs).sortedByDescending { it.score }
                    val suspicious = (rawSuspicious + variantSuspicious)

                    val suspiciousScores = scores.take(n)

                    val tp = tpCount(suspiciousScores, suspicious)
                    val fp = fpCount(suspiciousScores, suspicious)
                    val fn = fnCount(suspiciousScores, suspicious)
                    println("$tp\t$fp\t$fn")
                }

                println()
            }

            println("------------------------")
        }
    }
}