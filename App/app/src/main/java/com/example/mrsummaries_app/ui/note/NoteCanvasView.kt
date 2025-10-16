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
 * - Strokes are stored and serialized as simple objects for persistence.
 * - Supports stylus primary button detection to temporarily switch to eraser.
 *
 * This is not a production-ready ink engine but a reasonable starting point matching requirements.
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

    private var tool = Tool.PEN
    private var onChange: (() -> Unit)? = null

    fun setOnChangeListener(f: () -> Unit) {
        onChange = f
    }

    fun setTool(t: Tool) {
        if (tool == t) {
            // clicking again closes any potential configuration UI — placeholder
            tool = t
        } else tool = t
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        strokes.forEach { s ->
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
                    // highlight implemented as straight line
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
        }
        // draw current path
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
            Tool.LASSO -> { /* placeholder: could select strokes */ }
            Tool.TEXT -> { /* click to add text */ if (event.action == MotionEvent.ACTION_UP) addTextAt(x, y) }
        }
        return true
    }

    private fun addTextAt(x: Float, y: Float) {
        strokes.add(Stroke.TextStroke("Text", x, y))
        onChange?.invoke()
        invalidate()
    }

    private fun handlePen(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPath.moveTo(x, y)
                currentStroke = Stroke.PathStroke(Path(currentPath), color = Color.BLACK, width = 6f)
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
                currentStroke = Stroke.HighlightStroke(startX = x, startY = y, endX = x, endY = y, color = Color.YELLOW, width = 36f)
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