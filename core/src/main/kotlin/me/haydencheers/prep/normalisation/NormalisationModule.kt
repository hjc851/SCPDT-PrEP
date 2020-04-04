package me.haydencheers.prep.normalisation

import me.haydencheers.prep.NormalisationConfig
import me.haydencheers.prep.results.ResultModule
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NormalisationModule {
    @Inject
    lateinit var resultsModule: ResultModule

    fun execute(normalisation: NormalisationConfig) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @PreDestroy
    fun willDestroy() {

    }
}