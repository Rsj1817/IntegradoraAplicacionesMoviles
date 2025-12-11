package com.example.myapplication.data

import android.app.Application
import java.io.File

data class RecordingMeta(
    val category: String = "",
    val title: String = "",
    val notes: String = ""
)

class RecordingMetadataRepository(private val app: Application) {
    private val metaFile = File(app.filesDir, "recordings_meta.txt")

    fun setCategory(fileName: String, category: String) {
        val map = loadAll().toMutableMap()
        val current = map[fileName] ?: RecordingMeta()
        map[fileName] = current.copy(category = category)
        save(map)
    }

    fun setTitle(fileName: String, title: String) {
        val map = loadAll().toMutableMap()
        val current = map[fileName] ?: RecordingMeta()
        map[fileName] = current.copy(title = title)
        save(map)
    }

    fun setNotes(fileName: String, notes: String) {
        val map = loadAll().toMutableMap()
        val current = map[fileName] ?: RecordingMeta()
        map[fileName] = current.copy(notes = notes)
        save(map)
    }

    fun getCategory(fileName: String): String? = loadAll()[fileName]?.category
    fun getTitle(fileName: String): String? = loadAll()[fileName]?.title
    fun getNotes(fileName: String): String? = loadAll()[fileName]?.notes

    fun loadAll(): Map<String, RecordingMeta> {
        if (!metaFile.exists()) return emptyMap()
        val lines = metaFile.readLines()
        val result = mutableMapOf<String, RecordingMeta>()
        for (line in lines) {
            val parts = line.split('|')
            if (parts.isNotEmpty()) {
                val name = parts.getOrNull(0) ?: continue
                val cat = parts.getOrNull(1) ?: ""
                val title = parts.getOrNull(2) ?: ""
                val notes = parts.getOrNull(3) ?: ""
                if (name.isNotBlank()) {
                    result[name] = RecordingMeta(cat, title, notes)
                }
            }
        }
        return result
    }

    private fun save(map: Map<String, RecordingMeta>) {
        if (!metaFile.parentFile.exists()) metaFile.parentFile.mkdirs()
        val content = buildString {
            map.forEach { (k, v) ->
                append(k)
                    .append('|').append(v.category)
                    .append('|').append(v.title)
                    .append('|').append(v.notes)
                    .append('\n')
            }
        }
        metaFile.writeText(content)
    }

    fun delete(fileName: String) {
        val map = loadAll().toMutableMap()
        map.remove(fileName)
        save(map)
    }
}
