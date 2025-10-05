package com.example.mrsummaries_app.note

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    // Pen icon (pencil) from Material Icons
    val Pen = Icons.Filled.Edit

    // Simple custom eraser icon (vector)
    val Eraser: ImageVector by lazy {
        Builder(
            name = "Eraser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Slanted eraser body
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(15.0f, 3.5f)
                lineTo(21.0f, 9.5f)
                lineTo(13.0f, 17.5f)
                lineTo(7.0f, 11.5f)
                close()
            }
            // Small base/edge under the eraser
            path(
                fill = SolidColor(Color(0xFF000000)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3.0f, 19.0f)
                lineTo(13.5f, 19.0f)
                lineTo(13.5f, 21.0f)
                lineTo(3.0f, 21.0f)
                close()
            }
        }.build()
    }
}