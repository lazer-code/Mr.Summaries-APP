package com.example.mrsummaries_app.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.storage.FileRepository

/**
 * FilesAdapter that uses findViewById so it does not depend on generated binding names.
 * Uses the following IDs in res/layout/item_note_file.xml:
 *  - tv_file_name
 *  - tv_file_path
 *  - iv_icon
 *  - iv_more (optional)
 */
class FilesAdapter(private val onClick: (FileRepository.FileSystemNode) -> Unit) :
    ListAdapter<FileRepository.FileSystemNode, FilesAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FileRepository.FileSystemNode>() {
            override fun areItemsTheSame(oldItem: FileRepository.FileSystemNode, newItem: FileRepository.FileSystemNode) =
                oldItem.relativePath == newItem.relativePath

            override fun areContentsTheSame(oldItem: FileRepository.FileSystemNode, newItem: FileRepository.FileSystemNode) =
                oldItem == newItem
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView? = view.findViewById(R.id.tv_file_name)
        private val tvPath: TextView? = view.findViewById(R.id.tv_file_path)
        private val ivIcon: ImageView? = view.findViewById(R.id.iv_icon)
        private val ivMore: ImageView? = view.findViewById(R.id.iv_more)

        fun bind(node: FileRepository.FileSystemNode) {
            tvName?.text = node.name
            tvPath?.text = node.relativePath
            ivIcon?.setImageResource(if (node.isFolder) R.drawable.ic_folder else R.drawable.ic_note)
            itemView.setOnClickListener { onClick(node) }

            // Optional more button handler (safe if ivMore is absent)
            ivMore?.setOnClickListener {
                // TODO: show context menu (rename/delete/move)
                onClick(node) // currently performs same behavior as item click
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}