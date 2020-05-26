package me.haydencheers.prep.scripts.ev3

import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.PrEPConfig
import me.haydencheers.prep.SeedConfig
import me.haydencheers.prep.scripts.PrEPExec
import me.haydencheers.prep.scripts.ev2.Config
import me.haydencheers.prep.seeding.CommentingConfig
import me.haydencheers.prep.seeding.InjectConfig
import me.haydencheers.prep.seeding.MutateConfig
import me.haydencheers.prep.seeding.SimPlagConfig
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
import me.haydencheers.scpdt.util.CopyUtils
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt
import kotlin.streams.toList

object EV3All {
    @JvmStatic
    fun main(args: Array<String>) {
        val workingDir = Config.EV3_WORKING_ROOT.resolve("All")

        val globalSeed = Files.createTempDirectory("PrEP-EV3-Seed")

        for (datasetName in Config.DATASET_NAMES) {
            val dsRoot = Config.DATASET_ROOT.resolve(datasetName)

            val subs = Files.list(dsRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (sub in subs) {
                val dst = globalSeed.resolve("${datasetName}_${sub.fileName}")
                FileUtils.copyDir(sub, dst)
            }
        }

        for (datasetName in Config.DATASET_NAMES.shuffled()) {
            println("Dataset: $datasetName")

            val datasetSubmissionRoot = Config.DATASET_ROOT.resolve(datasetName)
            if (!Files.exists(datasetSubmissionRoot)) throw IllegalArgumentException("dataset ${datasetName} does not exist!")

            val submissions = Files.list(datasetSubmissionRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }
                .filter { !(it.fileName.toString().contains("c3279545") || it.fileName.toString().contains("c3283349_attempt_2017")) }
                .shuffled()

            for (submission in submissions) {
                println("\tSubmission: $submission")

                val fcount = Files.walk(submission)
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".java") }
                    .count()
                    .toInt()

                if (fcount == 0) {
                    println("\t\tSKIPPING: 0 files")
                    continue
                }

                for ((pname, pvalue) in Config.VARIANT_LEVELS) {
                    println("\t\t$pname")

                    val storeOut = workingDir.resolve("out/${datasetName}/${submission.fileName}/$pname")
                    val storeWork = workingDir.resolve("work/${datasetName}/${submission.fileName}/$pname")
                    val storeLogs = workingDir.resolve("logs/${datasetName}/${submission.fileName}/$pname")

                    if (Files.exists(storeOut) && Files.exists(storeWork) && Files.exists(storeLogs)) continue

                    Config.SEM.acquire()

                    CompletableFuture.runAsync {
                        val tmp = Files.createTempDirectory("PREP-EV3-All-${submission.fileName}-$pname")

                        val work = Files.createDirectories(tmp.resolve("work"))
                        val out = Files.createDirectory(tmp.resolve("out"))
                        val seed = FileUtils.copyDir(globalSeed, tmp.resolve("seed"))

                        Files.copy(Paths.get("db.blob"), tmp.resolve("db.blob"))

                        val srcRoot = Files.createDirectory(tmp.resolve("src"))
                        CopyUtils.copyDir(submission, srcRoot.resolve(submission.fileName))

                        val config = makeConfig(submission.fileName.toString())
                        val simplagConfig = makeSimplagConfig(pvalue, fcount)

                        val configFile = tmp.resolve("config.json")
                        val simplagConfigFile = tmp.resolve("simplag-config.json")

                        val outf = Files.createFile(tmp.resolve("stdout.txt"))
                        val errf = Files.createFile(tmp.resolve("stderr.txt"))

                        JsonSerialiser.serialise(config, configFile)
                        JsonSerialiser.serialise(simplagConfig, simplagConfigFile)

                        try {
                            val success = PrEPExec.execute(configFile, tmp, outf, errf, Config.RETRY_COUNT)

                            if (!success) {
                                System.err.println("Failed: ${submission.fileName} ${pname}")
                            } else {
                                Files.createDirectories(storeOut)
                                Files.createDirectories(storeWork)

                                FileUtils.copyDir(out, storeOut)
                                FileUtils.copyDir(work, storeWork)
                            }

                            Files.createDirectories(storeLogs)
                            Files.copy(outf, storeLogs.resolve("stdout.txt"), StandardCopyOption.REPLACE_EXISTING)
                            Files.copy(errf, storeLogs.resolve("stderr.txt"), StandardCopyOption.REPLACE_EXISTING)

                        } finally {
                            try {
                                Files.walk(tmp)
                                    .sorted(Comparator.reverseOrder())
                                    .forEach(Files::delete)
                            } catch (e: Exception) {}
                        }
                    }.whenComplete { void, t ->
                        t?.printStackTrace()
                        Config.SEM.release()
                    }
                }
            }
        }

