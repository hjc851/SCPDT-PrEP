package me.haydencheers.clustering

import me.haydencheers.strf.beans.BatchEvaluationResult
import me.haydencheers.strf.serialisation.STRFSerialiser
import java.nio.file.Paths
import java.util.*
import javax.json.JsonObject

// General Config

const val BUCKET_WIDTH = 1.0
const val KDE_PRECISION = 0.1
const val NODE_EDGE_RATIO = 0.7

const val MAX_CLUSTERS = 3
const val NODE_MAX_DEGREE = 4

// Env Config

val scoreRoot = Paths.get("/media/haydencheers/Data/PrEP/out/")
val compRoot = Paths.get("/media/haydencheers/Data/PrEP/comp/")

val datasetNames = arrayOf(
    "SENG1110_A1_2017"
    ,
    "SENG1110_A2_2017"
    ,
    "SENG2050_A1_2019"
    ,
    "SENG2050_A2_2019"
    ,
    "SENG4400_A1_2018"
    ,
    "SENG4400_A2_2018"
)

val toolNames = arrayOf(
    "Sherlock-Sydney"
    ,
//    "Sherlock-Warwick"
//    ,
    "Sim-3.0.2_Wine32"
    ,
    "JPlag"
    ,
    "Plaggie"
    ,
    "Naive Program Dependence Graph"
)

// Clustering Config

// Configuration params for clustering
// (m, d)
val datasetConfigs = mutableMapOf(
    "SENG1110_A1_2017" to (2 to 2),
    "SENG1110_A2_2017" to (2 to 2),
    "SENG2050_A1_2019" to (2 to 2),
    "SENG2050_A2_2019" to (3 to 4),
    "SENG4400_A1_2018" to (2 to 2),
    "SENG4400_A2_2018" to (1 to 2)
)

val toolConfigs = mutableMapOf(
    "Sherlock-Sydney" to 0.2,
    "Sherlock-Warwick" to 1.0,
    "Sim-3.0.2_Wine32" to 1.0,
    "JPlag" to 1.0,
    "Plaggie" to 1.0,
    "Naive Program Dependence Graph" to 0.2
)

// Stats

data class PRFScore (
    val precision: Double,
    val recall: Double,
    val fb: Double
)

fun tpCount(scores: List<Score>, suspicious: List<Pair<String, String>>): Int {
    return scores.count { it.isIn(suspicious) }
}

fun fpCount(scores: List<Score>, suspicious: List<Pair<String, String>>): Int {
    return scores.count { !it.isIn(suspicious) }
}

fun fnCount(scores: List<Score>, suspicious: List<Pair<String, String>>): Int {
    return suspicious.count { !it.isIn(scores) }
}

fun Pair<String, String>.isIn(suspiciousItems: List<Score>): Boolean {
    for (item in suspiciousItems) {
        if (item.lhs == first && item.rhs == second ||
            item.lhs == second && item.rhs == first) {
            return true
        }
    }
    return false
}

// Scores

data class Score (
    val lhs: String,
    val rhs: String,
    val score: Double
) {
    fun isIn(suspicious: List<Pair<String, String>>): Boolean {
        for (sus in suspicious) {
            if (sus.first == lhs && sus.second == rhs ||
                sus.first == rhs && sus.second == lhs) {
                return true
            }
        }
        return false
    }
}

fun isSameAuthor(lhs: String, rhs: String): Boolean {
    val l = lhs.split("_")[0]
    val r = rhs.split("_")[0]

    return l == r
}

fun loadSuspiciousScores(ds: String): List<Pair<String, String>> {
    val compfile = compRoot.resolve("${ds}.json")
    val comps = JsonSerialiser.deserialise(compfile, JsonObject::class)

    val suspicious = comps.get("knownSuspicious")!!.asJsonArray()
        .filterNot { isSameAuthor(it.asJsonObject().getString("lhs"), it.asJsonObject().getString("rhs")) }
        .map {
            Pair(
                it.asJsonObject().getString("lhs").replace("_", ""),
                it.asJsonObject().getString("rhs").replace("_", "")
            )
        }

    return suspicious
}

fun loadToolScores(tool: String, ds: String): List<Score> {
    val strfPath = scoreRoot.resolve(ds).resolve("scores-${tool}.strf")
    val strf = STRFSerialiser.deserialise(strfPath, BatchEvaluationResult::class)

    val scores = strf.comparisons
        .filterNot { isSameAuthor(it.lhs, it.rhs) }
        .map {
            Score(
                it.lhs.replace("_", ""),
                it.rhs.replace("_", ""),
                it.similarity
            )
        }
        .sortedByDescending { it.score }

    return scores
}

