package me.haydencheers.prep.detection

import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.results.ResultModule
import java.util.concurrent.Executors
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
open class DetectionModule {
    @Inject
    private lateinit var resultsModule: ResultModule

    @Inject
    private lateinit var toolBindingFactory: ToolBindingFactory

    open fun execute(
        config: DetectionConfig,
        listings: MutableList<SubmissionListing>
    ) {
        val executor = Executors.newFixedThreadPool(config.maxParallelism)
        val tools = toolBindingFactory.produceBindings()

        for (tool in tools) {
            for (l in listings) {
                for (r in listings) {
                    if (l != r) {
                        val sim = tool.evaluatePairwise(l.root, r.root)
                        TODO()
                    }
                }
            }
        }
    }
}