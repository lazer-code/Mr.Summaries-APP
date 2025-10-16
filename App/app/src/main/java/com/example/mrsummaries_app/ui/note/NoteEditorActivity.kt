package com.example.mrsummaries_app.ui.note

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.example.mrsummaries_app.databinding.ActivityNoteEditorBinding
import com.example.mrsummaries_app.storage.FileRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Note editor — now uses FileRepository.resolveToFile/getAbsolutePath to accept both
 * relative and absolute incoming paths.
 */
class NoteEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var repo: FileRepository
    private var notePath: String? = null
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = FileRepository(this)

        val incoming = intent.getStringExtra("note_path") ?: ""
        // Convert incoming to absolute path if needed
        notePath = if (incoming.isNotBlank()) repo.getAbsolutePath(incoming) else null

        val canvas = binding.noteCanvas
        binding.btnToolPen.setOnClickListener { canvas.setTool(NoteCanvasView.Tool.PEN) }
        binding.btnToolHighlighter.setOnClickListener { canvas.setTool(NoteCanvasView.Tool.HIGHLIGHTER) }
        binding.btnToolEraser.setOnClickListener { canvas.setTool(NoteCanvasView.Tool.ERASER) }
        binding.btnToolText.setOnClickListener { canvas.addTextAtCenter("Text ${SystemClock.uptimeMillis()}") }
        binding.btnUndo.setOnClickListener { canvas.undo() }
        binding.btnRedo.setOnClickListener { canvas.redo() }

        // load note if exists
        notePath?.let {
            val json = repo.readNote(it)
            if (json.isNotEmpty()) {
                canvas.loadFromJson(json)
            }
        }

        // Save on changes
        canvas.setOnChangeListener {
            notePath?.let { path ->
                CoroutineScope(Dispatchers.IO).launch {
                    repo.saveNote(path, canvas.toJson())
                }
            }
        }
    }
}