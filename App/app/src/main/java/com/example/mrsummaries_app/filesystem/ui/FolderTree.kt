package com.example.mrsummaries_app.filesystem.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.FileSystemItemType
import com.example.mrsummaries_app.filesystem.model.Folder
import com.example.mrsummaries_app.filesystem.model.Note
import com.example.mrsummaries_app.filesystem.viewmodel.FileSystemViewModel

@Composable
fun FolderTree(
    onFolderSelected: (String) -> Unit,
    onNoteSelected: (String) -> Unit,
    viewModel: FileSystemViewModel = viewModel()
) {
    val allItems by viewModel.allItems.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()

    val selectedItemId = remember { mutableStateOf<String?>(null) }
    val selectedItem = remember(selectedItemId.value, allItems) {
        allItems.find { it.id == selectedItemId.value }
    }

    val rootChildren = remember(allItems) {
        allItems.filter { it.parentId == "root" }
    }

    val showRenameDialog = remember { mutableStateOf(false) }
    val showMoveDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showAddDialog = remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val showRootAddDialog = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Folders",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        if (rootChildren.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No items yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showRootAddDialog.value = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add subfolder")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(items = rootChildren, key = { it.id }) { item: FileSystemItem ->
                    when (item) {
                        is Folder -> FolderNode(
                            folder = item,
                            viewModel = viewModel,
                            currentFolderId = currentFolderId,
                            allItems = allItems,
                            onFolderClick = { onFolderSelected(it) },
                            onNoteClick = { onNoteSelected(it) },
                            onLongPress = { id -> selectedItemId.value = id }
                        )
                        is Note -> DrawerNoteRow(
                            note = item,
                            isSelected = selectedItemId.value == item.id,
                            onClick = { onNoteSelected(item.id) },
                            onLongPress = { id -> selectedItemId.value = id }
                        )
                        else -> Unit
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedItem != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            selectedItem?.let { item ->
                SideMenuActionBarVertical(
                    selectedItem = item,
                    onRename = { showRenameDialog.value = true },
                    onMove = { showMoveDialog.value = true },
                    onDelete = { showDeleteDialog.value = true },
                    onAddSubfolder = {
                        if (item is Folder) showAddDialog.value = item.id to true
                    },
                    onAddNote = {
                        if (item is Folder) showAddDialog.value = item.id to false
                    },
                    onDismiss = { selectedItemId.value = null }
                )
            }
        }
    }

    if (showRenameDialog.value && selectedItem != null) {
        DrawerRenameDialog(
            item = selectedItem,
            onDismiss = {
                showRenameDialog.value = false
                selectedItemId.value = null
            },
            onRename = { newName ->
                viewModel.renameItem(selectedItem.id, newName)
                showRenameDialog.value = false
                selectedItemId.value = null
            }
        )
    }

    if (showMoveDialog.value && selectedItem != null) {
        DrawerMoveItemDialog(
            item = selectedItem,
            allItems = allItems,
            onDismiss = {
                showMoveDialog.value = false
                selectedItemId.value = null
            },
            onMove = { newParentId ->
                viewModel.moveItem(selectedItem.id, newParentId)
                showMoveDialog.value = false
                selectedItemId.value = null
            }
        )
    }

    if (showDeleteDialog.value && selectedItem != null) {
        DrawerDeleteDialog(
            item = selectedItem,
            onDismiss = {
                showDeleteDialog.value = false
                selectedItemId.value = null
            },
            onConfirm = {
                viewModel.deleteItem(selectedItem.id)
                showDeleteDialog.value = false
                selectedItemId.value = null
            }
        )
    }

    showAddDialog.value?.let { (parentId, isFolder) ->
        DrawerAddItemDialog(
            isFolder = isFolder,
            onDismiss = { showAddDialog.value = null },
            onCreate = { name ->
                if (isFolder) {
                    viewModel.createFolder(name, parentId)
                } else {
                    viewModel.createNote(name, parentId)
                }
                showAddDialog.value = null
            }
        )
    }

    if (showRootAddDialog.value) {
        DrawerAddItemDialog(
            isFolder = true,
            onDismiss = { showRootAddDialog.value = false },
            onCreate = { name ->
                viewModel.createFolder(name, "root")
                showRootAddDialog.value = false
            }
        )
    }
}

