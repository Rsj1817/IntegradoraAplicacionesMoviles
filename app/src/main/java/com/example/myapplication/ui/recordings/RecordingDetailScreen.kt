package com.example.myapplication.ui.recordings

import android.app.Application
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.RecordingMetadataRepository
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.TealAccent
import com.example.myapplication.ui.theme.TealDark
import com.example.myapplication.ui.theme.TealMid
import com.example.myapplication.viewmodel.PlaybackViewModel
import com.example.myapplication.viewmodel.PlaybackViewModelFactory
import kotlinx.coroutines.launch
import android.net.Uri
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(name: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: PlaybackViewModel = viewModel(factory = PlaybackViewModelFactory(app))
    val metaRepo = RecordingMetadataRepository(app)
    val scope = rememberCoroutineScope()

    // Estado de scroll para que si la pantalla es chica, se pueda bajar
    val scrollState = rememberScrollState()

    val files = remember { mutableStateOf(listOf<File>()) }
    
    val decodedName = remember(name) {
        try { Uri.decode(name) } catch (_: Exception) { name }
    }

    LaunchedEffect(Unit) {
        val recordings = metaRepo.loadAll()
        scope.launch { metaRepo.syncRecordingsFromServer() }
        fun findLocalFiles(): List<File> {
            val extMusic = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
            val baseDir = extMusic ?: context.cacheDir
            val snapRecDir = File(baseDir, "SnapRec")
            val cacheDir = context.cacheDir
            val allFiles = mutableListOf<File>()
            if (snapRecDir.exists()) {
                snapRecDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.let { allFiles.addAll(it) }
            }
            cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.let { allFiles.addAll(it) }
            return allFiles
        }
        var allFiles = findLocalFiles()
        if (allFiles.isEmpty() && recordings.isNotEmpty()) {
            for (fileName in recordings.keys) {
                metaRepo.downloadAudio(fileName)
            }
            allFiles = findLocalFiles()
        }
        files.value = allFiles.sortedByDescending { it.lastModified() }
    }

    val currentIndex = remember { mutableStateOf(0) }
    LaunchedEffect(files.value, currentIndex.value) {
        files.value.getOrNull(currentIndex.value)?.let { vm.load(it) }
    }
    LaunchedEffect(files.value, decodedName) {
        var idx = files.value.indexOfFirst { it.name == decodedName }
        if (idx < 0 && decodedName.isNotBlank()) {
            scope.launch {
                metaRepo.downloadAudio(decodedName)
                val extMusic = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
                val baseDir = extMusic ?: context.cacheDir
                val snapRecDir = File(baseDir, "SnapRec")
                val cacheDir = context.cacheDir
                val refreshed = buildList {
                    if (snapRecDir.exists()) {
                        snapRecDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.let { addAll(it.toList()) }
                    }
                    cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".mp4") }?.let { addAll(it.toList()) }
                }.sortedByDescending { it.lastModified() }
                files.value = refreshed
                idx = refreshed.indexOfFirst { it.name == decodedName }
                currentIndex.value = idx.coerceAtLeast(0)
            }
        } else {
            currentIndex.value = idx.coerceAtLeast(0)
        }
    }
    val favoriteState = remember { mutableStateOf(false) }
    val transcriptState = remember { mutableStateOf("") }

    val isPlaying by vm.isPlaying.collectAsState()
    val durationMs by vm.durationMs.collectAsState()
    val positionMs by vm.positionMs.collectAsState()

    val lastPrevClick = remember { mutableStateOf(0L) }

    val currentName = files.value.getOrNull(currentIndex.value)?.name ?: ""
    val titleState = remember { mutableStateOf(currentName) }
    val notesState = remember { mutableStateOf("") }
    val selected = remember { mutableStateOf("") }
    LaunchedEffect(decodedName) {
        if (decodedName.isBlank()) {
            titleState.value = ""
            notesState.value = ""
            selected.value = ""
            favoriteState.value = false
            transcriptState.value = ""
            return@LaunchedEffect
        }
        val initialTitle = titleState.value
        val initialNotes = notesState.value
        val initialCategory = selected.value
        val initialFavorite = favoriteState.value
        val initialTranscript = transcriptState.value
        val meta = metaRepo.getMeta(decodedName)
        if (titleState.value == initialTitle) {
            titleState.value = meta.title.takeIf { it.isNotBlank() } ?: decodedName
        }
        if (notesState.value == initialNotes) {
            notesState.value = meta.notes
        }
        if (selected.value == initialCategory) {
            selected.value = meta.category
        }
        if (favoriteState.value == initialFavorite) {
            favoriteState.value = meta.favorite
        }
        if (transcriptState.value == initialTranscript) {
            transcriptState.value = meta.transcript
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(16.dp)
            .verticalScroll(scrollState), 
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --- HEADER ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = titleState.value, fontSize = 22.sp, color = TealDark, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val newFav = !favoriteState.value
                    favoriteState.value = newFav
                    if (decodedName.isNotBlank()) {
                        scope.launch { metaRepo.setFavorite(decodedName, newFav) }
                    }
                }) {
                    Icon(imageVector = if (favoriteState.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorito", tint = TealAccent)
                }
            }
        }

        // --- TIEMPOS ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(positionMs), color = TealMid)
            Text(text = formatTime(durationMs), color = TealMid)
        }

        Slider(
            value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
            onValueChange = { frac -> vm.seekTo((frac * durationMs).toInt()) },
            colors = SliderDefaults.colors(thumbColor = TealAccent, activeTrackColor = TealMid)
        )

        // --- CONTROLES DE REPRODUCCIÓN ---
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val now = System.currentTimeMillis()
                if (now - lastPrevClick.value < 300) {
                    if (currentIndex.value > 0) currentIndex.value = currentIndex.value - 1
                } else {
                    vm.restart()
                }
                lastPrevClick.value = now
            }) {
                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = TealMid, modifier = Modifier.size(32.dp))
            }

            IconButton(onClick = { vm.playPause() }) {
                Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = TealDark, modifier = Modifier.size(48.dp))
            }

            IconButton(onClick = {
                if (currentIndex.value < files.value.lastIndex) currentIndex.value = currentIndex.value + 1
            }) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", tint = TealMid, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        // --- FORMULARIO ---
        OutlinedTextField(
            value = titleState.value,
            onValueChange = {
                titleState.value = it
                if (decodedName.isNotBlank()) scope.launch { metaRepo.setTitle(decodedName, it) }
            },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.size(12.dp))

        val categories = listOf("Trabajo", "Escuela", "Personal")
        val expanded = remember { mutableStateOf(false) }
        val currentFile = files.value.getOrNull(currentIndex.value)

        ExposedDropdownMenuBox(expanded = expanded.value, onExpandedChange = { expanded.value = it }) {
            OutlinedTextField(
                value = if (selected.value.isBlank()) "Selecciona categoría" else selected.value,
                onValueChange = {},
                label = { Text("Categoría") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor().clickable { expanded.value = !expanded.value },
                trailingIcon = {
                    IconButton(onClick = { expanded.value = !expanded.value }) {
                        Icon(
                            imageVector = if (expanded.value) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle",
                            tint = TealMid
                        )
                    }
                }
            )
            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selected.value = cat
                            expanded.value = false
                            if (decodedName.isNotBlank()) {
                                scope.launch { metaRepo.setCategory(decodedName, cat) }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        if (transcriptState.value.isNotBlank()) {
            Text(text = "Transcripción", color = TealDark)
            OutlinedTextField(
                value = transcriptState.value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
        }

        OutlinedTextField(
            value = notesState.value,
            onValueChange = {
                notesState.value = it
                if (decodedName.isNotBlank()) scope.launch { metaRepo.setNotes(decodedName, it) }
            },
            label = { Text("Notas adicionales") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Spacer(modifier = Modifier.size(24.dp))

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val f = files.value.getOrNull(currentIndex.value) ?: return@Button
                    scope.launch {
                        if (decodedName.isNotBlank()) {
                            metaRepo.setTitle(decodedName, titleState.value)
                            metaRepo.setNotes(decodedName, notesState.value)
                        }
                        if (selected.value.isNotBlank()) metaRepo.setCategory(f.name, selected.value)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = OffWhite),
                contentPadding = PaddingValues(vertical = 4.dp) 
            ) {
                Text("Actualizar", fontSize = 13.sp)
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val f = files.value.getOrNull(currentIndex.value) ?: return@Button
                    scope.launch {
                        val txt = metaRepo.transcribe(f.name, f)
                        transcriptState.value = txt
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealMid, contentColor = OffWhite),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Transcribir", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fila 2: Eliminar y Guardar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val f = files.value.getOrNull(currentIndex.value)
                    vm.release()
                    scope.launch {
                        if (f != null) {
                            try { f.delete() } catch (_: Exception) {}
                            metaRepo.delete(f.name)
                            val newList = files.value.toMutableList().also { it.remove(f) }
                            files.value = newList
                            if (newList.isEmpty()) {
                                onNavigateBack()
                            } else if (currentIndex.value >= newList.size) {
                                currentIndex.value = newList.lastIndex
                            }
                        } else {
                            metaRepo.delete(name)
                            onNavigateBack()
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = OffWhite),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Eliminar", fontSize = 13.sp)
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val f = files.value.getOrNull(currentIndex.value) ?: return@Button
                    scope.launch {
                        if (decodedName.isNotBlank()) {
                            metaRepo.setTitle(decodedName, titleState.value)
                            metaRepo.setNotes(decodedName, notesState.value)
                            if (selected.value.isNotBlank()) metaRepo.setCategory(decodedName, selected.value)
                        }
                    }
                    onNavigateBack()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = OffWhite),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Salir", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Espacio extra abajo
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
