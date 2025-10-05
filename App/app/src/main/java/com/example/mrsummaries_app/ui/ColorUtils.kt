package com.example.mrsummaries_app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

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

/**
 * Adjust brightness by mixing with white (positive) or black (negative).
 * amount in [-1, 1], where:
 *  - +0.1 = 10% lighter
 *  - -0.1 = 10% darker
 */
fun adjustBrightness(color: Color, amount: Float): Color {
    val a = amount.coerceIn(-1f, 1f)
    val target = if (a >= 0f) 1f else 0f
    val f = abs(a)
    return Color(
        red = color.red + (target - color.red) * f,
        green = color.green + (target - color.green) * f,
        blue = color.blue + (target - color.blue) * f,
        alpha = color.alpha
    )
}