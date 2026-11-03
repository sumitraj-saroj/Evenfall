package com.evenfall.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Player : Screen("player/{bookId}") {
        fun createRoute(bookId: Long) = "player/$bookId"
    }
    data object BookmarksHistory : Screen("bookmarks_history/{bookId}") {
        fun createRoute(bookId: Long) = "bookmarks_history/$bookId"
    }
    data object Settings : Screen("settings")
    data object FixLibrary : Screen("fix_library")
}
