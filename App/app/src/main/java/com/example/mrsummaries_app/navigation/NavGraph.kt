package com.example.mrsummaries_app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mrsummaries_app.home.HomeScreen
import com.example.mrsummaries_app.notepad.NotepadScreen
import com.example.mrsummaries_app.ui.summary.SummaryScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    onMenuClick: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onMenuClick = onMenuClick,
                onFilesClick = {
                    // Navigate to the files screen when the user clicks on Files button
                    navController.navigate("files/root")
                }
            )
        }

        composable(route = Screen.Notepad.route) {
            NotepadScreen(onMenuClick = onMenuClick)
        }

        composable(route = Screen.Summary.route) {
            SummaryScreen(onMenuClick = onMenuClick)
        }
    }
}