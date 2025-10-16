package com.example.mrsummaries_app

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import com.example.mrsummaries_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Defer tinting until layout pass so the toolbar/drawer background is resolved.
        binding.toolbar.post {
            // Try to read toolbar background color, fallback to theme surface/background if not a plain ColorDrawable
            val bgColor = (binding.toolbar.background as? ColorDrawable)?.color ?: resolveThemeColor(android.R.attr.colorBackground)
            val contrastTint = if (ColorUtils.calculateLuminance(bgColor) < 0.5) Color.WHITE else Color.BLACK

            // Tint toolbar navigation icon (hamburger)
            binding.toolbar.navigationIcon?.setTint(contrastTint)

            // Also tint NavigationView icons & text so items are visible in dark/light drawer backgrounds
            binding.navigationView.itemIconTintList = ColorStateList.valueOf(contrastTint)
            binding.navigationView.itemTextColor = ColorStateList.valueOf(contrastTint)
        }

        // get NavController via NavHostFragment (stable)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navController?.navigate(R.id.homeFragment)
                R.id.nav_quick_note -> navController?.navigate(R.id.notesFragment)
                R.id.nav_summaries -> navController?.navigate(R.id.summariesFragment)
                R.id.nav_notes -> navController?.navigate(R.id.notesFragment)
                R.id.nav_settings -> navController?.navigate(R.id.settingsFragment)
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(this, tv.resourceId) else tv.data
        } else {
            // fallback
            Color.WHITE
        }
    }
}