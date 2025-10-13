package com.example.mrsummaries_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.User
import com.example.mrsummaries_app.util.PreferenceManager

/**
 * ProfileFragment - manual view lookups (avoids generated view binding).
 *
 * Expected layout file: res/layout/fragment_profile.xml
 * Expected view IDs in layout:
 *   - @+id/image_profile          (ImageView)
 *   - @+id/text_user_name         (TextView)
 *   - @+id/text_user_email        (TextView)
 *   - @+id/text_university        (TextView)
 *   - @+id/text_favorite_subjects (TextView)
 *   - @+id/button_edit_profile    (Button)
 *   - @+id/button_app_settings    (Button)
 *   - @+id/button_logout          (Button)
 */
class ProfileFragment : Fragment() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout (manual view lookups are used in onViewCreated)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())

        // Find views
        val imageProfile: ImageView? = view.findViewById(R.id.image_profile)
        val textUserName: TextView? = view.findViewById(R.id.text_user_name)
        val textUserEmail: TextView? = view.findViewById(R.id.text_user_email)
        val textUniversity: TextView? = view.findViewById(R.id.text_university)
        val textFavoriteSubjects: TextView? = view.findViewById(R.id.text_favorite_subjects)
        val buttonEditProfile: Button? = view.findViewById(R.id.button_edit_profile)
        val buttonAppSettings: Button? = view.findViewById(R.id.button_app_settings)
        val buttonLogout: Button? = view.findViewById(R.id.button_logout)

        // Load user profile (mock for now)
        val user = User(
            id = "12345",
            name = "John Doe",
            email = "john.doe@university.edu",
            university = "Example University",
            profileImageUrl = "https://example.com/profile.jpg",
            favoriteSubjects = listOf("Math", "Computer Science")
        )

        // Display user profile safely
        textUserName?.text = user.name
        textUserEmail?.text = user.email
        textUniversity?.text = user.university

        val favoriteSubjects = if (user.favoriteSubjects.isEmpty()) {
            "No favorite subjects"
        } else {
            user.favoriteSubjects.joinToString(", ")
        }
        textFavoriteSubjects?.text = favoriteSubjects

        // Load profile image with Glide (guard against null ImageView)
        imageProfile?.let { iv ->
            Glide.with(requireContext())
                .load(user.profileImageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .circleCrop()
                .into(iv)
        }

        // Edit profile button
        buttonEditProfile?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setMessage("This feature will be available soon!")
                .setPositiveButton("OK", null)
                .show()
        }

        // App settings button -> navigate to settingsFragment if it exists in navigation graph
        buttonAppSettings?.setOnClickListener {
            // Use resources.getIdentifier to avoid compile-time reference to a possibly-missing R.id.settingsFragment.
            // If you prefer, add a destination with id "settingsFragment" to nav_graph.xml and then replace the
            // lookup with findNavController().navigate(R.id.settingsFragment)
            val resId = resources.getIdentifier("settingsFragment", "id", requireContext().packageName)
            if (resId != 0) {
                try {
                    findNavController().navigate(resId)
                } catch (_: Exception) {
                    // If navigation fails at runtime (e.g. wrong graph), show friendly message
                    AlertDialog.Builder(requireContext())
                        .setTitle("Settings")
                        .setMessage("Settings screen is not available yet.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Settings")
                    .setMessage("Settings screen is not available yet.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        // Logout button
        buttonLogout?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    preferenceManager.clearUserSession()
                    // Close the activity stack and return to launcher/login
                    requireActivity().finishAffinity()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}