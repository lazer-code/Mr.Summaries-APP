package com.example.mrsummaries_app.network

/**
 * Models used by the GitHub API Retrofit interface.
 * Field names match GitHub's JSON so Gson/Retrofit will map them automatically.
 */

data class RepoInfo(
    val default_branch: String
)

data class TreeResponse(
    val sha: String,
    val tree: List<TreeItem>,
    val truncated: Boolean
)

data class TreeItem(
    val path: String,
    val mode: String,
    val type: String, // "blob" or "tree"
    val sha: String,
    val size: Int? = null,
    val url: String
)

/**
 * Model for the Contents API (/repos/{owner}/{repo}/contents/{path})
 * type: "file" or "dir"
 * download_url: may be null for directories
 */
data class GitHubContentItem(
    val name: String,
    val path: String,
    val type: String,
    val download_url: String?
)