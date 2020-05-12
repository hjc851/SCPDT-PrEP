package me.haydencheers.prep

import java.time.Instant
import javax.json.bind.annotation.JsonbTransient
import kotlin.random.Random

class PrEPConfig {
    lateinit var submissionRoot: String
    lateinit var outputRoot: String
    lateinit var workingRoot: String

    var submissionsArePlaintiff: Boolean = false
    var executeFilewise: Boolean = true

    var randomSeed = Instant.now().nano
    @JsonbTransient
    lateinit var random: Random

    var seeding: SeedConfig? = null
    var normalisation: NormalisationConfig? = null
    var detection: DetectionConfig = DetectionConfig()
}

class SeedConfig {
    lateinit var dataRoot: String
    lateinit var configFiles: Array<String>

    var forceSeedSubmissions: Array<String> = emptyArray()
    var seedSubmissionSelectionChance = 0.2
}

class NormalisationConfig {
    var formatting: Boolean = true
    var ordering: Boolean = true
    var comments: Boolean = true
}

class DetectionConfig {
    var maxParallelism = Runtime.getRuntime().availableProcessors() / 2
    var mxHeap: String? = null

    var useJPlag = true
    var usePlaggie = true
    var useSim = true
    var useSherlockWarwick = true
    var useSherlockSydney = true

    var useNaiveStringEditDistance = true
    var useNaiveStringTiling = true
    var useNaiveTokenEditDistance = true
    var useNaiveTokenTiling = true
    var useNaiveTreeEditDistance = true
    var useNaivePDGEditDistance = true
}