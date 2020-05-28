package me.haydencheers.prep.scripts.ev3.script

import me.haydencheers.prep.scripts.ev2.format
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

object EV3AllAnalysisScript {
    @JvmStatic
    fun main(args: Array<String>) {
        val outroot = Paths.get("/media/haydencheers/Data/EV3Results/All")
        val results = Files.list(outroot)
            .filter { !Files.isHidden(it) && it.fileName.toString().endsWith(".json") }

        // pname::toolname::ds::submission.json
        val byTool = results.collect(Collectors.groupingBy { path: Path -> path.fileName.toString().split("::")[1] })

        byTool.toList().sortedBy { it.first }.forEach { (tool, toolfiles) ->
            println(tool)

            val byPLevel = toolfiles.stream()
                .collect(Collectors.groupingBy { path: Path -> path.fileName.toString().split("::")[0] })

            byPLevel.forEach { (pname, pfiles) ->
                var accumulator = 0.0
                var counter = 0

                for (file in pfiles) {
                    val json = readArray(file)

                    val components = file.fileName.toString()
                        .removeSuffix(".json")
                        .split("::")

                    val key = components[2] + "::" + components[3].replace("-", "_")

                    val jsonvalues = json.filter { obj -> obj as JsonObject; obj.getString("first").equals(key).xor(obj.getString("second").equals(key)) }
                        .groupBy { obj -> obj as JsonObject; setOf(obj.getString("first"), obj.getString("second")) }
                        .map { it.value.maxBy { obj -> obj as JsonObject; obj.getJsonNumber("third")?.doubleValue() ?: 0.0 }!! }

                    for (obj in jsonvalues) {
                        obj as JsonObject

                        val sim = obj.getJsonNumber("third")?.doubleValue()
                        if (sim != null) {
                            accumulator = ((accumulator * counter) + sim) / (++counter)
                        }
                    }
                }

                println("${pname} ${accumulator.format("2.2f")}")
            }
        }
    }

    fun readArray(path: Path): JsonArray {
        return Files.newBufferedReader(path).use {
            Json.createReader(it).readArray()
        }
    }
}