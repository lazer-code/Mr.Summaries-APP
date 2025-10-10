package com.example.mrsummaries_app.note

import androidx.compose.ui.geometry.Offset
import kotlin.math.floor

/**
 * A simple grid-based spatial index (spatial hash) for fast candidate lookups.
 *
 * - cellSizePx: size of each grid cell in pixels.
 * - rebuild(paths) populates the index with all stroke points, associating each point with its stroke index.
 * - query(center, radius) returns a set of stroke indices that have at least one point in cells intersecting the query circle.
 *
 * This is intentionally simple and fast to build compared to a full quadtree and works well for
 * erase queries which search a circular neighborhood.
 */
class SpatialIndex(private var cellSizePx: Float = 64f) {

    // Map from cell key (xIndex, yIndex packed into Long) -> list of (strokeIndex, pointIndex)
    private val cellMap = HashMap<Long, MutableList<Pair<Int, Int>>>()

    // Helper to pack two ints into a long key
    private fun key(ix: Int, iy: Int): Long = (ix.toLong() shl 32) or (iy.toLong() and 0xffffffffL)

    fun setCellSize(px: Float) {
        if (px > 1f) cellSizePx = px
    }

    fun clear() {
        cellMap.clear()
    }

    /**
     * Rebuild the index from the provided strokes.
     * Stroke points are expected to be in the same coordinate space as the query positions (pixels).
     */
    fun rebuild(paths: List<StrokePath>) {
        cellMap.clear()
        if (paths.isEmpty()) return
        val cs = cellSizePx.coerceAtLeast(8f)
        for ((strokeIdx, stroke) in paths.withIndex()) {
            for ((pointIdx, p) in stroke.points.withIndex()) {
                val ix = floor(p.x / cs).toInt()
                val iy = floor(p.y / cs).toInt()
                val k = key(ix, iy)
                val l = cellMap.getOrPut(k) { mutableListOf() }
                l.add(strokeIdx to pointIdx)
            }
        }
    }

    /**
     * Query candidate stroke indices whose points lie within cells that intersect the circle (center, radius).
     * Returns a set of stroke indices (no duplicates).
     *
     * This only returns candidates — callers should still run an exact distance check on returned strokes.
     */
    fun query(center: Offset, radius: Float): Set<Int> {
        if (cellMap.isEmpty()) return emptySet()
        val cs = cellSizePx.coerceAtLeast(8f)
        val minX = floor((center.x - radius) / cs).toInt()
        val maxX = floor((center.x + radius) / cs).toInt()
        val minY = floor((center.y - radius) / cs).toInt()
        val maxY = floor((center.y + radius) / cs).toInt()
        val out = mutableSetOf<Int>()
        for (ix in minX..maxX) {
            for (iy in minY..maxY) {
                val k = key(ix, iy)
                cellMap[k]?.let { list ->
                    for ((strokeIdx, _) in list) out.add(strokeIdx)
                }
            }
        }
        return out
    }
}