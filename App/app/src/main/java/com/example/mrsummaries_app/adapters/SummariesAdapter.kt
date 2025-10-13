package com.example.mrsummaries_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.Summary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummariesAdapter(
    private val summaries: List<Summary>,
    private val onSummaryClick: (Summary) -> Unit
) : RecyclerView.Adapter<SummariesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.summary_title)
        val authorTextView: TextView = view.findViewById(R.id.summary_author)
        val dateTextView: TextView = view.findViewById(R.id.summary_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val summary = summaries[position]

        holder.titleTextView.text = summary.title
        holder.authorTextView.text = "By: ${summary.author}"

        // Format the date
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        holder.dateTextView.text = sdf.format(Date(summary.timestamp))

        // Set click listener
        holder.itemView.setOnClickListener {
            onSummaryClick(summary)
        }
    }

    override fun getItemCount() = summaries.size
}