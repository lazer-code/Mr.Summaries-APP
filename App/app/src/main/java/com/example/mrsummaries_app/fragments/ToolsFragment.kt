package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.custom_views.DrawingView

class ToolsFragment : Fragment() {

    private lateinit var drawingView: DrawingView
    private lateinit var penButton: Button
    private lateinit var highlighterButton: Button
    private lateinit var eraserButton: Button
    private lateinit var clearButton: Button
    private lateinit var penSizeSeekBar: SeekBar
    private lateinit var penColorPalette: ViewGroup
    private lateinit var highlighterSizeSeekBar: SeekBar
    private lateinit var highlighterColorPalette: ViewGroup
    private lateinit var eraserSizeSeekBar: SeekBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawingView = view.findViewById(R.id.drawing_view)
        penButton = view.findViewById(R.id.pen_button)
        highlighterButton = view.findViewById(R.id.highlighter_button)
        eraserButton = view.findViewById(R.id.eraser_button)
        clearButton = view.findViewById(R.id.clear_button)

        penSizeSeekBar = view.findViewById(R.id.pen_size_seekbar)
        penColorPalette = view.findViewById(R.id.pen_color_palette)
        highlighterSizeSeekBar = view.findViewById(R.id.highlighter_size_seekbar)
        highlighterColorPalette = view.findViewById(R.id.highlighter_color_palette)
        eraserSizeSeekBar = view.findViewById(R.id.eraser_size_seekbar)

        setupDrawingTools()
    }

    private fun setupDrawingTools() {
        // Initially hide all tool customization options
        penSizeSeekBar.visibility = View.GONE
        penColorPalette.visibility = View.GONE
        highlighterSizeSeekBar.visibility = View.GONE
        highlighterColorPalette.visibility = View.GONE
        eraserSizeSeekBar.visibility = View.GONE

        // Set up pen button
        penButton.setOnClickListener {
            drawingView.setDrawingMode(DrawingView.DrawingMode.PEN)

            // Toggle pen customization visibility
            val isPenCustomizationVisible = penSizeSeekBar.visibility == View.VISIBLE
            penSizeSeekBar.visibility = if (isPenCustomizationVisible) View.GONE else View.VISIBLE
            penColorPalette.visibility = if (isPenCustomizationVisible) View.GONE else View.VISIBLE

            // Hide other tool customizations
            highlighterSizeSeekBar.visibility = View.GONE
            highlighterColorPalette.visibility = View.GONE
            eraserSizeSeekBar.visibility = View.GONE
        }

        // Set up highlighter button
        highlighterButton.setOnClickListener {
            drawingView.setDrawingMode(DrawingView.DrawingMode.HIGHLIGHTER)

            // Toggle highlighter customization visibility
            val isHighlighterCustomizationVisible = highlighterSizeSeekBar.visibility == View.VISIBLE
            highlighterSizeSeekBar.visibility = if (isHighlighterCustomizationVisible) View.GONE else View.VISIBLE
            highlighterColorPalette.visibility = if (isHighlighterCustomizationVisible) View.GONE else View.VISIBLE

            // Hide other tool customizations
            penSizeSeekBar.visibility = View.GONE
            penColorPalette.visibility = View.GONE
            eraserSizeSeekBar.visibility = View.GONE
        }

        // Set up eraser button
        eraserButton.setOnClickListener {
            drawingView.setDrawingMode(DrawingView.DrawingMode.ERASER)

            // Toggle eraser customization visibility
            eraserSizeSeekBar.visibility = if (eraserSizeSeekBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            // Hide other tool customizations
            penSizeSeekBar.visibility = View.GONE
            penColorPalette.visibility = View.GONE
            highlighterSizeSeekBar.visibility = View.GONE
            highlighterColorPalette.visibility = View.GONE
        }

        // Set up clear button
        clearButton.setOnClickListener {
            // Show confirmation dialog before clearing
            showClearConfirmationDialog()
        }

        // Configure seek bars
        penSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setPenSize(progress + 1) // Add 1 to avoid zero size
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        highlighterSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setHighlighterSize(progress + 5) // Add 5 for wider highlighter
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        eraserSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setEraserSize(progress + 10) // Add 10 for wider eraser
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set up color selection
        setupColorPalettes()
    }

    private fun setupColorPalettes() {
        // Setup pen colors
        for (i in 0 until penColorPalette.childCount) {
            val colorView = penColorPalette.getChildAt(i)
            colorView.setOnClickListener {
                val color = (it.background as? ColorDrawable)?.color ?: Color.BLACK
                drawingView.setPenColor(color)
            }
        }

        // Setup highlighter colors
        for (i in 0 until highlighterColorPalette.childCount) {
            val colorView = highlighterColorPalette.getChildAt(i)
            colorView.setOnClickListener {
                val color = (it.background as? ColorDrawable)?.color ?: Color.YELLOW
                drawingView.setHighlighterColor(color)
            }
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Drawing")
            .setMessage("Are you sure you want to clear the entire drawing?")
            .setPositiveButton("Clear") { _, _ ->
                drawingView.clearDrawing()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}