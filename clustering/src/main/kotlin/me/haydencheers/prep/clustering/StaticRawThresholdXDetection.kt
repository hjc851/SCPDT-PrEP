package me.haydencheers.prep.clustering

import me.haydencheers.clustering.*
import me.haydencheers.prep.clustering.util.ClusterGraph
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import javax.json.JsonObject

object StaticRawThresholdXDetection {
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

            println(ds)

            for (tool in toolNames) {
                val toolConfig = toolConfigs.getValue(tool)
                val kdePrecision = toolConfig

                val threshold = toolThresholds[tool]!!

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

                val suspiciousScores = scores.takeWhile { it.score >= threshold }

                val tp = tpCount(suspiciousScores, suspicious)
                val fp = fpCount(suspiciousScores, suspicious)
                val fn = fnCount(suspiciousScores, suspicious)

                println("$tp\t$fp\t$fn")
            }

            println()
        }
    }
}