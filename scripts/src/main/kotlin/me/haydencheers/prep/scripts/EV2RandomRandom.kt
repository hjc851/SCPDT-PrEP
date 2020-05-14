package me.haydencheers.prep.scripts

import me.haydencheers.prep.Application
import me.haydencheers.prep.DetectionConfig
import me.haydencheers.prep.PrEPConfig
import me.haydencheers.prep.SeedConfig
import me.haydencheers.prep.normalise.Forker
import me.haydencheers.prep.seeding.CommentingConfig
import me.haydencheers.prep.seeding.InjectConfig
import me.haydencheers.prep.seeding.MutateConfig
import me.haydencheers.prep.seeding.SimPlagConfig
import me.haydencheers.prep.util.FileUtils
import me.haydencheers.prep.util.JsonSerialiser
import me.haydencheers.scpdt.util.CopyUtils
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object EV2RandomRandom {
    @JvmStatic
    fun main(args: Array<String>) {
        val datasetRoot = Paths.get("/media/haydencheers/Data/PrEP/datasets")
        val workingDir = Paths.get("/media/haydencheers/Data/PrEP/EV2RandomRandom")

        for (datasetName in EV2Config.DATASET_NAMES) {
            println("Dataset: $datasetName")

            val datasetSubmissionRoot = datasetRoot.resolve(datasetName)
            if (!Files.exists(datasetSubmissionRoot)) throw IllegalArgumentException("dataset ${datasetName} does not exist!")

            val submissions = Files.list(datasetSubmissionRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (submission in submissions) {
                println("\tSubmission: $submission")

                val storeOut = workingDir.resolve("out/${datasetName}/${submission.fileName}")
                val storeWork = workingDir.resolve("work/${datasetName}/${submission.fileName}")
                val storeLogs = workingDir.resolve("logs/${datasetName}/${submission.fileName}")

                if (Files.exists(storeOut) && Files.exists(storeWork) && Files.exists(storeLogs))
                    continue

                EV2Config.SEM.acquire()

                CompletableFuture.runAsync {
                    val tmp = Files.createTempDirectory("PREP-EV2-Isolated-${submission.fileName}")
                    val work = Files.createDirectories(tmp.resolve("work"))
                    val out = Files.createDirectory(tmp.resolve("out"))
                    val seed = Files.createDirectory(tmp.resolve("seed"))
                    Files.copy(Paths.get("db.blob"), tmp.resolve("db.blob"))

                    val srcRoot = Files.createDirectory(tmp.resolve("src"))
                    CopyUtils.copyDir(submission, srcRoot.resolve(submission.fileName))

                    val config = makeConfig(submission.fileName.toString())
                    val simplagConfig = makeSimplagConfig()

                    val configFile = tmp.resolve("config.json")
                    val simplagConfigFile = tmp.resolve("simplag-config.json")

                    val outf = Files.createFile(tmp.resolve("stdout.txt"))
                    val errf = Files.createFile(tmp.resolve("stderr.txt"))

                    JsonSerialiser.serialise(config, configFile)
                    JsonSerialiser.serialise(simplagConfig, simplagConfigFile)

                    try {
                        val success = executePrEP(configFile, tmp, outf, errf, EV2Config.RETRY_COUNT)

                        if (!success) {
                            System.err.println("Failed: ${submission.fileName}")
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
                        } catch (e: Exception) {
                        }
                    }
                }.whenComplete { void: Void?, t: Throwable? ->
                    t?.printStackTrace()
                    EV2Config.SEM.release()
                }
            }
        }
    }

    private fun executePrEP(config: Path, workingDir: Path, out: Path, err: Path, retryCount: Int): Boolean {
        for (i in 1..retryCount) {
            val confp = config.toAbsolutePath().toString()
            val process =
                Forker.exec(Application::class.java, arrayOf(confp), workingDir = workingDir, out = out, err = err)

            val result = process.waitFor(EV2Config.TIMEOUT, TimeUnit.MINUTES)

            if (result) {
                val exitCode = process.exitValue()
                if (exitCode == 0) return true
            } else {
                System.err.println("Time Limit exceeded")
                process.destroy()
                return false
            }

        }

        return false
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
                forceSeedSubmissions = arrayOf(
                    subName
                )
                seedSubmissionSelectionChance = 1.0
            }

            detection = DetectionConfig().apply {
                maxParallelism = EV2Config.MAX_PARALLEL
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

    private fun makeSimplagConfig(): SimPlagConfig {
        return SimPlagConfig().apply {
            input = ""
            injectionSources = ""
            output = ""

            randomSeed = 11121993

            copies = EV2Config.VARIANT_COUNT

            inject = InjectConfig().apply {
                injectAssignment = false
                injectFile = false
                injectClass = false
                injectMethod = false
                injectBlock = false
            }

            mutate = MutateConfig().apply {
                commenting = CommentingConfig().apply {
                    addChance = EV2Config.random.nextDouble(0.0, 1.0)
                    add = EV2Config.random.nextBoolean()

                    removeChance = EV2Config.random.nextDouble(0.0, 1.0)
                    remove = EV2Config.random.nextBoolean()

                    mutateChance = EV2Config.random.nextDouble(0.0, 1.0)
                    mutate = EV2Config.random.nextBoolean()
                }

                renameIdentifiersChance = EV2Config.random.nextDouble(0.0, 1.0)
                renameIdentifiers = EV2Config.random.nextBoolean()

                reorderStatementsChance = EV2Config.random.nextDouble(0.0, 1.0)
                reorderStatements = EV2Config.random.nextBoolean()

                reorderMemberDeclarationsChance = EV2Config.random.nextDouble(0.0, 1.0)
                reorderMemberDeclarations = EV2Config.random.nextBoolean()

                swapOperandsChance = EV2Config.random.nextDouble(0.0, 1.0)
                swapOperands = EV2Config.random.nextBoolean()

                upcastDataTypesChance = EV2Config.random.nextDouble(0.0, 1.0)
                upcastDataTypes = EV2Config.random.nextBoolean()

                switchToIfChance = EV2Config.random.nextDouble(0.0, 1.0)
                switchToIf = EV2Config.random.nextBoolean()

                forToWhileChance = EV2Config.random.nextDouble(0.0, 1.0)
                forToWhile = EV2Config.random.nextBoolean()

                expandCompoundAssignmentChance = EV2Config.random.nextDouble(0.0, 1.0)
                expandCompoundAssignment = EV2Config.random.nextBoolean()

                expandIncDecChance = EV2Config.random.nextDouble(0.0, 1.0)
                expandIncDec = EV2Config.random.nextBoolean()

                splitVariableDeclarationsChance = EV2Config.random.nextDouble(0.0, 1.0)
                splitVariableDeclarations = EV2Config.random.nextBoolean()

                assignDefaultValueChance = EV2Config.random.nextDouble(0.0, 1.0)
                assignDefaultValue = EV2Config.random.nextBoolean()

                splitDeclAndAssignmentChance = EV2Config.random.nextDouble(0.0, 1.0)
                splitDeclAndAssignment = EV2Config.random.nextBoolean()

                groupVariableDeclarationsChance = EV2Config.random.nextDouble(0.0, 1.0)
                groupVariableDeclarations = EV2Config.random.nextBoolean()
            }
        }
    }
}