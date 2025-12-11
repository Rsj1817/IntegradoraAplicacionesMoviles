package com.example.myapplication.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.TealAccent
import com.example.myapplication.ui.theme.TealDark
import com.example.myapplication.ui.theme.TealMid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onExit: () -> Unit,
    onProfileClick: () -> Unit,
    onRecordingsClick: () -> Unit,
    onNewRecordingClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SnapRec", color = OffWhite) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Image(
                            painter = painterResource(id = R.drawable.salida),
                            contentDescription = "Salir",
                            modifier = Modifier.size(32.dp),
                            colorFilter = ColorFilter.tint(OffWhite)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Image(
                            painter = painterResource(id = R.drawable.perfil),
                            contentDescription = "Perfil",
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = OffWhite)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(TealMid),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icono),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "¡Bienvenido(a) a SnapRec!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Graba, organiza y gestiona tus notas de voz de manera inteligente",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HomeButton(icon = Icons.Default.Add, text = "Nueva Grabación", description = "Crear nueva nota", onClick = onNewRecordingClick)
            Spacer(modifier = Modifier.height(16.dp))
            HomeButton(icon = Icons.Default.Folder, text = "Mis Grabaciones", description = "Todas tus notas", onClick = onRecordingsClick)
            Spacer(modifier = Modifier.height(16.dp))
            HomeButton(icon = Icons.Default.Star, text = "Favoritos", description = "Notas importantes", onClick = onFavoritesClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeButton(icon: ImageVector, text: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealAccent),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(TealMid.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = OffWhite)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(text = text, fontWeight = FontWeight.Bold, color = OffWhite)
                Text(text = description, color = OffWhite.copy(alpha = 0.9f))
            }
        }
    }
}
