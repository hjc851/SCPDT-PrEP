package me.haydencheers.prep.seeding

import me.haydencheers.prep.SeedConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SeedModule {
    @Inject
    lateinit var resultsModule: ResultModule

    private val temp = Files.createTempDirectory("PrEP-seed")

    fun execute (
        config: SeedConfig,
        random: Random,
        listings: MutableList<SubmissionListing>,
        seedDataRoot: Path,
        configFiles: List<Path>,
        workingRoot: Path
    ) {
        val simplagBinding = SimPlagBinding()
        simplagBinding.thaw(Files.createDirectory(temp.resolve("simplag")))

        for (i in 0 until configFiles.size) {
            val configFile = configFiles[i]

            val seedSubmissionIds = mutableSetOf<String>()
            seedSubmissionIds.addAll(config.forceSeedSubmissions)

            for (listing in listings) {
                if (random.nextDouble(0.0, 1.0) <= config.seedSubmissionSelectionChance) {
                    seedSubmissionIds.add(listing.name)
                }
            }

            val seedListings = listings.filter { seedSubmissionIds.contains(it.name) }
            val generatedListings = simplagBinding.execute(seedListings, seedDataRoot, configFile, workingRoot.resolve("generated-$i"))

            listings.addAll(generatedListings)
            resultsModule.addSyntheticSubmissions(generatedListings.map { it.name })
        }

        simplagBinding.close()
    }

    @PreDestroy
    fun willDestroy() {
        Files.walk(temp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}