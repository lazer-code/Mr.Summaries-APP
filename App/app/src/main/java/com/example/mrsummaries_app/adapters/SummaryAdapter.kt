package com.example.mrsummaries_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.Summary

class SummaryAdapter(
    private val onItemClick: (Summary) -> Unit
) : ListAdapter<Summary, SummaryAdapter.SummaryViewHolder>(SummaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SummaryViewHolder(
        private val itemViewRoot: View
    ) : RecyclerView.ViewHolder(itemViewRoot) {

        private val titleTv: TextView = itemViewRoot.findViewById(R.id.text_summary_title)
        private val subjectTv: TextView = itemViewRoot.findViewById(R.id.text_summary_subject)
        private val dateTv: TextView = itemViewRoot.findViewById(R.id.text_summary_date)
        private val previewTv: TextView = itemViewRoot.findViewById(R.id.text_summary_preview)
        private val imageView: ImageView = itemViewRoot.findViewById(R.id.image_summary)

        init {
            itemViewRoot.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(summary: Summary) {
            titleTv.text = summary.title
            subjectTv.text = summary.subject
            dateTv.text = summary.dateCreated

            previewTv.text = if (summary.content.length > 100) {
                "${summary.content.substring(0, 100)}..."
            } else {
                summary.content
            }

            if (!summary.imageUrl.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                Glide.with(imageView.context)
                    .load(summary.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_summary)
                    .into(imageView)
            } else {
                imageView.visibility = View.GONE
                imageView.setImageDrawable(null)
            }
        }
    }

    class SummaryDiffCallback : DiffUtil.ItemCallback<Summary>() {
        override fun areItemsTheSame(oldItem: Summary, newItem: Summary): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Summary, newItem: Summary): Boolean {
            return oldItem == newItem
        }
    }
}