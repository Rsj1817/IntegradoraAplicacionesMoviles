package com.example.myapplication.ui.newrecording

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.myapplication.viewmodel.RecordViewModel
import com.example.myapplication.viewmodel.RecordViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewRecordingScreen(
    onNavigateBack: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as Application
    val recordViewModel: RecordViewModel = viewModel(factory = RecordViewModelFactory(app))
    val isRecording by recordViewModel.isRecording.collectAsState()
    val seconds by recordViewModel.timerSeconds.collectAsState()
    val isPaused by recordViewModel.isPaused.collectAsState()
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
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60),
                fontSize = 72.sp,
                color = TealDark
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (!isRecording) {
                IconButton(
                    onClick = {
                        if (recordAudioPermission.status.isGranted) {
                            recordViewModel.startRecording()
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
                            .background(TealAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.microfono),
                            contentDescription = "Grabar",
                            modifier = Modifier.size(64.dp),
                            colorFilter = ColorFilter.tint(OffWhite)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (recordAudioPermission.status.isGranted) {
                                if (isPaused) {
                                    recordViewModel.resumeRecording()
                                } else {
                                    recordViewModel.pauseRecording()
                                }
                            } else {
                                recordAudioPermission.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isPaused) TealMid else TealAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reanudar" else "Pausar",
                                modifier = Modifier.size(52.dp),
                                tint = OffWhite
                            )
                        }
                    }

                    IconButton(
                        onClick = { recordViewModel.stopRecording() },
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Detener grabación",
                                modifier = Modifier.size(52.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Presione para terminar de grabar" else "Presiona para iniciar la grabación",
                color = TealDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (!isRecording) "Listo para grabar" else if (isPaused) "Pausado" else "Grabando",
                color = TealMid
            )
        }
    }
}
