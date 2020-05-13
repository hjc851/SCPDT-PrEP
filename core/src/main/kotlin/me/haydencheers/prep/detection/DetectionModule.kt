package me.haydencheers.prep.detection

import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.scpdt.AbstractJavaSCPDTool
import me.haydencheers.scpdt.SCPDTool
import me.haydencheers.scpdt.util.CopyUtils
import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.beans.FileComparisonResult
import me.haydencheers.strf.beans.PairwiseComparisonResult
import java.lang.Exception
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
                        toolResults.add(Triple(llisting.name, rlisting.name, emptyList()))
                        sem.release()
                    }
                }
            }

            val end = Instant.now()
            val diff = Duration.between(start, end)

            resultsModule.addFilewiseScores(tool.id, toolResults)
            resultsModule.addFilewiseDuration(tool.id, diff.seconds)
        }

        while(!sem.tryAcquire(config.maxParallelism, 10, TimeUnit.SECONDS)) {
            println("Awaiting ${config.maxParallelism - sem.availablePermits()} permits")
        }
    }

    @PreDestroy
    open fun dispose() {
        Files.walk(tmp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }

    open fun executePlaintiffPairwise(
        config: DetectionConfig,
        submissionListings: List<SubmissionListing>,
        generatedListings: List<SubmissionListing>
    ) {
        // Execute tools
        val executor = Executors.newFixedThreadPool(config.maxParallelism)
        val sem = Semaphore(config.maxParallelism)

        for (tool in tools) {
            println("\tExecuting ${tool.id}")
            val results = mutableListOf<Triple<String, String, Double>>()

            val start = Instant.now()
            for (submission in submissionListings) {
                for (generated in generatedListings) {
                    sem.acquire()

                    CompletableFuture.runAsync(Runnable {
                        val tmp = Files.createTempDirectory("PrEP-plaintiff-tmp")
                        FileUtils.copyDir(submission.root, tmp.resolve(submission.name))
                        FileUtils.copyDir(generated.root, tmp.resolve(generated.name))

                        val result = tool.evaluateSubmissions(tmp)
                        results.addAll(result)

                        try {
                            Files.walk(tmp)
                                .sorted(Comparator.reverseOrder())
                                .forEach(Files::delete)
                        } catch (e: Exception) {}
                    }, executor)
                    .whenComplete { void, throwable ->
                        throwable?.printStackTrace(System.err)
                        results.add(Triple(submission.name, generated.name, 0.0))
                        sem.release()
                    }
                }
            }

            while(!sem.tryAcquire(config.maxParallelism, 10, TimeUnit.SECONDS)) {
                println("Awaiting ${config.maxParallelism - sem.availablePermits()} permits")
            }

            sem.release(config.maxParallelism)

            val end = Instant.now()
            val diff = Duration.between(start, end)

            resultsModule.addPairwiseScores(tool.id, results)
            resultsModule.addPairwiseDuration(tool.id, diff.seconds)
        }

        executor.shutdown()
    }

    open fun executePlaintiffFilewise(
        config: DetectionConfig,
        submissionListings: List<SubmissionListing>,
        generatedListings: List<SubmissionListing>
    ) {
        val sem = Semaphore(config.maxParallelism)

        for (tool in tools) {
            val start = Instant.now()
            println("\tExecuting ${tool.id}")

            val toolResults = Collections.synchronizedList(mutableListOf<Triple<String, String, List<Triple<String, String, Double>>>>())

            for (submission in submissionListings) {
                for (generated in generatedListings) {
                    while (!sem.tryAcquire(1, 5, TimeUnit.SECONDS)) {
                        println("\t\tAwaiting permit ${Date()} (${submission.name} x ${generated.name}) ...")
                    }

                    CompletableFuture.runAsync {
                        val result = tool.evaluateAllFiles(submission.root, generated.root)
                        val triple = Triple(submission.name, generated.name, result)
                        toolResults.add(triple)

                    }.whenComplete { void, throwable ->
                        throwable?.printStackTrace(System.err)
                        toolResults.add(Triple(submission.name, generated.name, emptyList()))
                        sem.release()
                    }
                }
            }

            val end = Instant.now()
            val diff = Duration.between(start, end)

            resultsModule.addFilewiseScores(tool.id, toolResults)
            resultsModule.addFilewiseDuration(tool.id, diff.seconds)
        }

        while(!sem.tryAcquire(config.maxParallelism, 10, TimeUnit.SECONDS)) {
            println("Awaiting ${config.maxParallelism - sem.availablePermits()} permits")
        }
    }
}