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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Dialog states (find selected item from the up-to-date allItems)
    val selectedItemForOperation = remember(selectedItemId, allItems) {
        allItems.find { it.id == selectedItemId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(currentFolder?.name ?: "My Files")
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    // Add new item
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Item"
                        )
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
            // Breadcrumb navigation
            BreadcrumbNavigation(
                path = currentPath,
                currentFolderId = currentFolderId,
                onFolderClick = { folderId ->
                    viewModel.navigateToFolder(folderId)
                }
            )

            Divider()

            // File listing
            FileSystemItemsList(
                items = currentItems,
                selectedItemId = selectedItemId,
                onItemClick = { item ->
                    when (item) {
                        is Folder -> viewModel.navigateToFolder(item.id)
                        is Note -> onNoteClick(item.id)
                        else -> Unit
                    }
                },
                onItemLongClick = { item ->
                    // Long-press selects the item and reveals the action bar
                    viewModel.selectItem(item.id)
                }
            )

            // Selection action bar (only shows when an item is selected)
            AnimatedVisibility(
                visible = selectedItemId != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SelectionActionBar(
                    onRenameClick = { showRenameDialog = true },
                    onMoveClick = { showMoveDialog = true },
                    onDeleteClick = { showDeleteConfirmDialog = true },
                    onDismissClick = { viewModel.selectItem(null) }
                )
            }
        }
    }

    // Dialog for adding new item (folder or note)
    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAddFolder = { name ->
                viewModel.createFolder(name)
                showAddItemDialog = false
            },
            onAddNote = { name ->
                viewModel.createNote(name)
                showAddItemDialog = false
            }
        )
    }

    // Dialog for renaming an item
    if (showRenameDialog && selectedItemForOperation != null) {
        RenameDialog(
            item = selectedItemForOperation,
            onDismiss = {
                showRenameDialog = false
                viewModel.selectItem(null)
            },
            onRename = { newName ->
                viewModel.renameItem(selectedItemForOperation.id, newName)
                showRenameDialog = false
                viewModel.selectItem(null)
            }
        )
    }

    // Dialog for moving an item
    if (showMoveDialog && selectedItemForOperation != null) {
        val item = selectedItemForOperation // local copy
        MoveItemDialogContent(
            item = item,
            allItems = allItems,
            currentFolderId = currentFolderId,
            onDismiss = {
                showMoveDialog = false
                viewModel.selectItem(null)
            },
            onMove = { newParentId ->
                viewModel.moveItem(item.id, newParentId)
                showMoveDialog = false
                viewModel.selectItem(null)
            }
        )
    }

    // Confirmation dialog for deletion
    if (showDeleteConfirmDialog && selectedItemForOperation != null) {
        DeleteConfirmationDialog(
            item = selectedItemForOperation,
            onDismiss = {
                showDeleteConfirmDialog = false
                viewModel.selectItem(null)
            },
            onConfirmDelete = {
                viewModel.deleteItem(selectedItemForOperation.id)
                showDeleteConfirmDialog = false
                viewModel.selectItem(null)
            }
        )
    }
}

@Composable
fun BreadcrumbNavigation(
    path: List<Folder>,
    currentFolderId: String,
    onFolderClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home icon for root
        IconButton(onClick = { onFolderClick("root") }) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home"
            )
        }

        // Path segments
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
                    .clickable { onFolderClick(folder.id) }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun FileSystemItemsList(
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
            items(items) { item ->
                val isSelected = item.id == selectedItemId

                FileSystemItemCard(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileSystemItemCard(
    item: FileSystemItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Use combinedClickable to support onClick AND onLongClick properly.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
            // Item icon
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = when (item.type) {
                    FileSystemItemType.FOLDER -> MaterialTheme.colorScheme.primary
                    FileSystemItemType.NOTE -> MaterialTheme.colorScheme.secondary
                    // If additional types exist, map them here; fallback to onSurface
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Modified: ${dateFormatter.format(Date(item.modifiedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Trailing type indicator
            when (item) {
                is Folder -> {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Folder",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                is Note -> {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Note",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun SelectionActionBar(
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
            // Rename button
            IconButton(onClick = onRenameClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                    Text("Rename", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Move button
            IconButton(onClick = onMoveClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move")
                    Text("Move", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    Text("Delete", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                }
            }

            // Cancel selection
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
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddFolder: (String) -> Unit,
    onAddNote: (String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("folder") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Item") },
        text = {
            Column {
                // Type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = "folder" }
                    ) {
                        RadioButton(
                            selected = selectedType == "folder",
                            onClick = { selectedType = "folder" }
                        )
                        Text("Folder", modifier = Modifier.padding(start = 8.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = "note" }
                    ) {
                        RadioButton(
                            selected = selectedType == "note",
                            onClick = { selectedType = "note" }
                        )
                        Text("Note", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name input
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
                    if (itemName.isNotBlank()) {
                        if (selectedType == "folder") {
                            onAddFolder(itemName)
                        } else {
                            onAddNote(itemName)
                        }
                        onDismiss()
                    }
                },
                enabled = itemName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    item: FileSystemItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(item.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${if (item.type == FileSystemItemType.FOLDER) "Folder" else "Note"}") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onRename(newName)
                    }
                },
                enabled = newName.isNotBlank() && newName != item.name
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    item: FileSystemItem,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val itemType = if (item.type == FileSystemItemType.FOLDER) "folder" else "note"
    val warningMessage = if (item.type == FileSystemItemType.FOLDER) {
        "This will delete the folder and all its contents. This action cannot be undone."
    } else {
        "This will permanently delete this note. This action cannot be undone."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $itemType") },
        text = {
            Column {
                Text("Are you sure you want to delete '${item.name}'?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog content for moving an item into another folder.
 * This is implemented as a top-level composable so references are resolvable where used.
 */
@Composable
fun MoveItemDialogContent(
    item: FileSystemItem,
    allItems: List<FileSystemItem>,
    currentFolderId: String,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit
) {
    // Extract folders from all items
    val folders: List<Folder> = remember(allItems) {
        allItems.filterIsInstance<Folder>()
            .filter { it.id != item.id } // Can't move to itself
    }

    // Don't show folders that are descendants of the current item (if it's a folder)
    val validFolders: List<Folder> = remember(folders, item) {
        if (item is Folder) {
            // Function to get all descendant folder IDs
            fun getDescendantIds(folderId: String): Set<String> {
                val descendants = mutableSetOf<String>()
                folders.filter { it.parentId == folderId }.forEach { childFolder ->
                    descendants.add(childFolder.id)
                    descendants.addAll(getDescendantIds(childFolder.id))
                }
                return descendants
            }

            val invalidIds = getDescendantIds(item.id) + item.id
            folders.filter { it.id !in invalidIds }
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
                    "Select destination folder for '${item.name}':",
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
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Files (Root)")
                    }
                }

                // Folder options - use explicit typing to avoid inference problems
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(validFolders) { folder: Folder ->
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
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
            ) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}