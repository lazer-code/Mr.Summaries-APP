package com.example.mrsummaries_app.ui.note

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.gson.Gson
import kotlin.math.abs

/**
 * Simplified canvas implementing pen, highlighter (straight line), eraser (stroke-based),
 * undo/redo and basic text boxes.
 *
 * Added:
 * - currentColor/currentWidth with setters
 * - onToolConfigRequest callback invoked when the same tool is clicked again
 * - basic lasso tool: draw arbitrary lasso path, select strokes whose bounds intersect lasso bounding rect
 * - selection visual overlay
 */
class NoteCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Tool { PEN, HIGHLIGHTER, ERASER, LASSO, TEXT }

    private val gson = Gson()

    private val strokes = mutableListOf<Stroke>()
    private val undone = mutableListOf<Stroke>()

    private var currentPath = Path()
    private var currentStroke: Stroke? = null

    // Appearance
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.BLACK
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val highlightPaint = Paint(paint).apply {
        strokeWidth = 24f
        color = Color.YELLOW
        alpha = 160
    }

    // Selection / lasso
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var tool = Tool.PEN
    private var onChange: (() -> Unit)? = null

    // Called when user clicks the currently-selected tool again to request configuration UI
    private var onToolConfigRequest: ((Tool) -> Unit)? = null

    // Current drawing parameters
    private var currentColor: Int = Color.BLACK
    private var currentWidth: Float = 6f

    // Lasso state
    private var lassoPath: Path? = null
    private var lassoBounds: RectF? = null
    private val selectedStrokeIndices = mutableSetOf<Int>()

    fun setOnChangeListener(f: () -> Unit) {
        onChange = f
    }

    fun setOnToolConfigRequestListener(f: (Tool) -> Unit) {
        onToolConfigRequest = f
    }

    fun setColor(color: Int) {
        currentColor = color
        // update paints used for live drawing (not retroactively changing strokes)
        paint.color = currentColor
        invalidate()
    }

    fun setWidth(width: Float) {
        currentWidth = width
        paint.strokeWidth = currentWidth
        highlightPaint.strokeWidth = currentWidth * 6f // keep highlighter wider by default multiplier
        invalidate()
    }

    fun getCurrentColor(): Int = currentColor
    fun getCurrentWidth(): Float = currentWidth

    fun setTool(t: Tool) {
        if (tool == t) {
            // clicking same tool again -> request configuration UI (Activity should show config bar)
            onToolConfigRequest?.invoke(t)
        } else {
            // clear lasso selection if switching away
            if (tool == Tool.LASSO) {
                lassoPath = null
                lassoBounds = null
                selectedStrokeIndices.clear()
            }
            tool = t
        }
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            undone.add(strokes.removeAt(strokes.size - 1))
            invalidate()
            onChange?.invoke()
        }
    }

    fun redo() {
        if (undone.isNotEmpty()) {
            strokes.add(undone.removeAt(undone.size - 1))
            invalidate()
            onChange?.invoke()
        }
    }

    fun addTextAtCenter(text: String) {
        // insert text as a simple stroke with text content stored
        strokes.add(Stroke.TextStroke(text, width / 2f, height / 2f))
        onChange?.invoke()
        invalidate()
    }

    // Added: function used by onTouchEvent for Tool.TEXT (fixes unresolved reference)
    private fun addTextAt(x: Float, y: Float) {
        strokes.add(Stroke.TextStroke("Text", x, y))
        onChange?.invoke()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw strokes
        strokes.forEachIndexed { idx, s ->
            when (s) {
                is Stroke.PathStroke -> {
                    paint.strokeWidth = s.width
                    paint.color = s.color
                    paint.alpha = s.alpha
                    canvas.drawPath(s.path, paint)
                }
                is Stroke.HighlightStroke -> {
                    highlightPaint.strokeWidth = s.width
                    highlightPaint.color = s.color
                    highlightPaint.alpha = s.alpha
                    canvas.drawLine(s.startX, s.startY, s.endX, s.endY, highlightPaint)
                }
                is Stroke.TextStroke -> {
                    val tpaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        textSize = 48f
                    }
                    canvas.drawText(s.text, s.x, s.y, tpaint)
                }
            }
            // draw selection box around selected strokes
            if (selectedStrokeIndices.contains(idx)) {
                val bounds = RectF()
                when (s) {
                    is Stroke.PathStroke -> {
                        s.path.computeBounds(bounds, true)
                    }
                    is Stroke.HighlightStroke -> {
                        bounds.set(minOf(s.startX, s.endX), minOf(s.startY, s.endY), maxOf(s.startX, s.endX), maxOf(s.startY, s.endY))
                    }
                    is Stroke.TextStroke -> {
                        bounds.set(s.x - 10f, s.y - 48f, s.x + 10f + (s.text.length * 12f), s.y + 12f)
                    }
                }
                canvas.drawRect(bounds, selectionPaint)
            }
        }

        // draw current stroke in progress
        currentStroke?.let { cs ->
            when (cs) {
                is Stroke.PathStroke -> {
                    paint.strokeWidth = cs.width
                    paint.color = cs.color
                    paint.alpha = cs.alpha
                    canvas.drawPath(cs.path, paint)
                }
                is Stroke.HighlightStroke -> {
                    highlightPaint.strokeWidth = cs.width
                    highlightPaint.color = cs.color
                    highlightPaint.alpha = cs.alpha
                    canvas.drawLine(cs.startX, cs.startY, cs.endX, cs.endY, highlightPaint)
                }
                else -> {}
            }
        }

        // draw lasso path (if drawing)
        lassoPath?.let { lp ->
            canvas.drawPath(lp, selectionPaint)
            lassoBounds?.let { lb ->
                // also draw its bounding rectangle lightly
                val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.CYAN
                    strokeWidth = 2f
                }
                canvas.drawRect(lb, rectPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // stylus button -> eraser while pressed
        val isStylusButtonPressed = (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

        val effectiveTool = if (isStylusButtonPressed) Tool.ERASER else tool

        val x = event.x
        val y = event.y

        when (effectiveTool) {
            Tool.PEN -> handlePen(event, x, y)
            Tool.HIGHLIGHTER -> handleHighlighter(event, x, y)
            Tool.ERASER -> handleEraser(event, x, y)
            Tool.LASSO -> handleLasso(event, x, y)
            Tool.TEXT -> { if (event.action == MotionEvent.ACTION_UP) addTextAt(x, y) }
        }
        return true
    }

    private fun handlePen(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath.moveTo(x, y)
                currentStroke = Stroke.PathStroke(Path(currentPath), color = currentColor, width = currentWidth)
                undone.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                (currentStroke as? Stroke.PathStroke)?.path = Path(currentPath)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                (currentStroke as? Stroke.PathStroke)?.let { strokes.add(it) }
                currentStroke = null
                onChange?.invoke()
            }
        }
        invalidate()
    }

    private fun handleHighlighter(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentStroke = Stroke.HighlightStroke(startX = x, startY = y, endX = x, endY = y, color = currentColor.takeIf { it != 0 } ?: Color.YELLOW, width = maxOf(currentWidth * 6f, 24f), alpha = 160)
                undone.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                (currentStroke as? Stroke.HighlightStroke)?.endX = x
                (currentStroke as? Stroke.HighlightStroke)?.endY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                (currentStroke as? Stroke.HighlightStroke)?.let { strokes.add(it) }
                currentStroke = null
                onChange?.invoke()
            }
        }
        invalidate()
    }

    private fun handleEraser(event: MotionEvent, x: Float, y: Float) {
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            // simple eraser: remove strokes that intersect point radius
            val it = strokes.iterator()
            val radius = 40f
            var removed = false
            while (it.hasNext()) {
                val s = it.next()
                if (s.intersects(x, y, radius)) {
                    it.remove()
                    removed = true
                }
            }
            if (removed) {
                onChange?.invoke()
                invalidate()
            }
        }
    }

    private fun handleLasso(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lassoPath = Path()
                lassoPath?.moveTo(x, y)
                lassoBounds = RectF(x, y, x, y)
                selectedStrokeIndices.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                lassoPath?.lineTo(x, y)
                lassoBounds?.let { it.union(x, y) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // compute bounding rect of lasso and select strokes whose bounds intersect the rect
                val lb = lassoBounds ?: RectF()
                val selected = mutableSetOf<Int>()
                strokes.forEachIndexed { idx, s ->
                    val bounds = RectF()
                    when (s) {
                        is Stroke.PathStroke -> s.path.computeBounds(bounds, true)
                        is Stroke.HighlightStroke -> bounds.set(minOf(s.startX, s.endX), minOf(s.startY, s.endY), maxOf(s.startX, s.endX), maxOf(s.startY, s.endY))
                        is Stroke.TextStroke -> bounds.set(s.x - 10f, s.y - 48f, s.x + 10f + (s.text.length * 12f), s.y + 12f)
                    }
                    if (RectF.intersects(bounds, lb)) {
                        selected.add(idx)
                    }
                }
                selectedStrokeIndices.clear()
                selectedStrokeIndices.addAll(selected)
                // keep lasso path visible for selection visual; user can switch tools to clear
                onChange?.invoke()
                invalidate()
            }
        }
        invalidate()
    }

    fun toJson(): String {
        val serial = strokes.map { it.toSerializable() }
        return gson.toJson(serial)
    }

    fun loadFromJson(json: String) {
        try {
            val arr = gson.fromJson(json, Array<SerializableStroke>::class.java) ?: emptyArray()
            strokes.clear()
            arr.forEach { strokes.add(Stroke.fromSerializable(it)) }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFromJsonObject(obj: Any) { /* placeholder */ }

    // Simple stroke models and serialization
    sealed class Stroke {
        abstract fun intersects(x: Float, y: Float, radius: Float): Boolean
        abstract fun toSerializable(): SerializableStroke

        data class PathStroke(var path: Path, val color: Int, val width: Float, val alpha: Int = 255) : Stroke() {
            override fun intersects(x: Float, y: Float, radius: Float): Boolean {
                // approximate by bounds
                val rect = RectF()
                path.computeBounds(rect, true)
                return rect.contains(x, y)
            }

            override fun toSerializable(): SerializableStroke {
                // Convert path to points approximation (not perfect)
                return SerializableStroke(type = "path", color = color, width = width, pathPoints = listOf())
            }
        }

        data class HighlightStroke(var startX: Float, var startY: Float, var endX: Float, var endY: Float, val color: Int, val width: Float, val alpha: Int = 255) : Stroke() {
            override fun intersects(x: Float, y: Float, radius: Float): Boolean {
                // check proximity to line
                val dx = endX - startX
                val dy = endY - startY
                val lengthSq = dx * dx + dy * dy
                if (lengthSq == 0f) return (Math.hypot((x - startX).toDouble(), (y - startY).toDouble()) <= radius)
                val t = ((x - startX) * dx + (y - startY) * dy) / lengthSq
                val projX = startX + t * dx
                val projY = startY + t * dy
                return Math.hypot((x - projX).toDouble(), (y - projY).toDouble()) <= radius
            }

            override fun toSerializable(): SerializableStroke {
                return SerializableStroke(type = "highlight", color = color, width = width, startX = startX, startY = startY, endX = endX, endY = endY)
            }
        }

        data class TextStroke(val text: String, val x: Float, val y: Float) : Stroke() {
            override fun intersects(x: Float, y: Float, radius: Float): Boolean {
                // simple bounding
                return abs(x - this.x) <= 100 && abs(y - this.y) <= 40
            }

            override fun toSerializable(): SerializableStroke {
                return SerializableStroke(type = "text", text = text, x = x, y = y)
            }
        }

        companion object {
            fun fromSerializable(s: SerializableStroke): Stroke {
                return when (s.type) {
                    "text" -> TextStroke(s.text ?: "", s.x ?: 0f, s.y ?: 0f)
                    "highlight" -> HighlightStroke(s.startX ?: 0f, s.startY ?: 0f, s.endX ?: 0f, s.endY ?: 0f, s.color ?: Color.YELLOW, s.width ?: 36f)
                    else -> PathStroke(Path(), s.color ?: Color.BLACK, s.width ?: 6f)
                }
            }
        }
    }

    data class SerializableStroke(
        val type: String,
        val color: Int? = null,
        val width: Float? = null,
        val pathPoints: List<Float>? = null,
        val text: String? = null,
        val x: Float? = null,
        val y: Float? = null,
        val startX: Float? = null,
        val startY: Float? = null,
        val endX: Float? = null,
        val endY: Float? = null
    )
}