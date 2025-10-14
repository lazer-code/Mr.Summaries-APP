package com.example.mrsummaries_app.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.mrsummaries_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity - defensive implementation that avoids platform-null assumptions
 * and guards ActionBar setup so the Kotlin analyzer/compiler doesn't hit
 * unexpected nullability when the layout or resources are missing.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find NavHostFragment and its NavController, bail out early if missing.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment == null) {
            Log.w(TAG, "NavHostFragment (R.id.nav_host_fragment) not found in activity_main. Navigation setup skipped.")
            return
        }
        val navController = navHostFragment.navController

        // Find BottomNavigationView and hook it up if present.
        val bottomNav = findViewById<BottomNavigationView?>(R.id.bottom_nav_view)
        if (bottomNav == null) {
            Log.w(TAG, "BottomNavigationView (R.id.bottom_nav_view) not found in activity_main. Bottom nav setup skipped.")
            return
        }

        NavigationUI.setupWithNavController(bottomNav, navController)

        // Setup ActionBar with navigation only if we have an ActionBar and resources are present.
        // Wrap in try/catch to avoid analyzer/runtime surprises from missing IDs/resources.
        if (supportActionBar != null) {
            try {
                val topLevelDestinations = setOf(
                    R.id.homeFragment,
                    R.id.notesFragment,
                    R.id.summariesFragment,
                    R.id.profileFragment
                )
                val appBarConfig = AppBarConfiguration(topLevelDestinations)
                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
            } catch (e: Exception) {
                // Log and continue — failing to wire the action bar should not crash the app or compilation step.
                Log.w(TAG, "Failed to set up ActionBar with NavController: ${e.message}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        return if (navController != null) {
            navController.navigateUp() || super.onSupportNavigateUp()
        } else {
            super.onSupportNavigateUp()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}