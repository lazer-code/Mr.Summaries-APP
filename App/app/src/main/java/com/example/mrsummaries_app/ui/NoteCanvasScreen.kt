@file:OptIn(ExperimentalComposeUiApi::class)

package com.example.mrsummaries_app.ui

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.note.CostumePen
import com.example.mrsummaries_app.note.DrawingTool
import com.example.mrsummaries_app.note.SpatialIndex
import com.example.mrsummaries_app.note.StrokePath
import com.example.mrsummaries_app.note.StylusDrawingCanvas
import com.example.mrsummaries_app.note.PenPreferences
import com.example.mrsummaries_app.ui.persistence.NoteContentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * NoteCanvasScreen with infinite vertical pages via LazyColumn.
 * Shows HoverBar (pen/eraser/undo/redo/color) at the top of the editor.
 *
 * topBarInsetDp lets the caller push the bar down (e.g., when the side-menu is hidden and
 * the floating hamburger button is visible at the top-left).
 *
 * Scrolling requirement:
 * - Only fingers (TOOL_TYPE_FINGER) can scroll the list of pages.
 * - Stylus (TOOL_TYPE_STYLUS) will NOT scroll; it is used for drawing/erasing only.
 *   We achieve this by dynamically toggling LazyColumn.userScrollEnabled based on the active tool type.
 */
