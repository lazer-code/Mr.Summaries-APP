package com.example.mrsummaries_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.ColorPickerDialog
import com.example.mrsummaries_app.CostumePen
import com.example.mrsummaries_app.DrawingTool
import com.example.mrsummaries_app.PenPreferences
import com.example.mrsummaries_app.StrokePath
import com.example.mrsummaries_app.StylusDrawingCanvas
import com.example.mrsummaries_app.ui.persistence.NoteContentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full editor integration for a note.
 * - Per-note drawing state is provided via [store].
 * - Pen presets and sizes are persisted globally via PenPreferences.
 * - Drawing content is persisted per note via NoteContentStore.
 */
@Composable
fun NoteCanvasScreen(
    noteId: String,
    noteName: String,
    store: NoteStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Tool state
    var eraseToggled by remember { mutableStateOf(false) }
    var spenPressed by remember { mutableStateOf(false) }
    val drawingTool = if (spenPressed || eraseToggled) DrawingTool.ERASE else DrawingTool.WRITE

    // Global pen settings/presets
    var currentColor by remember { mutableStateOf(Color.Blue) }
    var currentStrokeWidthDp by remember { mutableStateOf(6f) }
    var eraserSizeDp by remember { mutableStateOf(20f) }
    var costumePens by remember {
        mutableStateOf(
            listOf(
                CostumePen(Color.Blue, 6f),
                CostumePen(Color.Black, 5f),
                CostumePen(Color.Red, 6f),
                CostumePen(Color(0xFF4CAF50), 6f)
            )
        )
    }

    // Load global prefs and initial pen once
    LaunchedEffect(Unit) {
        runCatching { PenPreferences.loadCostumePens(context) }.getOrNull()?.let { loaded ->
            if (loaded.isNotEmpty()) costumePens = loaded
        }
        runCatching { PenPreferences.loadPenSettings(context) }.getOrNull()?.let { s ->
            currentStrokeWidthDp = s.penWidthDp
            eraserSizeDp = s.eraserSizeDp
        }
        if (store.paths.isEmpty() && costumePens.isNotEmpty()) {
            currentColor = costumePens.first().color
            currentStrokeWidthDp = costumePens.first().strokeWidthDp
        }
    }

    // Persist global pen settings and presets on change
    LaunchedEffect(costumePens) {
        runCatching { PenPreferences.saveCostumePens(context, costumePens) }
    }
    LaunchedEffect(currentStrokeWidthDp, eraserSizeDp) {
        runCatching { PenPreferences.savePenSettings(context, currentStrokeWidthDp, eraserSizeDp) }
    }

    // Load per-note drawing content once for this note
    LaunchedEffect(noteId) {
        val loaded = runCatching { NoteContentStore.load(context, noteId) }.getOrNull()
        if (loaded != null) {
            store.paths = loaded
            store.currentPath = emptyList()
            store.undonePaths = emptyList()
        }
    }

    // Color picker state (global)
    var showColorPicker by remember { mutableStateOf(false) }
    var pickerPurpose by remember { mutableStateOf(PickerPurpose.AddPreset) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Selected swatch index based on color+width
    val selectedIndex = remember(currentColor, currentStrokeWidthDp, costumePens) {
        costumePens.indexOfFirst { it.color == currentColor && it.strokeWidthDp == currentStrokeWidthDp }
    }

    fun selectPenAt(index: Int) {
        if (index in costumePens.indices) {
            val pen = costumePens[index]
            currentColor = pen.color
            currentStrokeWidthDp = pen.strokeWidthDp
        }
    }

    fun addOrMoveColorToFront(chosen: Color) {
        val existingIndex = costumePens.indexOfFirst { it.color == chosen }
        val newList = if (existingIndex >= 0) {
            val updated = costumePens.toMutableList()
            val old = updated.removeAt(existingIndex)
            updated.add(0, old.copy(color = chosen))
            updated
        } else {
            listOf(CostumePen(chosen, currentStrokeWidthDp)) + costumePens
        }
        costumePens = newList
        if (newList.isNotEmpty()) selectPenAt(0)
    }

    fun deletePresetAt(index: Int) {
        if (index in costumePens.indices && costumePens.size > 1) {
            val newList = costumePens.toMutableList()
            newList.removeAt(index)
            costumePens = newList
            val newSel = index.coerceAtMost(newList.lastIndex).coerceAtLeast(0)
            if (newList.isNotEmpty()) selectPenAt(newSel)
        }
    }

    fun persistNote() {
        scope.launch {
            withContext(Dispatchers.IO) {
                NoteContentStore.save(context, noteId, store.paths)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StylusDrawingCanvas(
            modifier = Modifier.fillMaxSize(),
            drawingTool = drawingTool,
            paths = store.paths,
            currentPath = store.currentPath,
            currentColor = currentColor,
            currentStrokeWidthDp = currentStrokeWidthDp,
            eraserSizeDp = eraserSizeDp,
            onCurrentPathChange = { store.currentPath = it },
            onPathAdded = { path ->
                store.paths = store.paths + listOf(StrokePath(path, currentColor, currentStrokeWidthDp))
                store.undonePaths = emptyList()
                persistNote()
            },
            onErasePath = { index ->
                if (index in store.paths.indices) {
                    store.undonePaths = store.undonePaths + listOf(store.paths[index])
                    store.paths = store.paths.toMutableList().apply { removeAt(index) }
                    persistNote()
                }
            },
            onStylusButtonChange = { pressed -> spenPressed = pressed }
        )

        // Import from ui package
        HoverBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            drawingTool = drawingTool,
            currentColor = currentColor,
            currentStrokeWidthDp = currentStrokeWidthDp,
            eraserSizeDp = eraserSizeDp,
            costumePens = costumePens,
            selectedIndex = selectedIndex,
            onToolChange = { tool -> eraseToggled = tool == DrawingTool.ERASE && !eraseToggled },
            onUndo = {
                if (store.paths.isNotEmpty()) {
                    store.undonePaths = store.undonePaths + listOf(store.paths.last())
                    store.paths = store.paths.dropLast(1)
                    persistNote()
                }
            },
            onRedo = {
                if (store.undonePaths.isNotEmpty()) {
                    store.paths = store.paths + listOf(store.undonePaths.last())
                    store.undonePaths = store.undonePaths.dropLast(1)
                    persistNote()
                }
            },
            onSelectIndex = { i -> selectPenAt(i) },
            onAddCostume = {
                pickerPurpose = PickerPurpose.AddPreset
                showColorPicker = true
                editingIndex = null
            },
            onLongPressIndex = { i ->
                editingIndex = i
                pickerPurpose = PickerPurpose.EditPreset
                showColorPicker = true
            },
            onPenWidthChange = { width -> currentStrokeWidthDp = width.coerceIn(1f, 24f) },
            onEraserSizeChange = { size -> eraserSizeDp = size.coerceIn(8f, 64f) },
            showPenSize = false,
            showEraserSize = false,
            setShowPenSize = { /* no-op */ },
            setShowEraserSize = { /* no-op */ }
        )
    }

    if (showColorPicker) {
        val initial = when (pickerPurpose) {
            PickerPurpose.AddPreset -> currentColor
            PickerPurpose.EditPreset -> editingIndex?.let { idx ->
                costumePens.getOrNull(idx)?.color ?: currentColor
            } ?: currentColor
        }
        val canDeleteInDialog = pickerPurpose == PickerPurpose.EditPreset && costumePens.size > 1

        ColorPickerDialog(
            initialColor = initial,
            showDelete = canDeleteInDialog,
            onDelete = {
                editingIndex?.let { idx -> deletePresetAt(idx) }
                showColorPicker = false
                editingIndex = null
            },
            onDismiss = {
                showColorPicker = false
                editingIndex = null
            },
            onConfirm = { color ->
                when (pickerPurpose) {
                    PickerPurpose.AddPreset -> addOrMoveColorToFront(color)
                    PickerPurpose.EditPreset -> {
                        editingIndex?.let { idx ->
                            if (idx in costumePens.indices) {
                                val updated = costumePens.toMutableList()
                                val old = updated[idx]
                                updated[idx] = old.copy(color = color)
                                costumePens = updated
                                if (selectedIndex == idx) currentColor = color
                            }
                        }
                    }
                }
                showColorPicker = false
                editingIndex = null
            }
        )
    }
}

private enum class PickerPurpose { AddPreset, EditPreset }