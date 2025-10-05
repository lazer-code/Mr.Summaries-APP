package com.example.mrsummaries_app.files

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

// Single source of truth for the file-tree node model.
sealed class FsNode(
    open val id: String,
    open var name: String
) {
    data class Folder(
        override val id: String,
        override var name: String,
        // Compose-observable list so UI recomposes on changes
        val children: SnapshotStateList<FsNode> = mutableStateListOf()
    ) : FsNode(id, name)

    data class Note(
        override val id: String,
        override var name: String
    ) : FsNode(id, name)
}