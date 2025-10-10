package com.example.mrsummaries_app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PerformanceSettingsDialog(
    minPointDistanceDp: Float,
    onMinPointDistanceChange: (Float) -> Unit,
    batchSize: Int,
    onBatchSizeChange: (Int) -> Unit,
    spatialIndexEnabled: Boolean,
    onSpatialIndexEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Performance settings") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Point sampling (min distance): ${"%.1f".format(minPointDistanceDp)} dp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = minPointDistanceDp,
                    onValueChange = onMinPointDistanceChange,
                    valueRange = 0.5f..12f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                )

                Text("Batch size (updates every N points): $batchSize", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = batchSize.toFloat(),
                    onValueChange = { onBatchSizeChange(it.toInt().coerceAtLeast(1)) },
                    valueRange = 1f..8f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                )

                Text("Use spatial index for eraser: ${if (spatialIndexEnabled) "ON" else "OFF"}", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = spatialIndexEnabled,
                    onCheckedChange = onSpatialIndexEnabledChange,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}