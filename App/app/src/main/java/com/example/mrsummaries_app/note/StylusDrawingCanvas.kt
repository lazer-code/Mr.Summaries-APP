@file:OptIn(ExperimentalComposeUiApi::class)

package com.example.mrsummaries_app.note

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.cancel

enum class DrawingTool { WRITE, ERASE }

/**
 * StylusDrawingCanvas detects SPen button, tracks hover position, and renders tip indicators.
 * Updated: writing is constrained to canvas bounds. Leaving the bounds ends the current stroke.
 * Re-entering starts a new stroke.
 */
@Composable
fun StylusDrawingCanvas(
    modifier: Modifier = Modifier,
    drawingTool: DrawingTool,
    paths: List<StrokePath>,
    currentPath: List<Offset>,
    currentColor: Color,
    currentStrokeWidthDp: Float,
    eraserSizeDp: Float,
    onCurrentPathChange: (List<Offset>) -> Unit,
    onPathAdded: (List<Offset>) -> Unit,
    onErasePath: (Int) -> Unit,
    onStylusButtonChange: (Boolean) -> Unit = {}
) {
    var hoverPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .background(Color.White)
            .pointerInteropFilter { ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER,
                    MotionEvent.ACTION_HOVER_MOVE -> {
                        hoverPosition = Offset(ev.x, ev.y)
                        val isPressed = (ev.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                        onStylusButtonChange(isPressed)
                        false
                    }
                    MotionEvent.ACTION_HOVER_EXIT,
                    MotionEvent.ACTION_CANCEL -> {
                        hoverPosition = null
                        false
                    }
                    MotionEvent.ACTION_BUTTON_PRESS -> {
                        onStylusButtonChange(true); false
                    }
                    MotionEvent.ACTION_BUTTON_RELEASE -> {
                        onStylusButtonChange(false); false
                    }
                    else -> {
                        val isPressed = (ev.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                        onStylusButtonChange(isPressed)
                        false
                    }
                }
            }
            // NOTE: avoid including `paths` here as a key to prevent cancelling pointer coroutine
            // every time drawing data changes (that caused freezes). Depend on stable inputs only.
            .pointerInput(drawingTool, eraserSizeDp) {
                // Keep the pointer-handling loop robust to cancellation:
                try {
                    while (true) {
                        awaitPointerEventScope {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // Find a stylus down pointer (pressed)
                            val stylusDown = event.changes.firstOrNull { it.type == PointerType.Stylus && it.pressed }
                            if (stylusDown != null) {
                                // Helper to determine if a point is inside this canvas
                                fun inBounds(p: Offset): Boolean =
                                    p.x >= 0f && p.y >= 0f && p.x <= size.width.toFloat() && p.y <= size.height.toFloat()

                                // start a new temporary path only if the press is inside bounds
                                var tempPath = if (inBounds(stylusDown.position)) listOf(stylusDown.position) else emptyList()
                                onCurrentPathChange(tempPath)

                                // Track this pointer id for the drag loop
                                val downId = stylusDown.id

                                // Drag loop for this pointer until release or pointer disappears
                                while (true) {
                                    val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val dragPointer = dragEvent.changes.find { it.id == downId }
                                    if (dragPointer == null || !dragPointer.pressed) {
                                        // pointer ended or no longer pressed -> finish
                                        break
                                    }

                                    val pos = dragPointer.position
                                    val inside = inBounds(pos)

                                    if (drawingTool == DrawingTool.ERASE) {
                                        if (inside) {
                                            val radius = eraserSizeDp.dp.toPx()
                                            val eraseIndex = paths.indexOfFirst { stroke ->
                                                stroke.points.any { p -> (p - pos).getDistance() < radius }
                                            }
                                            if (eraseIndex != -1) {
                                                onErasePath(eraseIndex)
                                            }
                                        }
                                        // Keep showing the eraser preview (no path accumulation)
                                        tempPath = if (inside) listOf(pos) else emptyList()
                                        onCurrentPathChange(tempPath)
                                    } else { // WRITE
                                        if (inside) {
                                            tempPath = when {
                                                tempPath.isEmpty() -> listOf(pos)
                                                else -> tempPath + listOf(pos)
                                            }
                                            onCurrentPathChange(tempPath)
                                        } else {
                                            // left the canvas; finish current stroke
                                            break
                                        }
                                    }
                                } // end drag loop

                                // If we have a drawn write path, commit it
                                if (drawingTool == DrawingTool.WRITE && tempPath.isNotEmpty()) {
                                    onPathAdded(tempPath)
                                }
                                // Clear any in-progress path indicator
                                onCurrentPathChange(emptyList())
                            } // end stylusDown handling
                        } // end awaitPointerEventScope
                    } // end main while
                } finally {
                    // Ensure we always clear in-progress drawing state when coroutine is cancelled
                    onCurrentPathChange(emptyList())
                    // Also ensure stylus button state isn't stuck
                    onStylusButtonChange(false)
                }
            }
    ) {
        // Draw persisted strokes
        for (stroke in paths) {
            val pts = stroke.points
            val widthPx = stroke.strokeWidthDp.dp.toPx()
            if (pts.size > 1) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (p in pts.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else if (pts.size == 1) {
                drawCircle(stroke.color, radius = maxOf(3.dp.toPx(), widthPx / 2f), center = pts[0])
            }
        }

        // In-progress rendering and indicators
        val eraserRadius = eraserSizeDp.dp.toPx()
        if (currentPath.isNotEmpty()) {
            val tip = currentPath.last()
            if (drawingTool == DrawingTool.WRITE) {
                if (currentPath.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPath[0].x, currentPath[0].y)
                        for (p in currentPath.drop(1)) lineTo(p.x, p.y)
                    }
                    drawPath(
                        path = path,
                        color = currentColor,
                        style = Stroke(width = currentStrokeWidthDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                drawCircle(color = currentColor, radius = maxOf(3.dp.toPx(), currentStrokeWidthDp.dp.toPx() / 2f), center = tip)
            } else {
                drawCircle(color = Color(0x66000000), radius = eraserRadius, center = tip, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = Color(0x11000000), radius = eraserRadius, center = tip)
            }
        } else {
            hoverPosition?.let { tip ->
                if (drawingTool == DrawingTool.WRITE) {
                    drawCircle(color = currentColor, radius = maxOf(3.dp.toPx(), currentStrokeWidthDp.dp.toPx() / 2f), center = tip)
                } else {
                    drawCircle(color = Color(0x66000000), radius = eraserRadius, center = tip, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = Color(0x11000000), radius = eraserRadius, center = tip)
                }
            }
        }
    }
}