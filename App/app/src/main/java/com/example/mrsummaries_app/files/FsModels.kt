package com.example.mrsummaries_app.files

// Single source of truth for the file-tree node model.
sealed class FsNode(
    open val id: String,
    open var name: String
) {
    data class Folder(
        override val id: String,
        override var name: String,
        val children: MutableList<FsNode> = mutableListOf()
    ) : FsNode(id, name)

    data class Note(
        override val id: String,
        override var name: String
    ) : FsNode(id, name)
}