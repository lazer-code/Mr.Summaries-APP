package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.adapters.SummariesAdapter
import com.example.mrsummaries_app.models.Course
import com.example.mrsummaries_app.models.Summary

class CourseSummariesFragment : Fragment() {

    private lateinit var courseNameTextView: TextView
    private lateinit var courseCodeTextView: TextView
    private lateinit var summariesRecyclerView: RecyclerView
    private lateinit var summariesAdapter: SummariesAdapter
    private var courseId: String? = null

    companion object {
        private const val ARG_COURSE_ID = "course_id"

        fun newInstance(courseId: String): CourseSummariesFragment {
            val fragment = CourseSummariesFragment()
            val args = Bundle().apply {
                putString(ARG_COURSE_ID, courseId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseId = it.getString(ARG_COURSE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_summaries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseNameTextView = view.findViewById(R.id.course_name)
        courseCodeTextView = view.findViewById(R.id.course_code)
        summariesRecyclerView = view.findViewById(R.id.summaries_recycler_view)

        // In a real app, fetch course and summaries from repository or local database
        loadCourseAndSummaries()
    }

    private fun loadCourseAndSummaries() {
        // Simulate loading course data
        // In a real app, this would fetch from a repository
        val course = when (courseId) {
            "1" -> Course("1", "Calculus I", "MATH101", "Introduction to differential calculus", 5)
            "2" -> Course("2", "Physics I", "PHYS101", "Mechanics and thermodynamics", 3)
            "3" -> Course("3", "Data Structures", "CS201", "Fundamental data structures and algorithms", 8)
            else -> Course("1", "Unknown Course", "UNKNOWN", "Course details not available", 0)
        }

        // Set course details
        courseNameTextView.text = course.name
        courseCodeTextView.text = course.courseCode

        // Simulate loading summaries data
        val summaries = when (courseId) {
            "1" -> listOf(
                Summary("101", "Limits and Continuity", "Prof. Smith", "limits_continuity.pdf", System.currentTimeMillis()),
                Summary("102", "Derivatives", "Prof. Smith", "derivatives.pdf", System.currentTimeMillis()),
                Summary("103", "Applications of Derivatives", "Prof. Smith", "applications_derivatives.pdf", System.currentTimeMillis()),
                Summary("104", "Integration", "Prof. Smith", "integration.pdf", System.currentTimeMillis()),
                Summary("105", "Applications of Integration", "Prof. Smith", "applications_integration.pdf", System.currentTimeMillis())
            )
            "2" -> listOf(
                Summary("201", "Kinematics", "Prof. Johnson", "kinematics.pdf", System.currentTimeMillis()),
                Summary("202", "Newton's Laws", "Prof. Johnson", "newton_laws.pdf", System.currentTimeMillis()),
                Summary("203", "Thermodynamics", "Prof. Johnson", "thermodynamics.pdf", System.currentTimeMillis())
            )
            "3" -> listOf(
                Summary("301", "Arrays and Lists", "Prof. Davis", "arrays_lists.pdf", System.currentTimeMillis()),
                Summary("302", "Stacks and Queues", "Prof. Davis", "stacks_queues.pdf", System.currentTimeMillis()),
                Summary("303", "Trees", "Prof. Davis", "trees.pdf", System.currentTimeMillis()),
                Summary("304", "Graphs", "Prof. Davis", "graphs.pdf", System.currentTimeMillis()),
                Summary("305", "Sorting Algorithms", "Prof. Davis", "sorting.pdf", System.currentTimeMillis()),
                Summary("306", "Searching Algorithms", "Prof. Davis", "searching.pdf", System.currentTimeMillis()),
                Summary("307", "Dynamic Programming", "Prof. Davis", "dynamic_programming.pdf", System.currentTimeMillis()),
                Summary("308", "Greedy Algorithms", "Prof. Davis", "greedy_algorithms.pdf", System.currentTimeMillis())
            )
            else -> emptyList()
        }

        // Set up the recycler view
        summariesAdapter = SummariesAdapter(summaries) { summary ->
            openSummary(summary)
        }

        summariesRecyclerView.layoutManager = LinearLayoutManager(context)
        summariesRecyclerView.adapter = summariesAdapter
    }

    private fun openSummary(summary: Summary) {
        // Open summary in SummaryFragment or Activity
        val fragment = SummaryFragment.Companion.newInstance(summary.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}