package me.haydencheers.prep.scripts.ev2

import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.JsonSerialiser
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import java.lang.StringBuilder
import java.nio.file.Files
import javax.json.JsonArray
import javax.json.JsonObject
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

data class Score (
    val ds: String,
    val submission: String,
    val label: String,
    val variantLevel: String,
    val tool: String,
    val score: Double,
    val transformationApplications: Int,
    val transformations: Set<String>
)

object EV2RandomAnalysis {
    @JvmStatic
    fun main(args: Array<String>) {
        val tex = mutableMapOf<String, String>()

        val workingDir = Config.EV2_WORKING_ROOT.resolve("All")
        val variantRoot = workingDir.resolve("work")
        val resultsDir = workingDir.resolve("out")

        val appliedTransformationsScores = mutableListOf<Score>()

        for (tool in Config.TOOLS) {
            println(tool)

            for ((pname, _) in Config.VARIANT_LEVELS) {
                var min = 100.0
                var max = 0.0
                var accumulator = 0.0
                var counter = 0L
                var taccumulator = 0.0
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
                        val scoreFile = submission.resolve("${pname}/scores-${tool}.strf")

                        val analytics = variantRoot.resolve("${datasetName}/${submission.fileName}/${pname}/generated-0/analytics.json.zip")

                        if (Files.exists(scoreFile)) {
                            val strf = STRFSerialiser.deserialise(scoreFile, BatchEvaluationResult::class)
                            val analytics = JsonSerialiser.deserialiseCompressed(analytics, JsonObject::class)

                            val transformationTypes = analytics.entries.filter { (it.value as JsonArray).size > 0 }
                            val transformationsApplied = transformationTypes.map { (it.value as JsonArray).size }.sum()

                            taccumulator = ((taccumulator*counter) + transformationsApplied) / (counter + 5)

                            val transformationNames = transformationTypes.map { it.key }.toSet()

                            val scores = strf.comparisons.groupBy { setOf(it.lhs, it.rhs) }
                                .filter { it.value.size >= 2 }
                                .map { it.value.maxBy { it.similarity }!! }

                            for (score in scores) {

                                if (score.similarity == 0.0)
                                    Unit

                                accumulator = ((accumulator*counter) + score.similarity) / ++counter
                                min = min(min, score.similarity)
                                max = max(max, score.similarity)
                                sd.increment(score.similarity)

                                appliedTransformationsScores.add(
                                    Score(datasetName, submission.fileName.toString(), score.lhs + score.rhs, pname, tool, score.similarity, transformationsApplied, transformationNames)
                                )
                            }
                        }
                    }
                }

                val i = toolidx[tool]!!
                val avg = accumulator
                val stddev = sd.result

                val strb = StringBuilder()
                strb.appendln("% " + tool + " " + pname)
                strb.appendln("\\addplot[mark=*,black] coordinates { ($i,${String.format("%2.2f", avg)}) };")
                strb.appendln("\\addplot[mark=-,black] coordinates { ($i,${String.format("%2.2f", max)})($i,${String.format("%2.2f", min)}) };")
                strb.appendln("\\addplot[mark=*,red] coordinates { ($i,${String.format("%2.2f", min(avg+stddev, 100.0))}) };")
                strb.appendln("\\addplot[mark=*,red] coordinates { ($i,${String.format("%2.2f", max(avg-stddev, 0.0))}) };")

                tex["$tool%%$pname"] = strb.toString()

                println("${pname}\t${accumulator.format("2.2f")}\t${min.format("2.2f")}\t${max.format("2.2f")}\t${sd.result.format("2.2f")}\t${taccumulator.format("2.2f")}")
            }

            println()
        }

//        tex.toList()
//            .groupBy { it.first.split("%%")[1] }    // Group by pname
//            .forEach {
//                println(it.key)
//
//                it.value.forEach {
//                    println(it.second)
//                }
//            }

//        val groupedByTool = appliedTransformationsScores
//            .groupBy { it.tool }
//            .toList()
//
//        val sortedDescending = appliedTransformationsScores.sortedByDescending { it.score }
//
//        println()
//        println("Top 5")
//        groupedByTool.forEach {
//            println(it.first)
//            val descScore = it.second.sortedBy { it.score }
//                .filter { it.transformations.size > 1 && it.transformations.size < 16 }
//                .take(20)
//
//            descScore.forEach {
//                println("\t" + it.score.format("2.2f") + "\t" + it.transformations.joinToString(", "))
//            }
//        }

//        println("Bottom 5")
//        groupedByTool.forEach {
//            println(it.first)
//            val descScore = it.second.sortedByDescending { it.score }
//                .filter { it.transformations.size > 1 }
//                .take(5)
//
//            descScore.forEach {
//                println("\t" + it.score.format("2.2f") + "\t" + it.transformations.joinToString(", "))
//            }
//        }
    }
}