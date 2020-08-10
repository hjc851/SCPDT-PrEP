package me.haydencheers.prep.clustering.old

import me.haydencheers.clustering.*
import me.haydencheers.prep.clustering.util.ClusterGraph
import me.haydencheers.prep.scripts.Config

object SeededGraphBucketDetection {
    @JvmStatic
    fun main(args: Array<String>) {
        println("BUCKET_WIDTH $BUCKET_WIDTH")
        println("NODE_EDGE_RATIO $NODE_EDGE_RATIO")
        println("MAX_COLLABORATION $MAX_CLUSTERS")
        println()

        val MAX_CLUSTERS = MAX_CLUSTERS + 2
        val NODE_MAX_DEGREE = NODE_MAX_DEGREE + 4

        for (ds in datasetNames) {
            // Get dataset Suspicious Scores
            val rawSuspicious = loadSuspiciousScores(ds)

            for (tool in toolNames) {
                // Load scores for the detection tool for this data set
                val rawScores = loadToolScores(tool, ds)

                for ((pname, pvalue) in Config.VARIANT_LEVELS) {
                    val variantScores = loadVariantScoresForTool(tool, ds, pname)
                    val variantSuspicious = getSuspiciousVariantPairs(variantScores)

                    val _vs = variantScores.filter { it.isIn(variantSuspicious) }

                    val scores = (rawScores + _vs).sortedBy { it.score }
                    val suspicious = rawSuspicious + variantSuspicious

                    val buckets = Bucketiser.bucketise(scores, BUCKET_WIDTH)
                    val graph = ClusterGraph()

                    var threshold = 100.0
                    val suspiciousScores = mutableListOf<Score>()

                    for (idx in buckets.indices.reversed()) {
                        val b = buckets[idx]

                        for (score in b.scores)
                            graph.addEdge(score.lhs, score.rhs)

                        val components = graph.components()
                        val maxNodeDegree = components.maxBy { it.largestNodeDegree() }?.largestNodeDegree() ?: 0
                        val largestConnectivity = components.maxBy { it.connectivityRatio() }?.connectivityRatio() ?: 0.0

                        threshold = b.start

                        val condition = { components.size > MAX_CLUSTERS || maxNodeDegree > NODE_MAX_DEGREE }
//                        val condition = { largestConnectivity > NODE_EDGE_RATIO || components.size > MAX_CLUSTERS }
//                        val condition = { largestConnectivity > NODE_EDGE_RATIO }
//                        val condition = { components.size > MAX_CLUSTERS }

                        suspiciousScores.addAll(b.scores)

                        if (condition()) {
                            break
                        } else {
                        }
                    }

                    val endGraph = ClusterGraph()
                    for (score in suspiciousScores)
                        endGraph.addEdge(score.lhs, score.rhs)

                    val endComponents = graph.components()
                    val endMaxNodeDegree = endComponents.maxBy { it.largestNodeDegree() }?.largestNodeDegree() ?: 0
                    val endLargestConnectivity = endComponents.maxBy { it.connectivityRatio() }?.connectivityRatio() ?: 0.0

                    println("${tool} - ${ds} - ${pname}")
                    println("Found ${suspiciousScores.size} at threshold ${threshold}")
                    calculateStatistics(suspiciousScores, suspicious)
                    println("--------------------------------")
                    println()
                }

                println("----------------------------------------------------------------")
                println("----------------------------------------------------------------")
                println("----------------------------------------------------------------")
                println()
            }
        }
    }
}