        Config.SEM.acquire(Config.MAX_PARALLEL)
        Config.SEM.release(Config.MAX_PARALLEL)

        Files.walk(globalSeed)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }

    private fun makeConfig(subName: String): PrEPConfig {
        return PrEPConfig().apply {
            submissionRoot = "src"
            outputRoot = "out"
            workingRoot = "work"

            submissionsArePlaintiff = true
            executeFilewise = false

            randomSeed = 11121993

            seeding = SeedConfig().apply {
                dataRoot = "seed"
                configFiles = arrayOf(
                    "simplag-config.json"
                )
                forceSeedSubmissions = emptyArray()
                seedSubmissionSelectionChance = 1.0
            }

            detection = DetectionConfig().apply {
                maxParallelism = Config.VARIANT_COUNT
                mxHeap = "2000M"

                useJPlag = true
                usePlaggie = true
                useSim = true
                useSherlockSydney = true
                useSherlockWarwick = true

                useNaiveStringEditDistance = true
                useNaiveStringTiling = true
                useNaiveTokenEditDistance = true
                useNaiveTokenTiling = true
                useNaiveTreeEditDistance = true
                useNaivePDGEditDistance = true
            }
        }
    }

    private fun makeSimplagConfig(chance: Double, fileCount: Int): SimPlagConfig {
        return SimPlagConfig().apply {
            input = ""
            injectionSources = ""
            output = ""

            randomSeed = 11121993

            copies = Config.VARIANT_COUNT

            inject = InjectConfig().apply {
                injectAssignment = false    // no point in doing this

                injectFile = true
                injectFileChance = chance               // chance to inject file
                injectFileMaxCount = fileCount          // max number of files

                injectClass = true
                injectClassChance = chance      // chance that any file can have a class injected
                injectClassMaxCount = 2         // max number of classes to inject for any file

                injectMethod = true
                injectMethodChance = chance     // chance that any class will be selected to inject method into
                injectMethodMaxCount = 8        // max number of methods to inject into any class

                injectBlock = true
                injectBlockChance = chance                              // chance any method can have statement injected into
                injectBlockMaxStatements = (200 * chance).roundToInt()  // max number of statements
            }

            mutate = MutateConfig().apply {
                commenting = CommentingConfig().apply {
                    addChance = 0.0
                    add = false //transformation == 1

                    removeChance = 0.0
                    remove = false //transformation == 2

                    mutateChance = 0.0
                    mutate = false //transformation == 3
                }

                renameIdentifiersChance = 0.0
                renameIdentifiers = false //transformation == 4

                reorderStatementsChance = 0.0
                reorderStatements = false //transformation == 5

                reorderMemberDeclarationsChance = 0.0
                reorderMemberDeclarations = false //transformation == 6

                swapOperandsChance = 0.0
                swapOperands = false //transformation == 7

                upcastDataTypesChance = 0.0
                upcastDataTypes = false //transformation == 8

                switchToIfChance = 0.0
                switchToIf = false //transformation == 9

                forToWhileChance = 0.0
                forToWhile = false //transformation == 10

                expandCompoundAssignmentChance = 0.0
                expandCompoundAssignment = false //transformation == 11

                expandIncDecChance = 0.0
                expandIncDec = false //transformation == 12

                splitVariableDeclarationsChance = 0.0
                splitVariableDeclarations = false //transformation == 13

                assignDefaultValueChance = 0.0
                assignDefaultValue = false //transformation == 14

                splitDeclAndAssignmentChance = 0.0
                splitDeclAndAssignment = false //transformation == 15

                groupVariableDeclarationsChance = 0.0
                groupVariableDeclarations = false //transformation == 16
            }
        }
    }
}