package me.haydencheers.prep.normalise.comment

import me.haydencheers.prep.normalise.Parser
import me.haydencheers.prep.normalise.order.Orderer
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener
import java.nio.file.Files
import java.nio.file.Paths

object CommentRemover {
    @JvmStatic
    fun main(args: Array<String>) {
        for (arg in args) {
            val file = Paths.get(arg)

            if (!Files.exists(file) || !Files.isRegularFile(file)) throw IllegalArgumentException("File $file does not exists, or is not a file")

            try {
                val ast = Parser.parse(file) as CompilationUnit
                ast.accept(CommentRemoverVisitor)

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

    object CommentRemoverVisitor: ASTVisitor() {
        override fun visit(node: BlockComment): Boolean {
            node.parent.setStructuralProperty(node.locationInParent, null)
            return super.visit(node)
        }

        override fun visit(node: LineComment): Boolean {
            node.parent.setStructuralProperty(node.locationInParent, null)
            return super.visit(node)
        }

        override fun visit(node: Javadoc): Boolean {
            node.parent.setStructuralProperty(node.locationInParent, null)
            return super.visit(node)
        }
    }
}