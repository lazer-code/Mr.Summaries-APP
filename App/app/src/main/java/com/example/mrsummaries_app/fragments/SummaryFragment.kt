package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.custom_views.SummaryView
import com.example.mrsummaries_app.models.Summary

class SummaryFragment : Fragment() {

    private lateinit var summaryTitleTextView: TextView
    private lateinit var summaryAuthorTextView: TextView
    private lateinit var summaryView: SummaryView
    private var summaryId: String? = null

    companion object {
        private const val ARG_SUMMARY_ID = "summary_id"

        fun newInstance(summaryId: String): SummaryFragment {
            val fragment = SummaryFragment()
            val args = Bundle().apply {
                putString(ARG_SUMMARY_ID, summaryId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            summaryId = it.getString(ARG_SUMMARY_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        summaryTitleTextView = view.findViewById(R.id.summary_title)
        summaryAuthorTextView = view.findViewById(R.id.summary_author)
        summaryView = view.findViewById(R.id.summary_view)

        // In a real app, fetch summary from repository or local database
        loadSummary()
    }

    private fun loadSummary() {
        // Simulate loading summary data
        // In a real app, this would fetch from a repository
        val summary = Summary(
            summaryId ?: "1",
            "Math 101 Lecture Notes",
            "Professor Smith",
            "math101.pdf",
            System.currentTimeMillis()
        )

        summaryTitleTextView.text = summary.title
        summaryAuthorTextView.text = "By: ${summary.author}"

        // Load the summary content into the SummaryView
        // This would typically load a PDF or other document
        summaryView.loadSummary(summary)
    }
}