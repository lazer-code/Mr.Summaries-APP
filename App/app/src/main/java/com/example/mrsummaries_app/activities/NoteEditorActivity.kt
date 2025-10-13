package com.example.mrsummaries_app.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.databinding.ActivityNoteEditorBinding
import com.example.mrsummaries_app.drawing.DrawingMode
import com.example.mrsummaries_app.drawing.PenStyle
import com.example.mrsummaries_app.models.Note
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.text.SimpleDateFormat
import java.util.*

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var toolsBottomSheetBehavior: BottomSheetBehavior<View>

    private var currentNote: Note? = null

    // Default settings
    private var penSize = 5f
    private var highlighterSize = 10f
    private var eraserSize = 20f
    private var currentPenStyle = PenStyle.NORMAL
    private var isDrawingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get note from intent or create new note
        val noteId = intent.getStringExtra("note_id")
        currentNote = if (noteId != null) {
            // In a real app, fetch from database
            Note(
                id = noteId,
                title = intent.getStringExtra("note_title") ?: "New Note",
                content = intent.getStringExtra("note_content") ?: "",
                subject = intent.getStringExtra("note_subject") ?: "",
                dateCreated = Date(intent.getLongExtra("note_date_created", System.currentTimeMillis())),
                dateModified = Date(intent.getLongExtra("note_date_modified", System.currentTimeMillis()))
            )
        } else {
            Note(
                id = UUID.randomUUID().toString(),
                title = "New Note",
                content = "",
                dateCreated = Date(),
                dateModified = Date()
            )
        }

        // Setup the note
        setupNoteContent()

        // Initialize bottom sheet for drawing tools - use the included layout's root view
        // binding.toolsBottomSheet is the generated binding for the included layout (layout_tools_bottom_sheet.xml)
        toolsBottomSheetBehavior = BottomSheetBehavior.from(binding.toolsBottomSheet.root)
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Setup drawing tools
        setupDrawingTools()

        // Setup text mode tools
        setupTextModeTools()

        // Register a back-press callback to replace onBackPressed() usage and avoid deprecation warning.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    private fun setupNoteContent() {
        currentNote?.let { note ->
            binding.etNoteTitle.setText(note.title)
            binding.etNoteContent.setText(note.content)
            binding.spinnerSubject.setSelection(getSubjectPosition(note.subject))

            // Set the date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.tvDateModified.text = "Last modified: ${dateFormat.format(note.dateModified)}"

            // Title change listener
            binding.etNoteTitle.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    note.title = s.toString()
                    note.dateModified = Date()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Content change listener
            binding.etNoteContent.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    note.content = s.toString()
                    note.dateModified = Date()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun getSubjectPosition(subject: String): Int {
        val subjects = resources.getStringArray(R.array.subjects)
        return subjects.indexOf(subject).coerceAtLeast(0)
    }

    private fun setupDrawingTools() {
        // Button to toggle between text and drawing mode
        binding.btnToggleDrawing.setOnClickListener { toggleDrawingMode() }

        // Pen button
        binding.btnPen.setOnClickListener {
            setActiveTool(DrawingMode.PENCIL)
            binding.drawingView.setDrawingMode(DrawingMode.PENCIL)
            binding.drawingView.setStrokeWidth(penSize)
            showPenOptions()
        }

        // Highlighter button
        binding.btnHighlighter.setOnClickListener {
            setActiveTool(DrawingMode.HIGHLIGHTER)
            binding.drawingView.setDrawingMode(DrawingMode.HIGHLIGHTER)
            binding.drawingView.setStrokeWidth(highlighterSize)
            showHighlighterOptions()
        }

        // Eraser button
        binding.btnEraser.setOnClickListener {
            setActiveTool(DrawingMode.ERASER)
            binding.drawingView.setDrawingMode(DrawingMode.ERASER)
            binding.drawingView.setStrokeWidth(eraserSize)
            showEraserOptions()
        }

        // Text insertion button
        binding.btnText.setOnClickListener {
            setActiveTool(DrawingMode.TEXT)
            binding.drawingView.setDrawingMode(DrawingMode.TEXT)
            showTextOptions()
        }

        // Lasso selection tool
        binding.btnLasso.setOnClickListener {
            setActiveTool(DrawingMode.LASSO)
            binding.drawingView.setDrawingMode(DrawingMode.LASSO)
            hideToolOptions()
        }

        // Undo / Redo / Clear
        binding.btnUndo.setOnClickListener { binding.drawingView.undo() }
        binding.btnRedo.setOnClickListener { binding.drawingView.redo() }
        binding.btnClear.setOnClickListener { showClearConfirmationDialog() }

        // NOTE: controls inside included bottom sheet must be accessed through binding.toolsBottomSheet

        // Setup pen size SeekBar
        binding.toolsBottomSheet.seekBarPenSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                penSize = progress.toFloat().coerceAtLeast(1f)
                updateSizePreview(binding.toolsBottomSheet.viewPenSizePreview, penSize)

                // adjust method name if your DrawingView uses a different getter
                if (binding.drawingView.getCurrentDrawingMode() == DrawingMode.PENCIL) {
                    binding.drawingView.setStrokeWidth(penSize)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Highlighter size SeekBar
        binding.toolsBottomSheet.seekBarHighlighterSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                highlighterSize = progress.toFloat().coerceAtLeast(1f)
                updateSizePreview(binding.toolsBottomSheet.viewHighlighterSizePreview, highlighterSize)

                if (binding.drawingView.getCurrentDrawingMode() == DrawingMode.HIGHLIGHTER) {
                    binding.drawingView.setStrokeWidth(highlighterSize)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Eraser size SeekBar
        binding.toolsBottomSheet.seekBarEraserSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                eraserSize = progress.toFloat().coerceAtLeast(1f)
                updateSizePreview(binding.toolsBottomSheet.viewEraserSizePreview, eraserSize)

                if (binding.drawingView.getCurrentDrawingMode() == DrawingMode.ERASER) {
                    binding.drawingView.setStrokeWidth(eraserSize)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Pen style buttons
        setupPenStyleButtons()

        // Color buttons
        setupColorButtons()

        // Initialize seekbars/previews
        binding.toolsBottomSheet.seekBarPenSize.progress = penSize.toInt()
        binding.toolsBottomSheet.seekBarHighlighterSize.progress = highlighterSize.toInt()
        binding.toolsBottomSheet.seekBarEraserSize.progress = eraserSize.toInt()
        updatePenStyleSelection()
    }

    private fun setupPenStyleButtons() {
        val tb = binding.toolsBottomSheet
        tb.btnNormalPen.setOnClickListener {
            currentPenStyle = PenStyle.NORMAL
            binding.drawingView.setPenStyle(PenStyle.NORMAL)
            updatePenStyleSelection()
        }
        tb.btnCalligraphicPen.setOnClickListener {
            currentPenStyle = PenStyle.CALLIGRAPHIC
            binding.drawingView.setPenStyle(PenStyle.CALLIGRAPHIC)
            updatePenStyleSelection()
        }
        tb.btnFountainPen.setOnClickListener {
            currentPenStyle = PenStyle.FOUNTAIN
            binding.drawingView.setPenStyle(PenStyle.FOUNTAIN)
            updatePenStyleSelection()
        }
        tb.btnMarkerPen.setOnClickListener {
            currentPenStyle = PenStyle.MARKER
            binding.drawingView.setPenStyle(PenStyle.MARKER)
            updatePenStyleSelection()
        }
    }

    private fun updatePenStyleSelection() {
        val tb = binding.toolsBottomSheet
        tb.btnNormalPen.alpha = if (currentPenStyle == PenStyle.NORMAL) 1.0f else 0.5f
        tb.btnCalligraphicPen.alpha = if (currentPenStyle == PenStyle.CALLIGRAPHIC) 1.0f else 0.5f
        tb.btnFountainPen.alpha = if (currentPenStyle == PenStyle.FOUNTAIN) 1.0f else 0.5f
        tb.btnMarkerPen.alpha = if (currentPenStyle == PenStyle.MARKER) 1.0f else 0.5f
    }

    private fun setupColorButtons() {
        val tb = binding.toolsBottomSheet
        val colorButtons: Map<View, Int> = mapOf(
            tb.btnColorBlack to Color.BLACK,
            tb.btnColorBlue to Color.BLUE,
            tb.btnColorRed to Color.RED,
            tb.btnColorGreen to Color.GREEN,
            tb.btnColorYellow to Color.YELLOW
        )

        colorButtons.forEach { (button, color) ->
            button.setBackgroundColor(color)
            button.setOnClickListener {
                binding.drawingView.setColor(color)
                updateColorSelection(button)
            }
        }

        // initial selection
        binding.drawingView.setColor(Color.BLACK)
        updateColorSelection(tb.btnColorBlack)
    }

    private fun updateColorSelection(selectedButton: View) {
        val tb = binding.toolsBottomSheet
        val colorButtons = listOf(
            tb.btnColorBlack,
            tb.btnColorBlue,
            tb.btnColorRed,
            tb.btnColorGreen,
            tb.btnColorYellow
        )
        colorButtons.forEach { btn ->
            btn.alpha = if (btn == selectedButton) 1.0f else 0.5f
        }
    }

    private fun setupTextModeTools() {
        val tb = binding.toolsBottomSheet
        tb.btnAddText.setOnClickListener {
            val text = tb.etTextInput.text.toString()
            if (text.isNotEmpty()) {
                val centerX = binding.drawingView.width / 2f
                val centerY = binding.drawingView.height / 2f
                binding.drawingView.addText(text, centerX, centerY)
                tb.etTextInput.text?.clear()
                toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSizePreview(view: View, size: Float) {
        val params = view.layoutParams
        val sizePx = dpToPx(size)
        params.width = sizePx
        params.height = sizePx
        view.layoutParams = params
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun toggleDrawingMode() {
        isDrawingMode = !isDrawingMode
        if (isDrawingMode) {
            binding.layoutTextMode.visibility = View.GONE
            binding.layoutDrawingMode.visibility = View.VISIBLE
            binding.btnToggleDrawing.setImageResource(R.drawable.ic_text_mode)
        } else {
            binding.layoutTextMode.visibility = View.VISIBLE
            binding.layoutDrawingMode.visibility = View.GONE
            binding.btnToggleDrawing.setImageResource(R.drawable.ic_draw)
            toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setActiveTool(mode: DrawingMode) {
        val tools = mapOf(
            DrawingMode.PENCIL to binding.btnPen,
            DrawingMode.HIGHLIGHTER to binding.btnHighlighter,
            DrawingMode.ERASER to binding.btnEraser,
            DrawingMode.TEXT to binding.btnText,
            DrawingMode.LASSO to binding.btnLasso
        )
        tools.forEach { (toolMode, button) ->
            button.alpha = if (toolMode == mode) 1.0f else 0.5f
        }
    }

    private fun showPenOptions() {
        val tb = binding.toolsBottomSheet
        tb.layoutPenOptions.visibility = View.VISIBLE
        tb.layoutHighlighterOptions.visibility = View.GONE
        tb.layoutEraserOptions.visibility = View.GONE
        tb.layoutTextOptions.visibility = View.GONE
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showHighlighterOptions() {
        val tb = binding.toolsBottomSheet
        tb.layoutPenOptions.visibility = View.GONE
        tb.layoutHighlighterOptions.visibility = View.VISIBLE
        tb.layoutEraserOptions.visibility = View.GONE
        tb.layoutTextOptions.visibility = View.GONE
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showEraserOptions() {
        val tb = binding.toolsBottomSheet
        tb.layoutPenOptions.visibility = View.GONE
        tb.layoutHighlighterOptions.visibility = View.GONE
        tb.layoutEraserOptions.visibility = View.VISIBLE
        tb.layoutTextOptions.visibility = View.GONE
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showTextOptions() {
        val tb = binding.toolsBottomSheet
        tb.layoutPenOptions.visibility = View.GONE
        tb.layoutHighlighterOptions.visibility = View.GONE
        tb.layoutEraserOptions.visibility = View.GONE
        tb.layoutTextOptions.visibility = View.VISIBLE
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideToolOptions() {
        toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Drawing")
            .setMessage("Are you sure you want to clear the entire drawing?")
            .setPositiveButton("Clear") { _, _ -> binding.drawingView.clearDrawing() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Use the back-press dispatcher so the same confirmation logic runs
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_share -> {
                shareNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleBackNavigation() {
        // If the bottom sheet is expanded, collapse it
        if (toolsBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            toolsBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }

        // Otherwise, prompt to save before exiting
        AlertDialog.Builder(this)
            .setTitle("Save Note")
            .setMessage("Do you want to save changes before exiting?")
            .setPositiveButton("Save") { _, _ ->
                saveNote()
                finish()
            }
            .setNegativeButton("Discard") { _, _ ->
                finish()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun saveNote() {
        currentNote?.let { note ->
            note.title = binding.etNoteTitle.text.toString()
            note.content = binding.etNoteContent.text.toString()
            note.subject = binding.spinnerSubject.selectedItem.toString()
            note.dateModified = Date()
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.tvDateModified.text = "Last modified: ${dateFormat.format(note.dateModified)}"
        }
    }

    private fun shareNote() {
        currentNote?.let { note ->
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TITLE, note.title)
                putExtra(android.content.Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
        }
    }
}