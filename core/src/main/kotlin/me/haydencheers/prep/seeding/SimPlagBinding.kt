package me.haydencheers.prep.seeding

import me.haydencheers.prep.beans.ListingsFactory
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
import java.io.Closeable
import java.nio.file.*
import kotlin.streams.toList

class SimPlagBinding: Closeable {

    private val rootFolderName = "/simplag"

    private lateinit var thawRoot: Path

    fun execute (
        submissions: List<SubmissionListing>,
        seedDataRoot: Path,
        configFile: Path,
        generatedOutputRoot: Path
    ): MutableList<SubmissionListing> {
        val java = "${System.getProperty("java.home")}/bin/java"
        val simplag = thawRoot.resolve("app-1.0-SNAPSHOT.jar")
            .toAbsolutePath()
            .toString()

        val tmp = Files.createTempDirectory("simplag-work")

        try {
            // Deserialise the config, override the in values, write to dir
            val cfgPath = tmp.resolve("config.json")
            val configBean = JsonSerialiser.deserialise(configFile, SimPlagConfig::class)
            configBean.input = "src"
            configBean.injectionSources = "seed"
            configBean.output = "out"
            JsonSerialiser.serialise(configBean, cfgPath)

            // Copy over the sources & seeds
            val srcRoot = Files.createDirectory(tmp.resolve("src"))
            val seedRoot = tmp.resolve("seed")
            val outRoot = Files.createDirectory(tmp.resolve("out"))

            for (submission in submissions) {
                FileUtils.copyDir(submission.root, srcRoot.resolve(submission.name))
            }

            FileUtils.copyDir(seedDataRoot, seedRoot)

            // Execute tool
            val proc = ProcessBuilder()
                .command(java, "-jar", simplag, cfgPath.toAbsolutePath().toString())
                .start()

            val result = proc.waitFor()

            val output = mutableListOf<SubmissionListing>()
            if (result == 0) {
                FileUtils.copyDir(outRoot, generatedOutputRoot)
                val generatedListings = ListingsFactory.produceForDirectory(generatedOutputRoot, true)
                generatedListings.forEach { it.name = "${generatedOutputRoot.fileName}-${it.name}" }
                output.addAll(generatedListings)
            } else {
                val out = proc.inputStream.bufferedReader().readLines()
                val err = proc.errorStream.bufferedReader().readLines()

                System.err.println("SimPlag returned error code: $result")
                out.forEach(System.out::println)
                err.forEach(System.err::println)

                throw IllegalStateException()
            }

            return output

        } finally {
            Files.walk(tmp)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }

    fun thaw(path: Path) {
        this.thawRoot = path

        val rootcp = this.javaClass.getResource(rootFolderName).path.removeSuffix("!${rootFolderName}").removePrefix("file:")
        val rootcppath = Paths.get(rootcp)

        val cpfs = FileSystems.newFileSystem(rootcppath, this.javaClass.classLoader)
        val simPlagRoot = cpfs.getPath(rootFolderName)
        val jars = Files.list(simPlagRoot)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (jar in jars) {
            val jaris = Files.newInputStream(jar)
            val target = path.resolve(jar.fileName.toString())
            Files.copy(jaris, target)
        }
    }

    override fun close() {
        Files.walk(thawRoot)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}