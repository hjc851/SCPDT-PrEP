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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object EV3IsolatedScript {
    fun produceBindings(): List<SCPDTool> {
        val tools = mutableListOf<SCPDTool>()

        //tools.add(JPlagSCPDT())
        //tools.add(PlaggieSCPDT())
        //tools.add(SimWineSCPDTool())
        //tools.add(SherlockWarwickSCPDT())
        //tools.add(SherlockSydneySCPDT())
        //tools.add(NaiveStringEditDistanceSCPDT())
        //tools.add(NaiveStringTilingSCPDT())
        //tools.add(NaiveTokenEditDistanceSCPDT())
        //tools.add(NaiveTokenTilingSCPDT())
        tools.add(NaiveTreeEditDistanceSCPDT())
        //tools.add(NaivePDGEditDistanceSCPDT())

        return tools
    }

    val MAX_PARALLEL = 48
    val sem = Semaphore(MAX_PARALLEL)
    val exec = Executors.newFixedThreadPool(MAX_PARALLEL)

    @JvmStatic
    fun main(args: Array<String>) {
        val sroot = Paths.get("/media/haydencheers/Data/SimPlag/src")
        val vroot = Paths.get("/media/haydencheers/Data/SimPlag/out/isolated")
        val outroot = Paths.get("/media/haydencheers/Data/EV3Results/Isolated")

        val sources = Files.list(sroot)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        AbstractJavaSCPDTool.mxHeap = "2000M"
        val bindings = produceBindings()
        for (binding in bindings) {
            binding.thaw()
        }

        for ((pname, _) in Config.VARIANT_LEVELS) {
            for (t in 1 .. 4) {
                val progroot = vroot.resolve(pname).resolve("$t")

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
                    val id = variant.fileName.toString().split("-")
                        .dropLast(1)
                        .joinToString("-")

                    collections.get(id)?.second?.add(variant)
                }

                for ((key, value) in collections) {
                    val (original, variants) = value
                    if (variants.isEmpty()) continue

                    // Copy all to a single directory
                    val tmp = Files.createTempDirectory("ev3i-tmp")
                    FileUtils.copyDirOnlyJava(original, tmp.resolve(original.fileName.toString().replace("-", "_")))
                    for (variant in variants) {
                        FileUtils.copyDirOnlyJava(variant, tmp.resolve(variant.fileName.toString().replace("-", "_")))
                    }

                    // Get the permit to run
                    sem.acquire()

                    // Spin off
                    CompletableFuture.runAsync(Runnable {

                        for (tool in bindings) {
                            val foutp = outroot.resolve("$pname::$t::${tool.id}::$key.json")
                            if (Files.exists(foutp)) continue

                            try {
                                val result = tool.evaluateSubmissions(tmp, executor = exec)
                                JsonSerialiser.serialise(result, foutp)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            println("$pname::$t::${tool.id}::$key")
                        }

                        /*for (tool in bindings) {
                            val foutp = outroot.resolve("$pname::$t::${tool.id}::$key.json")
                            if (Files.exists(foutp)) continue

                            val work = Files.createTempDirectory("fork")
                            val out = Files.createFile(work.resolve("stdout.txt"))
                            val err = Files.createFile(work.resolve("stderr.txt"))

                            val proc = Forker.exec(
                                SCPDTForkEntryPoint::class.java,
                                arrayOf (
                                    tool.javaClass.name,
                                    tool.id,
                                    tmp.toAbsolutePath().toString()
                                ),
                                workingDir = work,
                                out = out,
                                err = err
                            )

                            if (proc.waitFor(1, TimeUnit.MINUTES)) {
                                if (proc.exitValue() == 0) {
                                    Files.copy(out, foutp)
                                    println("$pname::$t::$key::${tool.id}")
                                } else {
                                    System.err.println("Error: $pname::$key::${tool.id}")
                                }
                            } else {
                                System.err.println("Timeout: $pname::$key::${tool.id}")
                                proc.destroy()
                            }

                            Files.walk(work)
                                .sorted(Comparator.reverseOrder())
                                .forEach(Files::delete)
                        }*/
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

        for (binding in bindings) {
            binding.close()
        }

        System.exit(0)
    }
}