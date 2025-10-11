package com.example.mrsummaries_app.notepad.note_components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class PathProperties(
    var path: Path = Path(),
    var color: Color = Color.Black,
    var strokeWidth: Float = 5f,
    var drawingMode: DrawingMode = DrawingMode.PEN,
    var points: MutableList<Offset> = mutableListOf(),
    val id: Long = System.currentTimeMillis(),
    val isHighlighter: Boolean = false
)

enum class DrawingMode {
    PEN,
    HIGHLIGHTER,
    ERASER,
}