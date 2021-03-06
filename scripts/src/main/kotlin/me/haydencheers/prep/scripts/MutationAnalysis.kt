package me.haydencheers.prep.scripts

import me.haydencheers.prep.util.JsonSerialiser
import java.nio.file.Paths
import javax.json.JsonObject

object MutationAnalysis {
    @JvmStatic
    fun main(args: Array<String>) {
        val ds = "SENG1110_A1_2017"
        val sub = "c3182914_attempt_2017-04-19-09-39-28"
        val pname = "p6"
        val trans = "2"

//        val root = Config.EV3_WORKING_ROOT
//            .resolve("Isolated/work")
//            .resolve(ds)
//            .resolve(sub)
//            .resolve(pname)
//            .resolve(trans)
//            .resolve("generated-0")
//            .resolve("analytics.json.zip")

        val root = Paths.get("/media/haydencheers/Data/SimPlag/out/analytics.json.zip")

        val analytics = JsonSerialiser.deserialiseCompressed(root, JsonObject::class)

        analytics.filter { it.value.asJsonArray().isNotEmpty() }
            .forEach {
                println("${it.key} - ${it.value.asJsonArray().size}")
            }

        Unit
    }
}