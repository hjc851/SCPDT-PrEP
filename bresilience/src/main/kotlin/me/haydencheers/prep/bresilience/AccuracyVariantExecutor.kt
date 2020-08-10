package me.haydencheers.prep.bresilience

import me.haydencheers.scpdt.jplag.JPlagSCPDT
import me.haydencheers.scpdt.naive.graph.NaivePDGEditDistanceSCPDT
import me.haydencheers.scpdt.plaggie.PlaggieSCPDT
import me.haydencheers.scpdt.sherlocksydney.SherlockSydneySCPDT
import me.haydencheers.scpdt.sherlockwarwick.SherlockWarwickSCPDT
import me.haydencheers.scpdt.sim.SimWineSCPDTool
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object AccuracyVariantExecutor {

    val tools = listOf(
        SherlockSydneySCPDT(),
        SherlockWarwickSCPDT(),
        JPlagSCPDT(),
        SimWineSCPDTool(),
        PlaggieSCPDT(),
        NaivePDGEditDistanceSCPDT()
    )

    val ds_names = listOf(
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

    val base_root = Paths.get("/media/haydencheers/Data/SymbExec/src")
    val variant_root = Paths.get("/media/haydencheers/Data/SymbExec/variant_src")

    val results_out = Paths.get("/media/haydencheers/Data/SymbExec/variant_results_ac")

    @JvmStatic
    fun main(args: Array<String>) {
        val MAX_PARALLEL = 64
        val sem = Semaphore(MAX_PARALLEL)

        for (tool in tools) tool.thaw()

        for (tool in tools) {
            for (ds in ds_names) {
                val ds_variant_root = variant_root.resolve(ds)
                val ds_base_root = base_root.resolve(ds)
                val ds_results = results_out.resolve("$ds-${tool.id}.txt")

                if (!Files.exists(ds_variant_root)) {
                    System.err.println("DS ${ds} variants not found")
                    continue
                }

                if (!Files.exists(ds_base_root)) {
                    System.err.println("DS ${ds} bases not found")
                    continue
                }

                val sims = ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>()

                val variant_bases = Files.list(ds_variant_root)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                for (variant_base in variant_bases) {
                    val base_id = variant_base.fileName.toString()
                    val base_path = ds_base_root.resolve(base_id)

                    if (!Files.exists(base_path)) {
                        System.err.println("Cannot find ${ds} ${base_id}")
                        continue
                    }

                    val variants = collectVariants(variant_base)

                    for (variant in variants) {
                        sem.acquire()
                        CompletableFuture.runAsync {
                            val lsim = tool.evaluatePairwise(base_path, variant)
                            val rsim = tool.evaluatePairwise(variant, base_path)

                            val level = variant.parent.parent.fileName.toString().removePrefix("L")
                            val tchance = variant.parent.fileName.toString().removePrefix("T")
                            val vid = variant.fileName.toString().removePrefix("V")

                            val lid = base_id
                            val rid = base_id + "-T${tchance}-L${level}-${vid}"

                            sims.getOrPut(lid) { ConcurrentHashMap() }.put(rid, lsim)
                            sims.getOrPut(rid) { ConcurrentHashMap() }.put(lid, rsim)

                            println("Finished $ds ${lid} - ${rid}")

                        }.whenComplete { void, throwable ->
                            throwable?.printStackTrace(System.err)
                            sem.release()
                        }
                    }
                }

                sem.acquire(MAX_PARALLEL)
                sem.release(MAX_PARALLEL)

                Files.newBufferedWriter(ds_results).use {
                    for ((lid, rscores) in sims) {
                        for ((rid, sim) in rscores) {
                            it.appendln("${lid}\t${rid}\t${sim}")
                        }
                    }
                }

                println("Finished $ds - ${tool.id}")
            }
        }

        for (tool in tools) tool.close()

        println("Finished")
        System.exit(0)
    }

    private fun collectVariants(root: Path): List<Path> {
        val variants = ArrayList<Path>(150)

        for (l in 1 .. 5) {
            for (t in arrayOf(20, 40, 60, 80, 100)) {
                for (v in 1 .. 10) {
                    val variant = root.resolve("L${l}")
                        .resolve("T${t}")
                        .resolve("V${v}")

                    if (Files.exists(variant)) {
                        variants.add(variant)
                    } else {
                        System.err.println("Expecting variant ${variant}")
                    }
                }
            }
        }

        return variants
    }
}