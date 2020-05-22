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

object EV25Transformations {
    @JvmStatic
    fun main(args: Array<String>) {
        val workingDir = Config.EV2_WORKING_ROOT.resolve("Isolated5")

        for (datasetName in Config.DATASET_NAMES) {
            println("Dataset: $datasetName")

            val datasetSubmissionRoot = Config.DATASET_ROOT.resolve(datasetName)
            if (!Files.exists(datasetSubmissionRoot)) throw IllegalArgumentException("dataset ${datasetName} does not exist!")

            val submissions = Files.list(datasetSubmissionRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (submission in submissions) {
                println("\tSubmission: $submission")

                for ((pname, pchance) in Config.VARIANT_LEVELS) {
                    println("\t\t$pname")

                    val storeOut = workingDir.resolve("out/${datasetName}/${submission.fileName}/${pname}")
                    val storeWork = workingDir.resolve("work/${datasetName}/${submission.fileName}/${pname}")
                    val storeLogs = workingDir.resolve("logs/${datasetName}/${submission.fileName}/${pname}")

                    if (Files.exists(storeOut) && Files.exists(storeWork) && Files.exists(storeLogs)) continue
                    Config.SEM.acquire()

                    CompletableFuture.runAsync {
                        val tmp = Files.createTempDirectory("PREP-EV2-Isolated5-${submission.fileName}")

                        val work = Files.createDirectories(tmp.resolve("work"))
                        val out = Files.createDirectory(tmp.resolve("out"))
                        val seed = Files.createDirectory(tmp.resolve("seed"))
                        Files.copy(Paths.get("db.blob"), tmp.resolve("db.blob"))

                        val srcRoot = Files.createDirectory(tmp.resolve("src"))
                        CopyUtils.copyDir(submission, srcRoot.resolve(submission.fileName))

                        val config = makeConfig(submission.fileName.toString())
                        val simplagConfig = makeSimplagConfig(pchance)

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

    private fun makeSimplagConfig(chance: Double): SimPlagConfig {
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
                    addChance = 0.0
                    add = false

                    removeChance = 0.0
                    remove = false

                    mutateChance = 0.0
                    mutate = false
                }

                renameIdentifiersChance = 0.0
                renameIdentifiers = false

                reorderStatementsChance = chance
                reorderStatements = true

                reorderMemberDeclarationsChance = chance
                reorderMemberDeclarations = true

                swapOperandsChance = chance
                swapOperands = true

                upcastDataTypesChance = 0.0
                upcastDataTypes = false

                switchToIfChance = 0.0
                switchToIf = false

                forToWhileChance = chance
                forToWhile = true

                expandCompoundAssignmentChance = 0.0
                expandCompoundAssignment = false

                expandIncDecChance = 0.0
                expandIncDec = false

                splitVariableDeclarationsChance = 0.0
                splitVariableDeclarations = false

                assignDefaultValueChance = 0.0
                assignDefaultValue = false

                splitDeclAndAssignmentChance = chance
                splitDeclAndAssignment = true

                groupVariableDeclarationsChance = 0.0
                groupVariableDeclarations = false
            }
        }
    }
}