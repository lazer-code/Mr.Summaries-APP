package com.example.mrsummaries_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController  // Added this import
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mrsummaries_app.filesystem.ui.FileExplorerScreen
import com.example.mrsummaries_app.filesystem.viewmodel.FileSystemViewModel
import com.example.mrsummaries_app.home.HomeScreen
import com.example.mrsummaries_app.navigation.AppDrawer
import com.example.mrsummaries_app.navigation.Screen
import com.example.mrsummaries_app.notepad.NotepadScreen
import com.example.mrsummaries_app.ui.summary.SummaryScreen
import com.example.mrsummaries_app.ui.theme.MrSummariesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MrSummariesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isDrawerOpen by remember { mutableStateOf(false) }
    val fileSystemViewModel: FileSystemViewModel = viewModel()

    AppDrawer(
        currentRoute = getCurrentRoute(navController),
        navigateToHome = {
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        },
        navigateToNotepad = {
            navController.navigate("notepad") {
                launchSingleTop = true
            }
        },
        navigateToSummary = {
            navController.navigate("summary") {
                launchSingleTop = true
            }
        },
        navigateToFolder = { folderId ->
            navController.navigate("files/$folderId") {
                launchSingleTop = true
            }
            fileSystemViewModel.navigateToFolder(folderId)
        },
        drawerState = drawerState,
        onDrawerStateChange = { isOpen ->
            isDrawerOpen = isOpen
        }
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                            isDrawerOpen = true
                        }
                    },
                    onFilesClick = {
                        navController.navigate("files/root")
                    }
                )
            }
            composable("notepad") {
                NotepadScreen(
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                            isDrawerOpen = true
                        }
                    }
                )
            }
            composable("summary") {
                SummaryScreen(
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                            isDrawerOpen = true
                        }
                    }
                )
            }
            // File Explorer Screen
            composable(
                route = "files/{folderId}",
                arguments = listOf(
                    navArgument("folderId") {
                        type = NavType.StringType
                        defaultValue = "root"
                    }
                )
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId") ?: "root"
                fileSystemViewModel.navigateToFolder(folderId)

                FileExplorerScreen(
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                            isDrawerOpen = true
                        }
                    },
                    onNoteClick = { noteId ->
                        navController.navigate("note/$noteId")
                    }
                )
            }
            // Note Editing Screen
            composable(
                route = "note/{noteId}",
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable

                // When viewing a note, don't pass the noteId parameter if your NotepadScreen doesn't accept it yet
                // Either update NotepadScreen to accept noteId or handle the navigation differently
                NotepadScreen(
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                            isDrawerOpen = true
                        }
                    }
                    // If NotepadScreen accepts noteId, uncomment this:
                    // noteId = noteId
                )
            }
        }
    }
}

@Composable
private fun getCurrentRoute(navController: NavController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route ?: "home"

    return when {
        route.startsWith("files") -> "files"
        route.startsWith("note") -> "files" // When editing a note, we still highlight the Files section
        else -> route
    }
}