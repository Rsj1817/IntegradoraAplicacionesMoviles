package com.example.myapplication.ui.favorites

import android.app.Application
import android.media.MediaPlayer
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.RecordingMetadataRepository
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.TealAccent
import com.example.myapplication.ui.theme.TealDark
import com.example.myapplication.ui.theme.TealMid
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onNavigateBack: () -> Unit, onOpenRecording: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val metaRepo = RecordingMetadataRepository(app)

    val favorites = remember { mutableStateOf(listOf<File>()) }
    val durations = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val metaMapState = remember { mutableStateOf<Map<String, com.example.myapplication.data.RecordingMeta>>(emptyMap()) }

    LaunchedEffect(Unit) {
        // Sincronizar audios del servidor primero
        metaRepo.syncRecordingsFromServer()
        
        // Buscar en ambas ubicaciones: Music/SnapRec y cacheDir
        val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
        val snapRecDir = File(musicDir, "SnapRec")
        val cacheDir = context.cacheDir
        
        val allFiles = mutableListOf<File>()
        
        // Buscar en SnapRec (accesible por USB) - TODOS los .mp4
        if (snapRecDir.exists()) {
            snapRecDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.let {
                allFiles.addAll(it)
            }
        }
        
        // Buscar en cacheDir (fallback) - TODOS los .mp4
        cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.let {
            allFiles.addAll(it)
        }
        
        val files = allFiles.sortedByDescending { it.lastModified() }
        val favs = mutableListOf<File>()
        for (f in files) {
            if (metaRepo.isFavorite(f.name)) {
                favs.add(f)
            }
        }
        favorites.value = favs

        metaMapState.value = metaRepo.loadAll()

        val map = mutableMapOf<String, Int>()
        favorites.value.forEach { f ->
            try {
                val player = MediaPlayer()
                player.setDataSource(f.absolutePath)
                player.prepare()
                map[f.name] = player.duration
                player.release()
            } catch (_: Exception) {}
        }
        durations.value = map
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("SnapRec", color = OffWhite) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Image(
                        painter = painterResource(id = R.drawable.flechasalida),
                        contentDescription = "Atrás",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(OffWhite)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TealDark)
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(OffWhite)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TealMid.copy(alpha = 0.12f))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = TealAccent)
                    Text(text = "Tus Favoritos", fontSize = 22.sp, color = TealDark, fontWeight = FontWeight.SemiBold)
                    Text(text = "Aquí tienes tus grabaciones más importantes", color = TealMid)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                items(favorites.value) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(OffWhite)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.microfono),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            colorFilter = ColorFilter.tint(TealMid)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val title = metaMapState.value[file.name]?.title
                            Text(text = (title?.takeIf { it.isNotBlank() } ?: file.name), color = TealDark, fontWeight = FontWeight.SemiBold)
                            Text(text = relativeTime(file.lastModified()), color = TealMid)
                        }
                        val dur = durations.value[file.name] ?: 0
                        Text(text = formatTime(dur), color = TealMid, modifier = Modifier.padding(end = 8.dp))
                        IconButton(onClick = { onOpenRecording(file.name) }) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Abrir", tint = TealAccent)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

private fun relativeTime(lastModified: Long): String {
    val diff = System.currentTimeMillis() - lastModified
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        hours < 1 -> "Hace minutos"
        hours == 1L -> "Hace 1 hora"
        hours < 24 -> "Hace ${hours} horas"
        days == 1L -> "Ayer"
        else -> "Hace ${days} días"
    }
}
