package me.haydencheers.prep.scripts.ev2

import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import java.nio.file.Files
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

object EV2RandomRandomAnalysis {
    @JvmStatic
    fun main(args: Array<String>) {
        val workingDir = Config.EV2_WORKING_ROOT.resolve("RandomRandom")
        val resultsDir = workingDir.resolve("out")

        for (tool in Config.TOOLS) {
            println(tool)

            var min = 100.0
            var max = 0.0
            var accumulator = 0.0
            var counter = 0L

            val sd = StandardDeviation()

            for (datasetName in Config.DATASET_NAMES) {
                val dsResultsRoot = resultsDir.resolve(datasetName)
                if (!Files.exists(dsResultsRoot)) continue

                val blacklist = mutableSetOf<String>()

                val dsSubmissions = Files.list(Config.DATASET_ROOT.resolve(datasetName))
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                for (dsSub in dsSubmissions) {
                    if (Files.walk(dsSub)
                            .filter { it.fileName.toString().endsWith(".java") }
                            .count() == 0L)
                        blacklist.add(dsSub.fileName.toString())
                }

                val submissions = Files.list(dsResultsRoot)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                for (submission in submissions) {
                    if (blacklist.contains(submission.fileName.toString())) continue
                    val scoreFile = submission.resolve("scores-${tool}.strf")

                    if (Files.exists(scoreFile)) {
                        val strf = STRFSerialiser.deserialise(scoreFile, BatchEvaluationResult::class)

                        val scores = strf.comparisons.groupBy { setOf(it.lhs, it.rhs) }
                            .map { it.value.maxBy { it.similarity }!! }

                        for (score in scores) {
                            accumulator = ((accumulator*counter) + score.similarity) / ++counter
                            min = min(min, score.similarity)
                            max = max(max, score.similarity)

                            sd.increment(score.similarity)

                            if (score.similarity == 0.0)
                                Unit
                        }
                    }
                }
            }

            println("${accumulator}\t${min}\t${max}\t${sd.result}")
            println()
        }
    }
}