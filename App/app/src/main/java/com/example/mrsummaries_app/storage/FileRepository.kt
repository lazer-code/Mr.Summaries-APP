package com.example.mrsummaries_app.storage

import android.content.Context
import java.io.File
import java.util.*

/**
 * FileRepository manages notes and folders under the app internal files directory.
 * - Notes are stored as `.note` JSON files.
 * - Folders are regular directories under context.filesDir.
 * - All public APIs accept and return relative paths (relative to the repository root).
 *
 * Examples:
 *  - root: "" (empty string) -> context.filesDir
 *  - folderPath: "CourseA/Week1"
 *  - note relative path: "CourseA/Week1/MyNote_....note"
 *
 * This supports infinitely nested folders using simple path strings.
 */
class FileRepository(private val context: Context) {

    private val root: File = context.filesDir

    /**
     * Represents a node in the notes filesystem (either a folder or a note file).
     * `relativePath` is path relative to [root] (no leading slash).
     */
    data class FileSystemNode(
        val name: String,
        val relativePath: String,
        val isFolder: Boolean
    )

    private fun folderFile(folderPath: String): File {
        val clean = folderPath.trim().trim('/')
        return if (clean.isEmpty()) root else File(root, clean)
    }

    /**
     * Create a folder at the relative path (can be nested, e.g. "CourseA/Week1").
     * Returns the normalized relative path of the created folder.
     */
    fun createFolder(folderPath: String): String {
        val clean = folderPath.trim().trim('/')
        val folder = folderFile(clean)
        folder.mkdirs()
        return clean
    }

    /**
     * Create a note with a required title inside [folderPath] (relative).
     * Returns the relative path to the created note file.
     */
    fun createNote(title: String, folderPath: String = ""): String {
        val sanitizedTitle = title.trim().ifEmpty { "Untitled" }
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            .replace(Regex("\\s+"), "_")
        val filename = "${sanitizedTitle}_${UUID.randomUUID()}.note"
        val folder = folderFile(folderPath)
        folder.mkdirs()
        val file = File(folder, filename)
        file.createNewFile()
        // initialize simple JSON skeleton for later editor parsing
        file.writeText("{\"title\":\"${escapeJson(title)}\",\"strokes\":[],\"texts\":[]}")
        // return relative path
        val rel = if (folderPath.isBlank()) filename else "${folderPath.trim().trim('/')}/$filename"
        return rel
    }

    /**
     * List immediate children (folders and note files) for the given relative folder path.
     * Does not recursively list deeper children.
     */
    fun listFolderContents(folderPath: String = ""): List<FileSystemNode> {
        val folder = folderFile(folderPath)
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        // Immediate children
        val children = folder.listFiles() ?: return emptyList()

        val folders = children.filter { it.isDirectory }.map {
            val rel = buildRelativePath(folderPath, it.name)
            FileSystemNode(name = it.name, relativePath = rel, isFolder = true)
        }.sortedBy { it.name.lowercase() }

        val files = children.filter { it.isFile && it.extension == "note" }.map {
            val rel = buildRelativePath(folderPath, it.name)
            // display name without extension
            val displayName = it.name.removeSuffix(".note")
            FileSystemNode(name = displayName, relativePath = rel, isFolder = false)
        }.sortedBy { it.name.lowercase() }

        return folders + files
    }

    private fun buildRelativePath(parent: String, child: String): String {
        val p = parent.trim().trim('/')
        return if (p.isEmpty()) child else "$p/$child"
    }

    /**
     * Read note content. Accepts either absolute file path or relative path.
     * Returns empty string if not found or not readable.
     */
    fun readNote(pathOrRelative: String): String {
        val file = resolveToFile(pathOrRelative)
        if (!file.exists() || !file.isFile) return ""
        return file.readText()
    }

    /**
     * Save note content. Accepts either absolute file path or relative path.
     * Ensures parent folders exist.
     */
    fun saveNote(pathOrRelative: String, content: String) {
        val file = resolveToFile(pathOrRelative)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /**
     * Delete node (file or folder). Path may be absolute or relative.
     */
    fun delete(pathOrRelative: String) {
        val file = resolveToFile(pathOrRelative)
        if (file.exists()) file.deleteRecursively()
    }

    /**
     * Convert a relative path (or already absolute path) into absolute File object.
     */
    fun resolveToFile(pathOrRelative: String): File {
        val p = pathOrRelative.trim()
        // If looks like absolute (starts with root path or '/data' etc) treat as absolute.
        if (p.isEmpty()) return root
        if (File(p).isAbsolute) return File(p)
        return File(root, p)
    }

    /**
     * Get absolute path for a relative path. If already absolute, returned as-is.
     */
    fun getAbsolutePath(pathOrRelative: String): String {
        return resolveToFile(pathOrRelative).absolutePath
    }

    private fun escapeJson(s: String): String {
        return s.replace("\"", "\\\"")
    }
}