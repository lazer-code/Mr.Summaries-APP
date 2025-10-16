package com.example.mrsummaries_app.ui.summaries

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.databinding.FragmentSummariesBinding
import com.example.mrsummaries_app.network.GitHubService
import com.example.mrsummaries_app.network.TreeItem
import com.example.mrsummaries_app.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.util.LinkedHashSet

private const val TAG = "SummariesFragment"

/**
 * Shows course folders and summaries found under the repository root folder "Summaries" (capital S).
 * Repository: owner = "lazer-code", repo = "Mr.Summaries-Summaries"
 *
 * Expected structure:
 *   Summaries/
 *     CourseA/
 *       Note1.md
 *       Note2.md
 *     CourseB/
 *       ...
 *
 * - Top level view shows one row per course folder (first segment after "Summaries/")
 * - Navigating into a course shows .md files directly under that course path (and nested folders)
 */
class SummariesFragment : Fragment() {

    private var _binding: FragmentSummariesBinding? = null
    private val binding get() = _binding!!

    // updated to target the repository you specified
    private val owner = "lazer-code"
    private val repo = "Mr.Summaries-Summaries"

    private var branch: String = "main"
    private var allMdPaths: List<String> = emptyList()
    private val pathStack = ArrayDeque<String>()

    private val adapter = FileNodeAdapter { node ->
        if (node.isFolder) {
            // node.path is a repo-relative path without the "Summaries/" root when used for display navigation
            // For this implementation we push the course path (without leading "Summaries/")
            pathStack.addLast(node.path)
            showForCurrentPath()
        } else {
            // node.path is full repo path like "Summaries/CourseA/Note.md"
            val bundle = bundleOf(
                "summary_path" to node.path,
                "summary_owner" to owner,
                "summary_repo" to repo,
                "summary_branch" to branch
            )
            findNavController().navigate(R.id.summaryDetailFragment, bundle)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = FragmentSummariesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvSummaries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSummaries.adapter = adapter

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (pathStack.isNotEmpty()) {
                pathStack.removeLast()
                showForCurrentPath()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        attemptLoadSummaries()
    }

    private fun attemptLoadSummaries() {
        lifecycleScope.launch(Dispatchers.IO) {
            val systemSaysOnline = NetworkUtils.isOnline(requireContext())
            var reachable = systemSaysOnline
            if (!systemSaysOnline) {
                reachable = NetworkUtils.probeInternet()
                Log.d(TAG, "probeInternet -> $reachable")
            }
            withContext(Dispatchers.Main) {
                if (!reachable) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("No network detected")
                        .setMessage("System reports no network. If you have a working connection, choose 'Try anyway'.")
                        .setPositiveButton("Retry") { _, _ -> attemptLoadSummaries() }
                        .setNeutralButton("Try anyway") { _, _ -> fetchRepoTreeAndShow() }
                        .setNegativeButton("Open settings") { _, _ ->
                            startActivity(NetworkUtils.openNetworkSettingsIntent())
                        }
                        .show()
                } else {
                    fetchRepoTreeAndShow()
                }
            }
        }
    }

    /**
     * Collect .md paths under the repository root folder "Summaries" (capital S).
     * The resulting allMdPaths entries are full repo-relative paths like:
     *   "Summaries/CourseA/Note1.md"
     */
    private fun fetchRepoTreeAndShow() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val repoInfo = GitHubService.api.getRepoInfo(owner, repo)
                branch = repoInfo.default_branch
                Log.d(TAG, "Default branch: $branch")

                // Try trees endpoint (may accept branch or tree SHA). If it fails, fallback to contents API.
                val treeResp = try {
                    GitHubService.api.getTree(owner, repo, branch, recursive = 1)
                } catch (e: Exception) {
                    Log.w(TAG, "git/trees call failed with branch name; will use contents API fallback", e)
                    null
                }

                val mdPaths = mutableListOf<String>()

                if (treeResp != null) {
                    for (item: TreeItem in treeResp.tree) {
                        // ensure we only collect markdown files under "Summaries/"
                        if (item.type == "blob" && item.path.startsWith("Summaries/") && item.path.endsWith(".md", ignoreCase = true)) {
                            mdPaths.add(item.path)
                        }
                    }
                    Log.d(TAG, "git/trees found ${mdPaths.size} markdown files under Summaries (if any)")
                }

                if (mdPaths.isEmpty()) {
                    Log.d(TAG, "Using contents API fallback; traversing 'Summaries'")
                    val found = LinkedHashSet<String>()
                    try {
                        fetchContentsRecursive("Summaries", found)
                        mdPaths.addAll(found)
                        Log.d(TAG, "contents traversal found ${found.size} markdown files")
                    } catch (e: Exception) {
                        Log.e(TAG, "contents traversal failed", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    allMdPaths = mdPaths.sorted()
                    if (allMdPaths.isEmpty()) {
                        Toast.makeText(requireContext(), "No summaries (.md) found under 'Summaries' in repo", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Found ${allMdPaths.size} summaries", Toast.LENGTH_SHORT).show()
                    }
                    pathStack.clear()
                    showForCurrentPath()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch summaries", e)
                withContext(Dispatchers.Main) {
                    if (e is UnknownHostException) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Network error")
                            .setMessage("Unable to reach api.github.com (DNS lookup failed). Check network/DNS and try again.")
                            .setPositiveButton("Retry") { _, _ -> attemptLoadSummaries() }
                            .setNeutralButton("Try anyway") { _, _ -> fetchRepoTreeAndShow() }
                            .setNegativeButton("Open settings") { _, _ ->
                                startActivity(NetworkUtils.openNetworkSettingsIntent())
                            }
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Error loading summaries: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private suspend fun fetchContentsRecursive(path: String, out: MutableSet<String>) {
        try {
            val items = GitHubService.api.getContents(owner, repo, path)
            for (it in items) {
                if (it.type == "dir") {
                    fetchContentsRecursive(it.path, out)
                } else if (it.type == "file" && it.path.endsWith(".md", ignoreCase = true)) {
                    out.add(it.path)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getContents failed for path=$path: ${e.message}")
            throw e
        }
    }

    /**
     * For display/navigation we want:
     * - top level (stack empty): show course folders under "Summaries" (first segment after "Summaries/")
     * - in a course (stack contains courseName or deeper): show folders/files inside that course
     *
     * allMdPaths entries look like: "Summaries/CourseA/Note.md"
     */
    private fun showForCurrentPath() {
        val prefix = pathStack.joinToString(separator = "/").trim('/')
        val display = buildFileNodeListForPrefix(prefix)
        adapter.submitList(display)
        binding.tvCurrentPath.text = if (prefix.isEmpty()) "Summaries" else prefix
    }

    private fun buildFileNodeListForPrefix(prefix: String): List<FileNode> {
        val prefixNormalized = prefix.trim().trim('/')
        val results = LinkedHashMap<String, FileNode>()

        for (p in allMdPaths) {
            if (!p.startsWith("Summaries/")) continue
            val withoutRoot = p.removePrefix("Summaries/").trimStart('/')
            if (prefixNormalized.isEmpty()) {
                val remainder = withoutRoot
                val firstSegment = remainder.substringBefore('/')
                if (remainder.contains('/')) {
                    // course folder
                    results[firstSegment] = FileNode(name = firstSegment, path = firstSegment, isFolder = true)
                } else {
                    // markdown directly under Summaries root (unlikely); expose as file
                    results[firstSegment] = FileNode(name = firstSegment.removeSuffix(".md"), path = "Summaries/$firstSegment", isFolder = false)
                }
            } else {
                if (!withoutRoot.startsWith("$prefixNormalized/")) continue
                val remainder = withoutRoot.removePrefix("$prefixNormalized/").trimStart('/')
                val firstSegment = remainder.substringBefore('/')
                if (remainder.contains('/')) {
                    val folderPath = "$prefixNormalized/$firstSegment"
                    results[firstSegment] = FileNode(name = firstSegment, path = "Summaries/$folderPath", isFolder = true)
                } else {
                    val filePath = "$prefixNormalized/$firstSegment"
                    results[firstSegment] = FileNode(name = firstSegment.removeSuffix(".md"), path = "Summaries/$filePath", isFolder = false)
                }
            }
        }

        val folders = results.values.filter { it.isFolder }.sortedBy { it.name.lowercase() }
        val files = results.values.filter { !it.isFolder }.sortedBy { it.name.lowercase() }
        return (folders + files).toList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}