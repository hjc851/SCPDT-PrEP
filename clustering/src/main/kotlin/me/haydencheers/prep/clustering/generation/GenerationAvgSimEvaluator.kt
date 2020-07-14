package me.haydencheers.prep.clustering.generation

import me.haydencheers.clustering.datasetNames
import me.haydencheers.clustering.loadVariantScoresForTool
import me.haydencheers.clustering.partitionVariantIds
import me.haydencheers.clustering.toolNames
import me.haydencheers.prep.scripts.Config
import java.nio.file.Paths

object GenerationAvgSimEvaluator {

    @JvmStatic
    fun main(args: Array<String>) {
        val variantRoot = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV1B")

        for (ds in datasetNames) {
            for (tool in toolNames) {
                for ((pname, pvalue) in Config.VARIANT_LEVELS) {
                    val scores = loadVariantScoresForTool(tool, ds, pname)
                    val partitions = partitionVariantIds(scores)

                    val g0g1scores = scores.filter { it.isIn(partitions.g0g1links) }
                    val g0g1avg = g0g1scores.map { it.score }.average()

                    val g1g2scores = scores.filter { it.isIn(partitions.g1g2links) }
                    val g1g2avg = g1g2scores.map { it.score }.average()

                    val g0g2scores = scores.filter { it.isIn(partitions.g0g2links) }
                    val g0g2avg = g0g2scores.map { it.score }.average()

                    println("$ds\t$tool\t$pname")
                    println("gen0-gen1\t${g0g1avg}")
                    println("gen1-gen2\t${g1g2avg}")
                    println("gen0-gen2\t${g0g2avg}")
//                    println("gen1-sib\t")
//                    println("gen2-sib\t")
//                    println("gen2-all\t")
                    println("------------------------")
                    println()
                }
            }
        }
    }
}