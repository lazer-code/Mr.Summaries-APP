package com.example.mrsummaries_app.custom_views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream
import java.util.Stack

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    enum class Tool {
        PEN, HIGHLIGHTER, ERASER
    }

    private val paths = mutableListOf<Stroke>()
    private val undoStack = Stack<Stroke>()
    private val redoStack = Stack<Stroke>()

    private var currentPath = Path()
    private var currentStroke: Stroke? = null

    private var currentTool = Tool.PEN
    private var currentColor = Color.BLACK
    private var currentSize = 10f

    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    private val canvasPaint = Paint(Paint.DITHER_FLAG)

    // Tool properties
    private var penSize = 5f
    private var penColor = Color.BLACK
    private var highlighterSize = 20f
    private var highlighterColor = Color.YELLOW
    private var eraserSize = 20f

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        // Default paint settings
        canvasPaint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Create new bitmap and canvas when size changes
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the canvas bitmap
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        // Draw all saved paths
        for (stroke in paths) {
            canvas.drawPath(stroke.path, stroke.paint)
        }

        // Draw current path if it exists
        currentStroke?.let {
            canvas.drawPath(it.path, it.paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }

        return true
    }

    private fun touchStart(x: Float, y: Float) {
        currentPath = Path()
        currentPath.moveTo(x, y)

        val paint = createPaintForCurrentTool()

        currentStroke = Stroke(currentPath, paint)
    }

    private fun touchMove(x: Float, y: Float) {
        currentPath.lineTo(x, y)
    }

    private fun touchUp() {
        currentStroke?.let {
            paths.add(it)
            undoStack.push(it)
            redoStack.clear()
        }
        currentStroke = null
    }

    private fun createPaintForCurrentTool(): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        when (currentTool) {
            Tool.PEN -> {
                paint.color = penColor
                paint.strokeWidth = penSize
            }
            Tool.HIGHLIGHTER -> {
                paint.color = highlighterColor
                paint.strokeWidth = highlighterSize
                paint.alpha = 128  // Semi-transparent for highlighter effect
            }
            Tool.ERASER -> {
                paint.color = Color.WHITE
                paint.strokeWidth = eraserSize
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }

        return paint
    }

    fun setTool(tool: Tool) {
        currentTool = tool
    }

    fun setPenColor(color: Int) {
        penColor = color
        if (currentTool == Tool.PEN) {
            currentColor = color
        }
    }

    fun setPenSize(size: Float) {
        penSize = size
        if (currentTool == Tool.PEN) {
            currentSize = size
        }
    }

    fun setHighlighterColor(color: Int) {
        highlighterColor = color
        if (currentTool == Tool.HIGHLIGHTER) {
            currentColor = color
        }
    }

    fun setHighlighterSize(size: Float) {
        highlighterSize = size
        if (currentTool == Tool.HIGHLIGHTER) {
            currentSize = size
        }
    }

    fun setEraserSize(size: Float) {
        eraserSize = size
        if (currentTool == Tool.ERASER) {
            currentSize = size
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val removedStroke = undoStack.pop()
            paths.remove(removedStroke)
            redoStack.push(removedStroke)

            // Redraw everything
            redrawCanvas()
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val strokeToRedo = redoStack.pop()
            paths.add(strokeToRedo)
            undoStack.push(strokeToRedo)

            // Redraw everything
            redrawCanvas()
            invalidate()
        }
    }

    private fun redrawCanvas() {
        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        for (stroke in paths) {
            drawCanvas?.drawPath(stroke.path, stroke.paint)
        }
    }

    fun clearCanvas() {
        paths.clear()
        undoStack.clear()
        redoStack.clear()

        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun getDrawingData(): ByteArray {
        // Save the bitmap to a byte array
        val outputStream = ByteArrayOutputStream()
        canvasBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    fun loadDrawing(data: ByteArray) {
        try {
            val options = BitmapFactory.Options()
            options.inMutable = true
            val loadedBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

            val canvas = Canvas(canvasBitmap!!)
            canvas.drawBitmap(loadedBitmap, 0f, 0f, null)

            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Inner class to represent a stroke with its paint
    data class Stroke(val path: Path, val paint: Paint)
}