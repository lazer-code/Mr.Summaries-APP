@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mrsummaries_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.StrokePath
import com.example.mrsummaries_app.files.FsNode
import com.example.mrsummaries_app.files.FsRepository

private val BrandTeal = Color(0xFF003153)

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FsRepository.ensureInitialized(context)
        if (FsRepository.root.children.isEmpty()) {
            // Seed only on truly empty storage
            FsRepository.resetToSample()
        }
    }

    val selectedNoteId = FsRepository.selectedNoteId
    val root = FsRepository.root

    val noteStores = remember { mutableStateMapOf<String, NoteStore>() }
    var isMenuOpen by remember { mutableStateOf(true) }

    val isDark = isSystemInDarkTheme()
    // Top bar background: brand teal in light mode; theme surface in dark mode
    val topBarContainer = if (!isDark) BrandTeal else MaterialTheme.colorScheme.surface
    val topBarContent = contrastOn(topBarContainer)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mr. Summaries") },
                navigationIcon = {
                    IconButton(onClick = { isMenuOpen = !isMenuOpen }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarContainer,
                    titleContentColor = topBarContent,
                    navigationIconContentColor = topBarContent,
                    actionIconContentColor = topBarContent
                )
            )
        }
    ) { padding ->
        Row(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Collapsible side menu
            if (isMenuOpen) {
                val sideMenuBg = if (!isDark) BrandTeal else MaterialTheme.colorScheme.surface
                val menuIconTint = contrastOn(sideMenuBg)

                SideMenuTree(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(sideMenuBg),
                    root = root,
                    selectedNoteId = selectedNoteId,
                    onNoteOpened = { isMenuOpen = false },
                    iconTint = menuIconTint
                )

                // Vertical divider adapted to background
                val dividerColor = if (!isDark) Color.White.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(dividerColor)
                )
            }

            // Note viewer/editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val selected = FsRepository.selectedNoteId?.let { FsRepository.findNode(it) as? FsNode.Note }
                if (selected == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select a note from the left to open it.")
                    }
                } else {
                    val store = noteStores.getOrPut(selected.id) { NoteStore() }
                    NoteCanvasScreen(
                        noteId = selected.id,
                        noteName = selected.name,
                        store = store
                    )
                }
            }
        }
    }
}

/** Per-note editor state hoisted to HomeScreen so it survives note switching. */
class NoteStore {
    var paths by mutableStateOf<List<StrokePath>>(emptyList())
    var currentPath by mutableStateOf<List<Offset>>(emptyList())
    var undonePaths by mutableStateOf<List<StrokePath>>(emptyList())
}

private data class FlatRow(
    val node: FsNode,
    val depth: Int,
    val isCycleMarker: Boolean = false
)

@Composable
private fun SideMenuTree(
    modifier: Modifier,
    root: FsNode.Folder,
    selectedNoteId: String?,
    onNoteOpened: () -> Unit,
    iconTint: Color
) {
    val expanded = remember { mutableStateListOf(root.id) }
    var showMenuForId by remember { mutableStateOf<String?>(null) }
    var selectedActionId by remember { mutableStateOf<String?>(null) }

    var nameDialogKind by remember { mutableStateOf<NameDialogKind?>(null) }
    var nameDialogParentId by remember { mutableStateOf<String?>(null) }
    var nameDialogTargetId by remember { mutableStateOf<String?>(null) }
    var nameDialogInitial by remember { mutableStateOf("") }

    var moveSourceId by remember { mutableStateOf<String?>(null) }
    var showMovePicker by remember { mutableStateOf(false) }

    fun isExpanded(id: String) = expanded.contains(id)
    fun toggleExpanded(id: String) { if (isExpanded(id)) expanded.remove(id) else expanded.add(id) }

    val rows = flattenTree(root, expandedIds = expanded.toSet())

    Column(modifier = modifier.padding(8.dp)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows, key = { _, r -> r.node.id }) { _, row ->
                val showInline = selectedActionId == row.node.id
                TreeRow(
                    row = row,
                    selectedNoteId = selectedNoteId,
                    isExpanded = isExpanded(row.node.id),
                    showInlineActions = showInline,
                    onToggleExpand = { toggleExpanded(row.node.id) },
                    onClickNode = {
                        selectedActionId = row.node.id
                        when (val n = row.node) {
                            is FsNode.Note -> {
                                FsRepository.selectNote(n.id)
                                onNoteOpened()
                            }
                            is FsNode.Folder -> toggleExpanded(n.id)
                        }
                    },
                    onNewFolder = {
                        val parentId = when (val n = row.node) {
                            is FsNode.Folder -> n.id
                            is FsNode.Note -> findParentFolderId(n.id)
                        }
                        nameDialogKind = NameDialogKind.CreateFolder
                        nameDialogParentId = parentId
                        nameDialogTargetId = null
                        nameDialogInitial = ""
                    },
                    onNewNote = {
                        val parentId = when (val n = row.node) {
                            is FsNode.Folder -> n.id
                            is FsNode.Note -> findParentFolderId(n.id)
                        }
                        nameDialogKind = NameDialogKind.CreateNote
                        nameDialogParentId = parentId
                        nameDialogTargetId = null
                        nameDialogInitial = ""
                    },
                    onRename = {
                        nameDialogKind = NameDialogKind.Rename
                        nameDialogParentId = null
                        nameDialogTargetId = row.node.id
                        nameDialogInitial = row.node.name
                    },
                    onOpenMore = { showMenuForId = row.node.id },
                    iconTint = iconTint
                )

                DropdownMenu(
                    expanded = showMenuForId == row.node.id,
                    onDismissRequest = { showMenuForId = null }
                ) {
                    if (row.node.id != root.id) {
                        DropdownMenuItem(
                            text = { Text("Move…", color = iconTint) },
                            onClick = {
                                showMenuForId = null
                                moveSourceId = row.node.id
                                showMovePicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = iconTint) },
                            onClick = {
                                showMenuForId = null
                                FsRepository.deleteNode(row.node.id)
                            }
                        )
                    }
                }
            }
        }

        // Create/Rename dialog
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
                            note?.let { FsRepository.selectNote(it.id); selectedActionId = it.id; onNoteOpened() }
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
                    if (moved && !expanded.contains(targetFolderId)) expanded.add(targetFolderId)
                    showMovePicker = false
                    moveSourceId = null
                },
                iconTint = iconTint
            )
        }
    }
}

