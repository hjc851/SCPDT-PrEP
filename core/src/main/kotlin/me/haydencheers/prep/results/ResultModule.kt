package me.haydencheers.prep.results

import me.haydencheers.prep.util.JsonSerialiser
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.beans.FileComparisonResult
import me.haydencheers.strf.beans.PairwiseComparisonResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PreDestroy
import javax.inject.Singleton
import javax.json.Json

@Singleton
class ResultModule {
    private var realSubmissions = mutableListOf<String>()
    private var syntheticSubmissions = mutableListOf<String>()
    private var simplagAnalytics = mutableListOf<Pair<String, Path>>()

    private var pairwiseScores = mutableMapOf<String, List<Triple<String, String, Double>>>()
    private var filewiseScores = mutableMapOf<String, List<Triple<String, String, List<Triple<String, String, Double>>>>>()

    private var pairwiseDurations = mutableMapOf<String, Long>()
    private var filewiseDurations = mutableMapOf<String, Long>()

    fun addRealSubmissions(submissions: List<String>) {
        realSubmissions.addAll(realSubmissions)
    }

    fun addSyntheticSubmissions(submissions: List<String>) {
        syntheticSubmissions.addAll(submissions)
    }

    fun addSimplagAnalytics(tag: String, path: Path) {
        simplagAnalytics.add(tag to path)
    }

    fun addPairwiseScores(tool: String, result: List<Triple<String, String, Double>>) {
        pairwiseScores[tool] = result
    }

    fun addFilewiseScores(tool: String, result: List<Triple<String, String, List<Triple<String, String, Double>>>>) {
        filewiseScores[tool] = result
    }

    fun storeResults(outputRoot: Path) {
        // Obfuscation Analytics
        for ((key, path) in simplagAnalytics) {
            val target = outputRoot.resolve("$key-analytics.json.zip")
            Files.copy(path, target)
        }

        // Submission ID listings
        val bean = Json.createObjectBuilder()
            .add("realSubmissions", Json.createArrayBuilder(realSubmissions).build())
            .add("syntheticSubmissions", Json.createArrayBuilder(syntheticSubmissions).build())
            .build()

        JsonSerialiser.serialise(bean, outputRoot.resolve("submissions.json"))

        // Scores
        saveEvaluationBeans(outputRoot)

        // Durations
        val durBean = Json.createObjectBuilder()
            .add("pairwise",
                Json.createArrayBuilder()
                    .apply {
                        for ((tool, duration) in pairwiseDurations) {
                            this.add(
                                Json.createObjectBuilder()
                                    .add("tool", tool)
                                    .add("duration", duration)
                                    .build()
                            )
                        }
                    }.build()
            )
            .add("filewise",
                Json.createArrayBuilder()
                    .apply {
                        for ((tool, duration) in filewiseDurations) {
                            this.add(
                                Json.createObjectBuilder()
                                    .add("tool", tool)
                                    .add("duration", duration)
                                    .build()
                            )
                        }
                    }.build()
            )
            .build()

        JsonSerialiser.serialise(durBean, outputRoot.resolve("durations.json"))
    }

    private fun saveEvaluationBeans(outputRoot: Path) {
        val tools = pairwiseScores.keys
        for (tool in tools) {
            val pairwise = pairwiseScores[tool]!!
            val filewise = filewiseScores[tool]!!

            val ids = mutableSetOf<String>()
            val pairwiseResults = mutableListOf<PairwiseComparisonResult>()

            for ((lhs, rhs, score) in pairwise) {
                ids.add(lhs)
                ids.add(rhs)

                val filewiseResults = filewise.filter { it.first == lhs && it.second == rhs || it.first == lhs && it.second == rhs }
                    .flatMap { it.third }
                    .map { FileComparisonResult(it.first, it.second, it.third) }

                val pairwise = PairwiseComparisonResult (
                    lhs,
                    rhs,
                    filewiseResults,
                    score
                )
                pairwiseResults.add(pairwise)
            }

            val resultBean = BatchEvaluationResult (
                tool,
                ids,
                pairwiseResults
            )

            STRFSerialiser.serialise(resultBean, outputRoot.resolve("scores-$tool.strf"))
        }
    }

    fun addPairwiseDuration(id: String, seconds: Long) {
        pairwiseDurations[id] = seconds
    }

    fun addFilewiseDuration(id: String, seconds: Long) {
        filewiseDurations[id] = seconds
    }
}