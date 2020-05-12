package me.haydencheers.prep

import me.haydencheers.prep.beans.ListingsFactory
import me.haydencheers.prep.detection.DetectionModule
import me.haydencheers.prep.normalisation.NormalisationModule
import me.haydencheers.prep.results.ResultModule
import me.haydencheers.prep.seeding.SeedModule
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
import me.haydencheers.scpdt.AbstractJavaSCPDTool
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.system.exitProcess

@Singleton
class PrEPPipeline {

    lateinit var config: PrEPConfig

    @Inject
    lateinit var seedModule: SeedModule

    @Inject
    lateinit var normalisationModule: NormalisationModule

    @Inject
    lateinit var detectionModule: DetectionModule

    @Inject
    lateinit var resultsModule: ResultModule

    fun run(args: Array<String>) {
        if (args.count() != 1) throw IllegalArgumentException("Usage: Application <path to config file>")

        val configPath = Paths.get(args[0])
        val config = JsonSerialiser.deserialise(configPath, PrEPConfig::class)
        run(config, configPath.parent)
    }

    fun run(config: PrEPConfig, root: Path) {
        this.config = config
        this.config.random = Random(this.config.randomSeed)

        val submissionRoot = root.resolve(config.submissionRoot)
        val outputRoot = root.resolve(config.outputRoot)
        val workingRoot = root.resolve(config.workingRoot)

        // Validate config parameters
        if (!Files.exists(submissionRoot) || !Files.isDirectory(submissionRoot)) throw IllegalArgumentException("Input source ${submissionRoot} does not exist or is not a folder")

        if (Files.exists(outputRoot))
            Files.walk(outputRoot)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        Files.createDirectories(outputRoot)

        if (Files.exists(workingRoot))
            Files.walk(workingRoot)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        Files.createDirectories(workingRoot)

        // Copy projects to a tmp directory + load the projects as a mutable listing
        val projsRoot = workingRoot.resolve("submissions")
        FileUtils.copyDir(submissionRoot, projsRoot)

        for (dir in Files.list(projsRoot).filter { Files.isDirectory(it) && !Files.isHidden(it) }) {
            if (dir.fileName.toString().contains("-")) {
                Files.move(dir, dir.parent.resolve(dir.fileName.toString().replace("-", "_")))
            }
        }

        val listings = ListingsFactory.produceForDirectory(projsRoot, true)
        listings.sortBy { it.name }
        resultsModule.addRealSubmissions(listings.map { it.name })

        val submissionIds = listings.map { it.name }

        // Run the seed module
        if (config.seeding != null) {
            println("Seeding Plagiarised Submissions")

            val seeding = config.seeding!!
            val dataRoot = root.resolve(seeding.dataRoot)
            val configFiles = seeding.configFiles.map { root.resolve(it) }

            seedModule.execute(seeding, config.random, listings, dataRoot, configFiles, workingRoot)
        }

        // Run the normalisation module
        if (config.normalisation != null) {
            println("Normalising Submissions")
            normalisationModule.execute(config.normalisation!!, listings)
        }

        // Set detection mx heap
        if (config.detection.mxHeap != null) {
            println("Setting Java tool max heap: ${config.detection.mxHeap}")
            AbstractJavaSCPDTool.mxHeap = config.detection.mxHeap
        }

        // Run the detection module
        println("Executing Detection Tools for Submission-wise Similarity")
        if (config.submissionsArePlaintiff) {
            val submissionListings = listings.filter { submissionIds.contains(it.name) }
            val generatedListings = listings.filter { !submissionIds.contains(it.name) }
            println("Plaintiff Mode ${submissionListings.size} x ${generatedListings.size}")

            detectionModule.executePlaintiffPairwise(config.detection, submissionListings, generatedListings)
        } else {
            detectionModule.executePairwise(config.detection, listings)
        }

        if (config.executeFilewise) {
            println("Executing Detection Tools for File-wise Similarity")
            if (config.submissionsArePlaintiff) {
                val submissionListings = listings.filter { submissionIds.contains(it.name) }
                val generatedListings = listings.filter { !submissionIds.contains(it.name) }
                println("Plaintiff Mode ${submissionListings.size} x ${generatedListings.size}")

                detectionModule.executePlaintiffFilewise(config.detection, submissionListings, generatedListings)
            } else {
                detectionModule.executeFilewise(config.detection, listings)
            }
        }

        // Store the results
        resultsModule.storeResults(outputRoot)

        // Cleanup + exit
        exitProcess(0)
    }
}