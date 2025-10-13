package com.example.mrsummaries_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.Folder
import com.example.mrsummaries_app.models.Note

class FolderAdapter(
    private var items: MutableList<Any> = mutableListOf(),
    private val onFolderClicked: (Folder) -> Unit,
    private val onNoteClicked: (Note) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FOLDER = 1
        private const val VIEW_TYPE_NOTE = 2
    }

    private var selectedFolderId: String? = null

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val folderName: TextView = view.findViewById(R.id.folderName)
        private val folderIcon: ImageView = view.findViewById(R.id.folderIcon)

        fun bind(folder: Folder) {
            folderName.text = folder.name

            // Set selected state
            if (folder.id == selectedFolderId) {
                itemView.setBackgroundResource(R.color.folder_selected_bg)
            } else {
                itemView.background = null
            }

            itemView.setOnClickListener {
                onFolderClicked(folder)
                selectedFolderId = folder.id
                notifyDataSetChanged() // Update selection visually
            }
        }
    }

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val noteName: TextView = view.findViewById(R.id.noteName)

        fun bind(note: Note) {
            noteName.text = note.title
            itemView.setOnClickListener {
                onNoteClicked(note)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(view)
            }
            VIEW_TYPE_NOTE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_note, parent, false)
                NoteViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FolderViewHolder -> holder.bind(items[position] as Folder)
            is NoteViewHolder -> holder.bind(items[position] as Note)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Folder -> VIEW_TYPE_FOLDER
            is Note -> VIEW_TYPE_NOTE
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    fun updateItems(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}