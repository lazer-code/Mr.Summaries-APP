package com.example.mrsummaries_app.notepad.note_components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UndoRedoState {
    var undoCount by mutableStateOf(0)
        private set

    var redoCount by mutableStateOf(0)
        private set

    fun undo() {
        undoCount++
    }

    fun redo() {
        redoCount++
    }

    fun onUndoPerformed() {
        if (undoCount > 0) {
            undoCount--
        }
    }

    fun onRedoPerformed() {
        if (redoCount > 0) {
            redoCount--
        }
    }

    fun reset() {
        undoCount = 0
        redoCount = 0
    }

    fun clear() {
        reset()
    }
}