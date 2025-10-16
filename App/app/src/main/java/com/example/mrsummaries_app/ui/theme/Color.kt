package com.example.mrsummaries_app.ui.theme

import androidx.compose.ui.graphics.Color

// Simplified, neutral palette with a single accent (orange).
// Light / Dark variants are provided in Theme.kt via Material3 color schemes.

// Accent
val Accent = Color(0xFFFB8C00) // warm orange

// Neutral / greys
val Grey10 = Color(0xFF0A0A0A)
val Grey20 = Color(0xFF1F1F1F)
val Grey80 = Color(0xFFBDBDBD)
val Grey90 = Color(0xFFF5F5F5)

// Light theme specific
val LightBackground = Color(0xFFF6F7F9)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF202124)
val LightPrimary = Accent
val LightOnPrimary = Color(0xFFFFFFFF)

// Dark theme specific
val DarkBackground = Color(0xFF0F1112)
val DarkSurface = Color(0xFF141617)
val DarkOnSurface = Color(0xFFECECEC)
val DarkPrimary = Color(0xFFFFB74D) // softer orange in dark
val DarkOnPrimary = Color(0xFF000000)