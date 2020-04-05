package me.haydencheers.prep.detection

import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
open class DetectionModule {
    @Inject
    private lateinit var resultsModule: ResultModule

    @Inject
    private lateinit var toolBindingFactory: ToolBindingFactory

    private val tmp = Files.createTempDirectory("PrEP-tools")

    open fun execute(
        config: DetectionConfig,
        listings: MutableList<SubmissionListing>
    ) {
        val listings = listings.sortedBy { it.name }

        val sem = Semaphore(config.maxParallelism)
        val tools = toolBindingFactory.produceBindings()

        for (tool in tools) {
            println("\tExecuting ${tool.id}")
            tool.thaw(Files.createDirectory(tmp.resolve(tool.id)))

            for (l in 0 until listings.size) {
                val llst = listings[l]

                for (r in l+1 until listings.size) {
                    val rlst = listings[r]

                    while (!sem.tryAcquire(1, 5, TimeUnit.SECONDS)) {
                        println("Awaiting permit ...")
                    }

                    CompletableFuture.runAsync {
                        val sim = tool.evaluatePairwise(llst.root, rlst.root)
                        println("\t\t${llst.name} - ${rlst.name} - ${sim}")
                    }.whenComplete { void, throwable ->
                        throwable?.printStackTrace(System.err)
                        sem.release()
                    }
                }
            }
        }
    }

    @PreDestroy
    open fun dispose() {
        Files.walk(tmp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}