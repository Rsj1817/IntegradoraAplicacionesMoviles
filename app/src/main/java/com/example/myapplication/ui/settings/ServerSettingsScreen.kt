package com.example.myapplication.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.RecordingMetadataRepository
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.TealAccent
import com.example.myapplication.ui.theme.TealDark
import kotlinx.coroutines.launch

@Composable
fun ServerSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val repo = RecordingMetadataRepository(app)
    val current = repo.currentSavedBase() ?: ""
    val base = remember { mutableStateOf(current) }
    val status = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = base.value,
            onValueChange = { base.value = it },
            label = { Text("URL del servidor") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch {
                    val ok = repo.setManualBase(base.value.trim())
                    status.value = if (ok) "Conectado y guardado" else "No se pudo conectar"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
        ) {
            Text("Probar y guardar")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                repo.clearSavedBase()
                status.value = "Configuración eliminada"
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealDark)
        ) {
            Text("Borrar configuración")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(status.value)
    }
}
