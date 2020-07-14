package me.haydencheers.prep.scripts.ev2

import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.JsonSerialiser
import java.lang.IllegalArgumentException
import java.nio.file.Files
import javax.json.JsonObject
import kotlin.math.roundToInt
import kotlin.streams.toList

object EV25TransformationsTransformationCounter {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = Config.EV2_WORKING_ROOT.resolve("All")
        val work = root.resolve("work")

        for ((pname, pvalue) in Config.VARIANT_LEVELS) {
            val transformations = mutableMapOf<String, Int>()
            var counter = 0.0

            for (ds in Config.DATASET_NAMES) {
                val dsroot = work.resolve(ds)

                val subs = Files.list(dsroot)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                for (sub in subs) {
                    val analytics = sub.resolve(pname)
                        .resolve("generated-0")
                        .resolve("analytics.json.zip")

                    if (Files.exists(analytics)) {
                        val analyticsBean = JsonSerialiser.deserialiseCompressed(analytics, JsonObject::class)

                        analyticsBean.filter { it.value.asJsonArray().isNotEmpty() }
                            .forEach {
                                val size = if (it.key == "mutateReorderStatements") {
                                    (it.value.asJsonArray().size * pvalue).roundToInt()
                                } else {
                                     it.value.asJsonArray().size
                                }

                                transformations[it.key] = transformations.getOrDefault(it.key, 0) + size
                            }

                        counter += 5.0
                    }
                }
            }

            println(pname)
            for ((transformation, count) in transformations) {
                println("${transformation}\t${count.div(counter).format("2.2f")}")
            }
            println("all\t${transformations.values.sum().div(counter).format("2.2f")}")
            println()
        }
    }
}