package me.haydencheers.prep.scripts.ev3.script

import me.haydencheers.prep.scripts.ev2.format
import me.haydencheers.prep.scripts.ev2.toolidx
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

object EV3AllBenchmarkAnalysisScript {
    @JvmStatic
    fun main(args: Array<String>) {
        val tex = mutableMapOf<String, String>()

        val outroot = Paths.get("/media/haydencheers/Data/EV3Results/All")
        val results = Files.list(outroot)
            .filter { !Files.isHidden(it) && it.fileName.toString().endsWith(".json") }

        // pname::toolname::ds::submission.json
        val byTool = results.collect(Collectors.groupingBy { path: Path -> path.fileName.toString().split("::")[1] })

        byTool.toList().sortedBy { it.first }.forEach { (tool, toolfiles) ->
//            println(tool)

            val byPLevel = toolfiles.stream()
                .collect(Collectors.groupingBy { path: Path -> path.fileName.toString().split("::")[0] })

            byPLevel.forEach { (pname, pfiles) ->
                var min = 100.0
                var max = 0.0
                var accumulator = 0.0
                var counter = 0L
                var sd = StandardDeviation()

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

                        val sim = obj.getJsonNumber("third")?.doubleValue()?.absoluteValue
                        if (sim != null) {
                            accumulator = ((accumulator * counter) + sim) / (++counter)
                            min = min(min, sim)
                            max = max(max, sim)
                            sd.increment(sim)
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

                println("${tool}\t${pname}\t${accumulator.format("2.2f")}\t${min.format("2.2f")}\t${max.format("2.2f")}\t${sd.result.format("2.2f")}")
            }
        }

        println()
        tex.toList()
            .groupBy { it.first.split("%%")[1] }    // Group by pname
            .forEach {
                println(it.key)

                it.value.forEach {
                    println(it.second)
                }
            }
    }

    fun readArray(path: Path): JsonArray {
        return Files.newBufferedReader(path).use {
            Json.createReader(it).readArray()
        }
    }
}