@Composable
private fun FolderNode(
    folder: Folder,
    viewModel: FileSystemViewModel,
    currentFolderId: String,
    allItems: List<FileSystemItem>,
    onFolderClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onLongPress: (String) -> Unit,
    depth: Int = 0
) {
    val isSelected = currentFolderId == folder.id
    val isExpanded = viewModel.expandedFolders[folder.id] ?: false

    val children = remember(allItems, folder.id) {
        allItems.filter { it.parentId == folder.id }
    }
    val hasChildren = children.isNotEmpty()
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onFolderClick(folder.id) },
                onLongClick = { onLongPress(folder.id) }
            )
            .padding(
                start = (depth * 16).dp + 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            IconButton(
                onClick = { viewModel.toggleFolderExpansion(folder.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .rotate(chevronRotation)
                        .size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }

    AnimatedVisibility(
        visible = isExpanded && hasChildren,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column {
            children.filterIsInstance<Folder>().forEach { childFolder ->
                FolderNode(
                    folder = childFolder,
                    viewModel = viewModel,
                    currentFolderId = currentFolderId,
                    allItems = allItems,
                    onFolderClick = onFolderClick,
                    onNoteClick = onNoteClick,
                    onLongPress = onLongPress,
                    depth = depth + 1
                )
            }
            children.filterIsInstance<Note>().forEach { note ->
                DrawerNoteRow(
                    note = note,
                    isSelected = false,
                    onClick = { onNoteClick(note.id) },
                    onLongPress = onLongPress,
                    depth = depth + 1
                )
            }
        }
    }
}

@Composable
private fun DrawerNoteRow(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: (String) -> Unit,
    depth: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongPress(note.id) }
            )
            .padding(
                start = (depth * 16).dp + 56.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.TextSnippet,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = note.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Move dialog in the drawer.
 * FIX: explicit return in descendants() to avoid "Missing return statement".
 * Also render a single Root option separately and exclude "root" from list.
 */
@Composable
private fun DrawerMoveItemDialog(
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
                folders.filter { it.parentId == id }.forEach {
                    ids.add(it.id)
                    ids.addAll(descendants(it.id))
                }
                return ids // FIX: explicit return
            }
            val invalid = descendants(item.id) + item.id
            folders.filter { it.id !in invalid }
        } else {
            folders
        }
    }

    val selectedFolderId = remember { mutableStateOf<String?>(item.parentId ?: "root") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move ${if (item.type == FileSystemItemType.FOLDER) "Folder" else "Note"}") },
        text = {
            Column {
                MoveDestinationRow(
                    label = "Root",
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    selected = selectedFolderId.value == "root",
                    onSelect = { selectedFolderId.value = "root" }
                )
                validFolders.forEach { folder ->
                    MoveDestinationRow(
                        label = folder.name,
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        selected = selectedFolderId.value == folder.id,
                        onSelect = { selectedFolderId.value = folder.id }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onMove(selectedFolderId.value) },
                enabled = selectedFolderId.value != item.parentId
            ) { Text("Move") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MoveDestinationRow(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onSelect, onLongClick = {})
            .padding(vertical = 6.dp)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun SideMenuActionBarVertical(
    selectedItem: FileSystemItem,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onAddSubfolder: () -> Unit,
    onAddNote: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ActionItem(icon = Icons.Default.Edit, label = "Rename", onClick = onRename)
            ActionItem(icon = Icons.Default.DriveFileMove, label = "Move", onClick = onMove)
            ActionItem(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete,
                error = true
            )
            if (selectedItem is Folder) {
                ActionItem(
                    icon = Icons.Default.CreateNewFolder,
                    label = "Add subfolder",
                    onClick = onAddSubfolder
                )
                ActionItem(
                    icon = Icons.Default.NoteAdd,
                    label = "Add note",
                    onClick = onAddNote
                )
            }
            ActionItem(icon = Icons.Default.Close, label = "Cancel", onClick = onDismiss)
        }
    }
}

@Composable
private fun RowScope.ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    error: Boolean = false
) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    error: Boolean = false
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (error)
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        else
            ButtonDefaults.textButtonColors()
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DrawerRenameDialog(
    item: FileSystemItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val newNameState = remember { mutableStateOf(item.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${if (item.type == FileSystemItemType.FOLDER) "Folder" else "Note"}") },
        text = {
            OutlinedTextField(
                value = newNameState.value,
                onValueChange = { newNameState.value = it },
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newNameState.value.isNotBlank()) onRename(newNameState.value) },
                enabled = newNameState.value.isNotBlank() && newNameState.value != item.name
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DrawerAddItemDialog(
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val nameState = remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFolder) "Add Subfolder" else "Add Note") },
        text = {
            OutlinedTextField(
                value = nameState.value,
                onValueChange = { nameState.value = it },
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (nameState.value.isNotBlank()) onCreate(nameState.value) },
                enabled = nameState.value.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DrawerDeleteDialog(
    item: FileSystemItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val warning = if (item.type == FileSystemItemType.FOLDER)
        "This will delete the folder and all its contents. This action cannot be undone."
    else
        "This will permanently delete this note. This action cannot be undone."

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (item.type == FileSystemItemType.FOLDER) "Folder" else "Note"}") },
        text = { Text(warning) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}