package com.example.myapplication.data

import android.app.Application
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.network.RecordingItem
import com.example.myapplication.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.NetworkInterface
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.Request
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.myapplication.BuildConfig

data class RecordingMeta(
    val category: String = "",
    val title: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val transcript: String = ""
)

class RecordingMetadataRepository(private val app: Application) {
    private var chosenApi: ApiService? = null
    private val prefs = app.getSharedPreferences("retrofit_config", Context.MODE_PRIVATE)
    private fun saveBase(base: String) {
        prefs.edit().putString("last_base", base).apply()
    }
    private fun getSavedBase(): String? = prefs.getString("last_base", null)
    fun currentSavedBase(): String? = prefs.getString("last_base", null)
    fun clearSavedBase() {
        prefs.edit().remove("last_base").apply()
        chosenApi = null
    }
    suspend fun setManualBase(base: String): Boolean {
        val b = if (base.endsWith("/")) base else "$base/"
        return try {
            val svc = buildApi(b)
            svc.getRecordings()
            saveBase(b)
            chosenApi = svc
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val prod = Build.PRODUCT.lowercase()
        val model = Build.MODEL.lowercase()
        return fp.contains("generic") || fp.contains("unknown") ||
                prod.contains("sdk") || fp.contains("emulator") || model.contains("emulator")
    }

    private fun buildApi(base: String): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(if (base.endsWith("/")) base else "$base/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    private suspend fun ensureApi(): ApiService {
        chosenApi?.let { return it }
        val saved = getSavedBase()
        if (saved != null) {
            try {
                val svc = buildApi(saved)
                svc.getRecordings()
                chosenApi = svc
                return svc
            } catch (_: Exception) { }
        }
        if (!isEmulator()) {
            val discovered = discoverViaUdp()
            if (discovered != null) {
                try {
                    val svc = buildApi(discovered)
                    svc.getRecordings()
                    chosenApi = svc
                    saveBase(discovered)
                    return svc
                } catch (_: Exception) { }
            }
            try {
                val svc = buildApi(BuildConfig.SERVER_BASE)
                svc.getRecordings()
                chosenApi = svc
                saveBase(BuildConfig.SERVER_BASE)
                return svc
            } catch (_: Exception) { }
        } else {
            val base = "http://10.0.2.2:5000/"
            try {
                val svc = buildApi(base)
                svc.getRecordings()
                chosenApi = svc
                saveBase(base)
                return svc
            } catch (_: Exception) { }
        }
        val last = RetrofitClient.apiService
        chosenApi = last
        return last
    }

    private suspend fun scanSubnet(): String? = withContext(Dispatchers.IO) { null }

    private fun isReachable(base: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val url = if (base.endsWith("/")) base else "$base/"
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            resp.use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun guessLocalCandidates(): List<String> = emptyList()

    private suspend fun discoverViaUdp(): String? = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 800
            val msg = "DISCOVER_RETROFIT_API".toByteArray()
            val addr = InetAddress.getByName("255.255.255.255")
            val sendPacket = DatagramPacket(msg, msg.size, addr, 5001)
            socket.send(sendPacket)
            val buf = ByteArray(256)
            val recv = DatagramPacket(buf, buf.size)
            socket.receive(recv)
            val resp = String(recv.data, 0, recv.length).trim()
            if (resp.startsWith("RETROFIT_API ")) {
                val base = resp.removePrefix("RETROFIT_API ").trim()
                socket.close()
                return@withContext base
            }
            socket.close()
        } catch (_: SocketTimeoutException) {
        } catch (_: Exception) {
        }
        null
    }

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
            toMeta(ensureApi().getRecording(fileName))
        } catch (e: Exception) {
            RecordingMeta()
        }
    }

    suspend fun getMeta(fileName: String): RecordingMeta = getRemoteOrDefault(fileName)

    suspend fun setCategory(fileName: String, category: String) {
        ensureExists(fileName)
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(category = category)
        try {
            ensureApi().updateRecording(fileName, toItem(fileName, updated))
        } catch (_: Exception) {
            ensureApi().createRecording(toItem(fileName, updated))
        }
    }

    suspend fun setTitle(fileName: String, title: String) {
        ensureExists(fileName)
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(title = title)
        try {
            ensureApi().updateRecording(fileName, toItem(fileName, updated))
        } catch (_: Exception) {
            ensureApi().createRecording(toItem(fileName, updated))
        }
    }

    suspend fun setNotes(fileName: String, notes: String) {
        ensureExists(fileName)
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(notes = notes)
        try {
            ensureApi().updateRecording(fileName, toItem(fileName, updated))
        } catch (_: Exception) {
            ensureApi().createRecording(toItem(fileName, updated))
        }
    }

    suspend fun setFavorite(fileName: String, favorite: Boolean) {
        ensureExists(fileName)
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(favorite = favorite)
        try {
            ensureApi().updateRecording(fileName, toItem(fileName, updated))
        } catch (_: Exception) {
            ensureApi().createRecording(toItem(fileName, updated))
        }
    }

    suspend fun getCategory(fileName: String): String? = getRemoteOrDefault(fileName).category
    suspend fun getTitle(fileName: String): String? = getRemoteOrDefault(fileName).title
    suspend fun getNotes(fileName: String): String? = getRemoteOrDefault(fileName).notes
    suspend fun isFavorite(fileName: String): Boolean = getRemoteOrDefault(fileName).favorite

    suspend fun loadAll(): Map<String, RecordingMeta> {
        return try {
            val list = ensureApi().getRecordings()
            list.associate { it.fileName to toMeta(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun delete(fileName: String) {
        try {
            ensureApi().deleteRecording(fileName)
        } catch (_: Exception) {}
    }

    suspend fun getTranscript(fileName: String): String = getRemoteOrDefault(fileName).transcript

    suspend fun transcribe(fileName: String, file: File): String {
        return try {
            val body = RequestBody.create("audio/mp4".toMediaTypeOrNull(), file)
            val part = MultipartBody.Part.createFormData("file", file.name, body)
            val updated = ensureApi().transcribeRecording(fileName, part)
            updated.transcript.ifBlank { "Transcripción vacía desde el servidor" }
        } catch (e: Exception) {
            "Error al transcribir: ${e.message ?: "desconocido"}"
        }
    }

    suspend fun ensureExists(fileName: String) {
        try {
            ensureApi().createRecording(RecordingItem(fileName = fileName))
        } catch (_: Exception) {
        }
    }

    suspend fun uploadAudio(fileName: String, file: File): Boolean {
        return try {
            val body = RequestBody.create("audio/mp4".toMediaTypeOrNull(), file)
            val part = MultipartBody.Part.createFormData("file", file.name, body)
            ensureApi().uploadRecording(fileName, part)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getRecordingsDirectory(app: Application): File {
        val extMusic = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val snapRecDir = File((extMusic ?: app.cacheDir), "SnapRec")
        if (!snapRecDir.exists()) {
            snapRecDir.mkdirs()
        }
        return if (snapRecDir.exists() && snapRecDir.canWrite()) {
            snapRecDir
        } else {
            app.cacheDir
        }
    }

    suspend fun downloadAudio(fileName: String): Boolean {
        return try {
            Log.d("RecordingMetadata", "Iniciando descarga de: $fileName")
            val response = ensureApi().downloadRecording(fileName)
            if (response.isSuccessful) {
                val body = response.body() ?: return false
                
                val extMusic = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                val snapRecDir = File((extMusic ?: app.cacheDir), "SnapRec")
                if (!snapRecDir.exists()) {
                    snapRecDir.mkdirs()
                }
                val outputFile = if (snapRecDir.exists() && snapRecDir.canWrite()) {
                    File(snapRecDir, fileName)
                } else {
                    File(app.cacheDir, fileName)
                }
                
                body.byteStream().use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d("RecordingMetadata", "Archivo guardado en: ${outputFile.absolutePath}")
                Log.d("RecordingMetadata", "Archivo existe: ${outputFile.exists()}, tamaño: ${outputFile.length()}")
                true
            } else {
                Log.e("RecordingMetadata", "Error en respuesta: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("RecordingMetadata", "Excepción descargando $fileName: ${e.message}", e)
            false
        }
    }

    suspend fun syncRecordingsFromServer(): Boolean {
        return try {
            val recordings = ensureApi().getRecordings()
            if (recordings.isEmpty()) {
                Log.d("RecordingMetadata", "No hay grabaciones en el servidor")
                return false
            }
            
            val recordingsDir = getRecordingsDirectory(app)
            
            // Asegurar que el directorio existe
            if (!recordingsDir.exists()) {
                val created = recordingsDir.mkdirs()
                Log.d("RecordingMetadata", "Directorio creado: $created en ${recordingsDir.absolutePath}")
            }
            
            // Obtener lista de archivos locales (buscar en ambas ubicaciones)
            val localFiles = mutableSetOf<String>()
            
            val extMusic = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val snapRecDir = File((extMusic ?: app.cacheDir), "SnapRec")
            if (snapRecDir.exists()) {
                snapRecDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.forEach {
                    localFiles.add(it.name)
                }
            }
            
            // Buscar en cacheDir también
            app.cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.forEach {
                localFiles.add(it.name)
            }
            
            Log.d("RecordingMetadata", "Archivos locales encontrados: ${localFiles.size}")
            Log.d("RecordingMetadata", "Grabaciones en servidor: ${recordings.size}")
            
            var downloadedCount = 0
            // Descargar archivos que no están localmente
            for (recording in recordings) {
                if (!localFiles.contains(recording.fileName)) {
                    Log.d("RecordingMetadata", "Descargando: ${recording.fileName}")
                    val downloaded = downloadAudio(recording.fileName)
                    if (downloaded) {
                        downloadedCount++
                        Log.d("RecordingMetadata", "Descargado exitosamente: ${recording.fileName}")
                    } else {
                        // Si falla en el directorio principal, intentar en cacheDir
                        try {
                            val response = ensureApi().downloadRecording(recording.fileName)
                            if (response.isSuccessful) {
                                val body = response.body() ?: continue
                                val outputFile = File(app.cacheDir, recording.fileName)
                                body.byteStream().use { input ->
                                    outputFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                downloadedCount++
                                Log.d("RecordingMetadata", "Descargado en cacheDir: ${recording.fileName}")
                            } else {
                                Log.e("RecordingMetadata", "Error descargando ${recording.fileName}: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("RecordingMetadata", "Excepción descargando ${recording.fileName}: ${e.message}")
                        }
                    }
                } else {
                    Log.d("RecordingMetadata", "Ya existe localmente: ${recording.fileName}")
                }
            }
            
            Log.d("RecordingMetadata", "Sincronización completa. Descargados: $downloadedCount")
            true
        } catch (e: Exception) {
            Log.e("RecordingMetadata", "Error sincronizando: ${e.message}", e)
            false
        }
    }
    
    fun getLocalAudioFile(fileName: String): File? {
        val extMusic = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val snapRecDir = File((extMusic ?: app.cacheDir), "SnapRec")
        val file1 = File(snapRecDir, fileName)
        if (file1.exists()) {
            return file1
        }
        
        // Buscar en cacheDir
        val file2 = File(app.cacheDir, fileName)
        if (file2.exists()) {
            return file2
        }
        
        return null
    }
}
