package com.evenfall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evenfall.app.ui.navigation.Screen
import com.evenfall.app.ui.screens.bookmarks.BookmarksHistoryScreen
import com.evenfall.app.ui.screens.home.HomeScreen
import com.evenfall.app.ui.screens.onboarding.OnboardingScreen
import com.evenfall.app.ui.screens.player.PlayerScreen
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

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenBook = { bookId ->
                    navController.navigate(Screen.Player.createRoute(bookId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenAddFolder = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            PlayerScreen(
                onCollapse = {
                    navController.popBackStack()
                },
                onOpenBookmarks = { bookId ->
                    navController.navigate(Screen.BookmarksHistory.createRoute(bookId))
                }
            )
        }

        composable(
            route = Screen.BookmarksHistory.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            BookmarksHistoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            // Placeholder - will be implemented in Milestone 7
        }
    }
}
