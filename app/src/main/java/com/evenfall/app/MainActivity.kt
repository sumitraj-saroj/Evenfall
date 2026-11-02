package com.evenfall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.datastore.ThemeMode
import com.evenfall.app.ui.navigation.Screen
import com.evenfall.app.ui.screens.bookmarks.BookmarksHistoryScreen
import com.evenfall.app.ui.screens.home.HomeScreen
import com.evenfall.app.ui.screens.onboarding.OnboardingScreen
import com.evenfall.app.ui.screens.player.PlayerScreen
import com.evenfall.app.ui.screens.settings.SettingsScreen
import com.evenfall.app.ui.theme.ColorSchemeOption
import com.evenfall.app.ui.theme.EvenfallTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
            val pureBlack by preferencesManager.pureBlack.collectAsState(initial = false)
            val colorScheme by preferencesManager.colorSchemeOption.collectAsState(initial = ColorSchemeOption.EVENFALL_BLUE)

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }

            EvenfallTheme(
                darkTheme = darkTheme,
                pureBlack = pureBlack,
                colorSchemeOption = colorScheme
            ) {
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
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
