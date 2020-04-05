package me.haydencheers.prep.normalise

import java.nio.file.Path

interface Normaliser {
    fun normalise(paths: List<Path>)
}