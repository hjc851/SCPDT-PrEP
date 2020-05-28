package me.haydencheers.prep.scripts.ev3.script

import me.haydencheers.prep.normalise.Forker
import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
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
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object EV3AllForkingScript {
    val MAX_PARALLEL = 32
    val sem = Semaphore(MAX_PARALLEL)

    fun produceBindings(): List<SCPDTool> {
        val tools = mutableListOf<SCPDTool>()

        tools.add(JPlagSCPDT())
        tools.add(PlaggieSCPDT())
        tools.add(SimWineSCPDTool())
        tools.add(SherlockWarwickSCPDT())
        tools.add(SherlockSydneySCPDT())
        tools.add(NaiveStringEditDistanceSCPDT())
        tools.add(NaiveStringTilingSCPDT())
        tools.add(NaiveTokenEditDistanceSCPDT())
        tools.add(NaiveTokenTilingSCPDT())
        tools.add(NaiveTreeEditDistanceSCPDT())
        tools.add(NaivePDGEditDistanceSCPDT())

        return tools
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val sroot = Paths.get("/media/haydencheers/Data/SimPlag/src")
        val vroot = Paths.get("/media/haydencheers/Data/SimPlag/out/all")
        val outroot = Paths.get("/media/haydencheers/Data/EV3Results/All")

        val tools = produceBindings()

        val sources = Files.list(sroot)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

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

                // Get the permit to run
                sem.acquire()

                // Spin off
                CompletableFuture.runAsync(Runnable {
                    for (tool in tools) {
                        val foutp = outroot.resolve("$pname::${tool.id}::$key.json")
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

                        if (proc.waitFor(10, TimeUnit.SECONDS)) {
                            if (proc.exitValue() == 0) {
                                Files.copy(out, foutp)
                                println("$pname::$key::${tool.id}")
                            } else {
                                System.err.println("Error: $pname::$key::${tool.id}")
                            }
                        } else {
                            System.err.println("Timeout: $pname::$key::${tool.id}")
                            proc.destroy()

                            if (tool.id.contains("Sim")) {
                                Unit
                            }
                        }

                        Files.walk(work)
                            .sorted(Comparator.reverseOrder())
                            .forEach(Files::delete)
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

        sem.acquire(MAX_PARALLEL)
    }
}

object SCPDTForkEntryPoint {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 3) {
            throw IllegalStateException("Usage: AbstractForker <binding clsname> <tool id> <comparison dir>")
        }

        val clsname = args[0]
        val toolcls = Class.forName(clsname) as Class<out SCPDTool>
        val tool = toolcls.newInstance()

        val label = args[1]
        val dir = Paths.get(args[2])

        tool.thaw()

        try {
            val _out = System.out
            System.setOut(PrintStream(FileOutputStream("/dev/null")))

            val result = tool.evaluateSubmissions(dir)

            System.setOut(_out)
            JsonSerialiser.serialise(result, System.out)
        } finally {
            tool.close()
        }
    }
}