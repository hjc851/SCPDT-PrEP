package me.haydencheers.prep.scripts.ev3.script

import me.haydencheers.prep.scripts.Config
import me.haydencheers.prep.scripts.ev2.format
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object EV3LOCCount {
    @JvmStatic
    fun main(args: Array<String>) {
        println("General Metrics")
        doGeneralMetrics()
//        println()
//        println("Isolated")
//        doIsolated()
//        println()
//        println("All")
//        doAll()
    }

    fun doGeneralMetrics() {
        val src = Paths.get("/media/haydencheers/Data/SimPlag/src")
        val srcs = Files.list(src)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        val srcCount = srcs.count()
        var srcLocCount = 0.0

        for (src in srcs) srcLocCount += countLOC(src)
        val avgSrcLOC = srcLocCount / srcCount

        val sourceCount = Files.walk(src)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .count().toDouble()

        val avgFileCount = sourceCount / srcs.size

        println("Avg LOC\t${avgSrcLOC.format("2.2f")}")
        println("Avg Src Files\t${avgFileCount.format("2.2f")}")
    }

    fun doIsolated() {
        val src = Paths.get("/media/haydencheers/Data/SimPlag/src")
        val srcs = Files.list(src)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        val srcCount = srcs.count()
        var srcLocCount = 0.0

        for (src in srcs) srcLocCount += countLOC(src)
        val avgSrcLOC = srcLocCount / srcCount

        val root = Paths.get("/media/haydencheers/Data/SimPlag/out/isolated")
        for ((pname, _) in Config.VARIANT_LEVELS) {
            val proot = root.resolve(pname)

            for (i in 1 .. 4) {
                val vroot = proot.resolve(i.toString())
                val variants = Files.list(vroot)
                    .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                    .use { it.toList() }

                val varCount = variants.count()
                var locCount = 0.0

                for (variant in variants) locCount += countLOC(variant)
                val avgVarLOC = locCount / varCount
                val diff = avgVarLOC - avgSrcLOC

                println("${pname}\t${i}\t${avgVarLOC.format("2.2f")}\t${diff.format("2.2f")}")
            }
        }
    }

    fun doAll() {
        val src = Paths.get("/media/haydencheers/Data/SimPlag/src")
        val srcs = Files.list(src)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        val srcCount = srcs.count()
        var srcLocCount = 0.0

        for (src in srcs) srcLocCount += countLOC(src)
        val avgSrcLOC = srcLocCount / srcCount

        val root = Paths.get("/media/haydencheers/Data/SimPlag/out/all")
        for ((pname, _) in Config.VARIANT_LEVELS) {
            val proot = root.resolve(pname)
            val variants = Files.list(proot)
                .filter { Files.isDirectory(it) && !Files.isHidden(it) }
                .use { it.toList() }

            val varCount = variants.count()
            var locCount = 0.0

            for (variant in variants)
                locCount += countLOC(variant)
            val avgVarLOC = locCount / varCount
            val diff = avgVarLOC - avgSrcLOC

            println("${pname}\t${avgVarLOC.format("2.2f")}\t${diff.format("2.2f")}")
        }
    }

//    private fun countLOC(root: Path): Long {
//        return Files.walk(root)
//            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
//            .mapToLong {
//                Files.newBufferedReader(it, Charset.forName("ISO-8859-1")).lines().count()
//            }.sum()
//    }

    fun countLOC(root: Path): Int {
        return Files.walk(root)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .mapToInt {
                getLOC(it)
            }.sum()
    }

    private fun getLOC(file: Path): Int {
        val parser = ASTParser.newParser(AST.JLS13)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)

        val src = Files.newBufferedReader(file, Charset.forName("ISO-8859-1"))
            .lines()
            .toList()
            .joinToString("\n")

        parser.setSource(src.toCharArray())
        val root = parser.createAST(null)

        val formatted = root.toString()
        val lines = formatted.split("\n").size
        return lines
    }
}