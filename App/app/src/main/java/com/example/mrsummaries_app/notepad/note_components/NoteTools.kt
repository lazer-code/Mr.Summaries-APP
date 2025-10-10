package com.example.mrsummaries_app.notepad.note_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.utils.SafeHoverHandler

@Composable
fun NoteTools(
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onEraserSelected: (Boolean) -> Unit,
    onClearCanvas: () -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokeWidthPicker by remember { mutableStateOf(false) }
    var isEraserSelected by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.width(50.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            // Color picker button
            SafeHoverHandler(
                onHoverEnter = { /* Highlight button */ },
                onHoverExit = { /* Remove highlight */ },
                onHoverMove = { /* Handle hover feedback */ }
            ) {
                IconButton(onClick = { showColorPicker = !showColorPicker }) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Color",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Stroke width button
            SafeHoverHandler(
                onHoverEnter = { /* Highlight button */ },
                onHoverExit = { /* Remove highlight */ }
            ) {
                IconButton(onClick = { showStrokeWidthPicker = !showStrokeWidthPicker }) {
                    Icon(
                        imageVector = Icons.Default.LineWeight,
                        contentDescription = "Stroke Width",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Eraser button
            SafeHoverHandler(
                onHoverEnter = { /* Highlight button */ },
                onHoverExit = { /* Remove highlight */ }
            ) {
                IconButton(
                    onClick = {
                        isEraserSelected = !isEraserSelected
                        onEraserSelected(isEraserSelected)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Eraser",
                        tint = if (isEraserSelected) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Clear canvas button
            SafeHoverHandler(
                onHoverEnter = { /* Highlight button */ },
                onHoverExit = { /* Remove highlight */ }
            ) {
                IconButton(onClick = onClearCanvas) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Canvas",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            SafeHoverHandler(
                onHoverEnter = { /* Highlight button */ },
                onHoverExit = { /* Remove highlight */ }
            ) {
                IconButton(onClick = onSaveNote) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Color picker popup
    if (showColorPicker) {
        ColorPickerPopup(
            onColorSelected = {
                onColorSelected(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Stroke width picker popup
    if (showStrokeWidthPicker) {
        StrokeWidthPickerPopup(
            onStrokeWidthSelected = {
                onStrokeWidthChanged(it)
                showStrokeWidthPicker = false
            },
            onDismiss = { showStrokeWidthPicker = false }
        )
    }
}

@Composable
fun ColorPickerPopup(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green,
        Color.Yellow, Color.Magenta, Color.Cyan
    )

    Card(
        modifier = Modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Color", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                colors.forEach { color ->
                    SafeHoverHandler(
                        onHoverEnter = { /* Highlight color */ },
                        onHoverExit = { /* Remove highlight */ }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable {
                                    onColorSelected(color)
                                }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun StrokeWidthPickerPopup(
    onStrokeWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val strokeWidths = listOf(2f, 5f, 10f, 15f, 20f)

    Card(
        modifier = Modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Stroke Width", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                strokeWidths.forEach { width ->
                    SafeHoverHandler(
                        onHoverEnter = { /* Highlight option */ },
                        onHoverExit = { /* Remove highlight */ }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStrokeWidthSelected(width) }
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(width.dp)
                                    .background(Color.Black)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${width.toInt()} dp")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel")
            }
        }
    }
}