package com.example.mrsummaries_app.ui.notes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.databinding.FragmentNotesBinding
import com.example.mrsummaries_app.storage.FileRepository
import com.example.mrsummaries_app.ui.note.NoteEditorActivity

/**
 * Notes UI:
 * - Shows folders and notes in a filesystem.
 * - Supports infinite nested folders (pathStack).
 * - Creating a note requires a non-empty name and is saved in the currently displayed folder.
 */
class NotesFragment : Fragment() {
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: FileRepository

    private val pathStack = ArrayDeque<String>() // empty = root
    private val adapter = FilesAdapter { node ->
        if (node.isFolder) {
            pathStack.addLast(node.relativePath)
            showCurrentFolder()
        } else {
            // open editor: convert relative path -> absolute path
            val abs = repo.getAbsolutePath(node.relativePath)
            val intent = Intent(requireContext(), NoteEditorActivity::class.java)
            intent.putExtra("note_path", abs)
            startActivity(intent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        repo = FileRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter

        // FABs
        binding.fabNewFolder.setOnClickListener { showCreateFolderDialog() }
        binding.fabNewNote.setOnClickListener { showCreateNoteDialog() }

        // Back navigation: pop folder stack first
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (pathStack.isNotEmpty()) {
                pathStack.removeLast()
                showCurrentFolder()
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }

        showCurrentFolder()
    }

    private fun showCurrentFolder() {
        val prefix = pathStack.joinToString(separator = "/").trim('/')
        binding.tvCurrentFolder.text = if (prefix.isEmpty()) "Root" else prefix
        val nodes = repo.listFolderContents(prefix)
        adapter.submitList(nodes)
    }

    private fun showCreateFolderDialog() {
        val edit = EditText(requireContext())
        edit.hint = "Folder name"
        edit.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
            .setTitle("Create folder")
            .setView(edit)
            .setPositiveButton("Create") { d, _ ->
                val folderName = edit.text.toString().trim()
                if (folderName.isEmpty()) {
                    Toast.makeText(requireContext(), "Folder name required", Toast.LENGTH_SHORT).show()
                } else {
                    val current = pathStack.joinToString("/").trim('/')
                    val full = if (current.isEmpty()) folderName else "$current/$folderName"
                    repo.createFolder(full)
                    showCurrentFolder()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateNoteDialog() {
        val edit = EditText(requireContext())
        edit.hint = "Note title (required)"
        edit.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
            .setTitle("Create note")
            .setView(edit)
            .setPositiveButton("Create") { d, _ ->
                val title = edit.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Note title is required", Toast.LENGTH_SHORT).show()
                } else {
                    val current = pathStack.joinToString("/").trim('/')
                    val rel = repo.createNote(title, current)
                    val abs = repo.getAbsolutePath(rel)
                    val intent = Intent(requireContext(), NoteEditorActivity::class.java)
                    intent.putExtra("note_path", abs)
                    startActivity(intent)
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        showCurrentFolder()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}