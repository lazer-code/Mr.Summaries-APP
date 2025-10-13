package com.example.mrsummaries_app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.activities.NoteEditorActivity
import com.example.mrsummaries_app.models.Summary

/**
 * SummaryFragment - uses manual view lookups instead of generated ViewBinding / SafeArgs so it
 * compiles even when binding classes or SafeArgs are not set up.
 *
 * Expected layout: res/layout/fragment_summary.xml with IDs:
 *  - @+id/text_summary_title (TextView)
 *  - @+id/text_summary_subject (TextView)
 *  - @+id/text_summary_date (TextView)
 *  - @+id/text_summary_content (TextView)
 *  - @+id/image_summary (ImageView)
 *  - @+id/fab_create_note (View) -> FloatingActionButton or similar
 *
 * Navigation / arguments:
 * - This fragment reads a Summary object from arguments as a Serializable under key "summary".
 *   If you currently navigate using SafeArgs, keep doing so but also ensure the Summary is
 *   passed as Serializable (or adapt to Parcelable). Example (manual navigation):
 *     val bundle = Bundle().apply { putSerializable("summary", summary) }
 *     findNavController().navigate(R.id.summaryFragment, bundle)
 */
class SummaryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout (manual lookups used in onViewCreated)
        return inflater.inflate(R.layout.fragment_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Try to read the Summary from arguments. Expecting it under key "summary" as Serializable.
        val summary = (arguments?.getSerializable("summary") as? Summary)
            ?: run {
                // If no summary supplied, show an error dialog and return
                AlertDialog.Builder(requireContext())
                    .setTitle("Summary not found")
                    .setMessage("No summary data was provided to this screen.")
                    .setPositiveButton("OK") { _, _ -> requireActivity().onBackPressed() }
                    .setCancelable(false)
                    .show()
                return
            }

        // Find views
        val titleTv: TextView? = view.findViewById(R.id.text_summary_title)
        val subjectTv: TextView? = view.findViewById(R.id.text_summary_subject)
        val dateTv: TextView? = view.findViewById(R.id.text_summary_date)
        val contentTv: TextView? = view.findViewById(R.id.text_summary_content)
        val imageView: ImageView? = view.findViewById(R.id.image_summary)
        val fabCreateNote: View? = view.findViewById(R.id.fab_create_note)

        // Populate UI safely
        titleTv?.text = summary.title
        subjectTv?.text = summary.subject
        dateTv?.text = summary.dateCreated
        contentTv?.text = summary.content

        // Load image if available
        if (!summary.imageUrl.isNullOrEmpty()) {
            imageView?.let { iv ->
                iv.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(summary.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_summary)
                    .into(iv)
            }
        } else {
            imageView?.visibility = View.GONE
        }

        // Create note FAB -> start NoteEditorActivity with extras (activity will create a new note if no id passed)
        fabCreateNote?.setOnClickListener {
            createNoteFromSummary(summary)
        }

        // (Optional) If you want a share action in the UI, you can add a button in the layout and wire it:
        // val btnShare: View? = view.findViewById(R.id.btn_share_summary)
        // btnShare?.setOnClickListener { shareSummary(summary) }
        //
        // For now the fragment exposes two helper methods shareSummary() and downloadSummary()
        // that you can call from the UI (if you add buttons) or hook into a menu when menu resources exist.
    }

    private fun createNoteFromSummary(summary: Summary) {
        // Start NoteEditorActivity and pass the summary fields as extras so the activity can pre-fill fields.
        val intent = Intent(requireContext(), NoteEditorActivity::class.java).apply {
            putExtra("note_title", summary.title)
            putExtra("note_content", summary.content)
            putExtra("note_subject", summary.subject)
            // Do not put an id -> activity will treat as a new note. If your activity expects different extras,
            // adapt these keys accordingly.
        }
        startActivity(intent)
    }

    // Utility: share summary via ACTION_SEND
    private fun shareSummary(summary: Summary) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, summary.title)
            putExtra(Intent.EXTRA_TEXT, "${summary.title}\n\n${summary.content}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Summary"))
    }

    // Utility: simple download simulation (replace with real download logic)
    private fun downloadSummary(summary: Summary) {
        AlertDialog.Builder(requireContext())
            .setTitle("Download Summary")
            .setMessage("Summary \"${summary.title}\" downloaded successfully!")
            .setPositiveButton("OK", null)
            .show()
    }
}