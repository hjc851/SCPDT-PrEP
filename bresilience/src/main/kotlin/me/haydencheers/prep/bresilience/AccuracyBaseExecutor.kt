package me.haydencheers.prep.bresilience

import me.haydencheers.prep.util.FileUtils
import me.haydencheers.scpdt.jplag.JPlagSCPDT
import me.haydencheers.scpdt.naive.graph.NaivePDGEditDistanceSCPDT
import me.haydencheers.scpdt.plaggie.PlaggieSCPDT
import me.haydencheers.scpdt.sherlocksydney.SherlockSydneySCPDT
import me.haydencheers.scpdt.sherlockwarwick.SherlockWarwickSCPDT
import me.haydencheers.scpdt.sim.SimWineSCPDTool
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.streams.toList

object AccuracyBaseExecutor {
    val tools = listOf(
        /*SherlockSydneySCPDT(),
        SherlockWarwickSCPDT(),
        JPlagSCPDT(),
        SimWineSCPDTool(),
        PlaggieSCPDT(),
        NaivePDGEditDistanceSCPDT()*/

        JPlagSCPDT()
    )

    val root = Paths.get("/media/haydencheers/Data/SymbExec/src")

    val dataSets = listOf(
        "COMP2230_A1_2018",
        "COMP2240_A1_2018",
        "COMP2240_A2_2018",
        "COMP2240_A3_2018",
        "SENG1110_A1_2017",
        "SENG1110_A2_2017",
        "SENG2050_A1_2017",
        "SENG2050_A2_2017",
        "SENG2050_A1_2018",
        "SENG2050_A2_2018",
        "SENG2050_A1_2019",
        "SENG2050_A2_2019"
    )

    val out = Paths.get("/media/haydencheers/Data/SymbExec/acscores")

    @JvmStatic
    fun main(args: Array<String>) {

        val exec = Executors.newFixedThreadPool(32)

        for (tool in tools) tool.thaw()

        for (ds in dataSets) {
            val inp = Files.createTempDirectory("prep-tmp")

            val inputs = Files.list(root.resolve(ds))
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (input in inputs) {
                FileUtils.copyDir(
                    input,
                    inp.resolve(input.fileName.toString().replace("-", "_"))
                )
            }

            for (tool in tools) {
                val outf = out.resolve("${tool.id}-$ds.txt")

                val start = Instant.now()

                val sim = tool.evaluateSubmissions(inp)

                val finish = Instant.now()
                val elapsed = Duration.between(start, finish)

                Files.newBufferedWriter(outf).use { writer ->
                    for (result in sim) {
                        writer.appendln("${result.first}\t${result.second}\t${result.third}")
                    }
                }

                println("${ds}-${tool.id}")
                println("Elapsed ${elapsed.seconds}s")
                println()
            }
        }

        for (tool in tools) tool.close()
        exec.shutdownNow()
        System.exit(0)
    }
}