@file:OptIn(ExperimentalComposeUiApi::class)

package com.example.mrsummaries_app

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

enum class DrawingTool { WRITE, ERASE }

/**
 * StylusDrawingCanvas detects the SPen primary button using MotionEvent.buttonState
 * (via pointerInteropFilter). It calls onStylusButtonChange(true/false) when the
 * stylus primary button is pressed/released (or when move events show the bit).
 *
 * The composable itself is agnostic about whether erase is temporary (while the
 * stylus button is held) or persistent (toggled by UI). The host (MainActivity)
 * decides that by combining the stylus button state and a UI toggle.
 */
@Composable
fun StylusDrawingCanvas(
    modifier: Modifier = Modifier,
    drawingTool: DrawingTool,
    paths: List<List<Offset>>,
    currentPath: List<Offset>,
    onCurrentPathChange: (List<Offset>) -> Unit,
    onPathAdded: (List<Offset>) -> Unit,
    onErasePath: (Int) -> Unit,
    onStylusButtonChange: (Boolean) -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .background(Color.White)
            // listen to MotionEvent button changes to detect the SPen primary button
            .pointerInteropFilter { ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_BUTTON_PRESS -> {
                        onStylusButtonChange(true)
                        false
                    }
                    MotionEvent.ACTION_BUTTON_RELEASE -> {
                        onStylusButtonChange(false)
                        false
                    }
                    else -> {
                        // keep state in sync during moves using the buttonState bitmask
                        val isPressed = (ev.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                        onStylusButtonChange(isPressed)
                        false
                    }
                }
            }
            .pointerInput(drawingTool, paths) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val stylusDown = event.changes.firstOrNull { it.type == PointerType.Stylus && it.pressed }
                        if (stylusDown != null) {
                            var tempPath = listOf(stylusDown.position)
                            onCurrentPathChange(tempPath)

                            while (true) {
                                val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                val dragPointer = dragEvent.changes.find { it.id == stylusDown.id }
                                if (dragPointer == null || !dragPointer.pressed) break

                                tempPath = tempPath + dragPointer.position
                                onCurrentPathChange(tempPath)

                                if (drawingTool == DrawingTool.ERASE) {
                                    val eraseIndex = paths.indexOfFirst { path ->
                                        path.any { p -> (p - dragPointer.position).getDistance() < 40f }
                                    }
                                    if (eraseIndex != -1) {
                                        onErasePath(eraseIndex)
                                        break
                                    }
                                }
                            }

                            if (drawingTool == DrawingTool.WRITE && tempPath.isNotEmpty()) {
                                onPathAdded(tempPath)
                            }
                            onCurrentPathChange(emptyList())
                        }
                    }
                }
            }
    ) {
        // draw stored paths
        for (pathPoints in paths) {
            if (pathPoints.size > 1) {
                val path = Path().apply {
                    moveTo(pathPoints[0].x, pathPoints[0].y)
                    for (point in pathPoints.drop(1)) lineTo(point.x, point.y)
                }
                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else if (pathPoints.size == 1) {
                val p = pathPoints[0]
                drawCircle(Color.Blue, radius = 3.dp.toPx(), center = p)
            }
        }

        // draw current in-progress path
        if (currentPath.size > 1) {
            val path = Path().apply {
                moveTo(currentPath[0].x, currentPath[0].y)
                for (point in currentPath.drop(1)) lineTo(point.x, point.y)
            }
            drawPath(
                path = path,
                color = if (drawingTool == DrawingTool.WRITE) Color.Blue else Color.Red,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else if (currentPath.size == 1) {
            val p = currentPath[0]
            drawCircle(
                color = if (drawingTool == DrawingTool.WRITE) Color.Blue else Color.Red,
                radius = 3.dp.toPx(),
                center = p
            )
        }
    }
}