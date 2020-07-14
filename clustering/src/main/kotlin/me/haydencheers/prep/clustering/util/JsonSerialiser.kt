package me.haydencheers.clustering

import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.json.bind.JsonbBuilder
import kotlin.reflect.KClass

object JsonSerialiser {
    private val jsonb = JsonbBuilder.create()

    fun serialise(obj: Any, out: OutputStream) {
        jsonb.toJson(obj, out)
    }

    fun serialise(obj: Any, path: Path) {
        Files.newBufferedWriter(path).use { fout ->
            jsonb.toJson(obj, fout)
        }
    }

    fun <T : Any> deserialise(path: Path, cls: KClass<T>): T {
        Files.newBufferedReader(path).use { fin ->
            return jsonb.fromJson(fin, cls.java)
        }
    }
}