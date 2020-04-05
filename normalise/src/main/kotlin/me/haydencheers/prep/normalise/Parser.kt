package me.haydencheers.prep.normalise

import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ASTParser
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

object Parser {
    fun parse(path: Path): ASTNode {
        val buffer = StringBuilder()
        Files.lines(path, Charset.forName("ISO-8859-1"))
            .forEach { buffer.appendln(it) }

        val src = buffer.toString()
        val parser = ASTParser.newParser(AST.JLS13)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)
        parser.setSource(src.toCharArray())
        val ast = parser.createAST(null)

        return ast
    }
}