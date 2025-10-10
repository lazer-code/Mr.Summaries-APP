package com.example.mrsummaries_app.notepad.note_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border  // Added missing import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PenSettingsDialog(
    currentStrokeWidth: Float,
    currentColor: Color,
    onStrokeWidthChanged: (Float) -> Unit,
    onColorChanged: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "S Pen Settings",
                    style = MaterialTheme.typography.titleLarge
                )

                // Stroke width slider
                Text(
                    text = "Stroke Width",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = currentStrokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 1f..30f,
                    steps = 29
                )

                // Color selection
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val colors = listOf(
                        Color.Black, Color.Blue, Color.Red, Color.Green,
                        Color.Yellow, Color.Magenta, Color.Cyan
                    )

                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, shape = RoundedCornerShape(20.dp))
                                .border(
                                    width = 2.dp,
                                    color = if (currentColor == color)
                                        MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onColorChanged(color) }
                        )
                    }
                }

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(currentStrokeWidth.dp)
                            .background(currentColor)
                    )
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Done")
                }
            }
        }
    }
}