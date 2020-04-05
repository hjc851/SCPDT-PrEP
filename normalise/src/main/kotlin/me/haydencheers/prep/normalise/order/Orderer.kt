package me.haydencheers.prep.normalise.order

import me.haydencheers.prep.normalise.Parser
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener
import java.nio.file.Files
import java.nio.file.Paths

object Orderer {
    @JvmStatic
    fun main(args: Array<String>) {
        for (arg in args) {
            val file = Paths.get(arg)

            if (!Files.exists(file) || !Files.isRegularFile(file)) throw IllegalArgumentException("File $file does not exists, or is not a file")

            try {
                val ast = Parser.parse(file) as CompilationUnit
                ast.accept(OrdererVisitor)

                Files.newBufferedWriter(file).use { writer ->
                    val flattener = NaiveASTFlattener()
                    ast.accept(flattener)
                    writer.appendln(flattener.result)
                }

            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }
    }

    object OrdererVisitor: ASTVisitor() {
        override fun visit(node: TypeDeclaration): Boolean {
            val bd = node.bodyDeclarations()

            val declarations = mutableListOf<ASTNode>()
            while (bd.isNotEmpty()) {
                declarations.add(bd.removeAt(0) as ASTNode)
            }

            val groups = declarations.groupBy { it.nodeType }
            for ((key, members) in groups) {
                for (member in members.sortedBy { it.length }) {
                    bd.add(member)
                }
            }

            return true
        }
    }
}