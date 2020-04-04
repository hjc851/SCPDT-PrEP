package me.haydencheers.prep.util

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.json.bind.JsonbBuilder
import kotlin.reflect.KClass

object JsonSerialiser {
    private val jsonb = JsonbBuilder.create()
    private val ENTRY_NAME = "bean.data"

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

    fun serialiseCompressed(bean: Any, path: Path) {
        Files.newOutputStream(path).use { fout ->
            val zout = ZipOutputStream(fout)

            val entry = ZipEntry(ENTRY_NAME)
            zout.putNextEntry(entry)
            jsonb.toJson(bean, zout)
            zout.closeEntry()
        }
    }

    fun <T: Any> deserialiseCompressed(path: Path, cls: KClass<T>): T {
        Files.newInputStream(path).use { fin ->
            val zin = ZipInputStream(fin)
            val entry = zin.nextEntry

            if (entry.name != ENTRY_NAME) throw IllegalArgumentException("Invalid STRF file")

            return jsonb.fromJson(zin, cls.java)
        }
    }
}