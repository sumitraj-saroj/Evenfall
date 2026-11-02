package com.evenfall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.evenfall.app.ui.navigation.Screen
import com.evenfall.app.ui.theme.EvenfallTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EvenfallTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EvenfallAppNav()
                }
            }
        }
    }
}

@Composable
fun EvenfallAppNav() {
    val navController = rememberNavController()
    // Scaffold navigation
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            // Placeholder for Milestone 1 scaffold
        }
        composable(Screen.Onboarding.route) {
            // Placeholder for Milestone 1 scaffold
        }
        composable(Screen.Settings.route) {
            // Placeholder for Milestone 1 scaffold
        }
    }
}
