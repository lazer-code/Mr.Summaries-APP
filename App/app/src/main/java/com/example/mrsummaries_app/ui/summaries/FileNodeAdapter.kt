package com.example.mrsummaries_app.ui.summaries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R

/**
 * Adapter for displaying FileNode items (folders + markdown files).
 * Uses the existing res/layout/item_summary.xml which contains:
 *  - ImageView id = icon_summary
 *  - TextView id = tv_title
 *  - TextView id = tv_course
 *  - TextView id = tv_description
 *
 * This implementation uses findViewById and therefore does not depend on generated view binding.
 */
class FileNodeAdapter(
    private val onClick: (FileNode) -> Unit
) : ListAdapter<FileNode, FileNodeAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FileNode>() {
            override fun areItemsTheSame(oldItem: FileNode, newItem: FileNode): Boolean =
                oldItem.path == newItem.path

            override fun areContentsTheSame(oldItem: FileNode, newItem: FileNode): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView? = view.findViewById(R.id.icon_summary)
        private val title: TextView? = view.findViewById(R.id.tv_title)

        fun bind(node: FileNode) {
            title?.text = node.name
            icon?.setImageResource(if (node.isFolder) R.drawable.ic_folder else R.drawable.ic_summaries)
            itemView.setOnClickListener { onClick(node) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_summary, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}