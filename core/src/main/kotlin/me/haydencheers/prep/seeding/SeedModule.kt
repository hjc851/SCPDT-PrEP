package me.haydencheers.prep.seeding

import me.haydencheers.prep.SeedConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
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
        configFiles: List<Path>
    ) {
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
            runSimPlag(seedListings, seedDataRoot, configFile)
        }


        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun runSimPlag (
        submissions: List<SubmissionListing>,
        seedDataRoot: Path,
        configFile: Path
    ) {
        val simplag = this.javaClass.getResource("/simplag/app-1.0-SNAPSHOT.jar").path
        val java = System.getProperty("java.home") + "/bin/java"
        val proc = ProcessBuilder()
            .command(java, "-jar", simplag)
            .start()

        proc.waitFor()

        val out = proc.inputStream.reader()
            .readLines()

        val err = proc.errorStream.reader()
            .readLines()

        TODO()
    }

    @PreDestroy
    fun willDestroy() {
        Files.walk(temp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}