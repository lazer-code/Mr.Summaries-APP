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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.cancel
import kotlin.math.max

enum class DrawingTool { WRITE, ERASE }

/**
 * StylusDrawingCanvas detects SPen button, tracks hover position, and renders tip indicators.
 *
 * Changes in this file:
 * - Adds onCreateNextPage callback which is invoked when a committed write stroke reaches the
 *   bottom of the canvas (so the host can create a new page). The threshold is configurable
 *   via nextPageTriggerDp.
 *
 * Note: positions reported here are within the Canvas coordinate space (0..size.width, 0..size.height).
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
    minPointDistanceDp: Float = 2f,
    batchSize: Int = 3,
    spatialIndex: SpatialIndex? = null,
    nextPageTriggerDp: Float = 64f, // distance from bottom to trigger creating next page
    onCurrentPathChange: (List<Offset>) -> Unit,
    onPathAdded: (List<Offset>) -> Unit,
    onErasePath: (Int) -> Unit,
    onCreateNextPage: () -> Unit = {},
    onStylusButtonChange: (Boolean) -> Unit = {}
) {
    var hoverPosition by remember { mutableStateOf<Offset?>(null) }

    // up-to-date values for long-running pointer loop
    val currentDrawingTool by rememberUpdatedState(drawingTool)
    val currentPaths by rememberUpdatedState(paths)

    val density = LocalDensity.current
    val eraserSizePx = with(density) { eraserSizeDp.dp.toPx() }
    val minPointDistancePx = with(density) { minPointDistanceDp.dp.toPx() }
    val minPointDistanceSq = minPointDistancePx * minPointDistancePx
    val safeBatchSize = max(1, batchSize)
    val nextPageTriggerPx = with(density) { nextPageTriggerDp.dp.toPx() }

    // If a spatial index is provided, ensure its cell size is reasonable for queries
    LaunchedEffect(spatialIndex, eraserSizePx) {
        spatialIndex?.setCellSize(max(eraserSizePx, 32f))
    }

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
            // pointerInput keyed only by eraser size (other state read via rememberUpdatedState)
            .pointerInput(eraserSizeDp) {
                try {
                    while (true) {
                        awaitPointerEventScope {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val stylusDown = event.changes.firstOrNull { it.type == PointerType.Stylus && it.pressed }
                            if (stylusDown != null) {
                                fun inBounds(p: Offset): Boolean =
                                    p.x >= 0f && p.y >= 0f && p.x <= size.width && p.y <= size.height

                                // mutable buffer to avoid allocations per event
                                val buffer = ArrayList<Offset>()
                                var lastPoint: Offset? = null

                                if (inBounds(stylusDown.position)) {
                                    buffer += stylusDown.position
                                    lastPoint = stylusDown.position
                                }
                                if (buffer.isNotEmpty()) onCurrentPathChange(buffer.toList())

                                val downId = stylusDown.id
                                var prevPressed = true

                                while (true) {
                                    val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val dragPointer = dragEvent.changes.find { it.id == downId }

                                    if (dragPointer == null) break

                                    val curPressed = dragPointer.pressed
                                    if (prevPressed && !curPressed) break
                                    prevPressed = curPressed
                                    if (!curPressed) break

                                    val pos = dragPointer.position
                                    val inside = inBounds(pos)

                                    if (currentDrawingTool == DrawingTool.ERASE) {
                                        if (inside) {
                                            val radius = eraserSizePx
                                            var eraseIndex = -1
                                            val candidateStrokeIndices = spatialIndex?.query(pos, radius)
                                                ?: (0 until currentPaths.size).toSet()
                                            for (si in candidateStrokeIndices) {
                                                val stroke = currentPaths.getOrNull(si) ?: continue
                                                val rSq = radius * radius
                                                if (stroke.points.any { p ->
                                                        val dx = p.x - pos.x
                                                        val dy = p.y - pos.y
                                                        dx * dx + dy * dy < rSq
                                                    }) {
                                                    eraseIndex = si
                                                    break
                                                }
                                            }
                                            if (eraseIndex != -1) {
                                                onErasePath(eraseIndex)
                                            }
                                            buffer.clear()
                                            buffer += pos
                                            lastPoint = pos
                                            onCurrentPathChange(buffer.toList())
                                        } else {
                                            buffer.clear()
                                            lastPoint = null
                                            onCurrentPathChange(emptyList())
                                        }
                                    } else { // WRITE
                                        if (inside) {
                                            val shouldAdd = lastPoint == null ||
                                                    ((pos.x - lastPoint.x).let { it * it } + (pos.y - lastPoint.y).let { it * it }) >= minPointDistanceSq
                                            if (shouldAdd) {
                                                buffer += pos
                                                lastPoint = pos
                                                if (buffer.size % safeBatchSize == 0) {
                                                    onCurrentPathChange(buffer.toList())
                                                }
                                            }
                                        } else {
                                            break
                                        }
                                    }
                                } // end drag loop

                                // Commit write path if any
                                if (currentDrawingTool == DrawingTool.WRITE && buffer.isNotEmpty()) {
                                    onPathAdded(buffer.toList())

                                    // If the stroke reached near the bottom of this canvas, request creating a next page
                                    val lastY = buffer.last().y
                                    if (lastY >= size.height - nextPageTriggerPx) {
                                        onCreateNextPage()
                                    }
                                }
                                onCurrentPathChange(emptyList())
                            } // end stylusDown handling
                        } // end awaitPointerEventScope
                    } // end while
                } finally {
                    onCurrentPathChange(emptyList())
                    // avoid forcing stylus button state false here
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
                drawCircle(stroke.color, radius = max(3.dp.toPx(), widthPx / 2f), center = pts[0])
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
                drawCircle(color = currentColor, radius = max(3.dp.toPx(), currentStrokeWidthDp.dp.toPx() / 2f), center = tip)
            } else {
                drawCircle(color = Color(0x66000000), radius = eraserRadius, center = tip, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = Color(0x11000000), radius = eraserRadius, center = tip)
            }
        } else {
            hoverPosition?.let { tip ->
                if (drawingTool == DrawingTool.WRITE) {
                    drawCircle(color = currentColor, radius = max(3.dp.toPx(), currentStrokeWidthDp.dp.toPx() / 2f), center = tip)
                } else {
                    drawCircle(color = Color(0x66000000), radius = eraserRadius, center = tip, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = Color(0x11000000), radius = eraserRadius, center = tip)
                }
            }
        }
    }
}