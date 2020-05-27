package me.haydencheers.prep.scripts.ev3

import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.scripts.ev2.format
import me.haydencheers.prep.scripts.simplag.Analytics
import me.haydencheers.prep.util.JsonSerialiser
import java.nio.file.Files
import kotlin.streams.toList

object EV3IsolatedAnalyticsAggregator {
    @JvmStatic
    fun main(args: Array<String>) {
        val workingDir = Config.EV3_WORKING_ROOT.resolve("Isolated")
        val variantRoot = workingDir.resolve("work")
        val resultsDir = workingDir.resolve("out")

        for (i in 1 .. 4) {
            println("Transformation $i")

            for ((pname, _) in Config.VARIANT_LEVELS) {
                var accumulator = 0.0
                var counter = 0

                for (datasetName in Config.DATASET_NAMES) {
                    val dsResultsRoot = resultsDir.resolve(datasetName)
                    if (!Files.exists(dsResultsRoot)) continue

                    val submissions = Files.list(dsResultsRoot)
                        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                        .use { it.toList() }

                    for (submission in submissions) {
                        val analyticsFile = variantRoot.resolve(datasetName)
                            .resolve(submission.fileName.toString())
                            .resolve(pname)
                            .resolve("$i")
                            .resolve("generated-0")
                            .resolve("analytics.json.zip")

                        if (Files.exists(analyticsFile)) {
                            val analytics = JsonSerialiser.deserialiseCompressed(analyticsFile, Analytics::class)
                            when (i) {
                                1 -> {
                                    accumulator = ((accumulator * counter) + analytics.injectedFiles.size) / (counter + 5)
                                }

                                2 -> {
                                    accumulator = ((accumulator * counter) + analytics.injectedClasses.size) / (counter + 5)
                                }

                                3 -> {
                                    accumulator = ((accumulator * counter) + analytics.injectedMethods.size) / (counter + 5)
                                }

                                4 -> {
                                    accumulator = ((accumulator * counter) + analytics.injectedStatements.size) / (counter + 5)
                                }
                            }
                            counter += 5
                        }
                    }
                }

                println("$pname ${accumulator.format("2.2f")}")
            }

            println()
        }
    }
}