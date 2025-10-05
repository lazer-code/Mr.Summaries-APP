package com.example.mrsummaries_app.ui.persistence

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.mrsummaries_app.note.StrokePath
import com.example.mrsummaries_app.files.FsRepository
import java.io.File

/**
 * Per-note persistence stored inside the note's own directory as "strokes.paths".
 * Format per line: width|argb|x1,y1;x2,y2;...
 */
object NoteContentStore {
    private const val STROKES_FILE = "strokes.paths"

    private fun fileFor(context: Context, noteId: String): File? {
        FsRepository.ensureInitialized(context) // make sure base paths exist
        val noteDir = FsRepository.noteDirectory(noteId) ?: return null
        return File(noteDir, STROKES_FILE)
    }

    suspend fun save(context: Context, noteId: String, paths: List<StrokePath>) {
        val f = fileFor(context, noteId) ?: return
        // Write using buffered writer line-by-line to avoid building one large String in memory
        f.parentFile?.let { if (!it.exists()) it.mkdirs() }
        f.bufferedWriter().use { bw ->
            for (stroke in paths) {
                val width = stroke.strokeWidthDp
                val argb = stroke.color.toArgb()
                val pts = stroke.points.joinToString(";") { "${it.x},${it.y}" }
                bw.append("$width|$argb|$pts")
                bw.newLine()
            }
        }
    }

    suspend fun load(context: Context, noteId: String): List<StrokePath> {
        val f = fileFor(context, noteId) ?: return emptyList()
        if (!f.exists()) return emptyList()
        return f.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val parts = line.split("|")
                if (parts.size != 3) return@mapNotNull null
                val width = parts[0].toFloatOrNull() ?: return@mapNotNull null
                val colorArgb = parts[1].toIntOrNull() ?: return@mapNotNull null
                val pts = parts[2].split(";").mapNotNull { pt ->
                    val xy = pt.split(",")
                    if (xy.size != 2) return@mapNotNull null
                    val x = xy[0].toFloatOrNull() ?: return@mapNotNull null
                    val y = xy[1].toFloatOrNull() ?: return@mapNotNull null
                    Offset(x, y)
                }
                StrokePath(pts, Color(colorArgb), width)
            }.toList()
        }
    }
}