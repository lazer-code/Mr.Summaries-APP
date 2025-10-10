package com.example.mrsummaries_app.notepad.note_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NoteEditor(
    modifier: Modifier = Modifier,
    isDrawingMode: Boolean,
    noteText: String,
    onNoteTextChanged: (String) -> Unit,
    paths: List<PathProperties> = emptyList(),
    currentPath: PathProperties? = null,
    currentColor: Color = Color.Black,
    currentStrokeWidth: Float = 5f,
    isEraser: Boolean = false,
    showSPenMessage: Boolean = false,
    undoState: UndoRedoState // This parameter was missing a default value
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (isDrawingMode) {
            // Drawing canvas
            DrawingCanvas(
                strokeColor = if (isEraser) Color.White else currentColor,
                strokeWidth = currentStrokeWidth,
                drawingMode = if (isEraser) DrawingMode.ERASER else DrawingMode.PEN,
                onPathDrawn = { /* Handle path drawn if needed */ },
                undoState = undoState // Pass the undoState to DrawingCanvas
            )

            // Show message when finger touch is detected
            if (showSPenMessage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Please use a stylus for drawing",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Text editing mode
            TextField(
                value = noteText,
                onValueChange = onNoteTextChanged,
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text("Start typing...") },
                textStyle = TextStyle(fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}