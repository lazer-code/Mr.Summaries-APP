package com.example.mrsummaries_app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Returns a high-contrast foreground color (default White/Black) against the given background.
 * threshold ~0.5 means "light background" returns dark foreground (Black), otherwise White.
 */
fun contrastOn(
    background: Color,
    light: Color = Color.White,
    dark: Color = Color.Black,
    threshold: Float = 0.5f
): Color {
    return if (background.luminance() > threshold) dark else light
}