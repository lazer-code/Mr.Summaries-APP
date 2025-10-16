package com.example.mrsummaries_app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Models (RepoInfo, TreeResponse, TreeItem, GitHubContentItem) are assumed to exist in same package
interface GitHubApi {
    @GET("repos/{owner}/{repo}")
    suspend fun getRepoInfo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): RepoInfo

    @GET("repos/{owner}/{repo}/git/trees/{tree_sha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tree_sha") treeSha: String,
        @Query("recursive") recursive: Int = 0
    ): TreeResponse

    // Use contents API for listing folder contents. Note: Retrofit encodes path segments automatically,
    // but to be explicit callers should pass the path as-is (e.g. "Summaries" or "Summaries/CourseA")
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): List<GitHubContentItem>
}

object GitHubService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: GitHubApi = retrofit.create(GitHubApi::class.java)

    /**
     * Build a safe raw.githubusercontent.com URL for a path that may contain spaces or special characters.
     * Encodes each path segment but preserves slashes.
     * Example: owner/repo/branch/"Summaries/Course A/Note 1.md"
     */
    fun rawUrl(owner: String, repo: String, branch: String, path: String): String {
        val cleanPath = path.trimStart('/')
        val encoded = cleanPath.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.toString())
        }
        return "https://raw.githubusercontent.com/$owner/$repo/$branch/$encoded"
    }
}