private enum class NameDialogKind { CreateFolder, CreateNote, Rename }

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = text.trim()
                    if (name.isNotEmpty()) onConfirm(name)
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TreeRow(
    row: FlatRow,
    selectedNoteId: String?,
    isExpanded: Boolean,
    showInlineActions: Boolean,
    onToggleExpand: () -> Unit,
    onClickNode: () -> Unit,
    onNewFolder: () -> Unit,
    onNewNote: () -> Unit,
    onRename: () -> Unit,
    onOpenMore: () -> Unit,
    iconTint: Color
) {
    val depthPadding = (row.depth * 16).coerceAtMost(16 * 12)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (row.node is FsNode.Note && row.node.id == selectedNoteId)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(start = depthPadding.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val n = row.node) {
            is FsNode.Folder -> {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = iconTint
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = "Folder",
                    tint = iconTint
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    n.name,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClickNode),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = iconTint
                )
                if (showInlineActions) {
                    IconButton(onClick = onNewFolder) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = iconTint)
                    }
                    IconButton(onClick = onNewNote) {
                        Icon(Icons.Filled.NoteAdd, contentDescription = "New Note", tint = iconTint)
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = iconTint)
                    }
                }
                Text(
                    "⋮",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onOpenMore() },
                    color = iconTint
                )
            }
            is FsNode.Note -> {
                Spacer(Modifier.width(40.dp))
                Icon(Icons.Filled.Description, contentDescription = "Note", tint = iconTint)
                Spacer(Modifier.width(8.dp))
                Text(
                    n.name,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClickNode),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = iconTint
                )
                if (showInlineActions) {
                    IconButton(onClick = onNewNote) {
                        Icon(Icons.Filled.NoteAdd, contentDescription = "New Note", tint = iconTint)
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = iconTint)
                    }
                }
                Text(
                    "⋮",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onOpenMore() },
                    color = iconTint
                )
            }
        }
    }
}

private fun flattenTree(
    root: FsNode.Folder,
    expandedIds: Set<String>
): List<FlatRow> {
    val out = mutableListOf<FlatRow>()
    val visited = hashSetOf<String>()

    fun walk(node: FsNode, depth: Int) {
        out.add(FlatRow(node, depth))
        if (node is FsNode.Folder) {
            if (!visited.add(node.id)) {
                out.add(FlatRow(node, depth + 1, isCycleMarker = true))
                return
            }
            if (expandedIds.contains(node.id)) {
                node.children.forEach { child -> walk(child, depth + 1) }
            }
        }
    }
    walk(root, 0)
    return out
}

private fun findParentFolderId(nodeId: String): String {
    fun search(folder: FsNode.Folder): String? {
        folder.children.forEach { child ->
            if (child.id == nodeId) return folder.id
            if (child is FsNode.Folder) search(child)?.let { return it }
        }
        return null
    }
    return search(FsRepository.root) ?: FsRepository.root.id
}