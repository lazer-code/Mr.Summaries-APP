package com.example.mrsummaries_app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mrsummaries_app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // In a production app you would wire up preferences and viewmodels. Here placeholders.
        b.switchUiMode.isChecked = false
        b.btnContact.setOnClickListener {
            // show contact info
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}