package com.example.mrsummaries_app.notepad

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mrsummaries_app.filesystem.viewmodel.FileSystemViewModel
import com.example.mrsummaries_app.notepad.note_components.DrawingCanvas
import com.example.mrsummaries_app.notepad.note_components.DrawingMode
import com.example.mrsummaries_app.notepad.note_components.PathProperties
import com.example.mrsummaries_app.notepad.note_components.UndoRedoState
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(
    onMenuClick: () -> Unit,
    noteId: String? = null,
    fileSystemViewModel: FileSystemViewModel = viewModel()
) {
    var noteText by remember { mutableStateOf("") }
    var isDrawingMode by remember { mutableStateOf(false) }
    var drawingMode by remember { mutableStateOf(DrawingMode.PEN) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var showStylusMessage by remember { mutableStateOf(false) }
    var stylusOnly by remember { mutableStateOf(true) }

    // Size settings for each tool with default values
    var penSize by remember { mutableStateOf(5f) }
    var highlighterSize by remember { mutableStateOf(20f) }
    var eraserSize by remember { mutableStateOf(30f) }

    // Current stroke width based on selected tool
    val strokeWidth = when (drawingMode) {
        DrawingMode.PEN -> penSize
        DrawingMode.HIGHLIGHTER -> highlighterSize
        DrawingMode.ERASER -> eraserSize
    }

    // State for showing size adjustment slider
    var showSizeAdjustment by remember { mutableStateOf(false) }
    var adjustingToolMode by remember { mutableStateOf<DrawingMode?>(null) }

    // Available colors
    val colors = listOf(
        Color.Black, Color.Blue, Color.Red, Color.Green, Color.Yellow,
        Color.Magenta, Color.Cyan, Color(0xFF800000), Color(0xFF008080), Color(0xFF800080)
    )

    // For text undo/redo
    val textHistory = remember { mutableStateListOf("") }
    val textFutureHistory = remember { mutableStateListOf<String>() }
    var textHistoryIndex by remember { mutableStateOf(0) }

    // For drawing undo/redo
    val undoRedoState = remember { UndoRedoState() }

    // Local current note id that can be set when user saves a new note
    var currentNoteId by remember { mutableStateOf(noteId) }

    // Save dialog visibility
    var showSaveDialog by remember { mutableStateOf(false) }

    // Collect once and pass into save dialog so existing folders show up
    val allItems by fileSystemViewModel.allItems.collectAsState()
    val currentFolderIdByVM by fileSystemViewModel.currentFolderId.collectAsState()

    // Load note content if currentNoteId is provided
    LaunchedEffect(currentNoteId) {
        currentNoteId?.let { id ->
            val note = fileSystemViewModel.getNote(id)
            if (note != null) {
                noteText = note.content
                // Initialize text history with existing content so first undo works correctly
                textHistory.clear()
                textHistory.add(noteText)
                textHistoryIndex = textHistory.lastIndex
            }
        }
    }

    // Save note content when it changes (only for existing saved notes)
    LaunchedEffect(noteText) {
        currentNoteId?.let { id ->
            // Avoid spamming repository with empty content
            fileSystemViewModel.updateNoteContent(id, noteText)
        }
    }

    // Auto-hide the stylus message after a delay
    LaunchedEffect(showStylusMessage) {
        if (showStylusMessage) {
            delay(2000) // Hide message after 2 seconds
            showStylusMessage = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mr. Summaries") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    // Toggle stylus-only mode (affects DrawingCanvas)
                    IconButton(onClick = { stylusOnly = !stylusOnly }) {
                        Icon(
                            imageVector = if (stylusOnly) Icons.Default.Edit else Icons.Default.TouchApp,
                            contentDescription = if (stylusOnly) "Stylus Only" else "Touch Enabled"
                        )
                    }

                    // Undo Button
                    IconButton(
                        onClick = {
                            if (isDrawingMode) {
                                undoRedoState.undo()
                            } else {
                                // Text undo logic
                                if (textHistoryIndex > 0) {
                                    textFutureHistory.add(0, noteText)
                                    textHistoryIndex--
                                    noteText = textHistory[textHistoryIndex]
                                }
                            }
                        },
                        enabled = (isDrawingMode || textHistoryIndex > 0)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Undo,
                            contentDescription = "Undo",
                            tint = if (isDrawingMode || textHistoryIndex > 0)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = {
                            if (isDrawingMode) {
                                undoRedoState.redo()
                            } else {
                                // Text redo logic
                                if (textFutureHistory.isNotEmpty()) {
                                    noteText = textFutureHistory.removeAt(0)
                                    textHistoryIndex++
                                    if (textHistoryIndex >= textHistory.size) {
                                        textHistory.add(noteText)
                                    } else {
                                        textHistory[textHistoryIndex] = noteText
                                    }
                                }
                            }
                        },
                        enabled = (isDrawingMode || textFutureHistory.isNotEmpty())
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Redo,
                            contentDescription = "Redo",
                            tint = if (isDrawingMode || textFutureHistory.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Save Button -> open folder picker dialog
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save note"
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
            // Tool Selection Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        isDrawingMode = false
                        showSizeAdjustment = false
                        // Clear drawing undo state when switching modes
                        undoRedoState.reset()
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (!isDrawingMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text",
                        tint = if (!isDrawingMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        isDrawingMode = true
                        drawingMode = DrawingMode.PEN
                        showSizeAdjustment = true
                        adjustingToolMode = DrawingMode.PEN
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (isDrawingMode && drawingMode == DrawingMode.PEN)
                            MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Pen",
                        tint = if (isDrawingMode && drawingMode == DrawingMode.PEN)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        isDrawingMode = true
                        drawingMode = DrawingMode.HIGHLIGHTER
                        showSizeAdjustment = true
                        adjustingToolMode = DrawingMode.HIGHLIGHTER
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (isDrawingMode && drawingMode == DrawingMode.HIGHLIGHTER)
                            MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BorderColor,
                        contentDescription = "Highlighter",
                        tint = if (isDrawingMode && drawingMode == DrawingMode.HIGHLIGHTER)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        isDrawingMode = true
                        drawingMode = DrawingMode.ERASER
                        showSizeAdjustment = true
                        adjustingToolMode = DrawingMode.ERASER
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (isDrawingMode && drawingMode == DrawingMode.ERASER)
                            MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Eraser",
                        tint = if (isDrawingMode && drawingMode == DrawingMode.ERASER)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Size adjustment UI
            AnimatedVisibility(
                visible = showSizeAdjustment,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Size",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = when (adjustingToolMode) {
                            DrawingMode.PEN -> penSize
                            DrawingMode.HIGHLIGHTER -> highlighterSize
                            DrawingMode.ERASER -> eraserSize
                            null -> penSize
                        },
                        onValueChange = { newValue ->
                            when (adjustingToolMode) {
                                DrawingMode.PEN -> penSize = newValue
                                DrawingMode.HIGHLIGHTER -> highlighterSize = newValue
                                DrawingMode.ERASER -> eraserSize = newValue
                                null -> Unit
                            }
                        },
                        valueRange = 1f..50f
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Color palette (two rows)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // First row of colors
                            colors.take(5).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(color, RoundedCornerShape(16.dp))
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedColor == color)
                                                MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Second row of colors
                            colors.drop(5).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(color, RoundedCornerShape(16.dp))
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedColor == color)
                                                MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // Note Canvas/TextField Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        // Hide size adjustment when clicking on the canvas area
                        showSizeAdjustment = false
                    }
            ) {
                if (isDrawingMode) {
                    DrawingCanvas(
                        backgroundColor = Color.White,
                        strokeColor = selectedColor,
                        strokeWidth = strokeWidth,
                        drawingMode = drawingMode,
                        stylusOnly = stylusOnly,
                        onModeChanged = { newMode ->
                            // This is called when stylus button changes the mode
                            drawingMode = newMode
                            showSizeAdjustment = false
                        },
                        onPathDrawn = { _: List<PathProperties> ->
                            // Optional: handle drawn paths
                        },
                        undoState = undoRedoState
                    )

                    if (showStylusMessage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Please use a stylus for drawing",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Text editing mode
                    TextField(
                        value = noteText,
                        onValueChange = { newText ->
                            // Add to history for undo functionality
                            if (newText != noteText) {
                                textFutureHistory.clear() // Clear redo history when new text is typed
                                if (textHistoryIndex == textHistory.lastIndex) {
                                    textHistory.add(newText)
                                    textHistoryIndex = textHistory.lastIndex
                                } else {
                                    textHistory[textHistoryIndex + 1] = newText
                                    textHistoryIndex++
                                }
                            }
                            noteText = newText
                        },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Start typing your notes...") },
                        textStyle = TextStyle(fontSize = 16.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }

    // Save dialog
    if (showSaveDialog) {
        SaveNoteDialog(
            allItems = allItems,
            initialFolderId = currentFolderIdByVM,
            initialName = if (currentNoteId == null)
                "Untitled Note"
            else
                (fileSystemViewModel.getNote(currentNoteId!!)?.name ?: "Untitled Note"),
            onDismiss = { showSaveDialog = false },
            onConfirm = { folderId, name ->
                // Create new note if we don't have one yet; otherwise rename and move
                if (currentNoteId == null) {
                    val newId = fileSystemViewModel.createNoteAndReturnId(name, folderId)
                    currentNoteId = newId
                    // Persist current content
                    fileSystemViewModel.updateNoteContent(newId, noteText)
                } else {
                    // Existing note: move and rename to chosen location/name
                    fileSystemViewModel.renameItem(currentNoteId!!, name)
                    fileSystemViewModel.moveItem(currentNoteId!!, folderId)
                    // Content auto-saves via LaunchedEffect(noteText)
                }
                showSaveDialog = false
            },
            onCreateFolder = { parentId, folderName ->
                // Create and return the new folder id so dialog can auto-select it
                fileSystemViewModel.createFolderAndReturnId(folderName, parentId)
            }
        )
    }
}