package com.example.mrsummaries_app.ui.summaries

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.databinding.ItemSummaryBinding
import com.example.mrsummaries_app.network.GitHubContentItem

class SummariesAdapter(private val onClick: (GitHubContentItem) -> Unit) :
    ListAdapter<GitHubContentItem, SummariesAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GitHubContentItem>() {
            override fun areItemsTheSame(oldItem: GitHubContentItem, newItem: GitHubContentItem) = oldItem.path == newItem.path
            override fun areContentsTheSame(oldItem: GitHubContentItem, newItem: GitHubContentItem) = oldItem == newItem
        }
    }

    inner class VH(private val binding: ItemSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GitHubContentItem) {
            binding.tvTitle.text = item.name
            binding.tvCourse.text = "Course"
            binding.tvDescription.text = "Tap to open summary"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}