package com.example.mrsummaries_app.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
 * Note editor with swatch-based color picker and improved toolbar/config interactions.
 *
 * Changes:
 * - Tools bar placed on top (see layout).
 * - Clicking the currently-selected tool:
 *     - PEN / HIGHLIGHTER / ERASER -> toggles a config bar with size adjustments
 *     - TEXT -> opens a text input (keyboard) to insert text into the canvas
 *     - LASSO -> no config bar
 * - Recent color swatches:
 *     - '+' (btnCustomColor) opens the color picker and adds chosen color to recent swatches
 *     - Clicking a swatch when it's already the active color opens a small "edit swatch" dialog:
 *         - Change color (opens RGB dialog preloaded)
 *         - Remove color from recent bar
 * - Selected tool background is a rounded drawable (R.drawable.selected_tool_bg) and is tinted
 *   at runtime to be 10% brighter in dark backgrounds or 10% darker in light backgrounds.
 *
 * This file implements the behaviors requested; no PR/issue is created here.
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

        // Size adjustments
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

        // '+' custom color button (adds new swatch)
        binding.btnCustomColor.setOnClickListener { showColorPickerDialog(addToRecents = true) }

        // Canvas requests config UI when same tool re-clicked
        canvas.setOnToolConfigRequestListener { t ->
            // For PEN / HIGHLIGHTER / ERASER -> open config bar
            // For TEXT -> open keyboard/input dialog
            // For others (LASSO) -> nothing
            when (t) {
                NoteCanvasView.Tool.PEN, NoteCanvasView.Tool.HIGHLIGHTER, NoteCanvasView.Tool.ERASER -> toggleConfigBar()
                NoteCanvasView.Tool.TEXT -> showTextInputDialog()
                else -> hideConfigBar()
            }
        }

        // populate recent colors UI from prefs
        refreshRecentColorsUI()

        // ensure initial UI reflects default color
        updateToolIconTints()
        updateActiveToolVisual()

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

    private fun handleToolClick(t: NoteCanvasView.Tool) {
        val canvas = binding.noteCanvas
        if (activeTool == t) {
            // clicking the already-active tool: open config or input as per tool
            when (t) {
                NoteCanvasView.Tool.PEN, NoteCanvasView.Tool.HIGHLIGHTER, NoteCanvasView.Tool.ERASER -> toggleConfigBar()
                NoteCanvasView.Tool.TEXT -> showTextInputDialog()
                else -> hideConfigBar()
            }
        } else {
            // switching to a new tool -> change tool and hide config
            activeTool = t
            canvas.setTool(t)
            hideConfigBar()
            // For tools that need immediate sizing applied (e.g., eraser), ensure canvas width updated
            canvas.setWidth(currentSize)
        }
        updateActiveToolVisual()
    }

    private fun toggleConfigBar() {
        configVisible = !configVisible
        binding.configBar.visibility = if (configVisible) View.VISIBLE else View.GONE
        if (configVisible) {
            binding.tvSizeLabel.text = currentSize.toInt().toString()
        }

        // Only show color controls when config bar is visible AND the active tool supports colors (pen/highlighter)
        val showColorControls = configVisible && (activeTool == NoteCanvasView.Tool.PEN || activeTool == NoteCanvasView.Tool.HIGHLIGHTER)
        binding.recentColorsContainer.visibility = if (showColorControls) View.VISIBLE else View.GONE
        binding.btnCustomColor.visibility = if (showColorControls) View.VISIBLE else View.GONE
    }

    private fun hideConfigBar() {
        configVisible = false
        binding.configBar.visibility = View.GONE
        binding.recentColorsContainer.visibility = View.GONE
        binding.btnCustomColor.visibility = View.GONE
    }

    private fun updateToolIconTints() {
        binding.btnToolPen.imageTintList = android.content.res.ColorStateList.valueOf(currentColor)
        binding.btnToolHighlighter.imageTintList = android.content.res.ColorStateList.valueOf(currentColor)
    }

    private fun updateActiveToolVisual() {
        // Load rounded drawable and mutate it so we can tint it safely
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.selected_tool_bg)?.mutate()

        // Try to resolve a reasonable background color to base our 10% adjustment on.
        // Prefer the activity root background color if it's a ColorDrawable; otherwise fall back to theme background attr.
        val rootBgColor = (binding.root.background as? android.graphics.drawable.ColorDrawable)?.color
            ?: resolveThemeColor(android.R.attr.colorBackground)

        // Decide whether to brighten or darken based on luminance.
        val makeBrighter = ColorUtils.calculateLuminance(rootBgColor) < 0.5
        val adjusted = if (makeBrighter) {
            ColorUtils.blendARGB(rootBgColor, Color.WHITE, 0.10f)
        } else {
            ColorUtils.blendARGB(rootBgColor, Color.BLACK, 0.10f)
        }
        selectedBg?.setTint(adjusted)

        val defaultBg: android.graphics.drawable.Drawable? = null

        binding.btnToolPen.background = if (activeTool == NoteCanvasView.Tool.PEN) selectedBg else defaultBg
        binding.btnToolHighlighter.background = if (activeTool == NoteCanvasView.Tool.HIGHLIGHTER) selectedBg else defaultBg
        binding.btnToolEraser.background = if (activeTool == NoteCanvasView.Tool.ERASER) selectedBg else defaultBg
        binding.btnToolText.background = if (activeTool == NoteCanvasView.Tool.TEXT) selectedBg else defaultBg
        binding.btnToolLasso.background = if (activeTool == NoteCanvasView.Tool.LASSO) selectedBg else defaultBg
    }

    // ---- Recent colors management and UI ----

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

    private fun replaceRecentColor(oldColor: Int, newColor: Int) {
        val list = loadRecentColors()
        val idx = list.indexOfFirst { it == oldColor }
        if (idx != -1) {
            list[idx] = newColor
            saveRecentColors(list)
            refreshRecentColorsUI()
        }
    }

    private fun removeRecentColor(color: Int) {
        val list = loadRecentColors()
        list.removeAll { it == color }
        saveRecentColors(list)
        refreshRecentColorsUI()
    }

    private fun refreshRecentColorsUI() {
        val container = binding.recentColorsContainer
        container.removeAllViews()
        val colors = loadRecentColors()
        colors.forEach { color ->
            val btn = android.widget.ImageButton(this).apply {
                val lp = GridLayout.LayoutParams().apply {
                    width = dpToPx(48)
                    height = dpToPx(48)
                    marginEnd = dpToPx(8)
                }
                layoutParams = lp
                setPadding(dpToPx(6))
                // show color via simple oval drawable
                val gd = GradientDrawable()
                gd.shape = GradientDrawable.OVAL
                gd.setColor(color)
                gd.setStroke(dpToPx(1), ContextCompat.getColor(this@NoteEditorActivity, android.R.color.darker_gray))
                background = gd

                // Click behavior:
                // - If clicking a swatch that is already active color -> open swatch edit dialog (change/remove)
                // - Otherwise set the current color and apply it
                setOnClickListener {
                    if (currentColor == color) {
                        // open edit dialog for this swatch
                        showSwatchEditDialog(color)
                    } else {
                        currentColor = color
                        binding.noteCanvas.setColor(color)
                        updateToolIconTints()
                    }
                }
            }
            container.addView(btn)
        }
    }

    // ---- Color picker dialog (swatches) ----
    // addToRecents: if true, chosen color will be added to recent colors as a new swatch.
    private fun showColorPickerDialog(addToRecents: Boolean = false, preselectColor: Int? = null, onColorChosen: ((Int) -> Unit)? = null) {
        val inflater = LayoutInflater.from(this)
        val dialogBinding = DialogColorPickerBinding.inflate(inflater)

        val dialogView = dialogBinding.root

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
                if (addToRecents) addRecentColor(color)
                updateToolIconTints()
                onColorChosen?.invoke(color)
                dlg.dismiss()
            }
        }

        // More colors -> open RGB dialog for advanced users
        dialogBinding.btnMoreColors.setOnClickListener {
            dlg.dismiss()
            showRgbDialog(preselectColor) { chosen ->
                currentColor = chosen
                binding.noteCanvas.setColor(chosen)
                if (addToRecents) addRecentColor(chosen)
                updateToolIconTints()
                onColorChosen?.invoke(chosen)
            }
        }

        // Preselect indicator (optional): if preselectColor matches any swatch, highlight it (not implemented in xml)
        dlg.show()
    }

    // RGB dialog kept for advanced users (optional).
    // If onChosen provided, call it with final color.
    private fun showRgbDialog(preselectColor: Int? = null, onChosen: ((Int) -> Unit)? = null) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialog_color_picker_rgb, null)
        val seekR = v.findViewById<android.widget.SeekBar>(R.id.seekR)
        val seekG = v.findViewById<android.widget.SeekBar>(R.id.seekG)
        val seekB = v.findViewById<android.widget.SeekBar>(R.id.seekB)
        val preview = v.findViewById<View>(R.id.preview)
        val tvRgb = v.findViewById<android.widget.TextView>(R.id.tvRgb)

        val initial = preselectColor ?: currentColor
        var r = Color.red(initial)
        var g = Color.green(initial)
        var b = Color.blue(initial)
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
                onChosen?.invoke(chosen)
                // default behavior: set current color and add to recents
                currentColor = chosen
                binding.noteCanvas.setColor(chosen)
                addRecentColor(chosen)
                updateToolIconTints()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // When a swatch that is already active is clicked, offer options to change/remove it
    private fun showSwatchEditDialog(color: Int) {
        val options = arrayOf("Change color", "Remove color", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Edit swatch")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Change color -> open RGB dialog preloaded with current swatch; replace after chosen
                        showRgbDialog(preselectColor = color) { chosen ->
                            replaceRecentColor(color, chosen)
                            // if the swatch being edited is the active color, update canvas color too
                            if (currentColor == color) {
                                currentColor = chosen
                                binding.noteCanvas.setColor(chosen)
                                updateToolIconTints()
                            }
                        }
                    }
                    1 -> {
                        // Remove color
                        removeRecentColor(color)
                        // If removed color was currently active, fall back to black
                        if (currentColor == color) {
                            currentColor = Color.BLACK
                            binding.noteCanvas.setColor(currentColor)
                            updateToolIconTints()
                        }
                    }
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    // ---- Text input handling ----
    // Show a small input dialog to type text; inserts text at canvas center for simplicity.
    private fun showTextInputDialog() {
        val editText = EditText(this)
        editText.hint = "Type text..."
        editText.setText("")

        val dlg = AlertDialog.Builder(this)
            .setTitle("Insert text")
            .setView(editText)
            .setPositiveButton("Insert") { _, _ ->
                val text = editText.text.toString().ifBlank { "Text" }
                binding.noteCanvas.addTextAtCenter(text)
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Show keyboard when dialog opens
        dlg.setOnShowListener {
            editText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        dlg.show()
    }

    // helper to resolve theme attribute color (fallback)
    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(this, tv.resourceId) else tv.data
        } else {
            Color.WHITE
        }
    }

    private fun dpToPx(dp: Int): Int {
        val metrics = resources.displayMetrics
        return (dp * metrics.density).toInt()
    }
}