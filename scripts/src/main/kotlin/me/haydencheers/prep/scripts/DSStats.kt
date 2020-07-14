package me.haydencheers.prep.scripts

import me.haydencheers.prep.scripts.ev2.format
import me.haydencheers.prep.scripts.ev3.script.EV3LOCCount
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

object DSStats {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = Config.DATASET_ROOT

        for (ds in Config.DATASET_NAMES) {
            val dsroot = root.resolve(ds)

            println(dsroot)
            println("loc\t${avgLOC(dsroot).format("2.2f")}")
            println("files\t${avgFiles(dsroot).format("2.2f")}")
            println("classes\t${avgClasses(dsroot).format("2.2f")}")
            println("methods\t${avgMethods(dsroot).format("2.2f")}")
            println("statements\t${avgStatements(dsroot).format("2.2f")}")
        }
    }

    fun avgLOC(root: Path): Double {
        var counter = 0
        var accumulator = 0.0

        val subs = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (sub in subs) {
            val loc = EV3LOCCount.countLOC(sub)
            accumulator = (accumulator * counter + loc) / ++counter
        }

        return accumulator
    }

    fun avgFiles(root: Path): Double {
        var counter = 0
        var accumulator = 0.0

        val subs = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (sub in subs) {
            val sources = Files.walk(sub)
                .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
                .count()

            accumulator = (accumulator * counter + sources) / ++counter
        }

        return accumulator
    }

    fun avgClasses(root: Path): Double {
        var counter = 0
        var accumulator = 0.0

        val subs = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (sub in subs) {
            val classes = countClasses(sub)
            accumulator = (accumulator * counter + classes) / ++counter
        }

        return accumulator
    }

    private fun countClasses(subroot: Path): Int {
        val visitor = object : ASTVisitor() {
            var counter = 0

            override fun preVisit(node: ASTNode?) {
                if (node is AbstractTypeDeclaration) {
                    counter++
                }
            }
        }

        val sources = Files.walk(subroot)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .use { it.toList() }

        for (source in sources) {
            try {
                val ast = parse(source)
                ast.accept(visitor)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return visitor.counter
    }

    fun avgMethods(root: Path): Double {
        var counter = 0
        var accumulator = 0.0

        val subs = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (sub in subs) {
            val methods = countMethods(sub)
            accumulator = (accumulator * counter + methods) / ++counter
        }

        return accumulator
    }

    private fun countMethods(subroot: Path): Int {
        val visitor = object : ASTVisitor() {
            var counter = 0

            override fun preVisit(node: ASTNode?) {
                if (node is MethodDeclaration || node is ConstructorDeclaration) {
                    counter++
                }
            }
        }

        val sources = Files.walk(subroot)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .use { it.toList() }

        for (source in sources) {
            try {
                val ast = parse(source)
                ast.accept(visitor)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return visitor.counter
    }

    fun avgStatements(root: Path): Double {
        var counter = 0
        var accumulator = 0.0

        val subs = Files.list(root)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .use { it.toList() }

        for (sub in subs) {
            val statements = countStatements(sub)
            accumulator = (accumulator * counter + statements) / ++counter
        }

        return accumulator
    }

    private fun countStatements(subroot: Path): Int {
        val visitor = object : ASTVisitor() {
            var counter = 0

            override fun preVisit(node: ASTNode?) {
                if (node is Statement) {
                    counter++
                }
            }
        }

        val sources = Files.walk(subroot)
            .filter { Files.isRegularFile(it) && !Files.isHidden(it) && it.fileName.toString().endsWith(".java") }
            .use { it.toList() }

        for (source in sources) {
            try {
                val ast = parse(source)
                ast.accept(visitor)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return visitor.counter
    }

    private fun parse(file: Path): ASTNode {
        val parser = ASTParser.newParser(AST.JLS13)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)

        val src = Files.newBufferedReader(file, Charset.forName("ISO-8859-1"))
            .lines()
            .toList()
            .joinToString("\n")

        parser.setSource(src.toCharArray())
        val root = parser.createAST(null)
        return root
    }
}

