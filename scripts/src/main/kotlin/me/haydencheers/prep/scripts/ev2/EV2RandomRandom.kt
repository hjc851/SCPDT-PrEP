package me.haydencheers.prep.scripts.ev2

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
        val workingDir = Config.EV2_WORKING_ROOT.resolve("RandomRandom")

        for (datasetName in Config.DATASET_NAMES) {
            println("Dataset: $datasetName")

            val datasetSubmissionRoot = Config.DATASET_ROOT.resolve(datasetName)
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

                Config.SEM.acquire()

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
                        val success = executePrEP(
                            configFile,
                            tmp,
                            outf,
                            errf,
                            Config.RETRY_COUNT
                        )

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
                    Config.SEM.release()
                }
            }
        }
    }

    private fun executePrEP(config: Path, workingDir: Path, out: Path, err: Path, retryCount: Int): Boolean {
        for (i in 1..retryCount) {
            val confp = config.toAbsolutePath().toString()
            val process = Forker.exec(Application::class.java, arrayOf(confp), workingDir = workingDir, out = out, err = err)

            val result = process.waitFor(Config.TIMEOUT, TimeUnit.MINUTES)

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
                maxParallelism = Config.MAX_PARALLEL
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

            copies = Config.VARIANT_COUNT

            inject = InjectConfig().apply {
                injectAssignment = false
                injectFile = false
                injectClass = false
                injectMethod = false
                injectBlock = false
            }

            mutate = MutateConfig().apply {
                commenting = CommentingConfig().apply {
                    addChance = Config.random.nextDouble(0.0, 1.0)
                    add = Config.random.nextBoolean()

                    removeChance = Config.random.nextDouble(0.0, 1.0)
                    remove = Config.random.nextBoolean()

                    mutateChance = Config.random.nextDouble(0.0, 1.0)
                    mutate = Config.random.nextBoolean()
                }

                renameIdentifiersChance = Config.random.nextDouble(0.0, 1.0)
                renameIdentifiers = Config.random.nextBoolean()

                reorderStatementsChance = Config.random.nextDouble(0.0, 1.0)
                reorderStatements = Config.random.nextBoolean()

                reorderMemberDeclarationsChance = Config.random.nextDouble(0.0, 1.0)
                reorderMemberDeclarations = Config.random.nextBoolean()

                swapOperandsChance = Config.random.nextDouble(0.0, 1.0)
                swapOperands = Config.random.nextBoolean()

                upcastDataTypesChance = Config.random.nextDouble(0.0, 1.0)
                upcastDataTypes = Config.random.nextBoolean()

                switchToIfChance = Config.random.nextDouble(0.0, 1.0)
                switchToIf = Config.random.nextBoolean()

                forToWhileChance = Config.random.nextDouble(0.0, 1.0)
                forToWhile = Config.random.nextBoolean()

                expandCompoundAssignmentChance = Config.random.nextDouble(0.0, 1.0)
                expandCompoundAssignment = Config.random.nextBoolean()

                expandIncDecChance = Config.random.nextDouble(0.0, 1.0)
                expandIncDec = Config.random.nextBoolean()

                splitVariableDeclarationsChance = Config.random.nextDouble(0.0, 1.0)
                splitVariableDeclarations = Config.random.nextBoolean()

                assignDefaultValueChance = Config.random.nextDouble(0.0, 1.0)
                assignDefaultValue = Config.random.nextBoolean()

                splitDeclAndAssignmentChance = Config.random.nextDouble(0.0, 1.0)
                splitDeclAndAssignment = Config.random.nextBoolean()

                groupVariableDeclarationsChance = Config.random.nextDouble(0.0, 1.0)
                groupVariableDeclarations = Config.random.nextBoolean()
            }
        }
    }
}