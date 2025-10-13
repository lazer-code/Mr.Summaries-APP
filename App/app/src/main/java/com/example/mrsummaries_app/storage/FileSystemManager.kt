package com.example.mrsummaries_app.storage

import android.content.Context
import android.util.Log
import com.example.mrsummaries_app.models.Folder
import com.example.mrsummaries_app.models.Note
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Date

class FileSystemManager(private val context: Context) {

    companion object {
        private const val TAG = "FileSystemManager"
        private const val ROOT_FOLDER_FILE = "root_folder.dat"
        private const val NOTES_DIRECTORY = "notes"
    }

    private lateinit var rootFolder: Folder

    init {
        loadRootFolder()
    }

    private fun loadRootFolder() {
        try {
            val file = File(context.filesDir, ROOT_FOLDER_FILE)
            if (file.exists()) {
                FileInputStream(file).use { fis ->
                    ObjectInputStream(fis).use { ois ->
                        rootFolder = ois.readObject() as Folder
                    }
                }
            } else {
                // Create a new root folder if none exists
                rootFolder = Folder(id = "root", name = "Root")
                saveRootFolder()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading root folder", e)
            rootFolder = Folder(id = "root", name = "Root")
            saveRootFolder()
        }
    }

    private fun saveRootFolder() {
        try {
            val file = File(context.filesDir, ROOT_FOLDER_FILE)
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(rootFolder)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving root folder", e)
        }
    }

    fun getRootFolder(): Folder {
        return rootFolder
    }

    fun createFolder(name: String, parentFolder: Folder): Folder {
        val newFolder = Folder(name = name, parentId = parentFolder.id)
        parentFolder.addSubfolder(newFolder)
        saveRootFolder()
        return newFolder
    }

    fun createNote(title: String, parentFolder: Folder): Note {
        val newNote = Note(title = title, folderId = parentFolder.id)
        parentFolder.addNote(newNote)
        saveRootFolder()
        saveNoteContent(newNote)
        return newNote
    }

    fun findFolder(folderId: String): Folder? {
        if (rootFolder.id == folderId) {
            return rootFolder
        }
        return findFolderRecursive(rootFolder, folderId)
    }

    private fun findFolderRecursive(currentFolder: Folder, folderId: String): Folder? {
        for (subfolder in currentFolder.subfolders) {
            if (subfolder.id == folderId) {
                return subfolder
            }
            val found = findFolderRecursive(subfolder, folderId)
            if (found != null) {
                return found
            }
        }
        return null
    }

    fun findNote(noteId: String): Note? {
        return findNoteRecursive(rootFolder, noteId)
    }

    private fun findNoteRecursive(currentFolder: Folder, noteId: String): Note? {
        for (note in currentFolder.notes) {
            if (note.id == noteId) {
                return note
            }
        }

        for (subfolder in currentFolder.subfolders) {
            val found = findNoteRecursive(subfolder, noteId)
            if (found != null) {
                return found
            }
        }

        return null
    }

    fun saveNoteContent(note: Note) {
        try {
            val notesDir = File(context.filesDir, NOTES_DIRECTORY)
            if (!notesDir.exists()) {
                notesDir.mkdirs()
            }

            val noteFile = File(notesDir, "${note.id}.dat")
            FileOutputStream(noteFile).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(note)
                }
            }

            // Update the modified date
            note.lastModified = Date()
            saveRootFolder()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note content", e)
        }
    }

    fun loadNoteContent(noteId: String): Note? {
        try {
            val notesDir = File(context.filesDir, NOTES_DIRECTORY)
            val noteFile = File(notesDir, "$noteId.dat")

            if (noteFile.exists()) {
                FileInputStream(noteFile).use { fis ->
                    ObjectInputStream(fis).use { ois ->
                        return ois.readObject() as Note
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading note content", e)
        }

        return null
    }
}