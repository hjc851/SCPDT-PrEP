package me.haydencheers.prep.detection

import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import java.nio.file.Files
import java.util.concurrent.Executors
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
        val executor = Executors.newFixedThreadPool(config.maxParallelism)
        val tools = toolBindingFactory.produceBindings()

        for (tool in tools) {
            tool.thaw(Files.createDirectory(tmp.resolve(tool.id)))

            for (l in listings) {
                for (r in listings) {
                    if (l != r) {
                        TODO()
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