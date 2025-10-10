package com.example.mrsummaries_app.notepad

import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mrsummaries_app.notepad.note_components.DrawingCanvas
import com.example.mrsummaries_app.notepad.note_components.DrawingMode
import com.example.mrsummaries_app.notepad.note_components.PathProperties
import com.example.mrsummaries_app.notepad.note_components.UndoRedoState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(
    onMenuClick: () -> Unit
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
                    // Toggle stylus-only mode
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
                            } else if (textHistoryIndex > 0) {
                                // Text mode undo
                                textFutureHistory.add(0, noteText)
                                textHistoryIndex--
                                noteText = textHistory[textHistoryIndex]
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
                            } else if (textFutureHistory.isNotEmpty()) {
                                // Text mode redo
                                textHistory.add(noteText)
                                textHistoryIndex++
                                noteText = textFutureHistory.removeAt(0)
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

                    IconButton(onClick = { /* Save note */ }) {
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
                        undoRedoState.clear()
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (!isDrawingMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text Mode"
                    )
                }

                IconButton(
                    onClick = {
                        if (isDrawingMode && drawingMode == DrawingMode.PEN) {
                            // Already selected - show size adjustment
                            showSizeAdjustment = !showSizeAdjustment
                            adjustingToolMode = DrawingMode.PEN
                        } else {
                            // Not selected yet - select it
                            isDrawingMode = true
                            drawingMode = DrawingMode.PEN
                            showSizeAdjustment = false
                        }
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (isDrawingMode && drawingMode == DrawingMode.PEN)
                            MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Pen"
                    )
                }

                IconButton(
                    onClick = {
                        if (isDrawingMode && drawingMode == DrawingMode.HIGHLIGHTER) {
                            // Already selected - show size adjustment
                            showSizeAdjustment = !showSizeAdjustment
                            adjustingToolMode = DrawingMode.HIGHLIGHTER
                        } else {
                            // Not selected yet - select it
                            isDrawingMode = true
                            drawingMode = DrawingMode.HIGHLIGHTER
                            showSizeAdjustment = false
                        }
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (isDrawingMode && drawingMode == DrawingMode.HIGHLIGHTER)
                            MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BorderColor,
                        contentDescription = "Highlighter"
                    )
                }

                IconButton(
                    onClick = {
                        if (isDrawingMode && drawingMode == DrawingMode.ERASER) {
                            // Already selected - show size adjustment
                            showSizeAdjustment = !showSizeAdjustment
                            adjustingToolMode = DrawingMode.ERASER
                        } else {
                            // Not selected yet - select it
                            isDrawingMode = true
                            drawingMode = DrawingMode.ERASER
                            showSizeAdjustment = false
                        }
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = if (isDrawingMode && drawingMode == DrawingMode.ERASER)
                            MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Eraser"
                    )
                }
            }

            // Combined Size & Color Adjustment Panel (appears when a tool is double-clicked)
            AnimatedVisibility(
                visible = showSizeAdjustment,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Show color selection for pen and highlighter
                    if (adjustingToolMode == DrawingMode.PEN || adjustingToolMode == DrawingMode.HIGHLIGHTER) {
                        Text(
                            text = "Color",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Color selection grid - 2 rows of 5 colors
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // First row of colors
                                colors.take(5).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color, shape = RoundedCornerShape(16.dp))
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
                                            .background(color, shape = RoundedCornerShape(16.dp))
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
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    // Size adjustment UI
                    Text(
                        text = "Size",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small size indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Gray, RoundedCornerShape(5.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Slider
                        Slider(
                            value = when (adjustingToolMode) {
                                DrawingMode.PEN -> penSize
                                DrawingMode.HIGHLIGHTER -> highlighterSize
                                DrawingMode.ERASER -> eraserSize
                                else -> 5f
                            },
                            onValueChange = { newSize ->
                                when (adjustingToolMode) {
                                    DrawingMode.PEN -> penSize = newSize
                                    DrawingMode.HIGHLIGHTER -> highlighterSize = newSize
                                    DrawingMode.ERASER -> eraserSize = newSize
                                    else -> {}
                                }
                            },
                            valueRange = when (adjustingToolMode) {
                                DrawingMode.PEN -> 1f..100f
                                DrawingMode.HIGHLIGHTER -> 5f..100f
                                DrawingMode.ERASER -> 10f..100f
                                else -> 1f..100f
                            },
                            steps = 0,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Large size indicator
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.Gray, RoundedCornerShape(15.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Size text display
                        Text(
                            text = when (adjustingToolMode) {
                                DrawingMode.PEN -> "${penSize.toInt()}"
                                DrawingMode.HIGHLIGHTER -> "${highlighterSize.toInt()}"
                                DrawingMode.ERASER -> "${eraserSize.toInt()}"
                                else -> "5"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Size preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewColor = when (adjustingToolMode) {
                            DrawingMode.PEN -> selectedColor
                            DrawingMode.HIGHLIGHTER -> selectedColor.copy(alpha = 0.3f)
                            DrawingMode.ERASER -> Color.LightGray
                            else -> Color.Black
                        }

                        val previewSize = when (adjustingToolMode) {
                            DrawingMode.PEN -> penSize
                            DrawingMode.HIGHLIGHTER -> highlighterSize
                            DrawingMode.ERASER -> eraserSize
                            else -> 5f
                        }.coerceAtMost(100f)  // Ensure preview size is capped at 100dp

                        Box(
                            modifier = Modifier
                                .height(previewSize.dp)
                                .width(200.dp)
                                .background(previewColor, RoundedCornerShape(previewSize/2))
                        )
                    }
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
                        // Hide size adjustment when clicking on the canvas
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
                        onPathDrawn = { newPaths ->
                            // Optional: Do something with the paths
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

                    // Helpful indicator showing stylus button functionality
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Press stylus button for quick eraser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Text editing mode with undo/redo tracking
                    TextField(
                        value = noteText,
                        onValueChange = { newText ->
                            // Track text changes for undo/redo
                            if (noteText != newText) {
                                // Add current text to history
                                if (textHistoryIndex < textHistory.size - 1) {
                                    // Remove future history if we're in the middle of the history
                                    textHistory.subList(textHistoryIndex + 1, textHistory.size).clear()
                                    textFutureHistory.clear()
                                }

                                // Store current text for undo
                                textHistory.add(noteText)
                                textHistoryIndex = textHistory.size - 1

                                // Update text
                                noteText = newText

                                // Limit history size
                                if (textHistory.size > 100) {
                                    textHistory.removeAt(0)
                                    textHistoryIndex--
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Start typing...") },
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
}