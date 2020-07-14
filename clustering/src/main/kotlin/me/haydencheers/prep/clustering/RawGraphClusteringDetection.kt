package me.haydencheers.clustering

import me.haydencheers.prep.clustering.util.ClusterGraph
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import org.apache.commons.math3.stat.StatUtils
import javax.json.JsonObject

object RawGraphClusteringDetection {
    @JvmStatic
    fun main(args: Array<String>) {

        for (ds in datasetNames) {

            val dsConfig = datasetConfigs.getValue(ds)
            val (maxClusters, maxCollaboration) = dsConfig

            val compfile = compRoot.resolve("${ds}.json")
            val comps = JsonSerialiser.deserialise(compfile, JsonObject::class)

            val suspicious = comps.get("knownSuspicious")!!.asJsonArray()
                .filterNot { isSameAuthor(it.asJsonObject().getString("lhs"), it.asJsonObject().getString("rhs")) }
                .map {
                    Pair(
                        it.asJsonObject().getString("lhs").replace("_", ""),
                        it.asJsonObject().getString("rhs").replace("_", "")
                    )
                }

            for (tool in toolNames) {
                val toolConfig = toolConfigs.getValue(tool)
                val kdePrecision = toolConfig

                val strfPath = scoreRoot.resolve(ds).resolve("scores-${tool}.strf")
                val strf = STRFSerialiser.deserialise(strfPath, BatchEvaluationResult::class)

                val scores = strf.comparisons
                    .filterNot { isSameAuthor(it.lhs, it.rhs) }
                    .map {
                        Score(
                            it.lhs.replace("_", ""),
                            it.rhs.replace("_", ""),
                            it.similarity
                        )
                    }
                    .sortedByDescending { it.score }

                val clusters = Bucketiser.clusters(scores, kdePrecision)
                val graph = ClusterGraph()

                var threshold = 100.0
                val suspiciousScores = mutableListOf<Score>()

                for (idx in clusters.indices.reversed()) {
                    val b = clusters[idx]
                    val preNodes = graph.getNodes()

                    for (score in b.scores)
                        graph.addEdge(score.lhs, score.rhs)

                    val components = graph.components()
                    val maxNodeDegree = components.maxBy { it.largestNodeDegree() }?.largestNodeDegree() ?: 0
                    val largestConnectivity = components.maxBy { it.connectivityRatio() }?.connectivityRatio() ?: 0.0

                    threshold = b.start
                    val condition = { components.size > maxClusters || maxNodeDegree > maxCollaboration }

                    if (condition()) {
                        val intersectScores = b.scores.filter { preNodes.contains(it.lhs) || preNodes.contains(it.rhs) }
                        suspiciousScores.addAll(intersectScores)

                        break
                    }

                    suspiciousScores.addAll(b.scores)
                }

                println("${tool} - ${ds}")
                println("Found ${suspiciousScores.size} at threshold ${threshold}")
                calculateStatistics(suspiciousScores, suspicious)
            }
        }
    }
}