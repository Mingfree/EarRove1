package com.example.earrove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.earrove.ui.help.HelpScreen
import com.example.earrove.ui.home.HomeScreen
import com.example.earrove.ui.navigation.NavigationScreen
import com.example.earrove.ui.ocr.OcrScreen
import com.example.earrove.ui.settings.SettingsScreen
import com.example.earrove.ui.theme.EarRoveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarRoveTheme {
                EarRoveApp()
            }
        }
    }
}

@Composable
fun EarRoveApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("navigation") {
            NavigationScreen(navController = navController)
        }
        composable("ocr") {
            OcrScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("help") {
            HelpScreen(navController = navController)
        }
    }
}
