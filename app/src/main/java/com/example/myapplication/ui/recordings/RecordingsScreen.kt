package com.example.myapplication.ui.recordings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.TealAccent
import com.example.myapplication.ui.theme.TealDark
import com.example.myapplication.ui.theme.TealMid
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.myapplication.data.RecordingMetadataRepository
import com.example.myapplication.data.RecordingMeta
import android.app.Application

@Composable
fun RecordingsScreen(onNavigateBack: () -> Unit, onOpenRecording: (String) -> Unit, modifier: Modifier = Modifier) {
    val categories = listOf("Todas", "Trabajo", "Escuela", "Personal")
    val selected = remember { mutableStateOf("Todas") }
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val metaRepo = remember { RecordingMetadataRepository(app) }
    val metaMapState = remember { mutableStateOf<Map<String, RecordingMeta>>(emptyMap()) }
    val recordings = remember { mutableStateOf(listOf<File>()) }

    LaunchedEffect(Unit) {
        val dir = context.cacheDir
        val files = dir.listFiles { f ->
            f.isFile && f.name.startsWith("audio_") && f.name.endsWith(".mp4")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
        recordings.value = files
        metaMapState.value = metaRepo.loadAll()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis Grabaciones",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TealDark
            )
            IconButton(onClick = onNavigateBack) {
                Image(
                    painter = painterResource(id = R.drawable.salida),
                    contentDescription = "Atrás",
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(TealDark)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Button(
                    onClick = { selected.value = category },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected.value == category) TealDark else TealMid,
                        contentColor = OffWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(text = category)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val filtered = if (selected.value == "Todas") recordings.value else recordings.value.filter { f ->
            metaMapState.value[f.name]?.category == selected.value
        }
        if (filtered.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered.indices.toList()) { idx ->
                    val file = filtered[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onOpenRecording(file.name) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.file),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            colorFilter = ColorFilter.tint(TealMid)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            val metaTitle = metaMapState.value[file.name]?.title
                            Text(text = (metaTitle?.takeIf { it.isNotBlank() } ?: file.name), color = TealDark, fontWeight = FontWeight.SemiBold)
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            Text(text = sdf.format(file.lastModified()), color = TealMid)
                        }
                    }
                }
            }
        }
    }
}
