package me.haydencheers.prep.scripts.ev3.script

import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.JsonSerialiser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.json.JsonArray
import javax.json.JsonObject

object EV3AllDataSetAnalysis {
    @JvmStatic
    fun main(args: Array<String>) {
        val outroot = Paths.get("/media/haydencheers/Data/SimPlag/out/all")

        for ((pname, _) in Config.VARIANT_LEVELS) {
            val root = outroot.resolve(pname)

            val nonEmptyCount = Files.list(root)
                .filter { Files.isDirectory(it) && Files.list(it).count() > 0L }
                .count()

            val analyticsFile = root.resolve("analytics.json.zip")
            val analytics = JsonSerialiser.deserialiseCompressed(analyticsFile, JsonObject::class)

            val injectCount = analytics.values.map { it.asJsonArray().size }
                .sum()

            val avgInjectCount = injectCount.toDouble() / nonEmptyCount

            println("$pname\t$injectCount\t$avgInjectCount")

            Unit
        }


    }
}