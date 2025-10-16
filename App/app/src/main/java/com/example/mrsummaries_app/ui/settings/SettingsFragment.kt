package com.example.mrsummaries_app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
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
        // Wire the UI mode switch to toggle light/dark immediately.
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        // Set initial state: if night mode is enabled or system follows night, check the switch appropriately.
        b.switchUiMode.isChecked = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> false // MODE_NIGHT_FOLLOW_SYSTEM -> treat as false here; system UI will decide
        }

        b.switchUiMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Optionally show a small hint to the user that app will recreate to apply theme.
        }

        b.btnContact.setOnClickListener {
            // show contact info
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}