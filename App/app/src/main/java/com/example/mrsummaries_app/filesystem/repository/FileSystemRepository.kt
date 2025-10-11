package com.example.mrsummaries_app.filesystem.repository

import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.Folder
import com.example.mrsummaries_app.filesystem.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository that handles file system operations
 */
class FileSystemRepository {
    // In-memory storage of all items
    private val _items = MutableStateFlow<List<FileSystemItem>>(emptyList())

    // Root folder is the starting point of the file system
    private val rootFolder = Folder(
        id = "root",
        name = "Notes",
        parentId = null
    )

    init {
        // Initialize with root folder and some demo content
        val initialItems = listOf(
            rootFolder,
            Folder(name = "University Notes", parentId = "root"),
            Folder(name = "Math", parentId = "root"),
            Note(name = "Quick Note", parentId = "root")
        )
        _items.value = initialItems
    }

    // Get all items as a Flow
    fun getAllItems(): Flow<List<FileSystemItem>> = _items

    // Get items in a specific folder
    fun getItemsInFolder(folderId: String): Flow<List<FileSystemItem>> {
        return _items.map { items ->
            items.filter { it.parentId == folderId }
        }
    }

    // Get folder by ID
    fun getFolder(folderId: String): Folder? {
        return _items.value.find { it.id == folderId && it is Folder } as Folder?
    }

    // Get note by ID
    fun getNote(noteId: String): Note? {
        return _items.value.find { it.id == noteId && it is Note } as Note?
    }

    // Get path to item (breadcrumb)
    fun getPathToItem(itemId: String): List<Folder> {
        val path = mutableListOf<Folder>()
        var currentItem = _items.value.find { it.id == itemId }

        while (currentItem != null && currentItem.parentId != null) {
            val parentFolder = getFolder(currentItem.parentId!!)
            if (parentFolder != null) {
                path.add(0, parentFolder)
                currentItem = parentFolder
            } else {
                break
            }
        }

        return path
    }

    // Add new folder
    fun addFolder(name: String, parentId: String?): Folder {
        val newFolder = Folder(
            name = name,
            parentId = parentId
        )
        _items.value = _items.value + newFolder
        return newFolder
    }

    // Add new note
    fun addNote(name: String, parentId: String?): Note {
        val newNote = Note(
            name = name,
            parentId = parentId
        )
        _items.value = _items.value + newNote
        return newNote
    }

    // Update item name
    fun renameItem(itemId: String, newName: String): Boolean {
        val updatedItems = _items.value.map { item ->
            if (item.id == itemId) {
                when (item) {
                    is Folder -> item.copy(name = newName, modifiedAt = System.currentTimeMillis())
                    is Note -> item.copy(name = newName, modifiedAt = System.currentTimeMillis())
                    else -> item
                }
            } else {
                item
            }
        }
        _items.value = updatedItems
        return true
    }

    // Move item to another folder
    fun moveItem(itemId: String, newParentId: String?): Boolean {
        // Prevent moving an item into itself or its descendant
        if (itemId == newParentId || isDescendantOf(newParentId, itemId)) {
            return false
        }

        val updatedItems = _items.value.map { item ->
            if (item.id == itemId) {
                when (item) {
                    is Folder -> item.copy(parentId = newParentId, modifiedAt = System.currentTimeMillis())
                    is Note -> item.copy(parentId = newParentId, modifiedAt = System.currentTimeMillis())
                    else -> item
                }
            } else {
                item
            }
        }
        _items.value = updatedItems
        return true
    }

    // Delete item and all its children (if folder)
    fun deleteItem(itemId: String): Boolean {
        // First, collect all IDs that need to be removed (item and its descendants)
        val idsToRemove = mutableSetOf(itemId)

        // If it's a folder, collect all descendant IDs
        collectDescendantIds(itemId, idsToRemove)

        // Remove all collected items
        _items.value = _items.value.filterNot { it.id in idsToRemove }
        return true
    }

    // Check if an item is a descendant of another
    private fun isDescendantOf(itemId: String?, potentialParentId: String): Boolean {
        if (itemId == null) return false
        if (itemId == potentialParentId) return true

        val item = _items.value.find { it.id == itemId } ?: return false
        return isDescendantOf(item.parentId, potentialParentId)
    }

    // Collect all descendant IDs recursively
    private fun collectDescendantIds(folderId: String, idsToRemove: MutableSet<String>) {
        val children = _items.value.filter { it.parentId == folderId }
        for (child in children) {
            idsToRemove.add(child.id)
            if (child is Folder) {
                collectDescendantIds(child.id, idsToRemove)
            }
        }
    }

    // Update note content
    fun updateNoteContent(noteId: String, content: String): Boolean {
        val updatedItems = _items.value.map { item ->
            if (item.id == noteId && item is Note) {
                item.copy(content = content, modifiedAt = System.currentTimeMillis())
            } else {
                item
            }
        }
        _items.value = updatedItems
        return true
    }
}