package me.haydencheers.prep.results

import java.nio.file.Path
import javax.annotation.PreDestroy
import javax.inject.Singleton

@Singleton
class ResultModule {
    private var realSubmissions = mutableListOf<String>()
    private var syntheticSubmissions = mutableListOf<String>()

    fun addRealSubmissions(submissions: List<String>) {
        realSubmissions.addAll(realSubmissions)
    }

    fun addSyntheticSubmissions(submissions: List<String>) {
        syntheticSubmissions.addAll(submissions)
    }

    fun storeResults(outputRoot: Path) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @PreDestroy
    fun willDestroy() {

    }
}