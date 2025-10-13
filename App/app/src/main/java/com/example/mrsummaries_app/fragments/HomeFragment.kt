package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.adapters.CourseAdapter
import com.example.mrsummaries_app.adapters.RecentItemsAdapter
import com.example.mrsummaries_app.models.Course
import com.example.mrsummaries_app.models.Summary

class HomeFragment : Fragment() {

    private lateinit var recentItemsRecyclerView: RecyclerView
    private lateinit var coursesRecyclerView: RecyclerView
    private lateinit var recentItemsAdapter: RecentItemsAdapter
    private lateinit var coursesAdapter: CourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentItemsRecyclerView = view.findViewById(R.id.recent_items_recycler_view)
        coursesRecyclerView = view.findViewById(R.id.courses_recycler_view)

        setupRecentItems()
        setupCourses()
    }

    private fun setupRecentItems() {
        // Sample data - in a real app, this would come from a repository or database
        val recentItems = listOf(
            Summary("1", "Calculus Midterm Notes", "Prof. Smith", "calculus_midterm.pdf", System.currentTimeMillis()),
            Summary("2", "Physics Formula Sheet", "Jane Smith", "physics_formulas.pdf", System.currentTimeMillis()),
            Summary("3", "Data Structures Notes", "Mike Johnson", "data_structures.pdf", System.currentTimeMillis())
        )

        recentItemsAdapter = RecentItemsAdapter(recentItems) { summary ->
            // Handle click on recent item
            openSummary(summary)
        }

        recentItemsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recentItemsRecyclerView.adapter = recentItemsAdapter
    }

    private fun setupCourses() {
        // Sample data - in a real app, this would come from a repository or database
        val courses = listOf(
            Course("1", "Calculus I", "MATH101", "Introduction to differential calculus", 5, R.drawable.ic_math),
            Course("2", "Physics I", "PHYS101", "Mechanics and thermodynamics", 3, R.drawable.ic_physics),
            Course("3", "Data Structures", "CS201", "Fundamental data structures and algorithms", 8, R.drawable.ic_computer_science),
            Course("4", "Organic Chemistry", "CHEM201", "Structure and reactions of organic compounds", 4, R.drawable.ic_chemistry),
            Course("5", "Linear Algebra", "MATH201", "Vector spaces and linear transformations", 6, R.drawable.ic_math)
        )

        coursesAdapter = CourseAdapter(courses) { course ->
            // Handle click on course
            openCourseSummaries(course)
        }

        coursesRecyclerView.layoutManager = LinearLayoutManager(context)
        coursesRecyclerView.adapter = coursesAdapter
    }

    private fun openSummary(summary: Summary) {
        // Open summary in SummaryFragment or Activity
        val fragment = SummaryFragment.Companion.newInstance(summary.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openCourseSummaries(course: Course) {
        // Navigate to course summaries
        val fragment = CourseSummariesFragment.newInstance(course.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}