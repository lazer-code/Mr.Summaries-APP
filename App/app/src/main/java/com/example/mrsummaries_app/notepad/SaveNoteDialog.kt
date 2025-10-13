package com.example.mrsummaries_app.notepad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.Folder

/**
 * Dialog to select a folder destination and name when saving a note.
 * - Breadcrumb shows full path "Root > folder1 > folder2 > ... > currentFolder".
 * - Prevents saving directly to Root (user must pick a folder).
 * - Includes "New folder" creation within the currently viewed folder and auto-selects it.
 * - Returns chosen folderId and note name on confirm.
 */
@Composable
fun SaveNoteDialog(
    allItems: List<FileSystemItem>,
    initialFolderId: String = "root",
    initialName: String = "Untitled Note",
    onDismiss: () -> Unit,
    onConfirm: (folderId: String, noteName: String) -> Unit,
    // Return the newly created folder ID so we can auto-select it
    onCreateFolder: (parentId: String, folderName: String) -> String
) {
    var currentFolderId by remember { mutableStateOf(initialFolderId) }
    var noteName by remember { mutableStateOf(initialName) }

    // Map for quick lookup
    val itemMap = remember(allItems) { allItems.associateBy { it.id } }

    // Build full breadcrumb (excluding "root" node in the list; we render Root separately)
    val breadcrumb: List<Folder> = remember(allItems, currentFolderId) {
        val nodes = mutableListOf<Folder>()
        var node = itemMap[currentFolderId]
        while (node != null && node.parentId != null) {
            val parent = itemMap[node.parentId]
            if (parent is Folder) {
                nodes.add(0, parent) // add to front so we get root-most first
                node = parent
            } else break
        }
        nodes
    }

    // Current folder object (null for root)
    val currentFolder: Folder? = remember(allItems, currentFolderId) {
        itemMap[currentFolderId] as? Folder
    }

    // Children folders of the current folder
    val childFolders: List<Folder> = remember(allItems, currentFolderId) {
        allItems.filterIsInstance<Folder>().filter { it.parentId == currentFolderId }
    }

    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Note") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Name entry
                OutlinedTextField(
                    value = noteName,
                    onValueChange = { noteName = it },
                    label = { Text("Note name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Breadcrumb: Root > folder1 > folder2 > ... > currentFolder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Root link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { currentFolderId = "root" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Root",
                            tint = if (currentFolderId == "root")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Root",
                            color = if (currentFolderId == "root")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Ancestors (clickable)
                    breadcrumb.forEach { folder ->
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = folder.name,
                            color = if (currentFolderId == folder.id)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable { currentFolderId = folder.id }
                                .padding(horizontal = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Current folder (non-clickable label)
                    if (currentFolderId != "root") {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = currentFolder?.name ?: "Folder",
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Choose destination folder", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showNewFolder = !showNewFolder }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showNewFolder) "Cancel" else "New folder")
                    }
                }

                // New folder inline form
                if (showNewFolder) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Folder name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val name = newFolderName.trim()
                                if (name.isNotEmpty()) {
                                    val newId = onCreateFolder(currentFolderId, name)
                                    // auto-select the newly created folder
                                    currentFolderId = newId
                                    newFolderName = ""
                                    showNewFolder = false
                                }
                            },
                            enabled = newFolderName.isNotBlank()
                        ) { Text("Create") }
                    }
                }

                // Current folder's subfolders
                if (childFolders.isEmpty()) {
                    Text(
                        text = "No subfolders here",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(childFolders, key = { it.id }) { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { currentFolderId = folder.id },
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (currentFolderId == folder.id)
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

                // Hint: Notes cannot be saved directly to root
                if (currentFolderId == "root") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a folder. Notes cannot be saved directly to Root.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentFolderId, noteName.trim()) },
                enabled = noteName.isNotBlank() && currentFolderId != "root"
            ) { Text("Save here") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}