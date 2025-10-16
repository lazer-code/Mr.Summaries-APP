package com.example.mrsummaries_app.ui.summaries

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.network.GitHubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Summary Detail: downloads raw markdown and renders it as HTML in a WebView.
 * Requires the CommonMark dependency (see build.gradle note below).
 *
 * Expects arguments:
 *  - summary_owner (String)
 *  - summary_repo  (String)
 *  - summary_branch(String)
 *  - summary_path  (String) -> e.g. "summaries/CourseA/Note.md"
 */
class SummaryDetailFragment : Fragment() {

    private var owner: String? = null
    private var repo: String? = null
    private var branch: String? = null
    private var path: String? = null

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        owner = arguments?.getString("summary_owner")
        repo = arguments?.getString("summary_repo")
        branch = arguments?.getString("summary_branch")
        path = arguments?.getString("summary_path")
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_summary_detail, container, false)
        webView = root.findViewById(R.id.web_summary)
        progress = root.findViewById(R.id.progress_summary)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (owner.isNullOrBlank() || repo.isNullOrBlank() || branch.isNullOrBlank() || path.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing summary info", Toast.LENGTH_SHORT).show()
            return
        }

        // load markdown on background thread
        loadAndRenderMarkdown(owner!!, repo!!, branch!!, path!!)
    }

    private fun loadAndRenderMarkdown(owner: String, repo: String, branch: String, path: String) {
        progress.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rawUrl = GitHubService.rawUrl(owner, repo, branch, path)
                // simple raw GET
                val markdown = java.net.URL(rawUrl).readText()

                // convert markdown -> HTML using CommonMark
                val parser = Parser.builder().build()
                val document = parser.parse(markdown)
                val renderer = HtmlRenderer.builder().build()
                val contentHtml = renderer.render(document)

                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1">
                      <style>
                        body { font-family: -apple-system, Roboto, "Helvetica Neue", Arial; padding: 16px; color: #1F1B24; background: #FFFFFF; }
                        pre { background:#f6f6f9; padding:12px; border-radius:8px; overflow:auto; }
                        code { font-family: monospace; }
                        img { max-width:100%; height:auto; }
                        h1,h2,h3,h4 { color: #333; }
                        a { color: #1E88E5; text-decoration: none; }
                      </style>
                    </head>
                    <body>$contentHtml</body>
                    </html>
                """.trimIndent()

                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load summary: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}