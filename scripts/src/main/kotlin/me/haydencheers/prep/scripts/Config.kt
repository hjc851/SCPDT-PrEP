package me.haydencheers.prep.scripts

import java.nio.file.Paths
import java.util.concurrent.Semaphore
import kotlin.random.Random

object Config {
    val DATASET_ROOT = Paths.get("/media/haydencheers/Data/PrEP/datasets")
    val EV2_WORKING_ROOT = Paths.get("/media/haydencheers/Data/PrEP/EV2")
    val EV3_WORKING_ROOT = Paths.get("/media/haydencheers/Data/PrEP/EV3")

    val DATASET_NAMES = listOf(
        //"SENG1110_A1_2017",
        //"SENG1110_A2_2017",
        "SENG2050_A1_2019",
        "SENG2050_A2_2019",
        "SENG4400_A1_2018",
        "SENG4400_A2_2018"
    )

    val VARIANT_COUNT = 5

    val VARIANT_LEVELS = arrayOf(
        "p1" to 0.1,
        "p2" to 0.2,
        "p3" to 0.4,
        "p4" to 0.6,
        "p5" to 0.8,
        "p6" to 1.0
    )

    val RETRY_COUNT = 5

    val MAX_PARALLEL = 8
    val SEM = Semaphore(MAX_PARALLEL)

    val TIMEOUT = 2L

    val random = Random(11121993)

    val TOOLS = arrayOf(
        "JPlag",
        "Naive Program Dependence Graph",
        "Naive String Edit Distance",
        "Naive String Tiling",
        "Naive Token Edit Distance",
        "Naive Token Tiling",
        "Naive Tree Edit Distance",
        "Plaggie",
        "Sherlock-Sydney",
        "Sherlock-Warwick",
        "Sim-3.0.2_Wine32"
    )
}