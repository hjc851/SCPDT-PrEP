package me.haydencheers.prep.normalise.format

import com.google.common.io.CharSource
import java.io.Reader

class ReaderCharSource(val reader: Reader): CharSource() {
    override fun openStream(): Reader {
        return reader
    }
}