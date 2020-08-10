package me.haydencheers.prep.bresilience

import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
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
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.streams.toList

object ResileinceExecutor {

    val max_parallel = 48
    val exec = Executors.newFixedThreadPool(max_parallel)

    val root = Paths.get("/media/haydencheers/Data/SymbExec/src")
    val score_out = root.parent.resolve("acscores")

    val ds = "Collected_60"

    val collectedBases = listOf(
        "P1",
        "P2",
        "P3",
        "P4",
        "P5"
    )

    val algorithmsBases = listOf(
        "CHANGEMAKING_DP_1",
        "MST_KRUSKALS_2",
        "SORT_BUBBLE_1",
        "SORT_QUICKSORT_4",
        "CHANGEMAKING_DP_2",
        "MST_KRUSKALS_3",
        "SORT_BUBBLE_2",
        "STRINGMATCH_BM_1",
        "CHANGEMAKING_ITER_1",
        "MST_PRIMMS_1",
        "SORT_MERGE_1",
        "STRINGMATCH_BM_2",
        "CHANGEMAKING_ITER_2",
        "MST_PRIMMS_2",
        "SORT_MERGE_2",
        "STRINGMATCH_KMP_1",
        "CHANGEMAKING_REC_1",
        "MST_PRIMMS_3",
        "SORT_QUICKSORT_1",
        "STRINGMATCH_KMP_4",
        "CHANGEMAKING_REC_2",
        "MST_REVERSEDELETE_1",
        "SORT_QUICKSORT_2",
        "STRINGMATCH_RK_1",
        "MST_KRUSKALS_1",
        "MST_REVERSEDELETE_2",
        "SORT_QUICKSORT_3",
        "STRINGMATCH_RK_2"
    )

    val ds_bases = mapOf(
        "Collected_20" to collectedBases,
        "Collected_40" to collectedBases,
        "Collected_60" to collectedBases,
        "Algorithms_20" to algorithmsBases,
        "Algorithms_40" to algorithmsBases,
        "Algorithms_60" to algorithmsBases
    )

    val tools = listOf(
//        SherlockSydneySCPDT()
//        ,
//        SherlockWarwickSCPDT()
//        ,
        JPlagSCPDT()
//        ,
//        SimWineSCPDTool()
//        ,
//        PlaggieSCPDT()
//        ,
//        NaivePDGEditDistanceSCPDT()
    )

    fun sanitise(name: String): String {
        return name.replace('-', '_')
    }

    data class ProjectComparison (
        val lhs: String,
        val rhs: String,
        val similarity: Double
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val dsroot = root.resolve(ds)
        val dsbases = ds_bases[ds]!!

        val scoreout = score_out.resolve(ds)
        if (!Files.exists(scoreout)) Files.createDirectories(scoreout)

        for (tool in tools) tool.thaw()

        val sources = Files.list(dsroot)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .toList()

        for (tool in tools) {
            println(tool.id)
            val start = Instant.now()
            println("Started ${Date()}")

            val sem = Semaphore(max_parallel)

            val scoref = scoreout.resolve("scores-${tool.id}.json")
            if (Files.exists(scoref))
                continue

            val results = Collections.synchronizedList(mutableListOf<ProjectComparison>())
            val tmp = Files.createTempDirectory("ac_dsexec_tmp")

            for (base in dsbases) {
                val comps = sources.filter { it.fileName.toString().startsWith(base) && it.fileName.toString() != base }

                val lproj = tmp.resolve(sanitise(base))
                FileUtils.copyDirOnlyJava(dsroot.resolve(base), lproj)

                for (comp in comps) {
                    sem.acquire()

                    val rproj = tmp.resolve(sanitise(comp.fileName.toString()))
                    FileUtils.copyDirOnlyJava(comp, rproj)

                    CompletableFuture.runAsync {
                        val sim = tool.evaluatePairwise(lproj, rproj)

                        results.add(
                            ProjectComparison(
                                base, comp.fileName.toString(), sim
                            )
                        )

//                        println("${base} - ${comp.fileName}")

                    }.whenComplete { void, throwable ->
                        throwable?.printStackTrace()
                        sem.release()
//                        System.err.println("${base} - ${comp.fileName}")
                    }
                }
            }

            sem.acquire(max_parallel)

            val finish = Instant.now()
            val elapsed = Duration.between(start, finish)

            println("Finished ${Date()}")
            println("Elapsed: ${elapsed.seconds}s")

            JsonSerialiser.serialise(results, scoref)

            Files.walk(tmp)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }

        for (tool in tools) tool.close()

        println("Done")
        System.exit(0)
    }
}