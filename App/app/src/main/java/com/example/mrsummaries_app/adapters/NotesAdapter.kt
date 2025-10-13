package com.example.mrsummaries_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * NotesAdapter - uses manual view lookups instead of generated binding classes.
 *
 * This avoids issues when binding classes haven't been generated yet.
 * If you prefer to use View/DataBinding, make sure:
 *  - dataBinding/viewBinding is enabled in module build.gradle
 *  - layout file res/layout/item_note.xml exists and is valid
 *  - rebuild the project so ItemNoteBinding is generated
 */
class NotesAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onFavoriteClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Views from item_note.xml (ensure IDs match)
        private val titleTv: TextView = itemView.findViewById(R.id.text_note_title)
        private val dateTv: TextView = itemView.findViewById(R.id.text_note_date)
        private val subjectTv: TextView = itemView.findViewById(R.id.text_note_subject)
        private val previewTv: TextView = itemView.findViewById(R.id.text_note_preview)
        private val favoriteBtn: ImageButton = itemView.findViewById(R.id.button_favorite)
        private val drawingIcon: ImageView = itemView.findViewById(R.id.icon_drawing)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }

            favoriteBtn.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onFavoriteClick(getItem(pos))
                }
            }
        }

        fun bind(note: Note) {
            titleTv.text = note.title
            dateTv.text = dateFormat.format(note.dateModified)
            subjectTv.text = note.subject

            // Preview
            previewTv.text = if (note.content.length > 50) {
                "${note.content.substring(0, 50)}..."
            } else {
                note.content
            }

            // Favorite icon
            val favoriteIconRes = if (note.isFavorite) {
                R.drawable.ic_favorite_filled
            } else {
                R.drawable.ic_favorite_outline
            }
            favoriteBtn.setImageResource(favoriteIconRes)

            // Drawing indicator
            drawingIcon.visibility = if (!note.drawingData.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem == newItem
    }
}