package com.example.mrsummaries_app.activities

import android.R
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.example.mrsummaries_app.custom_views.DrawingView
import com.example.mrsummaries_app.storage.FileSystemManager
import java.util.Date

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var noteTitleEditText: EditText
    private lateinit var toolsBottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var penSizeSeekBar: SeekBar
    private lateinit var highlighterSizeSeekBar: SeekBar
    private lateinit var eraserSizeSeekBar: SeekBar

    private var noteId: String? = null
    private var folderId: String? = null
    private var currentTool = DrawingView.DrawingMode.PEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        // Get the note ID from intent
        noteId = intent.getStringExtra("note_id")
        folderId = intent.getStringExtra("folder_id")

        // Initialize file system manager
        fileSystemManager = FileSystemManager(this)

        // Set up the UI
        setupUI()

        // Load note content if editing an existing note
        if (noteId != null) {
            loadNote()
        }
    }

    private fun setupUI() {
        // Set up the toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (noteId != null) "Edit Note" else "New Note"

        // Find views
        drawingView = findViewById(R.id.drawing_view)
        noteTitleEditText = findViewById(R.id.note_title)
        toolsBottomSheet = findViewById(R.id.tools_bottom_sheet)

        // Set up tools bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(toolsBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Set up tool buttons
        findViewById<View>(R.id.pen_button).setOnClickListener {
            setTool(DrawingView.DrawingMode.PEN)
            showToolOptions(R.id.pen_options)
        }

        findViewById<View>(R.id.highlighter_button).setOnClickListener {
            setTool(DrawingView.DrawingMode.HIGHLIGHTER)
            showToolOptions(R.id.highlighter_options)
        }

        findViewById<View>(R.id.eraser_button).setOnClickListener {
            setTool(DrawingView.DrawingMode.ERASER)
            showToolOptions(R.id.eraser_options)
        }

        findViewById<View>(R.id.text_button).setOnClickListener {
            setTool(DrawingView.DrawingMode.TEXT)
            showToolOptions(R.id.text_options)
        }

        findViewById<View>(R.id.lasso_button).setOnClickListener {
            setTool(DrawingView.DrawingMode.LASSO)
            hideAllToolOptions()
        }

        findViewById<View>(R.id.clear_button).setOnClickListener {
            showClearConfirmationDialog()
        }

        // Set up seekbars
        penSizeSeekBar = findViewById(R.id.pen_size_seekbar)
        penSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setPenSize(progress + 1) // Add 1 to avoid zero size
                updateSizePreview(findViewById(R.id.pen_size_preview), progress + 1)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        highlighterSizeSeekBar = findViewById(R.id.highlighter_size_seekbar)
        highlighterSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setHighlighterSize(progress + 5) // Add 5 for wider highlighter
                updateSizePreview(findViewById(R.id.highlighter_size_preview), progress + 5)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        eraserSizeSeekBar = findViewById(R.id.eraser_size_seekbar)
        eraserSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                drawingView.setEraserSize(progress + 10) // Add 10 for wider eraser
                updateSizePreview(findViewById(R.id.eraser_size_preview), progress + 10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set up color selection
        setupColorPalettes()

        // Set up pen styles
        findViewById<View>(R.id.pen_style_normal).setOnClickListener {
            drawingView.setPenStyle(DrawingView.PenStyle.NORMAL)
        }

        findViewById<View>(R.id.pen_style_calligraphic).setOnClickListener {
            drawingView.setPenStyle(DrawingView.PenStyle.CALLIGRAPHIC)
        }

        findViewById<View>(R.id.pen_style_fountain).setOnClickListener {
            drawingView.setPenStyle(DrawingView.PenStyle.FOUNTAIN)
        }

        findViewById<View>(R.id.pen_style_marker).setOnClickListener {
            drawingView.setPenStyle(DrawingView.PenStyle.MARKER)
        }

        // Initialize size previews
        updateSizePreview(findViewById(R.id.pen_size_preview), penSizeSeekBar.progress + 1)
        updateSizePreview(findViewById(R.id.highlighter_size_preview), highlighterSizeSeekBar.progress + 5)
        updateSizePreview(findViewById(R.id.eraser_size_preview), eraserSizeSeekBar.progress + 10)
    }

    private fun setTool(tool: DrawingView.DrawingMode) {
        currentTool = tool
        drawingView.setDrawingMode(tool)

        // Show the bottom sheet if it's hidden
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun showToolOptions(optionsId: Int) {
        // Hide all tool options first
        hideAllToolOptions()

        // Show the selected tool options
        findViewById<View>(optionsId).visibility = View.VISIBLE
    }

    private fun hideAllToolOptions() {
        findViewById<View>(R.id.pen_options).visibility = View.GONE
        findViewById<View>(R.id.highlighter_options).visibility = View.GONE
        findViewById<View>(R.id.eraser_options).visibility = View.GONE
        findViewById<View>(R.id.text_options).visibility = View.GONE
    }

    private fun updateSizePreview(preview: View, size: Int) {
        val params = preview.layoutParams
        params.width = size
        params.height = size
        preview.layoutParams = params
    }

    private fun setupColorPalettes() {
        // Set up pen color palette
        val penColorPalette = findViewById<LinearLayout>(R.id.pen_color_palette)
        for (i in 0 until penColorPalette.childCount) {
            val colorView = penColorPalette.getChildAt(i)
            colorView.setOnClickListener {
                val color = (it.background as? ColorDrawable)?.color ?: Color.BLACK
                drawingView.setPenColor(color)
            }
        }

        // Set up highlighter color palette
        val highlighterColorPalette = findViewById<LinearLayout>(R.id.highlighter_color_palette)
        for (i in 0 until highlighterColorPalette.childCount) {
            val colorView = highlighterColorPalette.getChildAt(i)
            colorView.setOnClickListener {
                val color = (it.background as? ColorDrawable)?.color ?: Color.YELLOW
                drawingView.setHighlighterColor(color)
            }
        }
    }

    private fun loadNote() {
        // Load the note from storage
        val note = fileSystemManager.loadNoteContent(noteId!!)
        if (note != null) {
            noteTitleEditText.setText(note.title)

            // Load drawing data if available
            note.drawingData?.let {
                drawingView.loadDrawing(it)
            }
        }
    }

    private fun saveNote() {
        val title = noteTitleEditText.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title for the note", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the drawing data
        val drawingData = drawingView.getDrawingData()

        if (noteId != null) {
            // Update existing note
            val note = fileSystemManager.findNote(noteId!!)
            if (note != null) {
                note.title = title
                note.lastModified = Date()
                note.drawingData = drawingData
                fileSystemManager.saveNoteContent(note)
            }
        } else if (folderId != null) {
            // Create new note
            val folder = fileSystemManager.findFolder(folderId!!)
            if (folder != null) {
                val newNote = fileSystemManager.createNote(title, folder)
                newNote.drawingData = drawingData
                fileSystemManager.saveNoteContent(newNote)

                // Update the note ID
                noteId = newNote.id
            }
        }

        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Drawing")
            .setMessage("Are you sure you want to clear the entire drawing?")
            .setPositiveButton("Clear") { _, _ ->
                drawingView.clearDrawing()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_note_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_undo -> {
                drawingView.undo()
                true
            }
            R.id.action_redo -> {
                drawingView.redo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            // Ask to save before closing
            AlertDialog.Builder(this)
                .setTitle("Save Note")
                .setMessage("Do you want to save changes to this note?")
                .setPositiveButton("Save") { _, _ ->
                    saveNote()
                    super.onBackPressed()
                }
                .setNegativeButton("Discard") { _, _ ->
                    super.onBackPressed()
                }
                .show()
        }
    }
}