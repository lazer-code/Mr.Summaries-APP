package com.example.mrsummaries_app.filesystem.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

/**
 * Base interface for all file system items
 */
interface FileSystemItem {
    val id: String
    var name: String
    var parentId: String?
    val createdAt: Long
    var modifiedAt: Long
    val icon: ImageVector
    val type: FileSystemItemType
}

/**
 * Types of items in the file system
 */
enum class FileSystemItemType {
    FOLDER,
    NOTE,
    DOCUMENT,
    IMAGE,
    AUDIO,
    VIDEO
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
    val isFavorite: Boolean = false
) : FileSystemItem

/**
 * Represents a document file in the file system
 */
data class Document(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String,
    override var parentId: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis(),
    override val icon: ImageVector = Icons.Default.Description,
    override val type: FileSystemItemType = FileSystemItemType.DOCUMENT,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = ""
) : FileSystemItem

/**
 * Represents an image file in the file system
 */
data class ImageFile(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String,
    override var parentId: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis(),
    override val icon: ImageVector = Icons.Default.Image,
    override val type: FileSystemItemType = FileSystemItemType.IMAGE,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = ""
) : FileSystemItem

/**
 * Represents an audio file in the file system
 */
data class AudioFile(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String,
    override var parentId: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis(),
    override val icon: ImageVector = Icons.Default.AudioFile,
    override val type: FileSystemItemType = FileSystemItemType.AUDIO,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L, // in milliseconds
    val mimeType: String = ""
) : FileSystemItem

/**
 * Represents a video file in the file system
 */
data class VideoFile(
    override val id: String = UUID.randomUUID().toString(),
    override var name: String,
    override var parentId: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override var modifiedAt: Long = System.currentTimeMillis(),
    override val icon: ImageVector = Icons.Default.VideoFile,
    override val type: FileSystemItemType = FileSystemItemType.VIDEO,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val duration: Long = 0L, // in milliseconds
    val mimeType: String = ""
) : FileSystemItem