package me.haydencheers.clustering

import org.apache.commons.math3.random.EmpiricalDistribution
import java.lang.IllegalArgumentException
import java.util.stream.DoubleStream
import kotlin.streams.toList

object Bucketiser {
    data class Bucket (
        val start: Double,
        val end: Double,
        val scores: List<Score>
    )

    fun bucketise(scores: List<Score>, width: Double): List<Bucket> {
        if (width <= 0.0) throw IllegalArgumentException("Width must be > 0.0")
        if (scores.isEmpty()) return emptyList()

        val scores = scores.sortedBy { it.score }
        val buckets = mutableListOf<Bucket>()

        var lastIndex = 0
        var i = 0.0
        while (i <= 100+width) {
            // Get the index of the first item outside of this bucket range
            val nextIndex = scores.indexOfLast { it.score < i+width }

            val bucketScores = scores.subList(lastIndex, nextIndex+1)
            buckets.add(
                Bucket(
                    i,
                    i + width,
                    bucketScores
                )
            )

            lastIndex = nextIndex+1

            i += width
        }

        return buckets
    }

    data class Cluster (
        val start: Double,
        val end: Double,
        val scores: List<Score>
    )

    fun clusters(rawScores: List<Score>, precision: Double): List<Cluster> {
        val buckets = bucketise(rawScores, precision)

        val minima = mutableListOf<Int>()
        minima.add(0)
        for (i in 1 until buckets.size-1) {
            val l = buckets[i-1].scores.size
            val m = buckets[i].scores.size
            val r = buckets[i+1].scores.size

            if ((l > m && m < r) || (l > m && m <= r).xor(l >= m && m < r)) {
                minima.add(i)
            }
        }
        minima.add(buckets.size)

        val clusters = mutableListOf<Cluster>()
        for (i in 0 until minima.size-1) {
            val start = minima[i]
            val end = minima[i+1]

            val cbuckets = buckets.subList(start, end)
            val scores = cbuckets.flatMap { it.scores }

            val startScore = cbuckets.first().start
            val endScore = cbuckets.last().end

            val cluster = Cluster(startScore, endScore, scores)
            clusters.add(cluster)
        }

        return clusters

//        val rawScores = rawScores.sortedBy { it.score }
//        val scores = rawScores.map { it.score.roundToMultipleOf(precision) }
//            .toDoubleArray()
//
//
//
//        val ed = EmpiricalDistribution()
//        ed.load(scores)
//
//        val probs = DoubleStream.iterate(0.0, { i -> i <= 100+precision }, { i -> i+precision })
//            .map {
//                val perc = ed.density(it)
//                if (perc.isInfinite() || perc.isNaN()) 0.0
//                else perc
//            }.toList()
//
//        val minima = mutableListOf<Int>()
//        minima.add(0)
//        for (i in 1 until probs.size-1) {
//            val l = probs[i - 1]
//            val m = probs[i]
//            val r = probs[i + 1]
//
//            if ((l > m && m < r) || (l > m && m <= r).xor(l >= m && m < r)) {
//                minima.add(i)
//            }
//        }
//        minima.add(probs.size)
//
//        val clusters = mutableListOf<Cluster>()
//        for (i in 0 until minima.size-1) {
//            val start = minima[i] * precision
//            val end = minima[i+1] * precision
//
//            val cluster = mutableListOf<Score>()
//            for (item in rawScores) {
//                if (item.score >= start && item.score < end) {
//                    cluster.add(item)
//                }
//            }
//            clusters.add(Cluster(start, end, cluster))
//        }
//
//        return clusters
    }

    fun Double.roundToMultipleOf(num: Double): Double {
        return num * Math.floor(Math.abs(this/num))
    }
}