package com.example.myapplication.data

import android.app.Application
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.RecordingItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

data class RecordingMeta(
    val category: String = "",
    val title: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val transcript: String = ""
)

class RecordingMetadataRepository(private val app: Application) {
    private val api = RetrofitClient.apiService

    private fun toMeta(item: RecordingItem): RecordingMeta =
        RecordingMeta(
            category = item.category,
            title = item.title,
            notes = item.notes,
            favorite = item.favorite,
            transcript = item.transcript
        )

    private fun toItem(fileName: String, meta: RecordingMeta): RecordingItem =
        RecordingItem(
            fileName = fileName,
            category = meta.category,
            title = meta.title,
            notes = meta.notes,
            favorite = meta.favorite,
            transcript = meta.transcript
        )

    private suspend fun getRemoteOrDefault(fileName: String): RecordingMeta {
        return try {
            toMeta(api.getRecording(fileName))
        } catch (e: Exception) {
            RecordingMeta()
        }
    }

    suspend fun setCategory(fileName: String, category: String) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(category = category)
        api.updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun setTitle(fileName: String, title: String) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(title = title)
        api.updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun setNotes(fileName: String, notes: String) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(notes = notes)
        api.updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun setFavorite(fileName: String, favorite: Boolean) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(favorite = favorite)
        api.updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun getCategory(fileName: String): String? = getRemoteOrDefault(fileName).category
    suspend fun getTitle(fileName: String): String? = getRemoteOrDefault(fileName).title
    suspend fun getNotes(fileName: String): String? = getRemoteOrDefault(fileName).notes
    suspend fun isFavorite(fileName: String): Boolean = getRemoteOrDefault(fileName).favorite

    suspend fun loadAll(): Map<String, RecordingMeta> {
        val list = api.getRecordings()
        return list.associate { it.fileName to toMeta(it) }
    }

    suspend fun delete(fileName: String) {
        try {
            api.deleteRecording(fileName)
        } catch (_: Exception) {}
    }

    suspend fun getTranscript(fileName: String): String = getRemoteOrDefault(fileName).transcript

    suspend fun transcribe(fileName: String, file: File): String {
        val body = RequestBody.create("audio/mp4".toMediaTypeOrNull(), file)
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        val updated = api.transcribeRecording(fileName, part)
        return updated.transcript
    }
}
