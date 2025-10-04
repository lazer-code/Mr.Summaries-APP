package com.example.mrsummaries_app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.files.FsRepository

@Composable
fun MovePickerDialog(
    sourceId: String,
    onDismiss: () -> Unit,
    onMoveTo: (String) -> Unit,
    iconTint: Color? = null
) {
    val defaultBg = MaterialTheme.colorScheme.surface
    val resolvedTint = iconTint ?: contrastOn(defaultBg)

    val folders = FsRepository.collectAllFolders()
    val validTargets = folders.filter { FsRepository.isValidMove(sourceId, it.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder", color = resolvedTint) },
        text = {
            Column {
                LazyColumn {
                    itemsIndexed(validTargets, key = { _, f -> f.id }) { _, f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMoveTo(f.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = resolvedTint)
                            Spacer(Modifier.width(8.dp))
                            Text(f.name, color = resolvedTint)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = resolvedTint) }
        }
    )
}