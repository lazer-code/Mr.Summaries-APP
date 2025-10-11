package com.example.mrsummaries_app.notepad.note_components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 5f,
    drawingMode: DrawingMode = DrawingMode.PEN,
    stylusOnly: Boolean = true,
    onModeChanged: (DrawingMode) -> Unit = {},
    onPathDrawn: (List<PathProperties>) -> Unit = {},
    undoState: UndoRedoState,
    pathProperties: PathProperties? = null,
    onFingerTouchInStylusMode: () -> Unit = {},
    onDrawingStateChanged: (Boolean) -> Unit = {}
) {
    // Track all completed paths
    val paths = remember { mutableStateListOf<PathProperties>() }

    // Track paths that have been undone (for redo functionality)
    val undonePathsState = remember { mutableStateListOf<PathProperties>() }

    // Current path being drawn
    val currentPath = remember { mutableStateOf<PathProperties?>(null) }

    // Remember if stylus button was pressed
    val stylusButtonPressed = remember { mutableStateOf(false) }

    // For stroke eraser - track if we're currently erasing
    val isErasing = remember { mutableStateOf(false) }

    // For highlighter - track start point
    val highlighterStartPoint = remember { mutableStateOf<Offset?>(null) }

    // Force recomposition when drawing
    val drawCount = remember { mutableStateOf(0) }

    // Handle undo/redo actions
    LaunchedEffect(undoState.undoCount, undoState.redoCount) {
        when {
            undoState.undoCount > 0 -> {
                if (paths.isNotEmpty()) {
                    val lastIndex = paths.lastIndex
                    val lastPath = paths[lastIndex]
                    paths.removeAt(lastIndex)
                    undonePathsState.add(lastPath)
                    undoState.onUndoPerformed()
                }
            }
            undoState.redoCount > 0 -> {
                if (undonePathsState.isNotEmpty()) {
                    val lastIndex = undonePathsState.lastIndex
                    val pathToRedo = undonePathsState[lastIndex]
                    undonePathsState.removeAt(lastIndex)
                    paths.add(pathToRedo)
                    undoState.onRedoPerformed()
                }
            }
        }
    }

    // Notify when paths change
    LaunchedEffect(paths.size) {
        onPathDrawn(paths.toList())
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    val isStylusOrAllowed = if (stylusOnly) {
                        val toolType = event.getToolType(0)
                        toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                                toolType == MotionEvent.TOOL_TYPE_ERASER
                    } else {
                        true
                    }

                    if (!isStylusOrAllowed) {
                        onFingerTouchInStylusMode()
                        return@pointerInteropFilter false
                    }

                    // Detect stylus button press/release
                    val isStylusButtonCurrentlyPressed =
                        event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0

                    val currentPoint = Offset(event.x, event.y)
                    val adjustedWidth = strokeWidth

                    // Handle button press/release events
                    if (isStylusButtonCurrentlyPressed != stylusButtonPressed.value) {
                        if (isStylusButtonCurrentlyPressed) {
                            // Button just pressed
                            currentPath.value?.let { path ->
                                if (path.points.size > 1 && path.color != Color.LightGray.copy(alpha = 0.5f)) {
                                    paths.add(path)
                                }
                            }

                            // Switch to eraser mode
                            stylusButtonPressed.value = true
                            onModeChanged(DrawingMode.ERASER)
                            isErasing.value = true

                            // Create eraser indicator
                            val eraserPath = Path().apply {
                                addOval(Rect(
                                    left = event.x - adjustedWidth/2,
                                    top = event.y - adjustedWidth/2,
                                    right = event.x + adjustedWidth/2,
                                    bottom = event.y + adjustedWidth/2
                                ))
                            }

                            currentPath.value = PathProperties(
                                path = eraserPath,
                                color = Color.LightGray.copy(alpha = 0.5f),
                                strokeWidth = 1f
                            )
                        } else {
                            // Button just released
                            stylusButtonPressed.value = false

                            // Reset erasing state and clear eraser indicator
                            isErasing.value = false
                            currentPath.value = null

                            // Switch back to pen mode
                            onModeChanged(DrawingMode.PEN)

                            // If still touching, start a new pen stroke
                            if (event.action != MotionEvent.ACTION_UP) {
                                val newPath = PathProperties(
                                    path = Path().apply { moveTo(event.x, event.y) },
                                    color = strokeColor,
                                    strokeWidth = adjustedWidth,
                                    points = mutableListOf(currentPoint)
                                )
                                currentPath.value = newPath
                            }
                        }

                        // Force recomposition
                        drawCount.value++
                    }

                    // Determine current mode
                    val currentMode = if (stylusButtonPressed.value) DrawingMode.ERASER else drawingMode
                    val currentColor = if (currentMode == DrawingMode.ERASER) backgroundColor else strokeColor

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            onDrawingStateChanged(true)
                            // Clear redo history on new drawing
                            undonePathsState.clear()

                            // Don't create a new path if button press/release just handled it
                            if (currentPath.value == null) {
                                if (currentMode == DrawingMode.ERASER) {
                                    // Start erasing mode
                                    isErasing.value = true

                                    // Find paths to remove (stroke eraser)
                                    val pathsToRemove = mutableListOf<PathProperties>()

                                    for (path in paths) {
                                        if (isPathNearPoint(path, currentPoint, adjustedWidth)) {
                                            pathsToRemove.add(path)
                                        }
                                    }

                                    if (pathsToRemove.isNotEmpty()) {
                                        paths.removeAll(pathsToRemove)
                                    }

                                    // Create visual indicator for eraser
                                    val eraserPath = Path().apply {
                                        addOval(Rect(
                                            left = event.x - adjustedWidth/2,
                                            top = event.y - adjustedWidth/2,
                                            right = event.x + adjustedWidth/2,
                                            bottom = event.y + adjustedWidth/2
                                        ))
                                    }

                                    currentPath.value = PathProperties(
                                        path = eraserPath,
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        strokeWidth = 1f
                                    )
                                }
                                else if (currentMode == DrawingMode.HIGHLIGHTER) {
                                    // For highlighter, just store start point
                                    highlighterStartPoint.value = currentPoint

                                    // Create temporary path for visual feedback
                                    val newPath = PathProperties(
                                        path = Path().apply { moveTo(event.x, event.y) },
                                        color = currentColor.copy(alpha = 0.3f),
                                        strokeWidth = adjustedWidth,
                                        points = mutableListOf(currentPoint),
                                        isHighlighter = true
                                    )
                                    currentPath.value = newPath
                                }
                                else {
                                    // Normal pen drawing
                                    val newPath = PathProperties(
                                        path = Path().apply { moveTo(event.x, event.y) },
                                        color = currentColor,
                                        strokeWidth = adjustedWidth,
                                        points = mutableListOf(currentPoint)
                                    )
                                    currentPath.value = newPath
                                }
                            }
                            drawCount.value++
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (currentMode == DrawingMode.ERASER && isErasing.value) {
                                // Continue erasing - check for paths to remove
                                val pathsToRemove = mutableListOf<PathProperties>()

                                for (path in paths) {
                                    if (isPathNearPoint(path, currentPoint, adjustedWidth)) {
                                        pathsToRemove.add(path)
                                    }
                                }

                                if (pathsToRemove.isNotEmpty()) {
                                    paths.removeAll(pathsToRemove)
                                }

                                // Update visual eraser indicator
                                val newPath = Path().apply {
                                    addOval(Rect(
                                        left = event.x - adjustedWidth/2,
                                        top = event.y - adjustedWidth/2,
                                        right = event.x + adjustedWidth/2,
                                        bottom = event.y + adjustedWidth/2
                                    ))
                                }

                                currentPath.value = PathProperties(
                                    path = newPath,
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    strokeWidth = 1f
                                )
                            }
                            else if (currentMode == DrawingMode.HIGHLIGHTER) {
                                // For highlighter, update visual with straight line
                                highlighterStartPoint.value?.let { startPoint ->
                                    val highlighterPath = Path().apply {
                                        moveTo(startPoint.x, startPoint.y)
                                        lineTo(currentPoint.x, currentPoint.y)
                                    }

                                    currentPath.value = PathProperties(
                                        path = highlighterPath,
                                        color = currentColor.copy(alpha = 0.3f),
                                        strokeWidth = adjustedWidth,
                                        points = mutableListOf(startPoint, currentPoint),
                                        isHighlighter = true
                                    )
                                }
                            }
                            else {
                                // Continue regular drawing
                                currentPath.value?.let { path ->
                                    path.path.lineTo(event.x, event.y)
                                    path.points.add(currentPoint)
                                    path.strokeWidth = adjustedWidth
                                }
                            }
                            drawCount.value++
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            onDrawingStateChanged(false)
                            if (currentMode == DrawingMode.ERASER) {
                                isErasing.value = false
                                currentPath.value = null
                            }
                            else if (currentMode == DrawingMode.HIGHLIGHTER) {
                                // Complete highlighter stroke
                                highlighterStartPoint.value?.let { startPoint ->
                                    val finalHighlighterPath = Path().apply {
                                        moveTo(startPoint.x, startPoint.y)
                                        lineTo(currentPoint.x, currentPoint.y)
                                    }

                                    val highlighterStroke = PathProperties(
                                        path = finalHighlighterPath,
                                        color = currentColor.copy(alpha = 0.3f),
                                        strokeWidth = adjustedWidth,
                                        points = mutableListOf(startPoint, currentPoint),
                                        isHighlighter = true
                                    )

                                    paths.add(highlighterStroke)
                                    highlighterStartPoint.value = null
                                }
                                currentPath.value = null
                            }
                            else {
                                // Finish regular drawing path
                                currentPath.value?.let { path ->
                                    if (path.points.size > 1) {
                                        // Ensure we don't add an eraser indicator path to regular paths
                                        if (path.color != Color.LightGray.copy(alpha = 0.5f)) {
                                            paths.add(path)
                                        }
                                    }
                                    currentPath.value = null
                                }
                            }

                            // If stylus button was pressed, reset to PEN mode on touch release
                            if (stylusButtonPressed.value) {
                                stylusButtonPressed.value = false
                                onModeChanged(DrawingMode.PEN)
                            }

                            drawCount.value++
                            true
                        }

                        else -> false
                    }
                }
        ) {
            // Draw all completed paths
            paths.forEach { path ->
                drawPath(
                    path = path.path,
                    color = path.color,
                    style = Stroke(
                        width = path.strokeWidth,
                        cap = StrokeCap.Round,
                        join = if (path.isHighlighter) StrokeJoin.Bevel else StrokeJoin.Round
                    )
                )
            }

            // Draw current path
            currentPath.value?.let { path ->
                drawPath(
                    path = path.path,
                    color = path.color,
                    style = Stroke(
                        width = path.strokeWidth,
                        cap = StrokeCap.Round,
                        join = if (path.isHighlighter) StrokeJoin.Bevel else StrokeJoin.Round
                    )
                )
            }

            // Force recomposition
            drawCount.value
        }
    }
}

// Helper function to check if a path is near a point (for stroke eraser)
private fun isPathNearPoint(path: PathProperties, point: Offset, threshold: Float): Boolean {
    // For highlighters (thicker strokes), we need to adjust the threshold
    val effectiveThreshold = if (path.isHighlighter) {
        // Make it easier to select highlighters for erasing
        threshold * 1.2f
    } else {
        threshold
    }

    // Check if any point in the path is within the threshold distance
    for (pathPoint in path.points) {
        val distance = sqrt(
            (pathPoint.x - point.x).pow(2) +
                    (pathPoint.y - point.y).pow(2)
        )

        if (distance < effectiveThreshold + path.strokeWidth / 2) {
            return true
        }
    }

    return false
}