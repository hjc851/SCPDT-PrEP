package me.haydencheers.prep.crawler

import me.haydencheers.prep.beans.ListingsFactory
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import org.kohsuke.github.GitHub
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import javax.annotation.PreDestroy
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

private val downloadTemplate = "https://api.github.com/repos/%s/zipball/master"

@ApplicationScoped
open class GitHubCrawlerModule {
    @Inject
    private lateinit var resultsModule: ResultModule

    private val tmp = Files.createTempDirectory("PrEP-GHCrawler")

    open fun execute(
        search: String,
        suboutroot: Path,
        listings: MutableList<SubmissionListing>
    ) {
        try {
            val pages = GitHub.connectAnonymously()
                .searchRepositories()
                .q(search)
                .list()

            for (repo in pages) {
                try {
                    println("\tFound: ${repo.fullName}")

                    val safename = repo.fullName.replace("/", "_").replace("-", "_")
                    val tmpFile = tmp.resolve("$safename.zip")

                    val url = URL(String.format(downloadTemplate, repo.fullName))
                    val rbc = Channels.newChannel(url.openStream())
                    val fout = FileOutputStream(tmpFile.toAbsolutePath().toString())
                    fout.channel.transferFrom(rbc, 0, Long.MAX_VALUE)

                    val subout = Files.createDirectories(suboutroot.resolve("crawled_$safename"))
                    val subzip = ZipFile(tmpFile.toFile())
                    val entries = subzip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val enpath = subout.resolve(entry.name)

                        if (entry.isDirectory) {
                            Files.createDirectories(enpath)
                        } else {
                            val fin = subzip.getInputStream(entry)
                            Files.copy(fin, enpath, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }

                    val listing = ListingsFactory.produceForSubmission(subout, true)
                    listings.add(listing)

                } catch (e: Exception) {
                    System.err.println("Error processing ${repo.fullName}")
                    e.printStackTrace(System.err)
                }
            }

        } catch (e: Exception) {
            System.err.println("Error crawling github")
            e.printStackTrace(System.err)
        }
    }

    @PreDestroy
    open fun dispose() {
        try {
            Files.walk(tmp)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        } catch (e: Exception) {}
    }
}