@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mrsummaries_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.mrsummaries_app.files.FsNode
import com.example.mrsummaries_app.files.FsRepository
import com.example.mrsummaries_app.note.StrokePath

private data class FlatRow(val node: FsNode, val depth: Int)
private fun flattenTree(root: FsNode.Folder, expanded: Set<String>): List<FlatRow> {
    val out = mutableListOf<FlatRow>()
    fun walk(node: FsNode, depth: Int) {
        out += FlatRow(node, depth)
        if (node is FsNode.Folder && expanded.contains(node.id)) {
            for (child in node.children) walk(child, depth + 1)
        }
    }
    walk(root, 0)
    return out
}

private enum class NameDialogKind { CreateFolder, CreateNote, Rename }
@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Per-note editor state hoisted to HomeScreen so it survives note switching. */
class NoteStore {
    var pages by mutableStateOf<List<List<StrokePath>>>(listOf(emptyList()))
    var currentPath by mutableStateOf<List<Offset>>(emptyList())
    var undonePaths by mutableStateOf<List<StrokePath>>(emptyList())
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { FsRepository.ensureInitialized(context) }
    val treeVersion = FsRepository.treeVersion
    val selectedNoteId = FsRepository.selectedNoteId
    val root = FsRepository.root

    val noteStores = remember { mutableStateMapOf<String, NoteStore>() }
    var isMenuOpen by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkTheme()

    var showPerfSettings by remember { mutableStateOf(false) }
    var perfMinPointDistanceDp by remember { mutableStateOf(2f) }
    var perfBatchSize by remember { mutableStateOf(3) }
    var perfSpatialIndexEnabled by remember { mutableStateOf(true) }

    val expanded = remember { mutableStateSetOf<String>() }
    LaunchedEffect(root) { expanded.add(root.id) }

    var showMenuForId by remember { mutableStateOf<String?>(null) }
    var nameDialogKind by remember { mutableStateOf<NameDialogKind?>(null) }
    var nameDialogParentId by remember { mutableStateOf<String?>(null) }
    var nameDialogTargetId by remember { mutableStateOf<String?>(null) }
    var nameDialogInitial by remember { mutableStateOf("") }
    var moveSourceId by remember { mutableStateOf<String?>(null) }
    var showMovePicker by remember { mutableStateOf(false) }

    val rows = remember(treeVersion, root, expanded.toSet()) { flattenTree(root, expanded.toSet()) }

