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

    private fun isEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val prod = Build.PRODUCT.lowercase()
        val model = Build.MODEL.lowercase()
        return fp.contains("generic") || fp.contains("unknown") ||
                prod.contains("sdk") || fp.contains("emulator") || model.contains("emulator")
    }

    private fun buildApi(base: String): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
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
                if (isReachable(saved)) {
                    val svc = buildApi(saved)
                    svc.getRecordings()
                    chosenApi = svc
                    return svc
                }
            } catch (_: Exception) { }
        }
        val candidates = if (isEmulator()) {
            listOf("http://10.0.2.2:5000/")
        } else {
            listOf(
                "http://192.168.0.204:5000/",
                "http://10.30.170.186:5000/",
                "http://127.0.0.1:5000/",
                "http://10.221.7.186:5000/"
            )
        }
        val hotspotGuesses = guessLocalCandidates()
        if (!isEmulator()) {
            val discovered = discoverViaUdp()
            if (discovered != null) {
                try {
                    if (isReachable(discovered)) {
                        val svc = buildApi(discovered)
                        svc.getRecordings()
                        chosenApi = svc
                        saveBase(discovered)
                        return svc
                    }
                } catch (_: Exception) { }
            }
        }
        for (base in candidates + hotspotGuesses) {
            try {
                if (isReachable(base)) {
                    val svc = buildApi(base)
                    svc.getRecordings()
                    chosenApi = svc
                    saveBase(base)
                    return svc
                }
            } catch (_: Exception) { }
        }
        val fallback = RetrofitClient.apiService
        chosenApi = fallback
        return fallback
    }

    private fun isReachable(base: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
            val url = if (base.endsWith("/")) base else "$base/"
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            resp.use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun guessLocalCandidates(): List<String> {
        val addrs = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val inetAddrs = intf.inetAddresses
                for (inet in inetAddrs) {
                    val host = inet.hostAddress ?: continue
                    if (host.contains(":")) continue // skip IPv6
                    if (host.startsWith("127.")) continue
                    val parts = host.split(".")
                    if (parts.size == 4) {
                        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                        val last = parts[3].toIntOrNull() ?: 0
                        val candidatesLast = listOf(1, 10, 20, 30, 50, 100, 101, 150, 200, 254, last - 1, last + 1)
                            .mapNotNull { if (it in 1..254) it else null }
                        candidatesLast.forEach { l ->
                            addrs.add("http://$prefix.$l:5000/")
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        // Common Android hotspot prefix
        val defaults = listOf(
            "http://192.168.43.1:5000/",
            "http://192.168.1.1:5000/",
            "http://192.168.1.100:5000/",
            "http://192.168.0.1:5000/",
            "http://192.168.0.100:5000/"
        )
        return (addrs + defaults).distinct()
    }

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

    suspend fun setCategory(fileName: String, category: String) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(category = category)
        ensureApi().updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun setTitle(fileName: String, title: String) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(title = title)
        ensureApi().updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun setNotes(fileName: String, notes: String) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(notes = notes)
        ensureApi().updateRecording(fileName, toItem(fileName, updated))
    }

    suspend fun setFavorite(fileName: String, favorite: Boolean) {
        val current = getRemoteOrDefault(fileName)
        val updated = current.copy(favorite = favorite)
        ensureApi().updateRecording(fileName, toItem(fileName, updated))
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
            val bases = mutableListOf<String>()
            val saved = getSavedBase()
            if (!saved.isNullOrBlank()) bases.add(saved)
            bases.addAll(
                listOf(
                    "http://192.168.0.204:5000/",
                    "http://10.30.170.186:5000/",
                    "http://127.0.0.1:5000/"
                )
            )
            try {
                val disc = discoverViaUdp()
                if (disc != null) bases.add(disc)
            } catch (_: Exception) { }
            bases.addAll(guessLocalCandidates())
            val client = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
            for (b in bases.distinct()) {
                val url = (if (b.endsWith("/")) b else "$b/") + "recordings"
                try {
                    val req = Request.Builder().url(url).get().build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "[]"
                            val type = object : TypeToken<List<RecordingItem>>() {}.type
                            val list = Gson().fromJson<List<RecordingItem>>(body, type)
                            if (list.isNotEmpty()) {
                                saveBase(b)
                                return list.associate { it.fileName to toMeta(it) }
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
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
            updated.transcript
        } catch (_: Exception) {
            ""
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
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val snapRecDir = File(musicDir, "SnapRec")
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
                
                // Intentar guardar en Music/SnapRec primero
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val snapRecDir = File(musicDir, "SnapRec")
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
            
            // Buscar en Music/SnapRec
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val snapRecDir = File(musicDir, "SnapRec")
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
        // Buscar en Music/SnapRec primero
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val snapRecDir = File(musicDir, "SnapRec")
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
