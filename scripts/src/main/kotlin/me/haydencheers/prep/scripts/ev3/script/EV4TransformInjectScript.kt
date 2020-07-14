package me.haydencheers.prep.scripts.ev3.script

import me.haydencheers.prep.normalise.Forker
import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
import me.haydencheers.scpdt.AbstractJavaSCPDTool
import me.haydencheers.scpdt.SCPDTool
import me.haydencheers.scpdt.jplag.JPlagSCPDT
import me.haydencheers.scpdt.naive.graph.NaivePDGEditDistanceSCPDT
import me.haydencheers.scpdt.naive.string.NaiveStringEditDistanceSCPDT
import me.haydencheers.scpdt.naive.string.NaiveStringTilingSCPDT
import me.haydencheers.scpdt.naive.token.NaiveTokenEditDistanceSCPDT
import me.haydencheers.scpdt.naive.token.NaiveTokenTilingSCPDT
import me.haydencheers.scpdt.naive.tree.NaiveTreeEditDistanceSCPDT
import me.haydencheers.scpdt.plaggie.PlaggieSCPDT
import me.haydencheers.scpdt.sherlocksydney.SherlockSydneySCPDT
import me.haydencheers.scpdt.sherlockwarwick.SherlockWarwickSCPDT
import me.haydencheers.scpdt.sim.SimWineSCPDTool
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.json.Json
import kotlin.Comparator
import kotlin.streams.toList

object EV4TransformInjectScript {
    val MAX_PARALLEL = 32
    val sem = Semaphore(MAX_PARALLEL)
    val exec = Executors.newFixedThreadPool(MAX_PARALLEL)

    fun produceBindings(): List<SCPDTool> {
        val tools = mutableListOf<SCPDTool>()

//        tools.add(JPlagSCPDT())
//        tools.add(PlaggieSCPDT())
//        tools.add(SimWineSCPDTool())
//        tools.add(SherlockWarwickSCPDT())
//        tools.add(SherlockSydneySCPDT())
//        tools.add(NaiveStringEditDistanceSCPDT())
//        tools.add(NaiveStringTilingSCPDT())
//        tools.add(NaiveTokenEditDistanceSCPDT())
//        tools.add(NaiveTokenTilingSCPDT())
        tools.add(NaiveTreeEditDistanceSCPDT())
//        tools.add(NaivePDGEditDistanceSCPDT())

        return tools
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val sroot = Paths.get("/media/haydencheers/Data/SimPlag/src")
        val vroot = Paths.get("/media/haydencheers/Data/SimPlag/out/transform_inject")
        val outroot = Paths.get("/media/haydencheers/Data/EV4Results")

        AbstractJavaSCPDTool.mxHeap = "3000M"
        val tools = produceBindings()

        val sources = Files.list(sroot)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (tool in tools) {
            tool.thaw()

            for ((pname, _) in Config.VARIANT_LEVELS) {
                val progroot = vroot.resolve(pname)

                val variantprogs = Files.list(progroot)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                val collections = mutableMapOf<String, Pair<Path, MutableList<Path>>>()

                for (source in sources) {
                    if (Files.walk(source).filter { it.fileName.toString().endsWith(".java") }.count() == 0L)
                        continue

                    val id = source.fileName.toString()
                    collections[id] = Pair(source, mutableListOf())
                }

                for (variant in variantprogs) {
                    if (Files.walk(variant).filter { it.fileName.toString().endsWith(".java") }.count() == 0L)
                        continue

                    val id = variant.fileName.toString().split("-")
                        .dropLast(1)
                        .joinToString("-")

                    collections.get(id)?.second?.add(variant)
                }

                for ((key, value) in collections) {
                    val (original, variants) = value

                    // Copy all to a single directory
                    val tmp = Files.createTempDirectory("ev3-tmp")
                    FileUtils.copyDirOnlyJava(original, tmp.resolve(original.fileName.toString().replace("-", "_")))
                    for (variant in variants) {
                        FileUtils.copyDirOnlyJava(variant, tmp.resolve(variant.fileName.toString().replace("-", "_")))
                    }

                    val projs = mutableListOf<Path>()
                    projs.add(original)
                    projs.addAll(variants)

                    // Get the permit to run
                    sem.acquire()

                    // Spin off
                    CompletableFuture.runAsync(Runnable {
                        val foutp = outroot.resolve("$pname::${tool.id}::$key.json")
                        if (Files.exists(foutp)) return@Runnable

                        try {
                            val jsonresult = Json.createObjectBuilder()

                            val sresult = tool.evaluateSubmissions(tmp, executor = exec)
                            jsonresult.add("submissions", Json.createArrayBuilder().apply {
                                sresult.forEach {
                                    this.add(
                                        Json.createObjectBuilder()
                                            .add("lhs", it.first)
                                            .add("rhs", it.second)
                                            .add("similarity", it.third)
                                            .build()
                                    )
                                }
                            }.build())

                            val fresultjson = Json.createArrayBuilder()
                            for (l in 0 until projs.size) {
                                val lproj = projs[l]
                                for (r in l+1 until projs.size) {
                                    val rproj = projs[r]

                                    val fresult = tool.evaluateAllFiles(lproj, rproj)
                                    fresultjson.add(
                                        Json.createObjectBuilder()
                                            .add("lhs", lproj.fileName.toString())
                                            .add("rhs", rproj.fileName.toString())
                                            .add("scores", Json.createArrayBuilder()
                                                    .apply {
                                                        fresult.forEach {
                                                            add(
                                                                Json.createObjectBuilder()
                                                                    .add("lhs", it.first)
                                                                    .add("rhs", it.second)
                                                                    .add("similarity", it.third)
                                                                    .build()
                                                            )
                                                        }
                                                    }
                                                    .build()
                                            )
                                            .build()
                                    )
                                }
                            }
                            jsonresult.add("files", fresultjson)
                            JsonSerialiser.serialise(jsonresult.build(), foutp)
                            println("${Date()}\t$pname::${tool.id}::$key")

                        } catch (e: Exception) {
                            e.printStackTrace(System.err)
                        }
                    })
                    .whenComplete { void, throwable ->
                        throwable?.printStackTrace()

                        // Return permit
                        sem.release()

                        // Disk cleanup
                        try {
                            Files.walk(tmp)
                                .sorted(Comparator.reverseOrder())
                                .forEach(Files::delete)
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        sem.acquire(MAX_PARALLEL)

        System.exit(0)
    }
}