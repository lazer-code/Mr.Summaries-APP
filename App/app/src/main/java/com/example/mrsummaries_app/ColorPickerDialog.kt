package com.example.mrsummaries_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    showDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var red by remember { mutableStateOf((initialColor.red * 255).roundToInt()) }
    var green by remember { mutableStateOf((initialColor.green * 255).roundToInt()) }
    var blue by remember { mutableStateOf((initialColor.blue * 255).roundToInt()) }
    var hex by remember { mutableStateOf(TextFieldValue("#%02X%02X%02X".format(red, green, blue))) }

    fun clamp(x: Int) = x.coerceIn(0, 255)
    fun currentColor() = Color(red / 255f, green / 255f, blue / 255f)

    fun updateFromHex(text: String) {
        val normalized = text.trim().removePrefix("#")
        if (normalized.length == 6) {
            val r = normalized.substring(0, 2).toIntOrNull(16)
            val g = normalized.substring(2, 4).toIntOrNull(16)
            val b = normalized.substring(4, 6).toIntOrNull(16)
            if (r != null && g != null && b != null) {
                red = r; green = g; blue = b
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showDelete) "Edit Colour" else "Pick Colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(currentColor())
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = {
                        hex = it
                        updateFromHex(it.text)
                    },
                    label = { Text("Hex (#RRGGBB)") },
                    singleLine = true
                )
                ChannelSlider("R", red) { newVal -> red = clamp(newVal); hex = TextFieldValue("#%02X%02X%02X".format(red, green, blue)) }
                ChannelSlider("G", green) { newVal -> green = clamp(newVal); hex = TextFieldValue("#%02X%02X%02X".format(red, green, blue)) }
                ChannelSlider("B", blue) { newVal -> blue = clamp(newVal); hex = TextFieldValue("#%02X%02X%02X".format(red, green, blue)) }
            }
        },
        // Put Delete (if any) on the left, OK on the right
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (showDelete && onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(onClick = { onConfirm(currentColor()) }) { Text("OK") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$label: $value")
        }
        androidx.compose.material3.Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f
        )
    }
}