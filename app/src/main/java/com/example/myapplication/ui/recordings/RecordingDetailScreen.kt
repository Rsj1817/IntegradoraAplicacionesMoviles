package com.example.myapplication.ui.recordings

import android.app.Application
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
    LaunchedEffect(Unit) {
        val dir = context.cacheDir
        files.value = dir.listFiles { f -> f.isFile && f.name.startsWith("audio_") && f.name.endsWith(".mp4") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    val currentIndex = remember { mutableStateOf(0) }
    LaunchedEffect(files.value, currentIndex.value) {
        files.value.getOrNull(currentIndex.value)?.let { vm.load(it) }
    }
    LaunchedEffect(files.value, name) {
        val idx = files.value.indexOfFirst { it.name == name }.coerceAtLeast(0)
        currentIndex.value = idx
    }
    val favoriteState = remember { mutableStateOf(false) }
    val transcriptState = remember { mutableStateOf("") }
    LaunchedEffect(currentIndex.value) {
        val f = files.value.getOrNull(currentIndex.value)
        if (f != null) {
            favoriteState.value = metaRepo.isFavorite(f.name)
            transcriptState.value = metaRepo.getTranscript(f.name)
        } else {
            favoriteState.value = false
            transcriptState.value = ""
        }
    }

    val isPlaying by vm.isPlaying.collectAsState()
    val durationMs by vm.durationMs.collectAsState()
    val positionMs by vm.positionMs.collectAsState()

    val lastPrevClick = remember { mutableStateOf(0L) }

    val currentName = files.value.getOrNull(currentIndex.value)?.name ?: ""
    val titleState = remember { mutableStateOf(currentName) }
    LaunchedEffect(currentName) {
        if (currentName.isNotBlank()) {
            val t = metaRepo.getTitle(currentName)
            titleState.value = (t ?: currentName)
        } else {
            titleState.value = currentName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(16.dp)
            .verticalScroll(scrollState), // Habilitar scroll por si acaso
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --- HEADER ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = titleState.value, fontSize = 22.sp, color = TealDark, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val f = files.value.getOrNull(currentIndex.value) ?: return@IconButton
                    val newFav = !favoriteState.value
                    favoriteState.value = newFav
                    scope.launch { metaRepo.setFavorite(f.name, newFav) }
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
                if (currentName.isNotBlank()) scope.launch { metaRepo.setTitle(currentName, it) }
            },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.size(12.dp))

        val categories = listOf("Trabajo", "Escuela", "Personal")
        val expanded = remember { mutableStateOf(false) }
        val currentFile = files.value.getOrNull(currentIndex.value)
        val selected = remember { mutableStateOf("") }
        LaunchedEffect(currentIndex.value) {
            val f = files.value.getOrNull(currentIndex.value)
            selected.value = if (f != null) metaRepo.getCategory(f.name) ?: "" else ""
        }

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
                            currentFile?.let { scope.launch { metaRepo.setCategory(it.name, cat) } }
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

        val notesState = remember { mutableStateOf("") }
        LaunchedEffect(currentIndex.value) {
            val f = files.value.getOrNull(currentIndex.value)
            if (f != null) {
                titleState.value = metaRepo.getTitle(f.name) ?: f.name
                notesState.value = metaRepo.getNotes(f.name) ?: ""
            } else {
                titleState.value = currentName
                notesState.value = ""
            }
        }
        OutlinedTextField(
            value = notesState.value,
            onValueChange = {
                notesState.value = it
                currentFile?.let { f -> scope.launch { metaRepo.setNotes(f.name, it) } }
            },
            label = { Text("Notas adicionales") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Spacer(modifier = Modifier.size(24.dp))

        // --- BOTONES DE ACCIÓN (Corregidos para ser más chicos y caber bien) ---

        // Fila 1: Actualizar y Transcribir
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val f = files.value.getOrNull(currentIndex.value) ?: return@Button
                    scope.launch {
                        metaRepo.setTitle(f.name, titleState.value)
                        metaRepo.setNotes(f.name, notesState.value)
                        if (selected.value.isNotBlank()) metaRepo.setCategory(f.name, selected.value)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealDark, contentColor = OffWhite),
                contentPadding = PaddingValues(vertical = 4.dp) // Botón menos alto
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
                    val f = files.value.getOrNull(currentIndex.value) ?: return@Button
                    vm.release()
                    try { f.delete() } catch (_: Exception) {}
                    scope.launch { metaRepo.delete(f.name) }
                    val newList = files.value.toMutableList().also { it.remove(f) }
                    files.value = newList
                    if (newList.isEmpty()) {
                        onNavigateBack()
                    } else if (currentIndex.value >= newList.size) {
                        currentIndex.value = newList.lastIndex
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
                        metaRepo.setTitle(f.name, titleState.value)
                        metaRepo.setNotes(f.name, notesState.value)
                        if (selected.value.isNotBlank()) metaRepo.setCategory(f.name, selected.value)
                    }
                    onNavigateBack()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = OffWhite),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Guardar", fontSize = 13.sp)
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