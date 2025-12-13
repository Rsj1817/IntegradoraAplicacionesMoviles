package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.recordings.RecordingsScreen
import com.example.myapplication.ui.recordings.RecordingDetailScreen
import com.example.myapplication.ui.favorites.FavoritesScreen
import android.net.Uri
import com.example.myapplication.ui.newrecording.NewRecordingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("retrofit_config", MODE_PRIVATE)
        prefs.edit().putString("last_base", "http://192.168.0.204:5000/").apply()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable("login") {
                            LoginScreen(
                                onAccept = { email, password ->
                                    if (email == "admin" && password == "admin123") {
                                        navController.navigate("home")
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onExit = { navController.popBackStack() },
                                onProfileClick = { },
                                onRecordingsClick = {
                                    navController.navigate("recordings")
                                },
                                onNewRecordingClick = {
                                    navController.navigate("new_recording")
                                },
                                onFavoritesClick = {
                                    navController.navigate("favorites")
                                }
                            )
                        }

                        composable("recordings") {
                            RecordingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onOpenRecording = { name ->
                                    navController.navigate("recording_detail/${Uri.encode(name)}")
                                },
                                onOpenFavorites = { navController.navigate("favorites") }
                            )
                        }

                        composable("favorites") {
                            FavoritesScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onOpenRecording = { name ->
                                    navController.navigate("recording_detail/${Uri.encode(name)}")
                                }
                            )
                        }

                        composable("recording_detail/{name}") { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            RecordingDetailScreen(
                                name = name,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("new_recording") {
                            NewRecordingScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onMoreOptionsClick = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
