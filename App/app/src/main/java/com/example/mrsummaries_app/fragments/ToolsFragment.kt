package com.example.mrsummaries_app.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.drawing.DrawingMode
import com.example.mrsummaries_app.drawing.DrawingView
import com.example.mrsummaries_app.drawing.PenStyle

/**
 * ToolsFragment (manual view lookups)
 *
 * This version does not rely on generated view binding; it uses findViewById against
 * the layout res/layout/fragment_tools.xml provided below. It expects the activity layout
 * to host the DrawingView with id @id/drawing_view.
 */
class ToolsFragment : Fragment() {

    private var drawingView: DrawingView? = null

    // UI references (nullable because layout may not include some optional controls)
    private var btnPencil: ImageButton? = null
    private var btnHighlighter: ImageButton? = null
    private var btnEraser: ImageButton? = null
    private var btnText: ImageButton? = null
    private var btnLasso: ImageButton? = null
    private var btnClear: ImageButton? = null
    private var btnUndo: ImageButton? = null

    private var colorBlack: View? = null
    private var colorRed: View? = null
    private var colorBlue: View? = null
    private var colorGreen: View? = null
    private var colorYellow: View? = null
    private var colorMagenta: View? = null
    private var colorCyan: View? = null

    private var seekBarSize: SeekBar? = null
    private var sizePreview: View? = null

    private var colorPickerContainer: View? = null

    private val availableColors = arrayOf(
        Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
        Color.YELLOW, Color.MAGENTA, Color.CYAN
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate a layout that contains the tools UI (see fragment_tools.xml)
        return inflater.inflate(R.layout.fragment_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DrawingView lives in the parent Activity layout (id = drawing_view)
        drawingView = requireActivity().findViewById(R.id.drawing_view)

        // Find views in this fragment's layout
        btnPencil = view.findViewById(R.id.btn_pencil)
        btnHighlighter = view.findViewById(R.id.btn_highlighter)
        btnEraser = view.findViewById(R.id.btn_eraser)
        btnText = view.findViewById(R.id.btn_text)
        btnLasso = view.findViewById(R.id.btn_lasso)
        btnClear = view.findViewById(R.id.btn_clear)
        btnUndo = view.findViewById(R.id.btn_undo)

        colorBlack = view.findViewById(R.id.color_black)
        colorRed = view.findViewById(R.id.color_red)
        colorBlue = view.findViewById(R.id.color_blue)
        colorGreen = view.findViewById(R.id.color_green)
        colorYellow = view.findViewById(R.id.color_yellow)
        colorMagenta = view.findViewById(R.id.color_magenta)
        colorCyan = view.findViewById(R.id.color_cyan)

        seekBarSize = view.findViewById(R.id.seek_bar_size)
        sizePreview = view.findViewById(R.id.view_size_preview)

        colorPickerContainer = view.findViewById(R.id.color_picker_container)

        setupToolButtons()
        setupColorPicker()
        setupSizeSlider()
    }

    private fun setupToolButtons() {
        btnPencil?.setOnClickListener {
            drawingView?.setDrawingMode(DrawingMode.PENCIL)
            updateToolSelection(TOOL_PENCIL)
        }

        btnHighlighter?.setOnClickListener {
            drawingView?.setDrawingMode(DrawingMode.HIGHLIGHTER)
            updateToolSelection(TOOL_HIGHLIGHTER)
        }

        btnEraser?.setOnClickListener {
            drawingView?.setDrawingMode(DrawingMode.ERASER)
            updateToolSelection(TOOL_ERASER)
        }

        btnText?.setOnClickListener {
            drawingView?.setDrawingMode(DrawingMode.TEXT)
            updateToolSelection(TOOL_TEXT)
        }

        btnLasso?.setOnClickListener {
            drawingView?.setDrawingMode(DrawingMode.LASSO)
            updateToolSelection(TOOL_LASSO)
        }

        btnClear?.setOnClickListener {
            // simple confirmation
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear Drawing")
                .setMessage("Are you sure you want to clear the entire drawing?")
                .setPositiveButton("Yes") { _, _ -> drawingView?.clearDrawing() }
                .setNegativeButton("No", null)
                .show()
        }

        btnUndo?.setOnClickListener {
            drawingView?.undo()
        }

        // initial selection
        updateToolSelection(TOOL_PENCIL)
    }

    private fun setupColorPicker() {
        val colorViews = arrayOf(
            colorBlack, colorRed, colorBlue, colorGreen,
            colorYellow, colorMagenta, colorCyan
        )

        for (i in colorViews.indices) {
            val cv = colorViews[i]
            val color = availableColors[i]
            cv?.setBackgroundColor(color)
            cv?.setOnClickListener {
                drawingView?.setColor(color)
                updateColorSelection(i)
            }
        }

        // initial selection
        updateColorSelection(0)
    }

    private fun setupSizeSlider() {
        seekBarSize?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val strokeWidth = progress.toFloat() + 1f
                drawingView?.setStrokeWidth(strokeWidth)
                updateSizePreview(strokeWidth)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // initialize
        val initialSize = 5f
        seekBarSize?.progress = initialSize.toInt()
        updateSizePreview(initialSize)
    }

    private fun updateToolSelection(selectedTool: Int) {
        btnPencil?.alpha = if (selectedTool == TOOL_PENCIL) 1.0f else 0.5f
        btnHighlighter?.alpha = if (selectedTool == TOOL_HIGHLIGHTER) 1.0f else 0.5f
        btnEraser?.alpha = if (selectedTool == TOOL_ERASER) 1.0f else 0.5f
        btnText?.alpha = if (selectedTool == TOOL_TEXT) 1.0f else 0.5f
        btnLasso?.alpha = if (selectedTool == TOOL_LASSO) 1.0f else 0.5f

        // Hide color picker when using eraser
        colorPickerContainer?.visibility = if (selectedTool == TOOL_ERASER) View.GONE else View.VISIBLE
    }

    private fun updateColorSelection(selectedIndex: Int) {
        val colorViews = arrayOf(
            colorBlack, colorRed, colorBlue, colorGreen,
            colorYellow, colorMagenta, colorCyan
        )
        for (i in colorViews.indices) {
            colorViews[i]?.alpha = if (i == selectedIndex) 1.0f else 0.5f
        }
    }

    private fun updateSizePreview(size: Float) {
        val px = dpToPx(size)
        sizePreview?.layoutParams?.let {
            it.width = px
            it.height = px
            sizePreview?.layoutParams = it
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // clear references
        drawingView = null
        btnPencil = null
        btnHighlighter = null
        btnEraser = null
        btnText = null
        btnLasso = null
        btnClear = null
        btnUndo = null
        colorBlack = null
        colorRed = null
        colorBlue = null
        colorGreen = null
        colorYellow = null
        colorMagenta = null
        colorCyan = null
        seekBarSize = null
        sizePreview = null
        colorPickerContainer = null
    }

    companion object {
        private const val TOOL_PENCIL = 0
        private const val TOOL_HIGHLIGHTER = 1
        private const val TOOL_ERASER = 2
        private const val TOOL_TEXT = 3
        private const val TOOL_LASSO = 4
    }
}