package com.example.mrsummaries_app.filesystem.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.FileSystemItemType
import com.example.mrsummaries_app.filesystem.model.Folder
import com.example.mrsummaries_app.filesystem.model.Note
import com.example.mrsummaries_app.filesystem.viewmodel.FileSystemViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    onMenuClick: () -> Unit,
    onNoteClick: (String) -> Unit,
    viewModel: FileSystemViewModel = viewModel()
) {
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val currentItems by viewModel.currentFolderItems.collectAsState()
    val selectedItemId by viewModel.selectedItemId.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    // FIX: must be var when using "by remember { mutableStateOf(...) }" and writing to it later
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Breadcrumb: Root > folder1 > folder2 > ... > currentFolder
            BreadcrumbBar(
                path = currentPath,
                currentFolderId = currentFolderId,
                currentFolderName = currentFolder?.name,
                onSegmentClick = { folderId ->
                    viewModel.navigateToFolder(folderId)
                }
            )

            Divider()

            // Items
            FileSystemItemsList(
                items = currentItems,
                selectedItemId = selectedItemId,
                onItemClick = { item: FileSystemItem ->
                    when (item) {
                        is Folder -> viewModel.navigateToFolder(item.id)
                        is Note -> onNoteClick(item.id)
                        else -> Unit
                    }
                },
                onItemLongClick = { item: FileSystemItem ->
                    viewModel.selectItem(item.id)
                }
            )

            // Selection bar
            AnimatedVisibility(
                visible = selectedItemId != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SelectionActionBar(
                    onRenameClick = { showRenameDialog = true },
                    onMoveClick = { showMoveDialog = true },
                    onDeleteClick = {
                        selectedItemId?.let { viewModel.deleteItem(it) }
                        viewModel.selectItem(null)
                    },
                    onDismissClick = { viewModel.selectItem(null) }
                )
            }
        }

        // Add dialog
        if (showAddItemDialog) {
            AddItemDialog(
                onDismiss = { showAddItemDialog = false },
                onAddFolder = { name: String ->
                    viewModel.createFolder(name)
                    showAddItemDialog = false
                },
                onAddNote = { name: String ->
                    val newId = viewModel.createNoteAndReturnId(name)
                    onNoteClick(newId)
                    showAddItemDialog = false
                }
            )
        }

        // Rename dialog
        if (showRenameDialog) {
            val item = allItems.find { it.id == selectedItemId }
            if (item != null) {
                RenameDialog(
                    item = item,
                    onDismiss = { showRenameDialog = false },
                    onRename = { newName: String ->
                        viewModel.renameItem(item.id, newName)
                        showRenameDialog = false
                    }
                )
            }
        }

        // Move dialog
        if (showMoveDialog) {
            val item = allItems.find { it.id == selectedItemId }
            if (item != null) {
                MoveItemDialog(
                    item = item,
                    allItems = allItems,
                    onDismiss = { showMoveDialog = false },
                    onMove = { newParentId: String? ->
                        viewModel.moveItem(item.id, newParentId)
                        showMoveDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    path: List<Folder>, // ancestors from root-most to parent of current
    currentFolderId: String,
    currentFolderName: String?,
    onSegmentClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home/Root
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home"
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Root",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable { onSegmentClick("root") }
                .padding(horizontal = 4.dp),
            color = if (currentFolderId == "root")
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )

        // Ancestors
        path.forEach { folder ->
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { onSegmentClick(folder.id) }
                    .padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (currentFolderId == folder.id)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        // Current (non-clickable)
        if (currentFolderId != "root") {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = currentFolderName ?: "Folder",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FileSystemItemsList(
    items: List<FileSystemItem>,
    selectedItemId: String?,
    onItemClick: (FileSystemItem) -> Unit,
    onItemLongClick: (FileSystemItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This folder is empty",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.id }) { item: FileSystemItem ->
                FileItemRow(
                    item = item,
                    isSelected = selectedItemId == item.id,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItemRow(
    item: FileSystemItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (item.type) {
                    FileSystemItemType.FOLDER -> Icons.Default.Folder
                    FileSystemItemType.NOTE -> Icons.Default.TextSnippet
                    else -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = when (item.type) {
                    FileSystemItemType.FOLDER -> MaterialTheme.colorScheme.primary
                    FileSystemItemType.NOTE -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    onRenameClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRenameClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                    Text("Rename", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onMoveClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move")
                    Text("Move", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDeleteClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    Text("Delete", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                }
            }
            IconButton(onClick = onDismissClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                    Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddFolder: (String) -> Unit,
    onAddNote: (String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("folder") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { type = "folder" }
                    ) {
                        RadioButton(selected = type == "folder", onClick = { type = "folder" })
                        Text("Folder", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { type = "note" }
                    ) {
                        RadioButton(selected = type == "note", onClick = { type = "note" })
                        Text("Note", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (type == "folder") onAddFolder(itemName) else onAddNote(itemName)
                },
                enabled = itemName.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameDialog(
    item: FileSystemItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(item.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text("New name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(newName) },
                enabled = newName.isNotBlank() && newName != item.name
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Move dialog:
 * - Excludes real Root from folders list (we render a single explicit Root option).
 * - Prevents moving a folder inside its own descendants.
 */
@Composable
private fun MoveItemDialog(
    item: FileSystemItem,
    allItems: List<FileSystemItem>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit
) {
    val folders = remember(allItems) { allItems.filterIsInstance<Folder>().filter { it.id != "root" } }

    val validFolders = remember(item, folders) {
        if (item is Folder) {
            fun descendants(id: String): Set<String> {
                val ids = mutableSetOf<String>()
                folders.filter { it.parentId == id }.forEach { child ->
                    ids.add(child.id)
                    ids.addAll(descendants(child.id))
                }
                return ids // explicit return to avoid "Missing return statement"
            }
            val invalid = descendants(item.id) + item.id
            folders.filter { it.id !in invalid }
        } else {
            folders
        }
    }

    var selectedFolderId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Folder") },
        text = {
            Column {
                Text(
                    text = "Select destination:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Root option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedFolderId = "root" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFolderId == "root")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Root")
                    }
                }

                // Other folders
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(validFolders, key = { it.id }) { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedFolderId = folder.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedFolderId == folder.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(folder.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onMove(selectedFolderId) },
                enabled = selectedFolderId != null && selectedFolderId != item.parentId
            ) { Text("Move") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}