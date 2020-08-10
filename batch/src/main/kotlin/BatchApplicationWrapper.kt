//package me.haydencheers.prep
//
//import me.haydencheers.prep.normalise.Forker
//import me.haydencheers.prep.util.JsonSerialiser
//import java.nio.file.Paths
//
//object BatchApplicationWrapper {
//    @JvmStatic
//    fun main(args: Array<String>) {
//        val root = Paths.get("/media/haydencheers/Data/PrEP/Variants")
//
//        val assignments = listOf(
//            "SENG1110_A1_2017",
//            "SENG1110_A2_2017",
//            "SENG2050_A1_2019",
//            "SENG2050_A2_2019",
//            "SENG4400_A1_2018",
//            "SENG4400_A2_2018"
//        )
//
//        val variantIds = arrayOf(
//            "p1" to "10",
//            "p2" to "20",
//            "p3" to "40",
//            "p4" to "60",
//            "p5" to "80",
//            "p6" to "100"
//        )
//
//        val configPath = root.resolve("_config.json")
//
//        for (assignment in assignments) {
//            for ((variant, perc) in variantIds) {
//                println("Assignment: $assignment")
//                println("Variant: $variant")
//
//                JsonSerialiser.serialise(
//                    PrEPConfig().apply {
//                        submissionRoot = "datasets/$assignment/$variant"
//                        outputRoot = "out/$assignment/$variant"
//                        workingRoot = "work/$assignment/$variant"
//
//                        submissionsArePlaintiff = false
//
//                        randomSeed = 11121993
//
//                        seeding = SeedConfig().apply {
//                            dataRoot = "seed"
//                            configFiles = arrayOf(
//                                "simplag-config-$perc.json"
//                            )
//                            seedSubmissionSelectionChance = 1.0
//                        }
//
//                        detection = DetectionConfig().apply {
//                            maxParallelism = 32
//
//                            useJPlag = true
//                            usePlaggie = true
//                            useSim = true
//                            useSherlockSydney = true
//                            useSherlockWarwick = true
//
//                            useNaiveStringEditDistance = true
//                            useNaiveStringTiling = true
//                            useNaiveTokenEditDistance = true
//                            useNaiveTokenTiling = true
//                            useNaiveTreeEditDistance = true
//                            useNaivePDGEditDistance = true
//                        }
//                    },
//                    configPath
//                )
//
//                println("Executing ...")
//                val result = Forker.exec(Application::class.java, arrayOf(configPath.toAbsolutePath().toString()))
//                println("Exited with $result")
////                Application.main(arrayOf(configPath.toAbsolutePath().toString()))
//                println()
//            }
//        }
//    }
//}