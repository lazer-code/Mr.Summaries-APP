@file:OptIn(ExperimentalFoundationApi::class)

package com.example.mrsummaries_app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.note.AppIcons
import com.example.mrsummaries_app.note.CostumePen
import com.example.mrsummaries_app.note.DrawingTool

// Calculate contrasting color (black or white) based on background color
@Composable
fun contrastOn(backgroundColor: Color): Color {
    // Calculate relative luminance using RGB components
    val red = backgroundColor.red
    val green = backgroundColor.green
    val blue = backgroundColor.blue

    // Using simplified luminance formula (perceived brightness)
    val brightness = (0.299 * red + 0.587 * green + 0.114 * blue)

    // Return black for light backgrounds, white for dark backgrounds
    return if (brightness > 0.5f) Color.Black else Color.White
}

private val BrandTeal = Color(0xFF003153)

/**
 * Editor toolbar above the canvas.
 */
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
    onPenWidthChange: (Float) -> Unit = {},
    onEraserSizeChange: (Float) -> Unit = {},
    showPenSize: Boolean = false,
    showEraserSize: Boolean = false,
    setShowPenSize: (Boolean) -> Unit = {},
    setShowEraserSize: (Boolean) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (!isDark) BrandTeal else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val tint = contrastOn(bg)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp) // was smaller; ensure enough room for 40dp circular buttons
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tool toggle: Pen
        ToolCircle(
            selected = drawingTool == DrawingTool.WRITE,
            tint = tint,
            content = { Icon(AppIcons.Pen, contentDescription = "Pen", tint = tint) },
            onClick = { onToolChange(DrawingTool.WRITE) }
        )
        // Tool toggle: Eraser
        ToolCircle(
            selected = drawingTool == DrawingTool.ERASE,
            tint = tint,
            content = { Icon(AppIcons.Eraser, contentDescription = "Eraser", tint = tint) },
            onClick = { onToolChange(DrawingTool.ERASE) }
        )

        // Divider
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 28.dp)
                .background(tint.copy(alpha = 0.3f))
        )

        // Undo / Redo
        IconButton(onClick = onUndo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = tint)
        }
        IconButton(onClick = onRedo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Redo, contentDescription = "Redo", tint = tint)
        }

        // Color swatches — horizontally scrollable, fills remaining space
        ColorPaletteRow(
            modifier = Modifier.weight(1f, fill = true),
            pens = costumePens,
            selectedIndex = selectedIndex,
            onSelectIndex = onSelectIndex,
            onLongPressIndex = onLongPressIndex,
            chipBorderColor = tint
        )

        // Add preset
        IconButton(onClick = onAddCostume, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Add preset", tint = tint)
        }

        // Optional steppers
        if (showPenSize) {
            StepperRow("Pen", currentStrokeWidthDp, 1f, 24f, onPenWidthChange, tint)
        }
        if (showEraserSize) {
            StepperRow("Eraser", eraserSizeDp, 8f, 64f, onEraserSizeChange, tint)
        }
    }
}

@Composable
private fun ToolCircle(
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val bg = if (selected) tint.copy(alpha = 0.15f) else Color.Transparent
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) tint else tint.copy(alpha = 0.4f)

    Surface(
        shape = CircleShape,
        color = bg,
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 0.dp else 0.dp,
        modifier = Modifier
            .size(40.dp)
            .border(borderWidth, borderColor, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(
    modifier: Modifier = Modifier,
    pens: List<CostumePen>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onLongPressIndex: (Int) -> Unit,
    chipBorderColor: Color
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(pens, key = { i, p -> p.hashCode() + i }) { index, pen ->
            val selected = index == selectedIndex
            val borderWidth = if (selected) 2.dp else 1.dp
            val borderColor = if (selected) contrastOn(pen.color) else chipBorderColor.copy(alpha = 0.4f)

            Surface(
                shape = CircleShape,
                color = pen.color,
                modifier = Modifier
                    .size(28.dp)
                    .border(borderWidth, borderColor, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .combinedClickable(
                            onClick = { onSelectIndex(index) },
                            onLongClick = { onLongPressIndex(index) }
                        )
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ${value.toInt()} dp", color = tint)
        IconButton(onClick = { onChange((value - 1f).coerceIn(min, max)) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease $label", tint = tint)
        }
        IconButton(onClick = { onChange((value + 1f).coerceIn(min, max)) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label", tint = tint)
        }
    }
}