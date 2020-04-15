package me.haydencheers.prep.detection

import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import me.haydencheers.scpdt.SCPDTool
import me.haydencheers.scpdt.util.CopyUtils
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.beans.FileComparisonResult
import me.haydencheers.strf.beans.PairwiseComparisonResult
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import kotlin.Comparator

@ApplicationScoped
open class DetectionModule {
    @Inject
    private lateinit var resultsModule: ResultModule

    @Inject
    private lateinit var toolBindingFactory: ToolBindingFactory

    private val tmp = Files.createTempDirectory("PrEP-tools")
    private lateinit var tools: List<SCPDTool>

    @PostConstruct
    private fun init() {
        this.tools = toolBindingFactory.produceBindings()

        for (tool in this.tools) {
            tool.thaw(Files.createDirectory(tmp.resolve(tool.id)))
        }
    }

    open fun executePairwise (
        config: DetectionConfig,
        listings: MutableList<SubmissionListing>
    ) {
        val ids = listings.map { it.name }.toSet()

        // Copy to a single directory for efficiency
        val srcRoot = Files.createDirectory(tmp.resolve("submissions-pairwise"))
        for (listing in listings) {
            CopyUtils.copyDir(listing.root, srcRoot.resolve(listing.name))
        }

        // Execute tools
        val executor = Executors.newFixedThreadPool(config.maxParallelism)
        for (tool in tools) {
            val start = Instant.now()

            println("\tExecuting ${tool.id}")
            val result = tool.evaluateSubmissions(srcRoot, executor = executor)

            val end = Instant.now()
            val diff = Duration.between(start, end)

            resultsModule.addPairwiseScores(tool.id, result)
            resultsModule.addPairwiseDuration(tool.id, diff.seconds)
        }
        executor.shutdown()
    }

    open fun executeFilewise (
        config: DetectionConfig,
        listings: MutableList<SubmissionListing>
    ) {
        val sem = Semaphore(config.maxParallelism)

        for (tool in tools) {
            val start = Instant.now()
            println("\tExecuting ${tool.id}")

            val toolResults = Collections.synchronizedList(mutableListOf<Triple<String, String, List<Triple<String, String, Double>>>>())

            for (l in 0 until listings.size) {
                val llisting = listings[l]

                for (r in l+1 until listings.size) {
                    val rlisting = listings[r]

                    while (!sem.tryAcquire(1, 5, TimeUnit.SECONDS)) {
                        println("\t\tAwaiting permit ${Date()} (${l+1} x ${r+1}) ...")
                    }

                    CompletableFuture.runAsync {
                        val result = tool.evaluateAllFiles(llisting.root, rlisting.root)
                        val triple = Triple(llisting.name, rlisting.name, result)
                        toolResults.add(triple)

                    }.whenComplete { void, throwable ->
                        throwable?.printStackTrace(System.err)
                        sem.release()
                    }
                }
            }

            val end = Instant.now()
            val diff = Duration.between(start, end)

            resultsModule.addFilewiseScores(tool.id, toolResults)
            resultsModule.addFilewiseDuration(tool.id, diff.seconds)
        }

        while(!sem.tryAcquire(config.maxParallelism, 15, TimeUnit.SECONDS)) {
            println("Awaiting ${config.maxParallelism - sem.availablePermits()} permits")
        }
    }

    @PreDestroy
    open fun dispose() {
        Files.walk(tmp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}