package com.example.mrsummaries_app.ui.summaries

/**
 * Lightweight model representing a folder or a file (markdown summary) inside the repo listing.
 * Kept in its own file so it is easily referenced by the fragment and adapter.
 */
data class FileNode(
    val name: String,
    val path: String,   // relative path inside repo (e.g. "Pages/CS101/Intro.md" or "CS101")
    val isFolder: Boolean
)