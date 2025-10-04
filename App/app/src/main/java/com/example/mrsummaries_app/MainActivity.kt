package com.example.mrsummaries_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.ui.theme.MrSummariesAppTheme

private enum class PickerPurpose { AddPreset, EditPreset }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MrSummariesAppTheme {
                DrawingScreen()
            }
        }
    }
}

@Composable
fun DrawingScreen() {
    val context = LocalContext.current

    // Strokes and undo/redo
    var paths by remember { mutableStateOf<List<StrokePath>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var undonePaths by remember { mutableStateOf<List<StrokePath>>(emptyList()) }

    // Tools state
    var eraseToggled by remember { mutableStateOf(false) }
    var spenPressed by remember { mutableStateOf(false) }
    val drawingTool = if (spenPressed || eraseToggled) DrawingTool.ERASE else DrawingTool.WRITE

    // Current pen settings
    var currentColor by remember { mutableStateOf(Color.Blue) }
    var currentStrokeWidthDp by remember { mutableStateOf(6f) }

    // Eraser size (dp)
    var eraserSizeDp by remember { mutableStateOf(20f) }

    // Costume pens (color + width)
    var costumePens by remember {
        mutableStateOf<List<CostumePen>>(
            listOf(
                CostumePen(Color.Blue, 6f),
                CostumePen(Color.Black, 5f),
                CostumePen(Color.Red, 6f),
                CostumePen(Color(0xFF4CAF50), 6f)
            )
        )
    }

    // Load saved presets and sizes
    LaunchedEffect(Unit) {
        runCatching { PenPreferences.loadCostumePens(context) }.getOrNull()?.let { loaded ->
            if (loaded.isNotEmpty()) costumePens = loaded
        }
        runCatching { PenPreferences.loadPenSettings(context) }.getOrNull()?.let { s ->
            currentStrokeWidthDp = s.penWidthDp
            eraserSizeDp = s.eraserSizeDp
        }
        if (paths.isEmpty() && costumePens.isNotEmpty()) {
            currentColor = costumePens.first().color
            currentStrokeWidthDp = costumePens.first().strokeWidthDp
        }
    }

    // Persist presets and sizes on change
    LaunchedEffect(costumePens) {
        runCatching { PenPreferences.saveCostumePens(context, costumePens) }
    }
    LaunchedEffect(currentStrokeWidthDp, eraserSizeDp) {
        runCatching { PenPreferences.savePenSettings(context, currentStrokeWidthDp, eraserSizeDp) }
    }

    // Color picker dialog state
    var showColorPicker by remember { mutableStateOf(false) }
    var pickerPurpose by remember { mutableStateOf(PickerPurpose.AddPreset) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Size panel visibility (closed automatically when writing starts)
    var showPenSize by remember { mutableStateOf(false) }
    var showEraserSize by remember { mutableStateOf(false) }

    // Compute selected index based on current color+width
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

    // Add or move a color to front (dedupe by color ONLY; keep width of existing or current)
    fun addOrMoveColorToFront(chosen: Color) {
        val idx = costumePens.indexOfFirst { it.color == chosen }
        if (idx >= 0) {
            // Move existing preset (keep its width)
            val existing = costumePens[idx]
            costumePens = listOf(existing) + costumePens.filterIndexed { i, _ -> i != idx }
            currentColor = existing.color
            currentStrokeWidthDp = existing.strokeWidthDp
        } else {
            // Create new with current width
            val created = CostumePen(chosen, currentStrokeWidthDp)
            costumePens = listOf(created) + costumePens
            currentColor = created.color
            currentStrokeWidthDp = created.strokeWidthDp
        }
    }

    fun deletePresetAt(index: Int) {
        if (costumePens.size <= 1 || index !in costumePens.indices) return
        val newList = costumePens.toMutableList().apply { removeAt(index) }
        costumePens = newList
        val newSel = index.coerceAtMost(newList.lastIndex).coerceAtLeast(0)
        if (newList.isNotEmpty()) selectPenAt(newSel)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StylusDrawingCanvas(
            modifier = Modifier.fillMaxSize(),
            drawingTool = drawingTool,
            paths = paths,
            currentPath = currentPath,
            currentColor = currentColor,
            currentStrokeWidthDp = currentStrokeWidthDp,
            eraserSizeDp = eraserSizeDp,
            onCurrentPathChange = { newPath: List<Offset> ->
                // close size adjustments when writing starts
                if (drawingTool == DrawingTool.WRITE && newPath.isNotEmpty()) {
                    showPenSize = false
                    showEraserSize = false
                }
                currentPath = newPath
            },
            onPathAdded = { path: List<Offset> ->
                paths = paths + listOf(StrokePath(path, currentColor, currentStrokeWidthDp))
                undonePaths = emptyList()
                currentPath = emptyList()
            },
            onErasePath = { index: Int ->
                if (index in paths.indices) {
                    undonePaths = undonePaths + listOf(paths[index])
                    paths = paths.toMutableList().apply { removeAt(index) }
                }
            },
            onStylusButtonChange = { pressed: Boolean ->
                spenPressed = pressed
            }
        )

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
            onToolChange = { tool: DrawingTool ->
                if (tool == DrawingTool.ERASE) eraseToggled = !eraseToggled else eraseToggled = false
            },
            onUndo = {
                if (paths.isNotEmpty()) {
                    undonePaths = undonePaths + listOf(paths.last())
                    paths = paths.dropLast(1)
                }
            },
            onRedo = {
                if (undonePaths.isNotEmpty()) {
                    paths = paths + listOf(undonePaths.last())
                    undonePaths = undonePaths.dropLast(1)
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
            onPenWidthChange = { width: Float -> currentStrokeWidthDp = width.coerceIn(1f, 24f) },
            onEraserSizeChange = { size: Float -> eraserSizeDp = size.coerceIn(8f, 64f) },
            showPenSize = showPenSize,
            showEraserSize = showEraserSize,
            setShowPenSize = { showPenSize = it },
            setShowEraserSize = { showEraserSize = it }
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
            onConfirm = { color: Color ->
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

@Composable
fun HoverBar(
    modifier: Modifier = Modifier,
    drawingTool: DrawingTool,
    currentColor: Color,
    currentStrokeWidthDp: Float,
    eraserSizeDp: Float,
    costumePens: List<CostumePen>,
    selectedIndex: Int,
    onToolChange: (DrawingTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    onAddCostume: () -> Unit,
    onLongPressIndex: (Int) -> Unit,
    onPenWidthChange: (Float) -> Unit,
    onEraserSizeChange: (Float) -> Unit,
    showPenSize: Boolean,
    showEraserSize: Boolean,
    setShowPenSize: (Boolean) -> Unit,
    setShowEraserSize: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .background(color = Color(0xCCFFFFFF), shape = MaterialTheme.shapes.medium)
            // Consume touches in the bar area so they don't reach the canvas behind
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume */ }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pen (Write)
            val onPenClick = {
                if (drawingTool != DrawingTool.WRITE) onToolChange(DrawingTool.WRITE)
                val newShow = !showPenSize
                setShowPenSize(newShow)
                if (newShow) setShowEraserSize(false)
            }
            if (drawingTool == DrawingTool.WRITE) {
                FilledTonalIconButton(onClick = onPenClick) { Icon(AppIcons.Pen, contentDescription = "Write") }
            } else {
                IconButton(onClick = onPenClick) { Icon(AppIcons.Pen, contentDescription = "Write") }
            }

            // Eraser
            val onEraserClick = {
                if (drawingTool != DrawingTool.ERASE) onToolChange(DrawingTool.ERASE) else onToolChange(DrawingTool.ERASE)
                val newShow = !showEraserSize
                setShowEraserSize(newShow)
                if (newShow) setShowPenSize(false)
            }
            if (drawingTool == DrawingTool.ERASE) {
                FilledTonalIconButton(onClick = onEraserClick) { Icon(AppIcons.Eraser, contentDescription = "Erase") }
            } else {
                IconButton(onClick = onEraserClick) { Icon(AppIcons.Eraser, contentDescription = "Erase") }
            }

            IconButton(onClick = onUndo) { Icon(Icons.Filled.Undo, contentDescription = "Undo") }
            IconButton(onClick = onRedo) { Icon(Icons.Filled.Redo, contentDescription = "Redo") }

            // Divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(Color(0x33000000))
            )

            // Swatches with click and long-press (edit)
            ColorPaletteRow(
                modifier = Modifier.weight(1f, fill = false),
                pens = costumePens,
                selectedIndex = selectedIndex,
                onSelectIndex = onSelectIndex,
                onLongPressIndex = onLongPressIndex
            )

            // "+" opens the picker to add a new preset
            IconButton(onClick = onAddCostume) { Icon(Icons.Filled.Add, contentDescription = "Add preset") }
        }

        if (showPenSize) {
            Spacer(modifier = Modifier.height(8.dp))
            SizeAdjustRow(
                label = "Pen width",
                value = currentStrokeWidthDp,
                min = 1f,
                max = 24f,
                onChange = onPenWidthChange
            )
        }
        if (showEraserSize) {
            Spacer(modifier = Modifier.height(8.dp))
            SizeAdjustRow(
                label = "Eraser size",
                value = eraserSizeDp,
                min = 8f,
                max = 64f,
                onChange = onEraserSizeChange
            )
        }
    }
}

@Composable
private fun SizeAdjustRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ${value.toInt()} dp")
        IconButton(onClick = { onChange((value - 1f).coerceIn(min, max)) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
        }
        IconButton(onClick = { onChange((value + 1f).coerceIn(min, max)) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorPaletteRow(
    modifier: Modifier = Modifier,
    pens: List<CostumePen>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onLongPressIndex: (Int) -> Unit
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(pens, key = { _, pen -> "${pen.color.value}-${pen.strokeWidthDp}" }) { index, pen ->
            val selected = index == selectedIndex
            val innerRadius = (pen.strokeWidthDp.coerceIn(1f, 24f) / 24f) * 10f // up to ~10dp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .then(if (selected) Modifier.border(2.dp, Color.Black, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(pen.color)
                    .combinedClickable(
                        onClick = { onSelectIndex(index) },
                        onLongClick = { onLongPressIndex(index) }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(innerRadius.dp.coerceAtLeast(4.dp))
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }
    }
}