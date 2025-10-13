package com.example.mrsummaries_app.filesystem.repository

import android.content.Context
import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.FileSystemItemType
import com.example.mrsummaries_app.filesystem.model.Folder
import com.example.mrsummaries_app.filesystem.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Repository that handles file system operations and persists to disk.
 *
 * Persistence:
 * - Single JSON file in internal storage: filesDir/filesystem.json
 * - Only fields that actually exist in the models are saved/loaded.
 *   For Note we persist: id, name, parentId, createdAt, modifiedAt, content.
 *   For Folder we persist: id, name, parentId, createdAt, modifiedAt, color, isExpanded.
 * - If the file is missing or invalid we bootstrap a single Root folder (id="root").
 */
class FileSystemRepository(
    private val context: Context
) {
    // In-memory state of all items
    private val _items = MutableStateFlow<List<FileSystemItem>>(emptyList())

    // Storage file
    private val storageFile by lazy { File(context.filesDir, "filesystem.json") }

    init {
        val loaded = loadFromDisk()
        if (loaded.isNullOrEmpty()) {
            // Bootstrap minimal structure with Root
            _items.value = listOf(
                Folder(
                    id = "root",
                    name = "Root",
                    parentId = null
                )
            )
            persist()
        } else {
            _items.value = loaded
            // Ensure Root exists and is valid
            if (_items.value.none { it.id == "root" && it is Folder && it.parentId == null }) {
                _items.value = listOf(
                    Folder(id = "root", name = "Root", parentId = null)
                ) + _items.value
                persist()
            }
        }
    }

    // Read APIs
    fun getAllItems(): Flow<List<FileSystemItem>> = _items

    fun getItemsInFolder(folderId: String): Flow<List<FileSystemItem>> {
        return _items.map { items -> items.filter { it.parentId == folderId } }
    }

    fun getFolder(folderId: String): Folder? {
        return _items.value.find { it.id == folderId && it is Folder } as Folder?
    }

    fun getNote(noteId: String): Note? {
        return _items.value.find { it.id == noteId && it is Note } as Note?
    }

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

    // Mutations
    fun addFolder(name: String, parentId: String?): Folder {
        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId
        )
        _items.value = _items.value + newFolder
        persist()
        return newFolder
    }

    fun addNote(name: String, parentId: String?): Note {
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId
        )
        _items.value = _items.value + newNote
        persist()
        return newNote
    }

    fun renameItem(itemId: String, newName: String): Boolean {
        var changed = false
        val updated = _items.value.map { item ->
            if (item.id == itemId) {
                changed = true
                when (item) {
                    is Folder -> item.copy(name = newName, modifiedAt = System.currentTimeMillis())
                    is Note -> item.copy(name = newName, modifiedAt = System.currentTimeMillis())
                    else -> item
                }
            } else item
        }
        if (changed) {
            _items.value = updated
            persist()
        }
        return changed
    }

    fun moveItem(itemId: String, newParentId: String?): Boolean {
        // Prevent moving into itself or its descendants
        if (itemId == newParentId || isDescendantOf(newParentId, itemId)) return false

        var changed = false
        val updated = _items.value.map { item ->
            if (item.id == itemId) {
                changed = true
                when (item) {
                    is Folder -> item.copy(parentId = newParentId, modifiedAt = System.currentTimeMillis())
                    is Note -> item.copy(parentId = newParentId, modifiedAt = System.currentTimeMillis())
                    else -> item
                }
            } else item
        }
        if (changed) {
            _items.value = updated
            persist()
        }
        return changed
    }

    fun deleteItem(itemId: String): Boolean {
        if (itemId == "root") return false // never delete root

        val toRemove = mutableSetOf<String>()
        toRemove.add(itemId)

        // If it's a folder, delete all descendants too
        val folder = getFolder(itemId)
        if (folder != null) {
            collectDescendantIds(folder.id, toRemove)
        }

        val sizeBefore = _items.value.size
        _items.value = _items.value.filter { it.id !in toRemove }
        val changed = _items.value.size != sizeBefore
        if (changed) persist()
        return changed
    }

    fun updateNoteContent(noteId: String, content: String): Boolean {
        var changed = false
        val updated = _items.value.map { item ->
            if (item.id == noteId && item is Note) {
                changed = true
                item.copy(content = content, modifiedAt = System.currentTimeMillis())
            } else item
        }
        if (changed) {
            _items.value = updated
            persist()
        }
        return changed
    }

    // Helpers
    private fun isDescendantOf(candidateId: String?, ancestorId: String): Boolean {
        if (candidateId == null) return false
        if (candidateId == ancestorId) return true
        val parent = _items.value.find { it.id == candidateId }?.parentId
        return isDescendantOf(parent, ancestorId)
    }

    private fun collectDescendantIds(folderId: String, ids: MutableSet<String>) {
        val children = _items.value.filter { it.parentId == folderId }
        children.forEach { child ->
            ids.add(child.id)
            if (child is Folder) collectDescendantIds(child.id, ids)
        }
    }

    // Persistence (JSON on disk)
    private fun persist() {
        try {
            val array = JSONArray()
            _items.value.forEach { item ->
                array.put(itemToJson(item))
            }
            storageFile.writeText(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromDisk(): List<FileSystemItem>? {
        return try {
            if (!storageFile.exists()) return null
            val text = storageFile.readText()
            if (text.isBlank()) return null
            val array = JSONArray(text)
            val list = mutableListOf<FileSystemItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                jsonToItem(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun itemToJson(item: FileSystemItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("name", item.name)
        obj.put("parentId", item.parentId)
        obj.put("createdAt", item.createdAt)
        obj.put("modifiedAt", item.modifiedAt)
        obj.put("type", item.type.name)

        when (item) {
            is Folder -> {
                obj.put("color", item.color)
                obj.put("isExpanded", item.isExpanded)
            }
            is Note -> {
                obj.put("content", item.content)
                obj.put("lastViewedAt", item.lastViewedAt)
            }
            else -> Unit
        }
        return obj
    }

    private fun jsonToItem(obj: JSONObject): FileSystemItem? {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val name = obj.optString("name", "")
        val parentId = if (obj.isNull("parentId")) null else obj.optString("parentId", null)
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        val modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis())
        val type = runCatching { FileSystemItemType.valueOf(obj.optString("type")) }.getOrNull()
            ?: FileSystemItemType.FOLDER

        return when (type) {
            FileSystemItemType.FOLDER -> {
                Folder(
                    id = id,
                    name = name.ifBlank { "Folder" },
                    parentId = parentId,
                    createdAt = createdAt,
                    modifiedAt = modifiedAt,
                    color = obj.optInt("color", 0xFF4285F4.toInt()),
                    isExpanded = obj.optBoolean("isExpanded", false)
                )
            }
            FileSystemItemType.NOTE -> {
                Note(
                    id = id,
                    name = name.ifBlank { "Untitled Note" },
                    parentId = parentId,
                    createdAt = createdAt,
                    modifiedAt = modifiedAt,
                    content = obj.optString("content", ""),
                )
            }
            else -> {
                // Only Folder/Note are used; fallback to Folder if unknown
                Folder(
                    id = id,
                    name = name.ifBlank { "Folder" },
                    parentId = parentId,
                    createdAt = createdAt,
                    modifiedAt = modifiedAt
                )
            }
        }
    }
}