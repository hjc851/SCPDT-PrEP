package me.haydencheers.prep.clustering.generation

import me.haydencheers.clustering.datasetNames
import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.JsonSerialiser
import java.nio.file.Paths
import javax.json.JsonArray
import javax.json.JsonObject

object GenerationAvgTransformCount {
    @JvmStatic
    fun main(args: Array<String>) {
        val variantRoot = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV2-Datasets/1595204234")

        val g1weight = 3*3
        val g2weight = 3*3*3

        for ((pname, pvalue) in Config.VARIANT_LEVELS) {

            var accumulator = 0.0
            var counter = 0

            for (ds in datasetNames) {

                val dspnamevariants = variantRoot.resolve(ds).resolve(pname)

                val gen1variantpath = dspnamevariants.resolve("gen-0").resolve("analytics.json.zip")
                val gen1variantanalytics = JsonSerialiser.deserialiseCompressed(gen1variantpath, JsonObject::class)

                val gen2variantpath = dspnamevariants.resolve("gen-1").resolve("analytics.json.zip")
                val gen2variantanalytics = JsonSerialiser.deserialiseCompressed(gen2variantpath, JsonObject::class)

                for (value in gen1variantanalytics.values) {
                    value as JsonArray

                    if (value.size > 0) {
                        accumulator += value.size
                    }
                }

                for (value in gen2variantanalytics.values) {
                    value as JsonArray

                    if (value.size > 0) {
                        accumulator += value.size
                    }
                }

                counter += g1weight
                counter += g2weight

                println("${pname}\t${accumulator / counter}")
            }
        }
    }
}