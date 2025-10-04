package com.example.mrsummaries_app.files

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

object FsRepository {
    // In-memory state used by the UI
    @Volatile
    var root: FsNode.Folder = FsNode.Folder(id = "root", name = "Root", children = mutableListOf())
        private set

    @Volatile
    var selectedNoteId: String? = null
        private set

    private var baseDir: File? = null
    private val idToDir = hashMapOf<String, File>()
    private val idToNode = hashMapOf<String, FsNode>()

    // Files/JSON keys
    private const val META_FILE = ".node.json"
    private const val TYPE_FOLDER = "folder"
    private const val TYPE_NOTE = "note"
    private const val KEY_ID = "id"
    private const val KEY_TYPE = "type"
    private const val KEY_NAME = "name"

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (baseDir == null) {
            baseDir = File(context.filesDir, "fs").apply { mkdirs() }
        }
        loadFromDiskOrSeed()
    }

    @Synchronized
    fun resetToSample() {
        val dir = baseDir ?: return
        if (dir.listFiles().isNullOrEmpty()) {
            val notes = ensureFolder(dir, "Notes")
            val math = ensureFolder(notes, "Math")
            val cs = ensureFolder(notes, "CS")
            ensureNote(math, "Algebra Note")
            ensureNote(cs, "Data Structures Note")
        }
        loadFromDiskOrSeed()
    }

    @Synchronized
    fun selectNote(id: String) {
        selectedNoteId = id
    }

    @Synchronized
    fun findNode(id: String): FsNode? = idToNode[id]

    @Synchronized
    fun collectAllFolders(): List<FsNode.Folder> {
        val out = mutableListOf<FsNode.Folder>()
        fun walk(n: FsNode) {
            if (n is FsNode.Folder) {
                out += n
                n.children.forEach { walk(it) }
            }
        }
        walk(root)
        return out
    }

    @Synchronized
    fun isValidMove(sourceId: String, targetFolderId: String): Boolean {
        val src = idToNode[sourceId] ?: return false
        val target = idToNode[targetFolderId] as? FsNode.Folder ?: return false
        if (sourceId == root.id) return false
        if (src is FsNode.Folder) {
            var ok = true
            fun containsDesc(d: FsNode) {
                if (d.id == target.id) { ok = false; return }
                if (d is FsNode.Folder) d.children.forEach { containsDesc(it) }
            }
            containsDesc(src)
            if (!ok) return false
        }
        return true
    }

    @Synchronized
    fun createFolder(parentId: String, name: String): FsNode.Folder? {
        val parent = (idToNode[parentId] as? FsNode.Folder) ?: return null
        val parentDir = idToDir[parent.id] ?: return null
        val unique = uniqueName(parentDir, name)
        val dir = ensureFolder(parentDir, unique)
        val meta = readMeta(dir) ?: return null
        val node = FsNode.Folder(meta.id, meta.name, mutableListOf())
        parent.children.add(node)
        indexNode(node, dir)
        return node
    }

    @Synchronized
    fun createNote(parentId: String, name: String): FsNode.Note? {
        val parent = (idToNode[parentId] as? FsNode.Folder) ?: return null
        val parentDir = idToDir[parent.id] ?: return null
        val unique = uniqueName(parentDir, name)
        val dir = ensureNote(parentDir, unique)
        val meta = readMeta(dir) ?: return null
        val node = FsNode.Note(meta.id, meta.name)
        parent.children.add(node)
        indexNode(node, dir)
        return node
    }

    @Synchronized
    fun renameNode(nodeId: String, newName: String): Boolean {
        val node = idToNode[nodeId] ?: return false
        val dir = idToDir[nodeId] ?: return false
        val parentDir = dir.parentFile ?: return false
        val unique = uniqueName(parentDir, newName)
        val target = File(parentDir, unique)
        if (target == dir) {
            node.name = unique
            writeMeta(dir, NodeMeta(node.id, typeOf(node), node.name))
            return true
        }
        if (dir.renameTo(target)) {
            node.name = unique
            writeMeta(target, NodeMeta(node.id, typeOf(node), node.name))
            idToDir[nodeId] = target
            return true
        }
        return false
    }

    @Synchronized
    fun deleteNode(nodeId: String): Boolean {
        val node = idToNode[nodeId] ?: return false
        if (nodeId == root.id) return false
        val dir = idToDir[nodeId] ?: return false

        // unlink from parent
        removeFromParent(root, nodeId)

        // delete files on disk
        dir.deleteRecursively()

        // clear indexes for this subtree
        clearIndexFor(node)

        if (selectedNoteId == nodeId) selectedNoteId = null
        return true
    }

    @Synchronized
    fun moveNode(sourceId: String, targetFolderId: String): Boolean {
        val src = idToNode[sourceId] ?: return false
        val srcDir = idToDir[sourceId] ?: return false
        val targetFolder = idToNode[targetFolderId] as? FsNode.Folder ?: return false
        val targetDir = idToDir[targetFolderId] ?: return false
        if (!isValidMove(sourceId, targetFolderId)) return false

        val unique = uniqueName(targetDir, src.name)
        val dest = File(targetDir, unique)
        if (!srcDir.renameTo(dest)) return false

        src.name = unique
        writeMeta(dest, NodeMeta(src.id, typeOf(src), src.name))

        removeFromParent(root, sourceId)
        targetFolder.children.add(src)
        idToDir[sourceId] = dest
        return true
    }

    // --------------------
    // Internal: load/save
    // --------------------

    private fun typeOf(node: FsNode): String = when (node) {
        is FsNode.Folder -> TYPE_FOLDER
        is FsNode.Note -> TYPE_NOTE
    }

    private data class NodeMeta(val id: String, val type: String, val name: String)

    private fun writeMeta(dir: File, meta: NodeMeta) {
        val f = File(dir, META_FILE)
        val json = JSONObject()
            .put(KEY_ID, meta.id)
            .put(KEY_TYPE, meta.type)
            .put(KEY_NAME, meta.name)
        f.writeText(json.toString())
    }

    private fun readMeta(dir: File): NodeMeta? {
        val f = File(dir, META_FILE)
        if (!f.exists()) return null
        return runCatching {
            val j = JSONObject(f.readText())
            NodeMeta(
                id = j.getString(KEY_ID),
                type = j.getString(KEY_TYPE),
                name = j.getString(KEY_NAME)
            )
        }.getOrNull()
    }

    private fun ensureFolder(parent: File, name: String): File {
        val d = File(parent, name).apply { mkdirs() }
        val meta = readMeta(d) ?: NodeMeta(UUID.randomUUID().toString(), TYPE_FOLDER, name)
        writeMeta(d, meta.copy(name = name))
        return d
    }

    private fun ensureNote(parent: File, name: String): File {
        val d = File(parent, name).apply { mkdirs() }
        val meta = readMeta(d) ?: NodeMeta(UUID.randomUUID().toString(), TYPE_NOTE, name)
        writeMeta(d, meta.copy(name = name))
        return d
    }

    private fun uniqueName(parent: File, base: String): String {
        val trimmed = base.trim().ifEmpty { "Untitled" }
        var candidate = trimmed
        var idx = 1
        while (File(parent, candidate).exists()) {
            candidate = "$trimmed (${idx++})"
        }
        return candidate
    }

    private fun removeFromParent(folder: FsNode.Folder, id: String): Boolean {
        val it = folder.children.listIterator()
        while (it.hasNext()) {
            val n = it.next()
            if (n.id == id) {
                it.remove()
                return true
            }
            if (n is FsNode.Folder && removeFromParent(n, id)) return true
        }
        return false
    }

    private fun clearAllIndexes() {
        idToDir.clear()
        idToNode.clear()
    }

    private fun indexNode(node: FsNode, dir: File) {
        idToDir[node.id] = dir
        idToNode[node.id] = node
        if (node is FsNode.Folder) {
            node.children.forEach { child ->
                val childDir = File(dir, child.name)
                if (child is FsNode.Folder || child is FsNode.Note) {
                    idToDir[child.id] = childDir
                    idToNode[child.id] = child
                }
            }
        }
    }

    private fun loadFromDiskOrSeed() {
        val dir = baseDir ?: return
        if (dir.listFiles().isNullOrEmpty()) {
            resetToSample()
            return
        }
        clearAllIndexes()
        root = FsNode.Folder(id = "root", name = "Root", children = mutableListOf())
        idToDir[root.id] = dir
        idToNode[root.id] = root

        fun loadFolder(parentFolder: FsNode.Folder, parentDir: File) {
            val childrenDirs = parentDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() } ?: emptyList()
            for (childDir in childrenDirs) {
                val meta = readMeta(childDir) ?: continue
                when (meta.type) {
                    TYPE_FOLDER -> {
                        val fNode = FsNode.Folder(id = meta.id, name = meta.name, children = mutableListOf())
                        parentFolder.children.add(fNode)
                        idToDir[fNode.id] = childDir
                        idToNode[fNode.id] = fNode
                        loadFolder(fNode, childDir)
                    }
                    TYPE_NOTE -> {
                        val nNode = FsNode.Note(id = meta.id, name = meta.name)
                        parentFolder.children.add(nNode)
                        idToDir[nNode.id] = childDir
                        idToNode[nNode.id] = nNode
                    }
                }
            }
        }

        loadFolder(root, dir)
        selectedNoteId?.let { if (!idToNode.containsKey(it)) selectedNoteId = null }
    }

    // Added: remove indexes for a node and its subtree
    private fun clearIndexFor(node: FsNode) {
        idToNode.remove(node.id)
        idToDir.remove(node.id)
        if (node is FsNode.Folder) {
            node.children.forEach { child -> clearIndexFor(child) }
        }
    }

    // Public helper for note persistence location
    @Synchronized
    fun noteDirectory(noteId: String): File? {
        return idToDir[noteId]?.takeIf { it.isDirectory }
    }
}