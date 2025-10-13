package com.example.mrsummaries_app.drawing

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.abs

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paths = LinkedList<Stroke>()
    private var currentStroke: Stroke? = null
    private val undoneStrokes = LinkedList<Stroke>()

    private var mX = 0f
    private var mY = 0f

    private var currentColor = Color.BLACK
    private var currentDrawingMode = DrawingMode.PENCIL
    private var currentPenStyle = PenStyle.NORMAL
    private var currentStrokeWidth = 5f

    private var canvasBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null
    private var canvasPath = Path()
    private var drawPaint = Paint().apply {
        color = currentColor
        isAntiAlias = true
        strokeWidth = currentStrokeWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // For pressure sensitivity when using a stylus
    private var lastPressure = 1.0f

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the saved bitmap
        canvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Draw the current stroke
        currentStroke?.let {
            canvas.drawPath(it.path, it.paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val pressure = if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            event.pressure
        } else {
            1.0f  // Default pressure for finger
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y, pressure)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y, pressure)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }

        return true
    }

    private fun touchStart(x: Float, y: Float, pressure: Float) {
        // Clear undone paths when starting a new stroke
        undoneStrokes.clear()

        // Reset the path
        canvasPath.reset()
        canvasPath.moveTo(x, y)

        // Record the start point
        mX = x
        mY = y
        lastPressure = pressure

        // Create a new paint for this stroke based on current settings
        val paint = Paint(drawPaint)
        applyModeAndStyleToPaint(paint)

        // Create a new stroke and set it as current
        currentStroke = Stroke(Path(canvasPath), paint)
    }

    private fun touchMove(x: Float, y: Float, pressure: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)

        // Only proceed if the movement is significant
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            when (currentPenStyle) {
                PenStyle.NORMAL -> {
                    // Simple curve
                    canvasPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                }
                PenStyle.CALLIGRAPHIC -> {
                    // Calligraphic effect with angled stroke
                    val angle = Math.atan2((y - mY).toDouble(), (x - mX).toDouble())
                    val xOffset = 3f * Math.sin(angle)
                    val yOffset = 3f * Math.cos(angle)

                    canvasPath.quadTo(
                        mX + xOffset.toFloat(), mY - yOffset.toFloat(),
                        (x + mX) / 2, (y + mY) / 2
                    )
                }
                PenStyle.FOUNTAIN -> {
                    // Fountain pen effect with pressure sensitivity
                    val avgPressure = (pressure + lastPressure) / 2
                    val strokeWidth = currentStrokeWidth * avgPressure
                    currentStroke?.paint?.strokeWidth = strokeWidth

                    canvasPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                }
                PenStyle.MARKER -> {
                    // Marker effect - more square caps and direct lines
                    canvasPath.lineTo(x, y)
                }
            }

            // Update the last point and pressure
            mX = x
            mY = y
            lastPressure = pressure

            // Update the current stroke's path
            currentStroke?.path = Path(canvasPath)
        }
    }

    private fun touchUp() {
        // Finish the path
        canvasPath.lineTo(mX, mY)

        // Add the current stroke to the list of paths
        currentStroke?.let {
            // Draw the stroke to the canvas bitmap
            drawCanvas?.drawPath(it.path, it.paint)
            // Add it to the list of strokes
            paths.add(it)
        }

        // Reset the current stroke and path
        currentStroke = null
        canvasPath.reset()
    }

    private fun applyModeAndStyleToPaint(paint: Paint) {
        when (currentDrawingMode) {
            DrawingMode.PENCIL -> {
                paint.color = currentColor
                paint.strokeWidth = currentStrokeWidth
                paint.xfermode = null

                when (currentPenStyle) {
                    PenStyle.NORMAL -> {
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeJoin = Paint.Join.ROUND
                    }
                    PenStyle.CALLIGRAPHIC -> {
                        paint.strokeCap = Paint.Cap.SQUARE
                        paint.strokeJoin = Paint.Join.BEVEL
                    }
                    PenStyle.FOUNTAIN -> {
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeJoin = Paint.Join.ROUND
                    }
                    PenStyle.MARKER -> {
                        paint.strokeCap = Paint.Cap.SQUARE
                        paint.strokeJoin = Paint.Join.MITER
                    }
                }
            }
            DrawingMode.HIGHLIGHTER -> {
                val alpha = 80 // Semi-transparent
                val color = Color.argb(
                    alpha,
                    Color.red(currentColor),
                    Color.green(currentColor),
                    Color.blue(currentColor)
                )
                paint.color = color
                paint.strokeWidth = currentStrokeWidth * 2
                paint.strokeCap = Paint.Cap.SQUARE
                paint.strokeJoin = Paint.Join.MITER
                paint.xfermode = null
            }
            DrawingMode.ERASER -> {
                paint.color = Color.WHITE
                paint.strokeWidth = currentStrokeWidth * 1.5f
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            DrawingMode.LASSO -> {
                paint.color = Color.BLUE
                paint.strokeWidth = 2f
                paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                paint.xfermode = null
            }
            DrawingMode.TEXT -> {
                // For text mode, we don't draw paths but use this to configure text appearance
                paint.color = currentColor
                paint.strokeWidth = 1f
                paint.style = Paint.Style.FILL
                paint.textSize = 48f
                paint.xfermode = null
            }
        }
    }

    fun setDrawingMode(mode: DrawingMode) {
        currentDrawingMode = mode
        // Update the paint properties for the current mode
        applyModeAndStyleToPaint(drawPaint)
    }

    fun setPenStyle(style: PenStyle) {
        currentPenStyle = style
        if (currentDrawingMode == DrawingMode.PENCIL) {
            applyModeAndStyleToPaint(drawPaint)
        }
    }

    fun setColor(color: Int) {
        currentColor = color
        if (currentDrawingMode != DrawingMode.ERASER) {
            drawPaint.color = color
        }
    }

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width

        when (currentDrawingMode) {
            DrawingMode.PENCIL -> drawPaint.strokeWidth = width
            DrawingMode.HIGHLIGHTER -> drawPaint.strokeWidth = width * 2
            DrawingMode.ERASER -> drawPaint.strokeWidth = width * 1.5f
            else -> drawPaint.strokeWidth = width
        }
    }

    fun undo() {
        if (paths.size > 0) {
            val removedStroke = paths.removeLast()
            undoneStrokes.add(removedStroke)
            redrawPaths()
            invalidate()
        }
    }

    fun redo() {
        if (undoneStrokes.size > 0) {
            val redoneStroke = undoneStrokes.removeLast()
            paths.add(redoneStroke)
            redrawPaths()
            invalidate()
        }
    }

    fun clearDrawing() {
        paths.clear()
        undoneStrokes.clear()
        currentStroke = null
        canvasPath.reset()
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    private fun redrawPaths() {
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        for (stroke in paths) {
            drawCanvas?.drawPath(stroke.path, stroke.paint)
        }
    }

    fun addText(text: String, x: Float, y: Float) {
        val textPaint = Paint(drawPaint).apply {
            color = currentColor
            style = Paint.Style.FILL
            textSize = currentStrokeWidth * 8
            isAntiAlias = true
        }

        drawCanvas?.drawText(text, x, y, textPaint)
        invalidate()

        // Create a Path representation of the text for undo capability
        val textPath = Path()
        textPaint.getTextPath(text, 0, text.length, x, y, textPath)
        paths.add(Stroke(textPath, textPaint))
    }

    fun getCurrentDrawingMode(): DrawingMode = currentDrawingMode

    fun saveToImage(): Bitmap? = canvasBitmap?.copy(Bitmap.Config.ARGB_8888, false)

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }

    data class Stroke(var path: Path, val paint: Paint)
}