    Box(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // Side menu
            if (isMenuOpen) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(if (isDark) Color(0xFF121212) else Color(0xFFF2F2F2))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        IconButton(onClick = { isMenuOpen = false }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Menu, contentDescription = "Close menu", tint = if (isDark) Color.White else Color.Black)
                        }
                        Text("Notes", modifier = Modifier.padding(start = 8.dp), color = if (isDark) Color.White else Color.DarkGray, style = MaterialTheme.typography.titleMedium)
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(rows, key = { index, r -> "${r.node.id}-$index" }) { _, row ->
                            val node = row.node
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (node) {
                                            is FsNode.Folder -> {
                                                if (expanded.contains(node.id)) expanded.remove(node.id) else expanded.add(node.id)
                                            }
                                            is FsNode.Note -> {
                                                FsRepository.selectNote(node.id)
                                                isMenuOpen = false // close menu on note click
                                            }
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width((row.depth * 12).dp))
                                if (node is FsNode.Folder) {
                                    Icon(Icons.Filled.Folder, contentDescription = "Folder", tint = if (isDark) Color.White else Color.Black)
                                } else {
                                    Icon(Icons.Filled.Description, contentDescription = "Note", tint = if (isDark) Color.White else Color.Black)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when (node) {
                                        is FsNode.Folder -> node.name
                                        is FsNode.Note -> node.name
                                        else -> "Item"
                                    },
                                    color = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                if (node is FsNode.Folder) {
                                    val expandedFlag = expanded.contains(node.id)
                                    IconButton(onClick = { if (expandedFlag) expanded.remove(node.id) else expanded.add(node.id) }) {
                                        Icon(
                                            imageVector = if (expandedFlag) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (expandedFlag) "Collapse" else "Expand",
                                            tint = if (isDark) Color.White else Color.Black
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Box {
                                    IconButton(onClick = { showMenuForId = if (showMenuForId == node.id) null else node.id }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = if (isDark) Color.White else Color.Black)
                                    }
                                    DropdownMenu(
                                        expanded = (showMenuForId == node.id),
                                        onDismissRequest = { showMenuForId = null },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        if (node.id != root.id) {
                                            DropdownMenuItem(text = { Text("Rename") }, onClick = {
                                                showMenuForId = null
                                                nameDialogKind = NameDialogKind.Rename
                                                nameDialogTargetId = node.id
                                                nameDialogInitial = node.name
                                            })
                                        }
                                        if (node is FsNode.Folder) {
                                            DropdownMenuItem(text = { Text("Add Folder") }, onClick = {
                                                showMenuForId = null
                                                nameDialogKind = NameDialogKind.CreateFolder
                                                nameDialogParentId = node.id
                                                nameDialogInitial = ""
                                                expanded.add(node.id)
                                            })
                                            DropdownMenuItem(text = { Text("Add Note") }, onClick = {
                                                showMenuForId = null
                                                nameDialogKind = NameDialogKind.CreateNote
                                                nameDialogParentId = node.id
                                                nameDialogInitial = ""
                                                expanded.add(node.id)
                                            })
                                        }
                                        if (node.id != root.id) {
                                            DropdownMenuItem(text = { Text("Move…") }, onClick = {
                                                showMenuForId = null
                                                moveSourceId = node.id
                                                showMovePicker = true
                                            })
                                            DropdownMenuItem(text = { Text("Delete") }, onClick = {
                                                showMenuForId = null
                                                FsRepository.deleteNode(node.id)
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Start) {
                        IconButton(
                            onClick = { showPerfSettings = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (isDark) Color.White else Color.Black)
                        }
                    }
                }
            }

            // Editor area
            Box(modifier = Modifier.fillMaxSize()) {
                val selected = selectedNoteId?.let { FsRepository.findNode(it) as? FsNode.Note }
                if (selected == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a note from the left to open it.")
                    }
                } else {
                    val store = noteStores.getOrPut(selected.id) { NoteStore() }
                    // Pass topBarInsetDp so the HoverBar sits below the floating hamburger when menu is hidden
                    val topBarInsetDp = if (isMenuOpen) 8.dp else 56.dp
                    NoteCanvasScreen(
                        noteId = selected.id,
                        noteName = selected.name,
                        store = store,
                        perfMinPointDistanceDp = perfMinPointDistanceDp,
                        perfBatchSize = perfBatchSize,
                        perfSpatialIndexEnabled = perfSpatialIndexEnabled,
                        topBarInsetDp = topBarInsetDp
                    )
                }

                // Floating hamburger when menu is hidden
                if (!isMenuOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        IconButton(
                            onClick = { isMenuOpen = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Color.Black)
                        }
                    }
                }
            }
        }

        // Name dialog
        nameDialogKind?.let { kind ->
            NameDialog(
                title = when (kind) {
                    NameDialogKind.CreateFolder -> "New Folder"
                    NameDialogKind.CreateNote -> "New Note"
                    NameDialogKind.Rename -> "Rename"
                },
                initial = nameDialogInitial,
                onDismiss = {
                    nameDialogKind = null
                    nameDialogParentId = null
                    nameDialogTargetId = null
                    nameDialogInitial = ""
                },
                onConfirm = { text ->
                    when (kind) {
                        NameDialogKind.CreateFolder -> {
                            val parentId = nameDialogParentId ?: root.id
                            FsRepository.createFolder(parentId, text)
                            if (!expanded.contains(parentId)) expanded.add(parentId)
                        }
                        NameDialogKind.CreateNote -> {
                            val parentId = nameDialogParentId ?: root.id
                            val note = FsRepository.createNote(parentId, text)
                            if (!expanded.contains(parentId)) expanded.add(parentId)
                            note?.let { FsRepository.selectNote(it.id) }
                        }
                        NameDialogKind.Rename -> {
                            val targetId = nameDialogTargetId
                            if (targetId != null) FsRepository.renameNode(targetId, text)
                        }
                    }
                    nameDialogKind = null
                    nameDialogParentId = null
                    nameDialogTargetId = null
                    nameDialogInitial = ""
                }
            )
        }

        // Move picker
        if (showMovePicker && moveSourceId != null) {
            MovePickerDialog(
                sourceId = moveSourceId!!,
                onDismiss = { showMovePicker = false; moveSourceId = null },
                onMoveTo = { targetFolderId ->
                    val moved = FsRepository.moveNode(moveSourceId!!, targetFolderId)
                    if (moved) expanded.add(targetFolderId)
                    showMovePicker = false
                    moveSourceId = null
                }
            )
        }

        // Performance settings dialog
        if (showPerfSettings) {
            PerformanceSettingsDialog(
                minPointDistanceDp = perfMinPointDistanceDp,
                onMinPointDistanceChange = { perfMinPointDistanceDp = it },
                batchSize = perfBatchSize,
                onBatchSizeChange = { perfBatchSize = it },
                spatialIndexEnabled = perfSpatialIndexEnabled,
                onSpatialIndexEnabledChange = { perfSpatialIndexEnabled = it },
                onDismiss = { showPerfSettings = false }
            )
        }
    }
}