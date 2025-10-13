package com.example.mrsummaries_app.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.mrsummaries_app.models.Summary
import java.io.File

class SummaryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0
    private var pageCount = 0
    private var summary: Summary? = null
    private var scale = 1.0f
    private var translateX = 0f
    private var translateY = 0f

    // Gesture detectors for zooming and panning
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    fun loadSummary(summary: Summary) {
        this.summary = summary

        // In a real app, you would download the PDF if not already present
        // and then open it

        try {
            val file = File(context.filesDir, summary.filePath)
            if (file.exists()) {
                openPdf(file)
            } else {
                // Download the file or show error
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openPdf(file: File) {
        try {
            // Close any existing renderer
            closePdfRenderer()

            // Open the PDF file
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)

            pageCount = pdfRenderer?.pageCount ?: 0

            // Open the first page
            if (pageCount > 0) {
                openPage(0)
            }

            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= pageCount) return

        // Close current page if any
        currentPage?.close()

        // Open the requested page
        currentPage = pdfRenderer?.openPage(pageIndex)
        currentPageIndex = pageIndex

        invalidate()
    }

    fun nextPage() {
        if (currentPageIndex < pageCount - 1) {
            openPage(currentPageIndex + 1)
        }
    }

    fun previousPage() {
        if (currentPageIndex > 0) {
            openPage(currentPageIndex - 1)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Apply transformations for zoom and pan
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(translateX / scale, translateY / scale)

        // Render the current page if available
        currentPage?.render(canvas, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle pinch to zoom
        scaleGestureDetector.onTouchEvent(event)

        // Handle swipe and tap
        gestureDetector.onTouchEvent(event)

        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor

            // Limit the scale range
            scale = scale.coerceIn(0.5f, 3.0f)

            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Pan the view
            translateX -= distanceX
            translateY -= distanceY

            // Limit translation to prevent moving too far off screen
            // This would be more complex in a real implementation

            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Reset zoom on double tap
            scale = 1.0f
            translateX = 0f
            translateY = 0f
            invalidate()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Implement page flipping on fling
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX < 0) {
                    nextPage()
                } else {
                    previousPage()
                }
            }
            return true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closePdfRenderer()
    }

    private fun closePdfRenderer() {
        currentPage?.close()
        pdfRenderer?.close()
        currentPage = null
        pdfRenderer = null
    }
}