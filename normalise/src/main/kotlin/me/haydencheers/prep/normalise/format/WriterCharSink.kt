package me.haydencheers.prep.normalise.format

import com.google.common.io.CharSink
import java.io.Writer

class WriterCharSink(val writer: Writer): CharSink() {
    override fun openStream(): Writer {
        return writer
    }
}