fun calculateStatistics(suspiciousScores: List<Score>, suspicious: List<Pair<String, String>>) {
    val tp = tpCount(suspiciousScores, suspicious)
    val fp = fpCount(suspiciousScores, suspicious)
    val fn = fnCount(suspiciousScores, suspicious)

    val precision = (tp.toDouble()) / (tp + fp)
    val recall = (tp.toDouble()) / (tp + fn)

    println("Precision: ${String.format("%2.2f", precision)}")
    println("Recall: ${String.format("%2.2f", recall)}")
    println("${fn} FN scores")
    println("${tp} TP scores of ${suspicious.size} total")
    for (score in suspiciousScores) {
        if (score.isIn(suspicious)) {
            println("\t${score.lhs} - ${score.rhs} : ${score.score}")
        }
    }

    println("${fp} FP scores")
    for (score in suspiciousScores) {
        if (!score.isIn(suspicious)) {
            println("\t${score.lhs} - ${score.rhs} : ${score.score}")
        }
    }
    println()
    println("TP\tFP\tFN")
    println("$tp\t$fp\t$fn")
    println()
}

//
//  Variants
//

data class VariantPartition (
    val g0: List<String>,
    val g1: List<String>,
    val g2: List<String>,

    val g0g1links: List<Pair<String, String>>,
    val g1g2links: List<Pair<String, String>>,
    val g0g2links: List<Pair<String, String>>,

    val g1sibs: List<List<String>>,
    val g2sibs: List<List<String>>
)

fun partitionVariantIds(scores: List<Score>): VariantPartition {
    val ids = scores.flatMap { listOf(it.lhs, it.rhs) }.toSet()
        .sortedBy { it.length }

    if (ids.size != 39)
        throw IllegalStateException("Expecting 39 assignments, found ${ids.size}")

    val originals = ids.subList(0, 3).sorted()
    val gen1 = ids.subList(3, 12).sorted()
    val gen2 = ids.subList(12, 39).sorted()

    val g0g1links = mutableListOf<Pair<String, String>>()
    val g1g2links = mutableListOf<Pair<String, String>>()
    val g0g2links = mutableListOf<Pair<String, String>>()

    val g1sibs = mutableListOf<List<String>>()
    val g2sibs = mutableListOf<List<String>>()

    for (i in 0 until originals.size) {
        val orig = originals[i]

        val g1sib = gen1.subList(i*3, i*3 + 3)
        g1sibs.add(gen1)

        for (j in 0 until 3) {
            val g1v = gen1[j + i*3]
            g0g1links.add(orig to g1v)

            val g2sib = gen2.subList(j*3 + i*3*3, j*3 + i*3*3+3)
            g2sibs.add(g2sib)

            for (k in 0 until 3) {
                val g2v = gen2[k + j*3 + i*3*3]

                g1g2links.add(g1v to g2v)
                g0g2links.add(orig to g2v)
            }
        }
    }

    return VariantPartition(
        originals, gen1, gen2,
        g0g1links, g1g2links, g0g2links,
        g1sibs, g2sibs
    )
}

val variantRoot = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV1B")

fun loadVariantScoresForTool(tool: String, ds: String, pname: String): List<Score> {
    val strfPath = variantRoot.resolve(ds).resolve(pname).resolve("out").resolve("scores-${tool}.strf")
    val strf = STRFSerialiser.deserialise(strfPath, BatchEvaluationResult::class)

    val scores = strf.comparisons.map {
        Score (
            it.lhs.removePrefix("gen_0_"),
            it.rhs.removePrefix("gen_0_"),
            it.similarity
        )
    }

    return scores
}

fun getSuspiciousVariantPairs(scores: List<Score>): List<Pair<String, String>> {

    val ids = scores.flatMap { listOf(it.lhs, it.rhs) }.toSet()
        .sortedBy { it.length }

    val originals = ids.subList(0, 3).sorted()
    val gen1 = ids.subList(3, 12).sorted()
    val gen2 = ids.subList(12, 39).sorted()

    val sus = mutableListOf<Pair<String, String>>()

    for (i in 0 until originals.size) {
        val orig = originals[i]

        for (j in 0 until 3) {
            val g1v = gen1[j + i*3]
            sus.add(orig to g1v)

            for (k in 0 until 3) {
                val g2v = gen2[k + j*3 + i*3*3]
                sus.add(g1v to g2v)
            }
        }
    }

    return sus
}