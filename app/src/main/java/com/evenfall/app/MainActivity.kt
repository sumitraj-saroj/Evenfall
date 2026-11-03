package com.evenfall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.datastore.ThemeMode
import com.evenfall.app.playback.PlaybackManager
import com.evenfall.app.ui.components.MiniPlayer
import com.evenfall.app.ui.navigation.Screen
import com.evenfall.app.ui.screens.bookmarks.BookmarksHistoryScreen
import com.evenfall.app.ui.screens.collections.ManageCollectionsScreen
import com.evenfall.app.ui.screens.fixlibrary.FixLibraryScreen
import com.evenfall.app.ui.screens.home.HomeScreen
import com.evenfall.app.ui.screens.onboarding.OnboardingScreen
import com.evenfall.app.ui.screens.player.PlayerScreen
import com.evenfall.app.ui.screens.scanlog.ScanLogScreen
import com.evenfall.app.ui.screens.settings.SettingsScreen
import com.evenfall.app.ui.screens.stats.StatsScreen
import com.evenfall.app.ui.theme.ColorSchemeOption
import com.evenfall.app.ui.theme.EvenfallTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var playbackManager: PlaybackManager

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
                    EvenfallAppNav(playbackManager)
                }
            }
        }
    }
}

@Composable
fun EvenfallAppNav(playbackManager: PlaybackManager) {
    val navController = rememberNavController()
    val playbackState by playbackManager.playbackState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPlayerOrOnboarding = currentRoute != null && (
        currentRoute.startsWith("player") ||
        currentRoute == Screen.Onboarding.route
    )
    val loadedBook = playbackState.currentBook
    val showMiniPlayer = loadedBook != null && !playbackState.isMiniPlayerDismissed && !isPlayerOrOnboarding

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
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
                    },
                    onOpenFixLibrary = {
                        navController.navigate(Screen.FixLibrary.route)
                    },
                    onOpenManageCollections = {
                        navController.navigate(Screen.ManageCollections.route)
                    },
                    onOpenStats = {
                        navController.navigate(Screen.Stats.route)
                    },
                    hasMiniPlayer = showMiniPlayer
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
                    },
                    hasMiniPlayer = showMiniPlayer
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenScanLog = {
                        navController.navigate(Screen.ScanLog.route)
                    },
                    onOpenManageCollections = {
                        navController.navigate(Screen.ManageCollections.route)
                    },
                    onOpenStats = {
                        navController.navigate(Screen.Stats.route)
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenBook = { bookId ->
                        navController.navigate(Screen.Player.createRoute(bookId))
                    }
                )
            }

            composable(Screen.ManageCollections.route) {
                ManageCollectionsScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.FixLibrary.route) {
                FixLibraryScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    hasMiniPlayer = showMiniPlayer
                )
            }

            composable(Screen.ScanLog.route) {
                ScanLogScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = showMiniPlayer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (loadedBook != null) {
                MiniPlayer(
                    playbackState = playbackState,
                    book = loadedBook,
                    onExpand = {
                        navController.navigate(Screen.Player.createRoute(loadedBook.id))
                    },
                    onDismiss = {
                        playbackManager.dismissMiniPlayer()
                    },
                    onTogglePlayPause = {
                        playbackManager.togglePlayPause()
                    },
                    onSkipBackward = {
                        playbackManager.skipBackward()
                    }
                )
            }
        }
    }
}
