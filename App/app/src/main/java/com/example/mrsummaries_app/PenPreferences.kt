package com.example.mrsummaries_app

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pen_prefs")

private val KEY_COSTUME_PENS = stringPreferencesKey("costume_pens")
private val KEY_PEN_WIDTH = floatPreferencesKey("pen_width_dp")
private val KEY_ERASER_SIZE = floatPreferencesKey("eraser_size_dp")

data class PenSettings(
    val penWidthDp: Float,
    val eraserSizeDp: Float
)

object PenPreferences {
    // Format: "<argb>:<width>;<argb>:<width>;..."
    private fun encode(pens: List<CostumePen>): String =
        pens.joinToString(";") { "${it.color.toArgb()}:${it.strokeWidthDp}" }

    private fun decode(data: String): List<CostumePen> =
        data.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) return@mapNotNull null
                val argb = parts[0].toIntOrNull() ?: return@mapNotNull null
                val width = parts[1].toFloatOrNull() ?: return@mapNotNull null
                CostumePen(color = Color(argb), strokeWidthDp = width)
            }

    suspend fun loadCostumePens(context: Context): List<CostumePen> {
        val encoded = context.dataStore.data
            .map { prefs -> prefs[KEY_COSTUME_PENS] ?: "" }
            .first()
        return if (encoded.isBlank()) emptyList() else decode(encoded)
    }

    suspend fun saveCostumePens(context: Context, pens: List<CostumePen>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COSTUME_PENS] = encode(pens)
        }
    }

    suspend fun loadPenSettings(context: Context): PenSettings {
        val penWidth = context.dataStore.data.map { it[KEY_PEN_WIDTH] ?: 6f }.first()
        val eraserSize = context.dataStore.data.map { it[KEY_ERASER_SIZE] ?: 20f }.first()
        return PenSettings(penWidthDp = penWidth, eraserSizeDp = eraserSize)
    }

    suspend fun savePenSettings(context: Context, penWidthDp: Float, eraserSizeDp: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PEN_WIDTH] = penWidthDp
            prefs[KEY_ERASER_SIZE] = eraserSizeDp
        }
    }
}