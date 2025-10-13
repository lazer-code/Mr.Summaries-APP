package com.example.mrsummaries_app.models

import java.io.Serializable
import java.util.UUID

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var parentId: String? = null,
    val subfolders: MutableList<Folder> = mutableListOf(),
    val notes: MutableList<Note> = mutableListOf()
) : Serializable {

    fun addSubfolder(folder: Folder) {
        folder.parentId = this.id
        subfolders.add(folder)
    }

    fun addNote(note: Note) {
        note.folderId = this.id
        notes.add(note)
    }

    fun removeNote(noteId: String): Boolean {
        val noteToRemove = notes.find { it.id == noteId }
        return notes.remove(noteToRemove)
    }

    fun removeSubfolder(folderId: String): Boolean {
        val folderToRemove = subfolders.find { it.id == folderId }
        return subfolders.remove(folderToRemove)
    }
}