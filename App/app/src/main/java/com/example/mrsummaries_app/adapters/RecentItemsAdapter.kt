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

class RecentItemsAdapter(
    private val items: List<Summary>,
    private val onItemClick: (Summary) -> Unit
) : RecyclerView.Adapter<RecentItemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.title_text_view)
        val authorTextView: TextView = view.findViewById(R.id.author_text_view)
        val dateTextView: TextView = view.findViewById(R.id.date_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.titleTextView.text = item.title
        holder.authorTextView.text = item.author

        // Format the date
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        holder.dateTextView.text = sdf.format(Date(item.timestamp))

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size
}