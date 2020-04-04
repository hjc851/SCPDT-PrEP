package me.haydencheers.prep

import me.haydencheers.prep.beans.ListingsFactory
import me.haydencheers.prep.detection.DetectionModule
import me.haydencheers.prep.normalisation.NormalisationModule
import me.haydencheers.prep.results.ResultModule
import me.haydencheers.prep.seeding.SeedModule
import me.haydencheers.prep.util.JsonSerialiser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

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

        // Validate config parameters
        if (!Files.exists(submissionRoot) || !Files.isDirectory(submissionRoot)) throw IllegalArgumentException("Input source ${submissionRoot} does not exist or is not a folder")

        if (Files.exists(outputRoot))
            Files.walk(outputRoot)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        Files.createDirectory(outputRoot)

        // Load the projects as a mutable listing
        val listings = ListingsFactory.produceForDirectory(submissionRoot)
        resultsModule.addRealSubmissions(listings.map { it.name })

        // Run the seed module
        if (config.seeding != null) {
            val seeding = config.seeding!!
            val dataRoot = root.resolve(seeding.dataRoot)
            val configFiles = seeding.configFiles.map { root.resolve(it) }

            seedModule.execute(seeding, config.random, listings, dataRoot, configFiles)
        }

        // Run the normalisation module
        if (config.normalisation != null) {
            normalisationModule.execute(config.normalisation!!)
        }

        // Run the detection module
        detectionModule.execute(config.detection, listings)

        // Store the results
        resultsModule.storeResults(outputRoot)

        // Cleanup + exit
        System.exit(0)
    }
}