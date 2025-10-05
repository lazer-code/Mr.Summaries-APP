@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.mrsummaries_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.mrsummaries_app.files.FsNode
import com.example.mrsummaries_app.files.FsRepository
import com.example.mrsummaries_app.note.StrokePath
import kotlin.math.max

private val BrandTeal = Color(0xFF003153)

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FsRepository.ensureInitialized(context)
    }

    // Observe tree changes so UI updates without theme toggling
    val treeVersion = FsRepository.treeVersion

    val selectedNoteId = FsRepository.selectedNoteId
    val root = FsRepository.root

    val noteStores = remember { mutableStateMapOf<String, NoteStore>() }
    var isMenuOpen by remember { mutableStateOf(true) }

    val isDark = isSystemInDarkTheme()
    // Top bar background: brand teal in light mode; theme surface in dark mode
    val topBarContainer = if (!isDark) BrandTeal else MaterialTheme.colorScheme.surface
    val topBarContent = contrastOn(topBarContainer)

    val sideMenuWidth = 280.dp

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
            if (isMenuOpen) {
                val sideMenuBg = if (!isDark) BrandTeal else MaterialTheme.colorScheme.surface
                val menuIconTint = contrastOn(sideMenuBg)

                SideMenuTree(
                    modifier = Modifier
                        .width(sideMenuWidth)
                        .fillMaxHeight()
                        .background(sideMenuBg),
                    root = root,
                    selectedNoteId = selectedNoteId,
                    onNoteOpened = { isMenuOpen = false },
                    iconTint = menuIconTint,
                    treeVersion = treeVersion,
                    sideMenuBg = sideMenuBg,
                    sideMenuWidth = sideMenuWidth
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

private enum class NameDialogKind { CreateFolder, CreateNote, Rename }

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SideMenuTree(
    modifier: Modifier,
    root: FsNode.Folder,
    selectedNoteId: String?,
    onNoteOpened: () -> Unit,
    iconTint: Color,
    treeVersion: Int, // triggers recomposition when repo changes
    sideMenuBg: Color,
    sideMenuWidth: Dp
) {
    val expanded = remember { mutableStateListOf(root.id) }
    var showMenuForId by remember { mutableStateOf<String?>(null) }

    var nameDialogKind by remember { mutableStateOf<NameDialogKind?>(null) }
    var nameDialogParentId by remember { mutableStateOf<String?>(null) }
    var nameDialogTargetId by remember { mutableStateOf<String?>(null) }
    var nameDialogInitial by remember { mutableStateOf("") }

    var moveSourceId by remember { mutableStateOf<String?>(null) }
    var showMovePicker by remember { mutableStateOf(false) }

    // Track row Y positions (absolute window Y in px) and row heights (px)
    val rowTopByIdPx = remember { mutableStateMapOf<String, Float>() }
    val rowHeightByIdPx = remember { mutableStateMapOf<String, Int>() }

    var sideRootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    // Popup measured height (px)
    val popupHeightPx = remember { mutableStateOf<Int?>(null) }

    fun isExpanded(id: String) = expanded.contains(id)
    fun toggleExpanded(id: String) { if (isExpanded(id)) expanded.remove(id) else expanded.add(id) }

    // Recompute rows when tree changes or expand set changes
    val rows = remember(treeVersion, expanded.toList()) {
        flattenTree(root, expandedIds = expanded.toSet())
    }

    // Menu background 10% brighter (dark) or 10% darker (light) than side menu
    val isDark = isSystemInDarkTheme()
    val menuBg = if (isDark) adjustBrightness(sideMenuBg, 0.10f) else adjustBrightness(sideMenuBg, -0.10f)
    val menuTint = contrastOn(menuBg)

    Box(
        modifier = modifier
            .padding(8.dp)
            .onGloballyPositioned { coords ->
                // Capture this container's top-left in window space for absolute coordinates
                sideRootWindowOffset = coords.positionInWindow()
            }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows, key = { _, r -> r.node.id }) { _, row ->
                // Each row reports its top Y in window and its height; we store absolute top and height
                RowAnchor(
                    onPositionInWindow = { topInWindow, heightPx ->
                        rowTopByIdPx[row.node.id] = topInWindow
                        rowHeightByIdPx[row.node.id] = heightPx
                    }
                ) {
                    TreeRow(
                        row = row,
                        selectedNoteId = selectedNoteId,
                        isExpanded = isExpanded(row.node.id),
                        onToggleExpand = { toggleExpanded(row.node.id) },
                        onClickNode = {
                            when (val n = row.node) {
                                is FsNode.Note -> {
                                    FsRepository.selectNote(n.id)
                                    onNoteOpened()
                                }
                                is FsNode.Folder -> toggleExpanded(n.id)
                            }
                        },
                        onOpenMore = { showMenuForId = row.node.id },
                        iconTint = iconTint
                    )
                }
            }
        }

        // Show popup anchored to window coordinates so we can place it precisely
        if (showMenuForId != null) {
            val densityLocal = density
            val menuNode = showMenuForId?.let { FsRepository.findNode(it) }
            // compute window coordinates for popup origin (top-left)
            val sideMenuWidthPx = with(densityLocal) { sideMenuWidth.toPx() }
            val gapPx = with(densityLocal) { 8.dp.toPx() } // small gap between side menu and popup
            val popupX = (sideRootWindowOffset.x + sideMenuWidthPx + gapPx).toInt()

            // Calculate vertical center alignment: place popup so its center equals row center.
            val rowTop = rowTopByIdPx[showMenuForId] ?: sideRootWindowOffset.y
            val rowHeight = (rowHeightByIdPx[showMenuForId]?.toFloat() ?: 0f)
            val measuredPopupHeight = popupHeightPx.value ?: 0
            val popupYFloat = rowTop + rowHeight / 2f - measuredPopupHeight / 2f
            // ensure not negative
            val popupY = max(0, popupYFloat.toInt())

            // Use Popup to place the menu outside the side menu and centered vertically on the row
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(popupX, popupY),
                onDismissRequest = { showMenuForId = null },
                properties = PopupProperties(focusable = true)
            ) {
                // Wrap content: surface and content are explicitly wrapContent so they don't stretch
                Surface(
                    color = menuBg,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start)
                        .wrapContentHeight()
                        .onGloballyPositioned { coords ->
                            // Measure the popup height so we can center it on the selected row
                            popupHeightPx.value = coords.size.height
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentWidth(Alignment.Start)
                            .wrapContentHeight()
                    ) {
                        if (menuNode != null) {
                            // small helper to render a single menu item (wrap-content, consistent padding)
                            @Composable
                            fun MenuItem(textLabel: String, onClick: () -> Unit) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentWidth(Alignment.Start)
                                        .wrapContentHeight()
                                        .clickable { onClick() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(textLabel, color = menuTint)
                                }
                            }

                            // Rename (for all except root)
                            if (menuNode.id != root.id) {
                                MenuItem("Rename") {
                                    showMenuForId = null
                                    nameDialogKind = NameDialogKind.Rename
                                    nameDialogTargetId = menuNode.id
                                    nameDialogInitial = menuNode.name
                                }
                            }
                            // Add subfolder (folders only)
                            if (menuNode is FsNode.Folder) {
                                MenuItem("Add Folder") {
                                    showMenuForId = null
                                    nameDialogKind = NameDialogKind.CreateFolder
                                    nameDialogParentId = menuNode.id
                                    nameDialogInitial = ""
                                }
                                MenuItem("Add Note") {
                                    showMenuForId = null
                                    nameDialogKind = NameDialogKind.CreateNote
                                    nameDialogParentId = menuNode.id
                                    nameDialogInitial = ""
                                }
                            }

                            // Existing actions for non-root
                            if (menuNode.id != root.id) {
                                MenuItem("Move…") {
                                    showMenuForId = null
                                    moveSourceId = menuNode.id
                                    showMovePicker = true
                                }
                                MenuItem("Delete") {
                                    showMenuForId = null
                                    FsRepository.deleteNode(menuNode.id)
                                }
                            }
                        }
                    }
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
                        note?.let { FsRepository.selectNote(it.id); onNoteOpened() }
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

@Composable
private fun RowAnchor(
    onPositionInWindow: (Float, Int) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                onPositionInWindow(pos.y, coords.size.height)
            }
    ) {
        content()
    }
}

@Composable
private fun TreeRow(
    row: FlatRow,
    selectedNoteId: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClickNode: () -> Unit,
    onOpenMore: () -> Unit,
    iconTint: Color
) {
    val depthPadding = row.depth * 12
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
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
    expandedIds: Set<String>,
    depth: Int = 0,
    out: MutableList<FlatRow> = mutableListOf(),
    visited: MutableSet<String> = mutableSetOf()
): List<FlatRow> {
    if (!visited.add(root.id)) {
        out.add(FlatRow(root, depth, isCycleMarker = true))
        return out
    }
    out.add(FlatRow(root, depth))
    if (!expandedIds.contains(root.id)) return out
    for (child in root.children) {
        when (child) {
            is FsNode.Folder -> flattenTree(child, expandedIds, depth + 1, out, visited)
            is FsNode.Note -> out.add(FlatRow(child, depth + 1))
        }
    }
    return out
}