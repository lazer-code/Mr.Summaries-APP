package com.example.mrsummaries_app.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home_screen")
    object Notepad : Screen("notepad_screen")
    object Summary : Screen("summary_screen")
}