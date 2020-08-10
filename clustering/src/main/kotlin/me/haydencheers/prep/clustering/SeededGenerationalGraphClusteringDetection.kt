package me.haydencheers.prep.clustering

import me.haydencheers.clustering.*
import me.haydencheers.prep.clustering.util.ClusterGraph
import me.haydencheers.prep.scripts.Config
import kotlin.math.max

val generationCombinations = mutableListOf(
//    Triple(true,  true,  false)  // G0 G1
//    Triple(false, true,  true)   // G1 G2
//    Triple(true,  false, true),   // G0 G2
    Triple(true,  true,  true)    // G0 G1 G2
)

object SeededGenerationalGraphClusteringDetection {
    @JvmStatic
    fun main(args: Array<String>) {
        for ((pname, pvalue) in Config.VARIANT_LEVELS) {
            println(pname)

            for (combination in generationCombinations) {
                println(combination.label())

                for (ds in datasetNames) {
                    println(ds)
                    println("--------------------------------")

                    // Get dataset Suspicious Scores
                    val rawSuspicious = loadSuspiciousScores(ds)

                    // DS config
                    val dsConfig = datasetConfigs.getValue(ds)
                    val (dsMaxCluster, dsMaxCollab) = dsConfig

                    for (tool in toolNames) {
                        // Get precision for this tool
                        val toolConfig = toolConfigs.getValue(tool)
                        val kdePrecision = toolConfig

                        // Load scores for the detection tool for this data set
                        val rawScores = loadToolScores(tool, ds)

                        // Load variants + suspicious scores
                        val variantScores = loadVariantScoresForTool(tool, ds, pname)

                        val vs = filterScoresForCombination(variantScores, combination)
                        val scores = (rawScores + vs).sortedBy { it.score }

                        Unit

                        val variantSuspicious = vs.map { it.lhs to it.rhs }
                        val suspicious = rawSuspicious + variantSuspicious

                        val (clusterOffset, seededMaxCollab) = combination.detectionOffsets()

                        val maxClusters = dsMaxCluster + clusterOffset
                        val maxCollaboration = max(dsMaxCollab, seededMaxCollab)

                        val clusters = Bucketiser.clusters(scores, kdePrecision)
                        val graph = ClusterGraph()

                        var threshold = 100.0
                        val suspiciousScores = mutableListOf<Score>()

                        // Reverse iterate through the clusters
                        for (idx in clusters.indices.reversed()) {
                            val b = clusters[idx]
                            val preNodes = graph.getNodes()

                            for (score in b.scores)
                                graph.addEdge(score.lhs, score.rhs)

                            val components = graph.components()
                            val maxNodeDegree = components.maxBy { it.largestNodeDegree() }?.largestNodeDegree() ?: 0

                            threshold = b.start
                            val condition = { components.size > maxClusters || maxNodeDegree > maxCollaboration }
                            if (condition()) {
//                                val intersectScores = b.scores.filter { preNodes.contains(it.lhs) || preNodes.contains(it.rhs) }
//                                suspiciousScores.addAll(intersectScores)

                                break
                            }

                            suspiciousScores.addAll(b.scores)
                        }

                        val endGraph = ClusterGraph()
                        for (score in suspiciousScores)
                            endGraph.addEdge(score.lhs, score.rhs)

                        val endComponents = graph.components()
                        val endMaxNodeDegree = endComponents.maxBy { it.largestNodeDegree() }?.largestNodeDegree() ?: 0
                        val endLargestConnectivity = endComponents.maxBy { it.connectivityRatio() }?.connectivityRatio() ?: 0.0

                        val tp = tpCount(suspiciousScores, suspicious)
                        val fp = fpCount(suspiciousScores, suspicious)
                        val fn = fnCount(suspiciousScores, suspicious)
                        println("$tp\t$fp\t$fn")

//                        println("${tool} - ${ds} - ${pname}")
//                        println(combination.label())
//                        println("Found ${suspiciousScores.size} at threshold ${threshold}")
//                        calculateStatistics(suspiciousScores, suspicious)
//                        println("--------------------------------")
//                        println()
                    }

                    println("--------------------------------")
                }
            }
        }
    }
}

fun filterScoresForCombination(variantScores: List<Score>, combination: Triple<Boolean, Boolean, Boolean>): List<Score> {
    val partition = partitionVariantIds(variantScores)
    val (g0, g1, g2) = combination

    val scores = mutableListOf<Score>()

    if (g0 && g1 && g2) {
        val links = partition.g0g1links + partition.g1g2links
        val genscores = variantScores.filter { it.isIn(links) }
        scores.addAll(genscores)
    } else if (g0 && g1) {
        val links = partition.g0g1links
        val genscores = variantScores.filter { it.isIn(links) }
        scores.addAll(genscores)
    } else if (g1 && g2) {
        val links = partition.g1g2links
        val genscores = variantScores.filter { it.isIn(links) }
        scores.addAll(genscores)
    } else if (g0 && g2) {
        val links = partition.g0g2links
        val genscores = variantScores.filter { it.isIn(links) }
        scores.addAll(genscores)
    }

    return scores
}

fun Triple<Boolean, Boolean, Boolean>.label(): String {
    val components = mutableListOf<String>()

    if (first)  components.add("G0")
    if (second) components.add("G1")
    if (third)  components.add("G2")

    return components.joinToString("-")
}

fun Triple<Boolean, Boolean, Boolean>.detectionOffsets(): Pair<Int, Int> {
    val (g0, g1, g2) = this

    return if (g0 && g1 && g2) {
        8 to 4
    } else if (g0 && g1) {
        3 to 4
    } else if (g1 && g2) {
        9 to 3
    } else if (g0 && g2) {
        3 to 9
    } else {
        throw IllegalStateException("Unexpected generation combination")
    }
}