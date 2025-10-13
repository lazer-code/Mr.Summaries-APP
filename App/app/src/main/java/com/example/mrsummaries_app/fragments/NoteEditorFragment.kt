package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.Note
import com.example.mrsummaries_app.storage.FileSystemManager
import com.example.mrsummaries_app.custom_views.DrawingView

class NoteEditorFragment : Fragment() {

    private lateinit var fileSystemManager: FileSystemManager
    private lateinit var noteId: String
    private lateinit var drawingView: DrawingView
    private lateinit var note: Note

    private var isToolbarExpanded = false

    companion object {
        private const val ARG_NOTE_ID = "note_id"

        fun newInstance(noteId: String): NoteEditorFragment {
            val fragment = NoteEditorFragment()
            val args = Bundle().apply {
                putString(ARG_NOTE_ID, noteId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            noteId = it.getString(ARG_NOTE_ID) ?: throw IllegalArgumentException("Note ID required")
        }

        fileSystemManager = FileSystemManager(requireContext())

        // Load the note data
        fileSystemManager.loadNoteContent(noteId)?.let {
            note = it
        } ?: throw IllegalStateException("Failed to load note")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_note_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupToolbars(view)

        // Load drawing data if available
        note.drawingData?.let {
            drawingView.loadDrawing(it)
        }
    }

    private fun setupViews(view: View) {
        drawingView = view.findViewById(R.id.drawingView)
    }

    private fun setupToolbars(view: View) {
        val penButton = view.findViewById<View>(R.id.penButton)
        val highlighterButton = view.findViewById<View>(R.id.highlighterButton)
        val eraserButton = view.findViewById<View>(R.id.eraserButton)

        val penToolbar = view.findViewById<View>(R.id.penToolbar)
        val highlighterToolbar = view.findViewById<View>(R.id.highlighterToolbar)
        val eraserToolbar = view.findViewById<View>(R.id.eraserToolbar)

        // Initially hide all toolbars
        penToolbar.visibility = View.GONE
        highlighterToolbar.visibility = View.GONE
        eraserToolbar.visibility = View.GONE

        penButton.setOnClickListener {
            if (penToolbar.visibility == View.VISIBLE) {
                penToolbar.visibility = View.GONE
            } else {
                // Hide other toolbars
                highlighterToolbar.visibility = View.GONE
                eraserToolbar.visibility = View.GONE
                // Show pen toolbar
                penToolbar.visibility = View.VISIBLE

                // Set pen tool active
                drawingView.setTool(DrawingView.Tool.PEN)
            }
        }

        highlighterButton.setOnClickListener {
            if (highlighterToolbar.visibility == View.VISIBLE) {
                highlighterToolbar.visibility = View.GONE
            } else {
                // Hide other toolbars
                penToolbar.visibility = View.GONE
                eraserToolbar.visibility = View.GONE
                // Show highlighter toolbar
                highlighterToolbar.visibility = View.VISIBLE

                // Set highlighter tool active
                drawingView.setTool(DrawingView.Tool.HIGHLIGHTER)
            }
        }

        eraserButton.setOnClickListener {
            if (eraserToolbar.visibility == View.VISIBLE) {
                eraserToolbar.visibility = View.GONE
            } else {
                // Hide other toolbars
                penToolbar.visibility = View.GONE
                highlighterToolbar.visibility = View.GONE
                // Show eraser toolbar
                eraserToolbar.visibility = View.VISIBLE

                // Set eraser tool active
                drawingView.setTool(DrawingView.Tool.ERASER)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveNote()
    }

    private fun saveNote() {
        // Save the drawing data
        note.drawingData = drawingView.getDrawingData()

        // Save the note to storage
        fileSystemManager.saveNoteContent(note)
    }
}