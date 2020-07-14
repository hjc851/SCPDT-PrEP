package me.haydencheers.prep.scripts.simplag

import me.haydencheers.prep.util.JsonSerialiser
import java.nio.file.Paths
import javax.json.JsonObject

object AnalyticsPrinter {
    @JvmStatic
    fun main(args: Array<String>) {
        val path = Paths.get("/media/haydencheers/Data/PrEP/EV2/Isolated5/work/SENG1110_A1_2017/c3059877_attempt_2017-04-16-08-54-48/p6/generated-0/analytics.json.zip")
        val json = JsonSerialiser.deserialiseCompressed(path, JsonObject::class)

        for ((key, value) in json) {
            if (value.asJsonArray().isNotEmpty()) {
                println("${key} - ${value.asJsonArray().size}")
            }
        }
    }
}