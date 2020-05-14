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

object EV2Random {

    val transformationIds = (1 .. 16).toList()

    @JvmStatic
    fun main(args: Array<String>) {
        val datasetRoot = Paths.get("/media/haydencheers/Data/PrEP/datasets")
        val workingDir = Paths.get("/media/haydencheers/Data/PrEP/EV2Random")

        for (datasetName in EV2Config.DATASET_NAMES) {
            println("Dataset: $datasetName")

            val datasetSubmissionRoot = datasetRoot.resolve(datasetName)
            if (!Files.exists(datasetSubmissionRoot)) throw IllegalArgumentException("dataset ${datasetName} does not exist!")

            val submissions = Files.list(datasetSubmissionRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (submission in submissions) {
                println("\tSubmission: $submission")

                for ((pname, pvalue) in EV2Config.VARIANT_LEVELS) {
                    println("\t\t$pname")

                    val transformationCount = EV2Config.random.nextInt(1, 17) // until is exclusive
                    val transformations = (1 .. transformationCount).map {
                        transformationIds.random(EV2Config.random)
                    }.toSet()

                    val storeOut = workingDir.resolve("out/${datasetName}/${submission.fileName}/$pname")
                    val storeWork = workingDir.resolve("work/${datasetName}/${submission.fileName}/$pname")
                    val storeLogs = workingDir.resolve("logs/${datasetName}/${submission.fileName}/$pname")

                    if (Files.exists(storeOut) && Files.exists(storeWork) && Files.exists(storeLogs))
                        continue

                    EV2Config.SEM.acquire()

                    CompletableFuture.runAsync {
                        val tmp = Files.createTempDirectory("PREP-EV2-Isolated-${submission.fileName}-$pname")
                        val work = Files.createDirectories(tmp.resolve("work"))
                        val out = Files.createDirectory(tmp.resolve("out"))
                        val seed = Files.createDirectory(tmp.resolve("seed"))
                        Files.copy(Paths.get("db.blob"), tmp.resolve("db.blob"))

                        val srcRoot = Files.createDirectory(tmp.resolve("src"))
                        CopyUtils.copyDir(submission, srcRoot.resolve(submission.fileName))

                        val config = makeConfig(submission.fileName.toString())
                        val simplagConfig = makeSimplagConfig(transformations, pvalue)

                        val configFile = tmp.resolve("config.json")
                        val simplagConfigFile = tmp.resolve("simplag-config.json")

                        val outf = Files.createFile(tmp.resolve("stdout.txt"))
                        val errf = Files.createFile(tmp.resolve("stderr.txt"))

                        JsonSerialiser.serialise(config, configFile)
                        JsonSerialiser.serialise(simplagConfig, simplagConfigFile)

                        try {
                            val success = executePrEP(configFile, tmp, outf, errf, EV2Config.RETRY_COUNT)

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
                    }.whenComplete { void: Void?, t: Throwable? ->
                        t?.printStackTrace()
                        EV2Config.SEM.release()
                    }
                }
            }
        }
    }

    private fun executePrEP(config: Path, workingDir: Path, out: Path, err: Path, retryCount: Int): Boolean {
        for (i in 1 .. retryCount) {
            val confp = config.toAbsolutePath().toString()
            val process = Forker.exec(Application::class.java, arrayOf(confp), workingDir = workingDir, out = out, err = err)

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

    private fun makeSimplagConfig(transformations: Set<Int>, chance: Double): SimPlagConfig {
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
                    addChance = chance
                    add = transformations.contains(1)

                    removeChance = chance
                    remove = transformations.contains(2)

                    mutateChance = chance
                    mutate = transformations.contains(3)
                }

                renameIdentifiersChance = chance
                renameIdentifiers = transformations.contains(4)

                reorderStatementsChance = chance
                reorderStatements = transformations.contains(5)

                reorderMemberDeclarationsChance = chance
                reorderMemberDeclarations = transformations.contains(6)

                swapOperandsChance = chance
                swapOperands = transformations.contains(7)

                upcastDataTypesChance = chance
                upcastDataTypes = transformations.contains(8)

                switchToIfChance = chance
                switchToIf = transformations.contains(9)

                forToWhileChance = chance
                forToWhile = transformations.contains(10)

                expandCompoundAssignmentChance = chance
                expandCompoundAssignment = transformations.contains(11)

                expandIncDecChance = chance
                expandIncDec = transformations.contains(12)

                splitVariableDeclarationsChance = chance
                splitVariableDeclarations = transformations.contains(13)

                assignDefaultValueChance = chance
                assignDefaultValue = transformations.contains(14)

                splitDeclAndAssignmentChance = chance
                splitDeclAndAssignment = transformations.contains(15)

                groupVariableDeclarationsChance = chance
                groupVariableDeclarations = transformations.contains(16)
            }
        }
    }
}