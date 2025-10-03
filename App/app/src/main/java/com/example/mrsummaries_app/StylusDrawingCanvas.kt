package com.example.mrsummaries_app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

enum class DrawingTool { WRITE, ERASE }

/**
 * StylusDrawingCanvas now exposes an onCurrentPathChange lambda that is called
 * continuously as the stylus moves. This allows the host (e.g. MainActivity)
 * to update and draw the in-progress stroke immediately instead of waiting
 * for the stylus to be lifted.
 */
@Composable
fun StylusDrawingCanvas(
    modifier: Modifier = Modifier,
    drawingTool: DrawingTool,
    paths: List<List<Offset>>,
    currentPath: List<Offset>,
    onCurrentPathChange: (List<Offset>) -> Unit, // new callback for live updates
    onPathAdded: (List<Offset>) -> Unit,
    onErasePath: (Int) -> Unit
) {
    Canvas(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .pointerInput(drawingTool, paths) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val stylusDown = event.changes.firstOrNull { it.type == PointerType.Stylus && it.pressed }
                        if (stylusDown != null) {
                            var tempPath = listOf(stylusDown.position)
                            // Immediately report the initial position so an initial dot/segment appears
                            onCurrentPathChange(tempPath)

                            while (true) {
                                val dragEvent = awaitPointerEvent(PointerEventPass.Initial)
                                val dragPointer = dragEvent.changes.find { it.id == stylusDown.id }
                                if (dragPointer == null || !dragPointer.pressed) {
                                    // pointer released -> exit drag loop
                                    break
                                }
                                tempPath = tempPath + dragPointer.position
                                // report updates continuously so the UI can draw the in-progress stroke
                                onCurrentPathChange(tempPath)

                                // If erasing, check if the current point is near an existing path
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

                            // pointer lifted (or erase broke out)
                            if (drawingTool == DrawingTool.WRITE && tempPath.size > 0) {
                                // Add the final path (if any). Use >0 so single taps can be added too.
                                onPathAdded(tempPath)
                            }
                            // Clear the live path
                            onCurrentPathChange(emptyList())
                        }
                    }
                }
            }
    ) {
        // Draw all stored paths
        for (pathPoints in paths) {
            if (pathPoints.size > 1) {
                val path = Path().apply {
                    moveTo(pathPoints[0].x, pathPoints[0].y)
                    for (point in pathPoints.drop(1)) {
                        lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (pathPoints.size == 1) {
                // draw a small point for single-tap strokes
                val p = pathPoints[0]
                drawCircle(Color.Blue, radius = 3.dp.toPx(), center = p)
            }
        }

        // Draw the current (in-progress) path
        if (currentPath.size > 1) {
            val path = Path().apply {
                moveTo(currentPath[0].x, currentPath[0].y)
                for (point in currentPath.drop(1)) {
                    lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = path,
                color = if (drawingTool == DrawingTool.WRITE) Color.Blue else Color.Red,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
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