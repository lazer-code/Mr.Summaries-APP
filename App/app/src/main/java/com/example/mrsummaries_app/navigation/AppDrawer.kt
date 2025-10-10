package com.example.mrsummaries_app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable  // Added missing import for clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrsummaries_app.filesystem.ui.FolderTree
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currentRoute: String,
    navigateToHome: () -> Unit,
    navigateToNotepad: () -> Unit,
    navigateToSummary: () -> Unit,
    navigateToFolder: (String) -> Unit,
    drawerState: DrawerState,
    onDrawerStateChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    // App Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Mr. Summaries",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your Digital University Notebook",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Navigation Items
                    Text(
                        text = "Navigation",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    DrawerNavigationItem(
                        icon = Icons.Default.Home,
                        label = "Home",
                        isSelected = currentRoute == "home",
                        onClick = {
                            navigateToHome()
                            coroutineScope.launch {
                                drawerState.close()
                                onDrawerStateChange(false)
                            }
                        }
                    )

                    DrawerNavigationItem(
                        icon = Icons.Default.Edit,
                        label = "Notepad",
                        isSelected = currentRoute == "notepad",
                        onClick = {
                            navigateToNotepad()
                            coroutineScope.launch {
                                drawerState.close()
                                onDrawerStateChange(false)
                            }
                        }
                    )

                    DrawerNavigationItem(
                        icon = Icons.Default.MenuBook,
                        label = "Summaries",
                        isSelected = currentRoute == "summary",
                        onClick = {
                            navigateToSummary()
                            coroutineScope.launch {
                                drawerState.close()
                                onDrawerStateChange(false)
                            }
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Folder Tree Navigation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        FolderTree(
                            onFolderSelected = { folderId ->
                                navigateToFolder(folderId)
                                coroutineScope.launch {
                                    drawerState.close()
                                    onDrawerStateChange(false)
                                }
                            }
                        )
                    }

                    // Footer
                    Text(
                        text = "v1.0.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        drawerState = drawerState,
        gesturesEnabled = false, // Disable gesture-based drawer opening
        content = content
    )
}

@Composable
fun DrawerNavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick), // This line had the "Unresolved reference 'clickable'" error
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}