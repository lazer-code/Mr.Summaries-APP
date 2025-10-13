package com.example.mrsummaries_app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.activities.NoteEditorActivity
import com.example.mrsummaries_app.adapters.NotesAdapter
import com.example.mrsummaries_app.adapters.SummaryAdapter
import com.example.mrsummaries_app.models.Note
import com.example.mrsummaries_app.models.Summary
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar
import java.util.UUID

/**
 * HomeFragment (manual view lookups)
 *
 * This version avoids generated view-binding so it compiles before binding classes
 * are generated. It expects the layout res/layout/fragment_home.xml to expose the
 * following view IDs:
 *  - @+id/card_math
 *  - @+id/card_physics
 *  - @+id/card_computer_science
 *  - @+id/card_chemistry
 *  - @+id/recycler_recent_notes
 *  - @+id/recycler_featured_summaries
 *  - @+id/fab_new_note
 *
 * Navigation:
 *  - Navigates to courseSummariesFragment with a Bundle { "subject" -> subject }
 *  - Navigates to summaryFragment and passes a Serializable "summary" in the Bundle
 *  - Starts NoteEditorActivity for opening/editing notes (passes extras expected by the activity)
 */
class HomeFragment : Fragment() {

    private lateinit var summaryAdapter: SummaryAdapter
    private lateinit var recentNotesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout (replace binding usage with manual findViewById)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Subject cards
        val cardMath: View? = view.findViewById(R.id.card_math)
        val cardPhysics: View? = view.findViewById(R.id.card_physics)
        val cardCS: View? = view.findViewById(R.id.card_computer_science)
        val cardChemistry: View? = view.findViewById(R.id.card_chemistry)

        cardMath?.setOnClickListener { navigateToCourseSummaries("Math") }
        cardPhysics?.setOnClickListener { navigateToCourseSummaries("Physics") }
        cardCS?.setOnClickListener { navigateToCourseSummaries("Computer Science") }
        cardChemistry?.setOnClickListener { navigateToCourseSummaries("Chemistry") }

        // Recent notes RecyclerView
        val recyclerRecentNotes: RecyclerView = view.findViewById(R.id.recycler_recent_notes)
        recentNotesAdapter = NotesAdapter(
            onItemClick = { note ->
                // Start NoteEditorActivity for the selected note (pass expected extras)
                val intent = Intent(requireContext(), NoteEditorActivity::class.java).apply {
                    putExtra("note_id", note.id)
                    putExtra("note_title", note.title)
                    putExtra("note_content", note.content)
                    putExtra("note_subject", note.subject)
                    putExtra("note_date_created", note.dateCreated.time)
                    putExtra("note_date_modified", note.dateModified.time)
                }
                startActivity(intent)
            },
            onFavoriteClick = { note ->
                note.isFavorite = !note.isFavorite
                // In a real app persist to DB; here just update UI list
                val current = recentNotesAdapter.currentList.toMutableList()
                val idx = current.indexOfFirst { it.id == note.id }
                if (idx >= 0) {
                    current[idx] = note
                    recentNotesAdapter.submitList(current)
                }
            }
        )
        recyclerRecentNotes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentNotesAdapter
        }
        recentNotesAdapter.submitList(getMockNotes())

        // Featured summaries RecyclerView
        val recyclerFeaturedSummaries: RecyclerView = view.findViewById(R.id.recycler_featured_summaries)
        summaryAdapter = SummaryAdapter { summary ->
            // Navigate to summaryFragment and pass summary as Serializable in a bundle
            val bundle = Bundle().apply { putSerializable("summary", summary) }
            // Ensure nav_graph contains a destination with id = summaryFragment
            findNavController().navigate(R.id.summaryFragment, bundle)
        }
        recyclerFeaturedSummaries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = summaryAdapter
        }
        summaryAdapter.submitList(getMockSummaries())

        // New note FAB
        val fabNewNote: FloatingActionButton? = view.findViewById(R.id.fab_new_note)
        fabNewNote?.setOnClickListener {
            // Start NoteEditorActivity to create a new note (no extras -> activity will create new)
            val intent = Intent(requireContext(), NoteEditorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToCourseSummaries(subject: String) {
        // Use nav_graph destination id courseSummariesFragment and pass subject via arguments bundle
        val bundle = Bundle().apply { putString("subject", subject) }
        findNavController().navigate(R.id.courseSummariesFragment, bundle)
    }

    private fun getMockNotes(): List<Note> {
        val cal = Calendar.getInstance()
        return listOf(
            Note(
                id = UUID.randomUUID().toString(),
                title = "Calculus Lecture Notes",
                content = "Derivatives and integrals are fundamental concepts in calculus...",
                dateCreated = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time,
                dateModified = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time,
                subject = "Math"
            ),
            Note(
                id = UUID.randomUUID().toString(),
                title = "Physics Formulas",
                content = "F = ma, E = mc², PV = nRT...",
                dateCreated = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -2) }.time,
                dateModified = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -2) }.time,
                subject = "Physics",
                isFavorite = true
            ),
            Note(
                id = UUID.randomUUID().toString(),
                title = "Programming Patterns",
                content = "Observer, Singleton, Factory, Strategy...",
                dateCreated = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -3) }.time,
                dateModified = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -3) }.time,
                subject = "Computer Science"
            ),
            Note(
                id = UUID.randomUUID().toString(),
                title = "Organic Chemistry",
                content = "Alkanes, Alkenes, Alkynes, Aromatic compounds...",
                dateCreated = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -4) }.time,
                dateModified = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -4) }.time,
                subject = "Chemistry",
                drawingData = "drawing data here"
            )
        )
    }

    private fun getMockSummaries(): List<Summary> {
        return listOf(
            Summary(
                id = 1,
                title = "Introduction to Calculus",
                content = "This summary covers the basics of differential calculus, including limits, derivatives, and their applications...",
                subject = "Math",
                dateCreated = "Oct 10, 2023",
                imageUrl = "https://example.com/calculus.jpg"
            ),
            Summary(
                id = 2,
                title = "Newtonian Mechanics",
                content = "This summary explores Newton's laws of motion and their applications in various physical systems...",
                subject = "Physics",
                dateCreated = "Oct 8, 2023"
            ),
            Summary(
                id = 3,
                title = "Data Structures & Algorithms",
                content = "A comprehensive overview of fundamental data structures and algorithms, including arrays, linked lists, trees, sorting, and searching...",
                subject = "Computer Science",
                dateCreated = "Oct 5, 2023",
                imageUrl = "https://example.com/algorithms.jpg"
            )
        )
    }
}