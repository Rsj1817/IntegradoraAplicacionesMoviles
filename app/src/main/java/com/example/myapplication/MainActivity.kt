package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.myapplication.ui.newrecording.NewRecordingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                                }
                            )
                        }

                        composable("recordings") {
                            RecordingsScreen(
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
