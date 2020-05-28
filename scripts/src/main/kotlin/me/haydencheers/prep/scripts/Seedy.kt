package me.haydencheers.prep.scripts

import me.haydencheers.prep.util.FileUtils
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

object Seedy {
    @JvmStatic
    fun main(args: Array<String>) {
        val dst = Files.createDirectory(Paths.get("/media/haydencheers/Data/PrEP/EV3/Seeds"))

        for (datasetName in Config.DATASET_NAMES) {
            val dsRoot = Config.DATASET_ROOT.resolve(datasetName)

            val subs = Files.list(dsRoot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            for (sub in subs) {
                val dst = dst.resolve("${datasetName}::${sub.fileName}")
                FileUtils.copyDirOnlyJava(sub, dst)
            }
        }
    }
}