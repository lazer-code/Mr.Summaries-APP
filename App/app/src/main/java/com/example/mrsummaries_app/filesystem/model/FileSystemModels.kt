package com.example.mrsummaries_app.filesystem.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

/**
 * Enum to define types of file system items
 */
enum class FileSystemItemType {
    FOLDER,
    NOTE
}

/**
 * Base interface for all file system items
 */
interface FileSystemItem {
    val id: String
    var name: String
    var parentId: String? // null means root level
    val createdAt: Long
    var modifiedAt: Long
    val icon: ImageVector
    val type: FileSystemItemType
}

/**
 * Represents a folder in the file system
 */
data class Folder(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String,
    override var parentId: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis(),
    override val icon: ImageVector = Icons.Default.Folder,
    override val type: FileSystemItemType = FileSystemItemType.FOLDER,
    val color: Int = 0xFF4285F4.toInt(), // Default folder color (blue)
    val isExpanded: Boolean = false
) : FileSystemItem

/**
 * Represents a note in the file system
 */
data class Note(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String,
    override var parentId: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis(),
    override val icon: ImageVector = Icons.Default.TextSnippet,
    override val type: FileSystemItemType = FileSystemItemType.NOTE,
    val content: String = "", // Note content
    val lastViewedAt: Long = System.currentTimeMillis(),
) : FileSystemItem