package me.haydencheers.prep.clustering.generation

import me.haydencheers.clustering.JsonSerialiser
import me.haydencheers.clustering.datasetNames
import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.PrEPConfig
import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.scpdt.jplag.JPlagSCPDT
import me.haydencheers.scpdt.naive.graph.NaivePDGEditDistanceSCPDT
import me.haydencheers.scpdt.plaggie.PlaggieSCPDT
import me.haydencheers.scpdt.sherlocksydney.SherlockSydneySCPDT
import me.haydencheers.scpdt.sherlockwarwick.SherlockWarwickSCPDT
import me.haydencheers.scpdt.sim.SimWineSCPDTool
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.beans.FileComparisonResult
import me.haydencheers.strf.beans.PairwiseComparisonResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.Comparator
import kotlin.streams.toList

object GenerationDataSetFilewisePrEPExecutor {

    val dsroot = Config.DATASET_ROOT

    val scoreroot = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV1B")
    val root = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV1B-Filewise")

    val variantroot = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV1B-Datasets/2-generation")

    val GENERATIONS = 2

    val exec = Executors.newFixedThreadPool(64)

    @JvmStatic
    fun main(args: Array<String>) {

        val tools = listOf(
            SherlockSydneySCPDT()
            ,
            SherlockWarwickSCPDT()
            ,
            JPlagSCPDT()
            ,
            SimWineSCPDTool()
            ,
            PlaggieSCPDT()
            ,
            NaivePDGEditDistanceSCPDT()
        )

        for (tool in tools) tool.thaw()

        val outroot = root
        val config = makeConfig()

        for (ds in datasetNames) {
            val roots = Files.newBufferedReader(variantroot.resolve(ds).resolve("roots.txt"))
                .use {
                    it.readLines()
                }

            for ((pname, pvalue) in Config.VARIANT_LEVELS) {
                val work = Files.createDirectories(outroot.resolve(ds).resolve(pname))

                for (tool in tools) {
                    val scoref = work.resolve("out").resolve("scores-${tool.id}.json")
                    if (Files.exists(scoref))
                        continue

                    val psrc = work.resolve("src")
                    val pout = work.resolve("out")

                    if (!Files.exists(psrc)) {
                        Files.createDirectories(psrc)

                        // Copy roots to src
                        val dsdir = Config.DATASET_ROOT.resolve(ds)
                        for (root in roots) {
                            FileUtils.copyDirOnlyJava(dsdir.resolve(root), psrc.resolve(root))
                        }

                        // Copy variants to src
                        val vardir = variantroot.resolve(ds).resolve(pname)
                        for (i in 0 until GENERATIONS) {
                            val genroot = vardir.resolve("gen-$i")
                            val variants = Files.list(genroot)
                                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                                .use { it.toList() }

                            for (variant in variants) {
                                FileUtils.copyDirOnlyJava(genroot.resolve(variant.fileName.toString()), psrc.resolve(variant.fileName.toString()))
                            }
                        }

                        val all = Files.list(psrc)
                            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                            .use { it.toList() }

                        for (sub in all) {
                            val sanitised = sub.fileName.toString().replace('-', '_')
                            Files.move(sub, sub.parent.resolve(sanitised))
                        }
                    }

                    if (!Files.exists(pout)) Files.createDirectory(pout)

                    println("Starting - $ds $pname")

                    val allProgs = Files.list(psrc)
                        .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                        .use { it.toList() }

                    println("\t${tool.id}")

                    data class ProjectFilewiseComparison (
                        val lhs: String,
                        val rhs: String,
                        val fcomps: List<FileComparisonResult>
                    )

                    val allComparisons = Collections.synchronizedList(mutableListOf<ProjectFilewiseComparison>())

                    val sem = Semaphore(64)
                    for (l in 0 until allProgs.size) {
                        val lproj = allProgs[l]
                        for (r in l+1 until allProgs.size) {
                            val rproj = allProgs[r]

                            while (!sem.tryAcquire(1, 10, TimeUnit.SECONDS)) {
                                println("\t${Date()} Awaiting permit ...")
                            }

                            CompletableFuture.runAsync {
                                val fsims = tool.evaluateAllFiles(lproj, rproj)

                                val fcomps = fsims.map { FileComparisonResult(it.first, it.second, it.third) }
                                val projcomp = ProjectFilewiseComparison(lproj.fileName.toString(), rproj.fileName.toString(), fcomps)
                                allComparisons.add(projcomp)

                            }.whenComplete { void, throwable ->
                                throwable?.printStackTrace()
                                sem.release()
                            }
                        }
                    }

                    while (!sem.tryAcquire(64, 10, TimeUnit.SECONDS)) {
                        println("\t${Date()} Awaiting ${64-sem.availablePermits()} permits")
                    }

                    JsonSerialiser.serialise(allComparisons, scoref)
                }
            }
        }

        for (tool in tools) tool.close()

        println("Done")
        System.exit(0)
    }

    private fun makeConfig(): PrEPConfig {
        return PrEPConfig().apply {
            submissionRoot = "src"
            outputRoot = "out"
            workingRoot = "work"

            submissionsArePlaintiff = false
            executeFilewise = false

            randomSeed = 11121993
            seeding = null

            detection = DetectionConfig().apply {
                maxParallelism = 64
                mxHeap = "2000M"

                useJPlag = true
                usePlaggie = true
                useSim = true
                useSherlockSydney = true
                useSherlockWarwick = true

                useNaiveStringEditDistance = false
                useNaiveStringTiling = false
                useNaiveTokenEditDistance = false
                useNaiveTokenTiling = false
                useNaiveTreeEditDistance = false
                useNaivePDGEditDistance = true
            }
        }
    }
}