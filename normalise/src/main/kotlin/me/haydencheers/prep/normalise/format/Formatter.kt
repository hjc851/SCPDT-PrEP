package me.haydencheers.prep.normalise.format

import com.google.googlejavaformat.java.Formatter as GFormatter
import org.apache.commons.io.input.CharSequenceReader
import java.io.Reader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object Formatter {
    val formatter = GFormatter()

    @JvmStatic
    fun main(args: Array<String>) {
        for (arg in args) {
            val file = Paths.get(arg)

            if (!Files.exists(file) || !Files.isRegularFile(file)) throw IllegalArgumentException("File $file does not exists, or is not a file")

            try {
                val contents = StringBuffer()
                Files.lines(file, Charset.forName("ISO-8859-1"))
                    .use { lines -> lines.forEach { contents.appendln(it) } }

                val reader = CharSequenceReader(contents)
                val charsource = ReaderCharSource(reader)

                Files.newBufferedWriter(file).use { writer ->
                    val charsink = WriterCharSink(writer)
                    formatter.formatSource(charsource, charsink)
                }

            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }
    }
}