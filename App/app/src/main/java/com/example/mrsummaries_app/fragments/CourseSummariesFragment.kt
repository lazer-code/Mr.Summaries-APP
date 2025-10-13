package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.adapters.SummaryAdapter
import com.example.mrsummaries_app.models.Summary
import android.widget.TextView

/**
 * CourseSummariesFragment - simplified version that doesn't rely on generated
 * view-binding or SafeArgs classes. This avoids "unresolved reference" errors
 * while layout-binding and SafeArgs are not set up in the project.
 *
 * Requirements:
 * - layout file: res/layout/fragment_course_summaries.xml
 *   - TextView with id = @+id/text_subject_title
 *   - RecyclerView with id = @+id/recycler_view_summaries
 *   - TextView with id = @+id/text_no_summaries
 *
 * Navigation:
 * - This fragment expects an argument named "subject" in the fragment arguments Bundle.
 *   If you are navigating with SafeArgs you can keep using that, otherwise set:
 *     val bundle = Bundle().apply { putString("subject", "Math") }
 *     findNavController().navigate(R.id.courseSummariesFragment, bundle)
 *
 * - When a summary is clicked we navigate to R.id.summaryFragment and pass the Summary
 *   object as a Serializable under the key "summary". Ensure your SummaryFragment reads
 *   it from arguments (it was declared Serializable in models.Summary).
 */
class CourseSummariesFragment : Fragment() {

    private lateinit var summaryAdapter: SummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout (use the existing XML file)
        return inflater.inflate(R.layout.fragment_course_summaries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read subject from arguments (fallback to "General" if not provided)
        val subject = arguments?.getString("subject") ?: "General"

        // Title TextView
        val titleTv: TextView = view.findViewById(R.id.text_subject_title)
        titleTv.text = subject

        // RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_summaries)
        setupRecyclerView(recyclerView)

        // No summaries TextView
        val noSummariesTv: TextView = view.findViewById(R.id.text_no_summaries)

        // Load summaries for the selected subject
        val summaries = when (subject) {
            "Math" -> getMathSummaries()
            "Physics" -> getPhysicsSummaries()
            "Computer Science" -> getComputerScienceSummaries()
            "Chemistry" -> getChemistrySummaries()
            else -> emptyList()
        }

        if (summaries.isEmpty()) {
            noSummariesTv.visibility = View.VISIBLE
        } else {
            noSummariesTv.visibility = View.GONE
        }

        summaryAdapter.submitList(summaries)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        summaryAdapter = SummaryAdapter { summary ->
            // Navigate to SummaryFragment and pass the Summary as Serializable
            val bundle = Bundle().apply { putSerializable("summary", summary) }
            // Make sure you have a destination with id = summaryFragment in your nav graph,
            // or replace with the appropriate destination id.
            findNavController().navigate(R.id.summaryFragment, bundle)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = summaryAdapter
        }
    }

    // ---------- Mock data -----------
    private fun getMathSummaries(): List<Summary> {
        return listOf(
            Summary(
                id = 1,
                title = "Calculus I: Limits and Derivatives",
                content = "This summary covers the definition of limits, continuity, rules of differentiation, and applications of derivatives including optimization and related rates...",
                subject = "Math",
                dateCreated = "Oct 10, 2023",
                imageUrl = "https://example.com/calculus_limits.jpg"
            ),
            Summary(
                id = 2,
                title = "Calculus II: Integration Techniques",
                content = "This summary explores methods of integration including substitution, integration by parts, partial fractions, and trigonometric substitutions...",
                subject = "Math",
                dateCreated = "Sep 25, 2023"
            ),
            Summary(
                id = 3,
                title = "Linear Algebra: Vector Spaces",
                content = "A comprehensive guide to vector spaces, linear independence, basis, dimension, and linear transformations...",
                subject = "Math",
                dateCreated = "Sep 15, 2023",
                imageUrl = "https://example.com/linear_algebra.jpg"
            ),
            Summary(
                id = 4,
                title = "Differential Equations: First Order",
                content = "This summary covers first order differential equations including separable, linear, exact, and homogeneous equations with applications...",
                subject = "Math",
                dateCreated = "Aug 30, 2023"
            )
        )
    }

    private fun getPhysicsSummaries(): List<Summary> {
        return listOf(
            Summary(
                id = 5,
                title = "Classical Mechanics: Newton's Laws",
                content = "This summary covers Newton's three laws of motion, momentum, forces, circular motion, and applications in various physical systems...",
                subject = "Physics",
                dateCreated = "Oct 8, 2023",
                imageUrl = "https://example.com/mechanics.jpg"
            ),
            Summary(
                id = 6,
                title = "Electromagnetism: Maxwell's Equations",
                content = "This summary presents Maxwell's equations and their implications for electric and magnetic fields, electromagnetic waves, and optics...",
                subject = "Physics",
                dateCreated = "Sep 20, 2023"
            ),
            Summary(
                id = 7,
                title = "Thermodynamics: Laws and Applications",
                content = "A comprehensive guide to the laws of thermodynamics, entropy, heat engines, and thermodynamic processes...",
                subject = "Physics",
                dateCreated = "Sep 5, 2023",
                imageUrl = "https://example.com/thermodynamics.jpg"
            )
        )
    }

    private fun getComputerScienceSummaries(): List<Summary> {
        return listOf(
            Summary(
                id = 8,
                title = "Data Structures & Algorithms",
                content = "This summary covers essential data structures like arrays, linked lists, stacks, queues, trees, and graphs, along with key algorithms for searching, sorting, and traversal...",
                subject = "Computer Science",
                dateCreated = "Oct 5, 2023",
                imageUrl = "https://example.com/algorithms.jpg"
            ),
            Summary(
                id = 9,
                title = "Object-Oriented Programming",
                content = "This summary explores OOP concepts including encapsulation, inheritance, polymorphism, and design patterns...",
                subject = "Computer Science",
                dateCreated = "Sep 18, 2023"
            ),
            Summary(
                id = 10,
                title = "Database Systems",
                content = "A comprehensive guide to relational database design, SQL, normalization, transactions, and concurrency control...",
                subject = "Computer Science",
                dateCreated = "Sep 1, 2023",
                imageUrl = "https://example.com/databases.jpg"
            )
        )
    }

    private fun getChemistrySummaries(): List<Summary> {
        return listOf(
            Summary(
                id = 11,
                title = "Organic Chemistry: Functional Groups",
                content = "This summary covers the major functional groups in organic chemistry, their properties, reactions, and synthesis methods...",
                subject = "Chemistry",
                dateCreated = "Oct 3, 2023",
                imageUrl = "https://example.com/organic_chem.jpg"
            ),
            Summary(
                id = 12,
                title = "Physical Chemistry: Quantum Mechanics",
                content = "This summary explores quantum mechanics principles applied to chemical systems, including the Schrödinger equation, atomic orbitals, and spectroscopy...",
                subject = "Chemistry",
                dateCreated = "Sep 12, 2023"
            ),
            Summary(
                id = 13,
                title = "Biochemistry: Metabolism",
                content = "A comprehensive guide to metabolic pathways including glycolysis, TCA cycle, and electron transport chain...",
                subject = "Chemistry",
                dateCreated = "Aug 25, 2023",
                imageUrl = "https://example.com/biochemistry.jpg"
            )
        )
    }
}