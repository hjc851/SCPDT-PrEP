package me.haydencheers.prep.normalisation

import me.haydencheers.prep.NormalisationConfig
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.normalise.*
import me.haydencheers.prep.normalise.comment.CommentNormaliser
import me.haydencheers.prep.normalise.format.FormattingNormaliser
import me.haydencheers.prep.normalise.order.OrderNormaliser
import me.haydencheers.prep.results.ResultModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NormalisationModule {
    @Inject
    lateinit var resultsModule: ResultModule

    fun execute(
        config: NormalisationConfig,
        listings: MutableList<SubmissionListing>
    ) {
        if (!config.comments && !config.formatting && !config.ordering) return

        val normalisers = mutableListOf<Normaliser>()
        if (config.comments) normalisers.add(CommentNormaliser())
        if (config.ordering) normalisers.add(OrderNormaliser())
        if (config.formatting) normalisers.add(FormattingNormaliser())

        listings.parallelStream().
            forEach { listing ->
            for (normaliser in normalisers) {
                normaliser.normalise(listing.files)
            }
            println("\tNormalised ${listing.name}")
        }
    }
}