package com.example.mrsummaries_app.filesystem.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.Folder
import com.example.mrsummaries_app.filesystem.model.Note
import com.example.mrsummaries_app.filesystem.repository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FileSystemViewModel : ViewModel() {
    private val repository = FileSystemRepository()

    // Current folder ID being viewed
    private val _currentFolderId = MutableStateFlow<String>("root")
    val currentFolderId: StateFlow<String> = _currentFolderId

    // Currently selected item (for operations)
    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId

    // Track expanded state of folders
    val expandedFolders = mutableStateMapOf<String, Boolean>()

    // All items in the file system (for move operations and global search)
    val allItems: StateFlow<List<FileSystemItem>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Current items in the viewed folder - FIXED IMPLEMENTATION
    val currentFolderItems: StateFlow<List<FileSystemItem>> = combine(
        repository.getAllItems(),
        _currentFolderId
    ) { items, folderId ->
        items.filter { it.parentId == folderId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Current folder details - FIXED IMPLEMENTATION
    val currentFolder: StateFlow<Folder?> = combine(
        repository.getAllItems(),
        _currentFolderId
    ) { items, folderId ->
        items.find { it.id == folderId && it is Folder } as? Folder
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Path to current folder (breadcrumb) - FIXED IMPLEMENTATION
    val currentPath: StateFlow<List<Folder>> = combine(
        repository.getAllItems(),
        _currentFolderId
    ) { items, folderId ->
        buildPathToItem(items, folderId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Navigation - Change current folder
    fun navigateToFolder(folderId: String) {
        _currentFolderId.value = folderId
        _selectedItemId.value = null
    }

    // Selection - Select an item for operations
    fun selectItem(itemId: String?) {
        _selectedItemId.value = itemId
    }

    // Toggle folder expansion (for tree view)
    fun toggleFolderExpansion(folderId: String) {
        expandedFolders[folderId] = !(expandedFolders[folderId] ?: false)
    }

    // CRUD Operations - FIXED TO ENSURE REACTIVITY
    fun createFolder(name: String, parentId: String? = currentFolderId.value) {
        viewModelScope.launch {
            repository.addFolder(name, parentId)
        }
    }

    fun createNote(name: String, parentId: String? = currentFolderId.value) {
        viewModelScope.launch {
            repository.addNote(name, parentId)
        }
    }

    // Create note and return its ID for immediate navigation
    fun createNoteAndReturnId(name: String, parentId: String? = currentFolderId.value): String {
        return repository.addNote(name, parentId).id
    }

    fun renameItem(itemId: String, newName: String) {
        viewModelScope.launch {
            repository.renameItem(itemId, newName)
        }
    }

    fun moveItem(itemId: String, newParentId: String?) {
        viewModelScope.launch {
            repository.moveItem(itemId, newParentId)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)

            // If we're deleting the current folder, navigate to parent
            if (itemId == currentFolderId.value) {
                val currentFolder = repository.getFolder(currentFolderId.value)
                currentFolder?.parentId?.let { parentId ->
                    navigateToFolder(parentId)
                } ?: navigateToFolder("root")
            }

            // Clear selection if deleting selected item
            if (itemId == selectedItemId.value) {
                selectItem(null)
            }
        }
    }

    fun updateNoteContent(noteId: String, content: String) {
        viewModelScope.launch {
            repository.updateNoteContent(noteId, content)
        }
    }

    // Get note by ID
    fun getNote(noteId: String): Note? {
        return repository.getNote(noteId)
    }

    // Helper function to build path to item
    private fun buildPathToItem(items: List<FileSystemItem>, itemId: String): List<Folder> {
        val path = mutableListOf<Folder>()
        var currentItem = items.find { it.id == itemId }

        while (currentItem != null && currentItem.parentId != null) {
            val parentFolder = items.find { it.id == currentItem.parentId && it is Folder } as? Folder
            if (parentFolder != null) {
                path.add(0, parentFolder)
                currentItem = parentFolder
            } else {
                break
            }
        }

        return path
    }
}