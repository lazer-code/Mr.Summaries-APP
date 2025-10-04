@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.mrsummaries_app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.AppIcons
import com.example.mrsummaries_app.CostumePen
import com.example.mrsummaries_app.DrawingTool

private val BrandTeal = Color(0xFF003153)

/**
 * Editor toolbar above the canvas. Background:
 * - Light mode: #003153 (brand)
 * - Dark mode: translucent white
 * Foreground (icons/text) auto-contrasts with the background.
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
    onPenWidthChange: (Float) -> Unit,
    onEraserSizeChange: (Float) -> Unit,
    showPenSize: Boolean,
    showEraserSize: Boolean,
    setShowPenSize: (Boolean) -> Unit,
    setShowEraserSize: (Boolean) -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundColor = if (!isDark) BrandTeal else Color(0xCCFFFFFF)
    val tint = contrastOn(backgroundColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (drawingTool == DrawingTool.WRITE) {
            FilledTonalIconButton(onClick = { onToolChange(DrawingTool.WRITE) }) {
                Icon(AppIcons.Pen, contentDescription = "Write", tint = tint)
            }
        } else {
            IconButton(onClick = { onToolChange(DrawingTool.WRITE) }) {
                Icon(AppIcons.Pen, contentDescription = "Write", tint = tint)
            }
        }

        if (drawingTool == DrawingTool.ERASE) {
            FilledTonalIconButton(onClick = { onToolChange(DrawingTool.ERASE) }) {
                Icon(AppIcons.Eraser, contentDescription = "Erase", tint = tint)
            }
        } else {
            IconButton(onClick = { onToolChange(DrawingTool.ERASE) }) {
                Icon(AppIcons.Eraser, contentDescription = "Erase", tint = tint)
            }
        }

        IconButton(onClick = onUndo) { Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = tint) }
        IconButton(onClick = onRedo) { Icon(Icons.Filled.Redo, contentDescription = "Redo", tint = tint) }

        // Divider
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(tint.copy(alpha = 0.3f))
        )

        // Swatches between Redo and +, horizontally scrollable, fills available space
        ColorPaletteRow(
            modifier = Modifier.weight(1f, fill = true),
            pens = costumePens,
            selectedIndex = selectedIndex,
            onSelectIndex = onSelectIndex,
            onLongPressIndex = onLongPressIndex,
            chipBorderColor = tint
        )

        // "+" pinned right — always visible
        IconButton(onClick = onAddCostume) {
            Icon(Icons.Filled.Add, contentDescription = "Add preset", tint = tint)
        }

        if (showPenSize) {
            StepperRow("Pen", currentStrokeWidthDp, 1f, 24f, onPenWidthChange, tint)
        }
        if (showEraserSize) {
            StepperRow("Eraser", eraserSizeDp, 8f, 64f, onEraserSizeChange, tint)
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

@Composable
private fun ColorPaletteRow(
    modifier: Modifier,
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
        itemsIndexed(pens, key = { _, pen -> "${pen.color.value}-${pen.strokeWidthDp}" }) { index, pen ->
            val selected = index == selectedIndex
            val innerRadius = (pen.strokeWidthDp.coerceIn(1f, 24f) / 24f) * 10f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .then(if (selected) Modifier.border(2.dp, chipBorderColor, CircleShape) else Modifier)
                    .background(pen.color, shape = CircleShape)
                    .combinedClickable(
                        onClick = { onSelectIndex(index) },
                        onLongClick = { onLongPressIndex(index) }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(innerRadius.dp.coerceAtLeast(4.dp))
                        .background(contrastOn(pen.color).copy(alpha = 0.9f), shape = CircleShape)
                )
            }
        }
    }
}