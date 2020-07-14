package me.haydencheers.clustering

import me.haydencheers.prep.clustering.util.ClusterGraph
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import javax.json.JsonObject

object RawGraphBucketDetection {
    @JvmStatic
    fun main(args: Array<String>) {
        for (ds in datasetNames) {
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

                val buckets = Bucketiser.bucketise(scores, BUCKET_WIDTH)
                val graph = ClusterGraph()

                var threshold = 100.0
                val suspiciousScores = mutableListOf<Score>()

                for (idx in buckets.indices.reversed()) {
                    val b = buckets[idx]

                    suspiciousScores.addAll(b.scores)
                    for (score in b.scores)
                        graph.addEdge(score.lhs, score.rhs)

                    val components = graph.components()
                    val maxNodeDegree = components.maxBy { it.largestNodeDegree() }?.largestNodeDegree() ?: 0
                    val largestConnectivity = components.maxBy { it.connectivityRatio() }?.connectivityRatio() ?: 0.0

                    threshold = b.start

                    if (components.size > MAX_CLUSTERS || maxNodeDegree > NODE_MAX_DEGREE) {
//                    if (largestConnectivity > NODE_EDGE_RATIO || components.size > MAX_COLLABORATION) {
//                    if (largestConnectivity > NODE_EDGE_RATIO) {
//                    if (components.size > MAX_COLLABORATION) {
                        break
                    }
                }

                val tp = tpCount(suspiciousScores, suspicious)
                val fp = fpCount(suspiciousScores, suspicious)
                val fn = fnCount(suspiciousScores, suspicious)

                val precision = (tp.toDouble()) / (tp + fp)
                val recall = (tp.toDouble()) / (tp + fn)

                println("${tool} - ${ds}")
                println("Found ${suspiciousScores.size} at threshold ${threshold}")
                println("Precision: ${String.format("%2.2f", precision)}")
                println("Recall: ${String.format("%2.2f", recall)}")
                println("${tp} TP scores of ${suspicious.size} total")
                for (score in suspiciousScores) {
                    if (score.isIn(suspicious)) {
                        println("\t${score.lhs} - ${score.rhs} : ${score.score}")
                    }
                }

                println("${fp} FP scores")
                for (score in suspiciousScores) {
                    if (!score.isIn(suspicious)) {
                        println("\t${score.lhs} - ${score.rhs} : ${score.score}")
                    }
                }
                println()
            }
        }
    }
}

