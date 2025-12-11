package com.example.myapplication.ui.newrecording

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.TealAccent
import com.example.myapplication.ui.theme.TealDark
import com.example.myapplication.ui.theme.TealMid
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewRecordingScreen(
    onNavigateBack: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = remember { mutableStateOf(false) }
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Scaffold(
        topBar = {
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
                actions = {
                    IconButton(onClick = onMoreOptionsClick) {
                        Image(
                            painter = painterResource(id = R.drawable.trespuntos),
                            contentDescription = "Más opciones",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(OffWhite)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealDark
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(OffWhite)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "00:00", fontSize = 72.sp, color = TealDark)
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(
                onClick = {
                    if (recordAudioPermission.status.isGranted) {
                        isRecording.value = !isRecording.value
                    } else {
                        recordAudioPermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isRecording.value) Color.Red else TealAccent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording.value) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener grabación",
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.microfono),
                            contentDescription = "Grabar",
                            modifier = Modifier.size(64.dp),
                            colorFilter = ColorFilter.tint(OffWhite)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording.value) "Presione para terminar de grabar" else "Presiona para iniciar la grabación",
                color = TealDark
            )
        }
    }
}
