package me.haydencheers.prep.clustering.generation

import me.haydencheers.clustering.JsonSerialiser
import me.haydencheers.clustering.datasetNames
import me.haydencheers.prep.beans.ListingsFactory
import me.haydencheers.prep.beans.SubmissionListing
import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.seeding.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import kotlin.streams.toList

object GenerationDataSetGenerator {

    val root = Paths.get("/media/haydencheers/Data/PrEP/Clustering-EV2-Datasets")
    val VARIANT_COUNT = 3
    val GENERATIONS = 2

    @JvmStatic
    fun main(args: Array<String>) {
        val tmp = Files.createTempDirectory("PrEP-clustering")
        val work = Files.createDirectories(root.resolve(Instant.now().epochSecond.toString()))

        Files.copy(Paths.get("db.blob"), tmp.resolve("db.blob"))

        Runtime.getRuntime().addShutdownHook(Thread() {
            try {
                Files.walk(tmp)
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        val simplag = SimPlagBinding()
        simplag.thaw(tmp.resolve("simplag"))

        for (ds in datasetNames) {
            val work = Files.createDirectory(work.resolve(ds))

            val dsroot = Config.DATASET_ROOT.resolve(ds)
            val rootSubmissions = Files.list(dsroot)
                .filter {
                    Files.walk(it)
                        .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".java") }
                        .count() > 0
                }
                .use { it.toList() }
                .shuffled()
                .take(VARIANT_COUNT)
                .map { ListingsFactory.produceForSubmission(it, false) }

            val orignals = rootSubmissions.map { it.name }
            val origf = work.resolve("roots.txt")
            Files.newBufferedWriter(origf).use {
                for (orig in orignals) {
                    it.write(orig)
                    it.newLine()
                }
            }

            for ((pname, pvalue) in Config.VARIANT_LEVELS) {
                val subsByDistance = mutableListOf<List<SubmissionListing>>()
                subsByDistance.add(rootSubmissions)

                for (depth in 0 until GENERATIONS) {
                    val basePrograms = subsByDistance[depth]

                    val work = work.resolve(pname)
                    Files.createDirectories(work)

                    val config =
                        makeSimplagConfig(
                            pvalue
                        )
                    val configpath = tmp.resolve("simplagconfig.json")
                    JsonSerialiser.serialise(config, configpath)

                    val results = simplag.execute(basePrograms, dsroot, configpath, Files.createDirectory(work.resolve("gen-$depth")))
                    subsByDistance.add(results)
                }
            }

            println()
            println()
            println("Done!!!!")
        }
    }

    private fun makeSimplagConfig(chance: Double): SimPlagConfig {
        return SimPlagConfig().apply {
            input = ""
            injectionSources = ""
            output = ""

            randomSeed = 11121993

            copies = VARIANT_COUNT

            inject = InjectConfig().apply {
                injectAssignment = false

                injectFile = false
                injectFileChance = 0.0
                injectFileMaxCount = 1

                injectClass = false
                injectClassChance = 0.0
                injectClassMaxCount = 1

                injectMethod = false
                injectMethodChance = 0.0
                injectMethodMaxCount = 2

                injectBlock = false
                injectBlockChance = 0.0
                injectBlockMaxStatements = 5
            }

            mutate = MutateConfig().apply {
                commenting = CommentingConfig().apply {
                    addChance = chance
                    add = true

                    removeChance = chance
                    remove = true

                    mutateChance = chance
                    mutate = true
                }

                renameIdentifiersChance = chance
                renameIdentifiers = true

                reorderStatementsChance = chance
                reorderStatements = true

                reorderMemberDeclarationsChance = chance
                reorderMemberDeclarations = true

                swapOperandsChance = chance
                swapOperands = true

                upcastDataTypesChance = chance
                upcastDataTypes = true

                switchToIfChance = chance
                switchToIf = true

                forToWhileChance = chance
                forToWhile = true

                expandCompoundAssignmentChance = chance
                expandCompoundAssignment = true

                expandIncDecChance = chance
                expandIncDec = true

                splitVariableDeclarationsChance = chance
                splitVariableDeclarations = true

                assignDefaultValueChance = chance
                assignDefaultValue = true

                splitDeclAndAssignmentChance = chance
                splitDeclAndAssignment = true

                groupVariableDeclarationsChance = chance
                groupVariableDeclarations = true
            }
        }
    }
}