@Composable
fun NoteCanvasScreen(
    noteId: String,
    noteName: String,
    store: NoteStore,
    perfMinPointDistanceDp: Float = 2f,
    perfBatchSize: Int = 3,
    perfSpatialIndexEnabled: Boolean = true,
    topBarInsetDp: Dp = 8.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var eraseToggled by remember { mutableStateOf(false) }
    var spenPressed by remember { mutableStateOf(false) }
    val drawingTool = if (spenPressed || eraseToggled) DrawingTool.ERASE else DrawingTool.WRITE

    var currentColor by remember { mutableStateOf(Color.Blue) }
    var currentStrokeWidthDp by remember { mutableStateOf(6f) }
    var eraserSizeDp by remember { mutableStateOf(20f) }
    var costumePens by remember {
        mutableStateOf(
            listOf(
                CostumePen(Color.Blue, 6f),
                CostumePen(Color.Black, 5f),
                CostumePen(Color.Red, 6f),
                CostumePen(Color(0xFF4CAF50), 6f)
            )
        )
    }

    val selectedIndex = remember(currentColor, currentStrokeWidthDp, costumePens) {
        costumePens.indexOfFirst { it.color == currentColor && it.strokeWidthDp == currentStrokeWidthDp }
    }
    fun selectPenAt(index: Int) {
        if (index in costumePens.indices) {
            val pen = costumePens[index]
            currentColor = pen.color
            currentStrokeWidthDp = pen.strokeWidthDp
        }
    }

    LaunchedEffect(costumePens) {
        runCatching { PenPreferences.saveCostumePens(context, costumePens) }
    }
    LaunchedEffect(currentStrokeWidthDp, eraserSizeDp) {
        runCatching { PenPreferences.savePenSettings(context, currentStrokeWidthDp, eraserSizeDp) }
    }

    LaunchedEffect(noteId) {
        val loaded = runCatching { NoteContentStore.load(context, noteId) }.getOrNull()
        if (loaded != null) {
            store.pages = listOf(loaded)
            store.currentPath = emptyList()
            store.undonePaths = emptyList()
        }
    }

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val pageHeightDp = screenHeightDp.dp
    val initialCellSizePx = with(density) { max(eraserSizeDp.dp.toPx(), 48f) }
    val spatialIndex = remember { SpatialIndex(cellSizePx = initialCellSizePx) }

    LaunchedEffect(store.pages, eraserSizeDp) {
        spatialIndex.setCellSize(with(density) { max(eraserSizeDp.dp.toPx(), 48f) })
        spatialIndex.rebuild(store.pages.flatten())
    }

    fun persistNote() {
        scope.launch {
            withContext(Dispatchers.IO) {
                NoteContentStore.save(context, noteId, store.pages.flatten())
            }
        }
    }

    if (store.pages.isEmpty()) {
        store.pages = listOf(emptyList())
    }

    val nextPageTriggerDp = 64f
    val barHeightDp = 48.dp

    // Allow scrolling only when the active pointer is a finger
    var listScrollEnabled by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Editing bar (HoverBar) shown at the top; inset when necessary
        HoverBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topBarInsetDp),
            drawingTool = drawingTool,
            currentColor = currentColor,
            currentStrokeWidthDp = currentStrokeWidthDp,
            eraserSizeDp = eraserSizeDp,
            costumePens = costumePens,
            selectedIndex = selectedIndex,
            onToolChange = { tool -> eraseToggled = tool == DrawingTool.ERASE && !eraseToggled },
            onUndo = {
                val lastPage = store.pages.indexOfLast { it.isNotEmpty() }
                if (lastPage != -1) {
                    val newPages = store.pages.toMutableList()
                    val lastStroke = newPages[lastPage].last()
                    store.undonePaths = store.undonePaths + lastStroke
                    newPages[lastPage] = newPages[lastPage].dropLast(1)
                    store.pages = newPages
                    persistNote()
                }
            },
            onRedo = {
                if (store.undonePaths.isNotEmpty()) {
                    val stroke = store.undonePaths.last()
                    val newPages = store.pages.toMutableList()
                    newPages[newPages.lastIndex] = newPages.last() + stroke
                    store.pages = newPages
                    store.undonePaths = store.undonePaths.dropLast(1)
                    persistNote()
                }
            },
            onSelectIndex = { i -> selectPenAt(i) },
            onAddCostume = { /* optional color picker */ },
            onLongPressIndex = { _ -> /* optional edit preset */ },
            onPenWidthChange = { width -> currentStrokeWidthDp = width.coerceIn(1f, 24f) },
            onEraserSizeChange = { size -> eraserSizeDp = size.coerceIn(8f, 64f) },
            showPenSize = false,
            showEraserSize = false,
            setShowPenSize = {},
            setShowEraserSize = {}
        )

        // Pages list below the bar (account for inset + bar height)
        LazyColumn(
            userScrollEnabled = listScrollEnabled,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarInsetDp + barHeightDp)
                // Detect active tool type and allow scroll only for fingers
                .pointerInteropFilter { ev ->
                    // Determine if any finger or stylus pointers are present
                    var hasFinger = false
                    var hasStylus = false
                    for (i in 0 until ev.pointerCount) {
                        when (ev.getToolType(i)) {
                            MotionEvent.TOOL_TYPE_FINGER -> hasFinger = true
                            MotionEvent.TOOL_TYPE_STYLUS -> hasStylus = true
                        }
                    }
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_POINTER_DOWN,
                        MotionEvent.ACTION_HOVER_MOVE,
                        MotionEvent.ACTION_MOVE -> {
                            // Enable scrolling if a finger is involved; disable when only stylus is present
                            listScrollEnabled = hasFinger && !hasStylus
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_POINTER_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            // Reset to allow finger scrolling by default when no pointers remain
                            listScrollEnabled = true
                        }
                    }
                    // Do not consume so stylus events still reach the canvases for drawing
                    false
                },
            contentPadding = PaddingValues(0.dp)
        ) {
            itemsIndexed(store.pages, key = { index, _ -> index }) { pageIndex, pagePaths ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pageHeightDp)
                ) {
                    StylusDrawingCanvas(
                        modifier = Modifier.fillMaxSize(),
                        drawingTool = drawingTool,
                        paths = pagePaths,
                        currentPath = store.currentPath,
                        currentColor = currentColor,
                        currentStrokeWidthDp = currentStrokeWidthDp,
                        eraserSizeDp = eraserSizeDp,
                        minPointDistanceDp = perfMinPointDistanceDp,
                        batchSize = perfBatchSize,
                        spatialIndex = if (perfSpatialIndexEnabled) spatialIndex else null,
                        nextPageTriggerDp = nextPageTriggerDp,
                        onCurrentPathChange = { store.currentPath = it },
                        onPathAdded = { path ->
                            val newPages = store.pages.toMutableList()
                            val updatedPage = newPages.getOrNull(pageIndex)?.toMutableList() ?: mutableListOf()
                            updatedPage.add(StrokePath(path, currentColor, currentStrokeWidthDp))
                            newPages[pageIndex] = updatedPage
                            store.pages = newPages
                            store.undonePaths = emptyList()
                            persistNote()
                        },
                        onErasePath = { index ->
                            val newPages = store.pages.toMutableList()
                            val pageList = newPages.getOrNull(pageIndex)?.toMutableList() ?: return@StylusDrawingCanvas
                            if (index in pageList.indices) {
                                val removed = pageList[index]
                                pageList.removeAt(index)
                                newPages[pageIndex] = pageList
                                store.undonePaths = store.undonePaths + listOf(removed)
                                store.pages = newPages
                                persistNote()
                            }
                        },
                        onCreateNextPage = {
                            if (pageIndex == store.pages.lastIndex) {
                                store.pages = store.pages + listOf(emptyList())
                            }
                        },
                        onStylusButtonChange = { pressed -> spenPressed = pressed }
                    )
                }
            }
        }
    }
}