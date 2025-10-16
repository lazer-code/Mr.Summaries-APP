package com.example.mrsummaries_app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.databinding.FragmentHomeBinding
import com.example.mrsummaries_app.storage.FileRepository
import com.example.mrsummaries_app.ui.note.NoteEditorActivity

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: FileRepository

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = FileRepository(requireContext())

        // Quick Note: create a new note and open the editor directly
        binding.quickNoteCard.setOnClickListener {
            try {
                val rel = repo.createNote("Quick Note") // stored in root
                val abs = repo.getAbsolutePath(rel)
                val intent = Intent(requireContext(), NoteEditorActivity::class.java)
                intent.putExtra("note_path", abs)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Search -> open summaries (you can pass search text via shared ViewModel if needed)
        binding.searchButton.setOnClickListener {
            val text = binding.searchInput.text?.toString().orEmpty()
            findNavController().navigate(R.id.summariesFragment)
        }

        binding.searchInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                findNavController().navigate(R.id.summariesFragment)
                true
            } else false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}