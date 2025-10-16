package com.example.mrsummaries_app.ui.note

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.databinding.ActivityNoteEditorBinding
import com.example.mrsummaries_app.databinding.DialogColorPickerBinding
import com.example.mrsummaries_app.storage.FileRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Note editor with swatch-based color picker.
 *
 * Changes:
 * - Tools bar placed on top (see layout).
 * - Color picker dialog shows actual color swatches so users don't need to read RGB.
 * - "More colors" opens the RGB dialog (kept for advanced users).
 * - New colors chosen via custom picker are added to recent colors.
 */
class NoteEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var repo: FileRepository
    private var notePath: String? = null
    private val gson = Gson()

    // prefs for recent colors
    private lateinit var prefs: SharedPreferences
    private val PREFS_NAME = "note_editor_prefs"
    private val KEY_RECENT_COLORS = "recent_note_colors" // stored as comma-separated ints
    private val RECENT_COLORS_MAX = 8

    // Local UI state
    private var activeTool: NoteCanvasView.Tool = NoteCanvasView.Tool.PEN
    private var configVisible = false
    private var currentSize = 6f
    private var currentColor = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = FileRepository(this)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val incoming = intent.getStringExtra("note_path") ?: ""
        notePath = if (incoming.isNotBlank()) repo.getAbsolutePath(incoming) else null

        val canvas = binding.noteCanvas

        // Initialize canvas defaults from our local state
        canvas.setColor(currentColor)
        canvas.setWidth(currentSize)

        // Wire tool buttons
        binding.btnToolPen.setOnClickListener { handleToolClick(NoteCanvasView.Tool.PEN) }
        binding.btnToolHighlighter.setOnClickListener { handleToolClick(NoteCanvasView.Tool.HIGHLIGHTER) }
        binding.btnToolEraser.setOnClickListener { handleToolClick(NoteCanvasView.Tool.ERASER) }
        binding.btnToolText.setOnClickListener { handleToolClick(NoteCanvasView.Tool.TEXT) }
        binding.btnToolLasso.setOnClickListener { handleToolClick(NoteCanvasView.Tool.LASSO) }

        binding.btnUndo.setOnClickListener { canvas.undo() }
        binding.btnRedo.setOnClickListener { canvas.redo() }

        // Close button
        binding.btnClose.setOnClickListener { finish() }

        // Config bar size adjustments
        binding.btnSizeMinus.setOnClickListener {
            currentSize = (currentSize - 2f).coerceAtLeast(1f)
            binding.tvSizeLabel.text = currentSize.toInt().toString()
            canvas.setWidth(currentSize)
        }
        binding.btnSizePlus.setOnClickListener {
            currentSize = (currentSize + 2f).coerceAtMost(96f)
            binding.tvSizeLabel.text = currentSize.toInt().toString()
            canvas.setWidth(currentSize)
        }

        // custom color button opens color picker dialog (swatches)
        binding.btnCustomColor.setOnClickListener { showColorPickerDialog() }

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

        // Canvas requests config UI when same tool re-clicked
        canvas.setOnToolConfigRequestListener { t ->
            if (t == NoteCanvasView.Tool.PEN || t == NoteCanvasView.Tool.HIGHLIGHTER || t == NoteCanvasView.Tool.TEXT) {
                toggleConfigBar()
            } else {
                hideConfigBar()
            }
        }

        // populate recent colors UI from prefs
        refreshRecentColorsUI()

        // ensure initial UI reflects default color
        updateToolIconTints()
    }

    private fun handleToolClick(t: NoteCanvasView.Tool) {
        val canvas = binding.noteCanvas
        if (activeTool == t) {
            // toggle config bar for configurable tools
            if (t == NoteCanvasView.Tool.PEN || t == NoteCanvasView.Tool.HIGHLIGHTER || t == NoteCanvasView.Tool.TEXT) {
                toggleConfigBar()
            } else {
                hideConfigBar()
            }
        } else {
            activeTool = t
            canvas.setTool(t)
            hideConfigBar()
            updateActiveToolVisual()
        }
    }

    private fun toggleConfigBar() {
        configVisible = !configVisible
        binding.configBar.visibility = if (configVisible) View.VISIBLE else View.GONE
        if (configVisible) {
            binding.tvSizeLabel.text = currentSize.toInt().toString()
        }
    }

    private fun hideConfigBar() {
        configVisible = false
        binding.configBar.visibility = View.GONE
    }

    private fun updateToolIconTints() {
        binding.btnToolPen.imageTintList = android.content.res.ColorStateList.valueOf(currentColor)
        binding.btnToolHighlighter.imageTintList = android.content.res.ColorStateList.valueOf(currentColor)
    }

    private fun updateActiveToolVisual() {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.selected_tool_bg)
        val defaultBg: android.graphics.drawable.Drawable? = null
        binding.btnToolPen.background = if (activeTool == NoteCanvasView.Tool.PEN) selectedBg else defaultBg
        binding.btnToolHighlighter.background = if (activeTool == NoteCanvasView.Tool.HIGHLIGHTER) selectedBg else defaultBg
        binding.btnToolEraser.background = if (activeTool == NoteCanvasView.Tool.ERASER) selectedBg else defaultBg
        binding.btnToolText.background = if (activeTool == NoteCanvasView.Tool.TEXT) selectedBg else defaultBg
        binding.btnToolLasso.background = if (activeTool == NoteCanvasView.Tool.LASSO) selectedBg else defaultBg
    }

    // ---- Recent colors management ----

    private fun loadRecentColors(): MutableList<Int> {
        val csv = prefs.getString(KEY_RECENT_COLORS, null)
        val out = mutableListOf<Int>()
        if (csv == null) {
            // default palette
            out.add(Color.BLACK)
            out.add(Color.YELLOW)
            out.add(Color.BLUE)
            out.add(Color.RED)
            return out
        }
        csv.split(",").mapNotNull {
            try {
                it.toInt()
            } catch (e: Exception) {
                null
            }
        }.forEach { out.add(it) }
        if (out.isEmpty()) {
            out.add(Color.BLACK)
            out.add(Color.YELLOW)
            out.add(Color.BLUE)
            out.add(Color.RED)
        }
        return out
    }

    private fun saveRecentColors(list: List<Int>) {
        val s = list.joinToString(",")
        prefs.edit().putString(KEY_RECENT_COLORS, s).apply()
    }

    private fun addRecentColor(color: Int) {
        val list = loadRecentColors()
        // remove existing occurrence
        list.removeAll { it == color }
        // add to front
        list.add(0, color)
        // trim
        val trimmed = list.take(RECENT_COLORS_MAX)
        saveRecentColors(trimmed)
        refreshRecentColorsUI()
    }

    private fun refreshRecentColorsUI() {
        val container = binding.recentColorsContainer
        container.removeAllViews()
        val colors = loadRecentColors()
        colors.forEach { color ->
            val btn = android.widget.ImageButton(this).apply {
                val lp = android.widget.LinearLayout.LayoutParams(64, 64)
                lp.marginEnd = 12
                layoutParams = lp
                setPadding(8)
                background = ContextCompat.getDrawable(this@NoteEditorActivity, R.drawable.swatch_bg)
                // show color via background tint or drawable
                val gd = GradientDrawable()
                gd.shape = GradientDrawable.OVAL
                gd.setColor(color)
                gd.setStroke(1, ContextCompat.getColor(this@NoteEditorActivity, android.R.color.darker_gray))
                background = gd
                setOnClickListener {
                    currentColor = color
                    binding.noteCanvas.setColor(color)
                    updateToolIconTints()
                }
            }
            container.addView(btn)
        }
    }

    // ---- Color picker dialog (swatches) ----

    private fun showColorPickerDialog() {
        val inflater = LayoutInflater.from(this)
        val dialogBinding = DialogColorPickerBinding.inflate(inflater)

        val dialogView = dialogBinding.root

        // map of swatch view ids to colors (must match dialog_color_picker.xml swatch backgrounds)
        val swatchMap = mapOf(
            dialogBinding.swatch1 to Color.parseColor("#000000"),
            dialogBinding.swatch2 to Color.parseColor("#FF0000"),
            dialogBinding.swatch3 to Color.parseColor("#00FF00"),
            dialogBinding.swatch4 to Color.parseColor("#0000FF"),
            dialogBinding.swatch5 to Color.parseColor("#FFFF00"),
            dialogBinding.swatch6 to Color.parseColor("#FF00FF"),
            dialogBinding.swatch7 to Color.parseColor("#00FFFF"),
            dialogBinding.swatch8 to Color.parseColor("#4CAF50"),
            dialogBinding.swatch9 to Color.parseColor("#2196F3"),
            dialogBinding.swatch10 to Color.parseColor("#9C27B0"),
            dialogBinding.swatch11 to Color.parseColor("#FF9800"),
            dialogBinding.swatch12 to Color.parseColor("#795548"),
            dialogBinding.swatch13 to Color.parseColor("#E91E63"),
            dialogBinding.swatch14 to Color.parseColor("#607D8B"),
            dialogBinding.swatch15 to Color.parseColor("#F44336"),
            dialogBinding.swatch16 to Color.parseColor("#03A9F4"),
            dialogBinding.swatch17 to Color.parseColor("#8BC34A"),
            dialogBinding.swatch18 to Color.parseColor("#FFC107")
        )

        val builder = AlertDialog.Builder(this)
            .setTitle("Pick a color")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)

        val dlg = builder.create()

        // set click handlers for swatches
        swatchMap.forEach { (view, color) ->
            view.setOnClickListener {
                currentColor = color
                binding.noteCanvas.setColor(color)
                addRecentColor(color)
                updateToolIconTints()
                dlg.dismiss()
            }
        }

        // More colors -> open RGB dialog for advanced users
        dialogBinding.btnMoreColors.setOnClickListener {
            dlg.dismiss()
            showRgbDialog()
        }

        dlg.show()
    }

    // RGB dialog kept for advanced users (optional)
    private fun showRgbDialog() {
        // reuse previous simple RGB dialog code if desired; keep it optional for advanced users
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialog_color_picker_rgb, null)
        val seekR = v.findViewById<android.widget.SeekBar>(R.id.seekR)
        val seekG = v.findViewById<android.widget.SeekBar>(R.id.seekG)
        val seekB = v.findViewById<android.widget.SeekBar>(R.id.seekB)
        val preview = v.findViewById<View>(R.id.preview)
        val tvRgb = v.findViewById<android.widget.TextView>(R.id.tvRgb)

        var r = Color.red(currentColor)
        var g = Color.green(currentColor)
        var b = Color.blue(currentColor)
        seekR.progress = r
        seekG.progress = g
        seekB.progress = b
        preview.setBackgroundColor(Color.rgb(r, g, b))
        tvRgb.text = "R:$r G:$g B:$b"

        val listener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                r = seekR.progress
                g = seekG.progress
                b = seekB.progress
                val c = Color.rgb(r, g, b)
                preview.setBackgroundColor(c)
                tvRgb.text = "R:$r G:$g B:$b"
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }
        seekR.setOnSeekBarChangeListener(listener)
        seekG.setOnSeekBarChangeListener(listener)
        seekB.setOnSeekBarChangeListener(listener)

        AlertDialog.Builder(this)
            .setTitle("Custom color")
            .setView(v)
            .setPositiveButton("OK") { _, _ ->
                val chosen = Color.rgb(seekR.progress, seekG.progress, seekB.progress)
                currentColor = chosen
                binding.noteCanvas.setColor(chosen)
                addRecentColor(chosen)
                updateToolIconTints()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}