package com.example.mrsummaries_app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.mrsummaries_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity - conservative implementation that looks up views by id.
 *
 * This version expects:
 *  - a NavHostFragment with id = @id/nav_host_fragment in activity_main.xml
 *  - a BottomNavigationView with id = @id/bottom_nav_view in activity_main.xml
 *  - a navigation graph at res/navigation/nav_graph.xml
 *  - a menu resource at res/menu/menu_bottom_nav.xml
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find NavHostFragment and its NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        // Find BottomNavigationView and hook it up
        val bottomNav = findViewById<BottomNavigationView?>(R.id.bottom_nav_view)
        if (navController != null && bottomNav != null) {
            NavigationUI.setupWithNavController(bottomNav, navController)

            // Optional: setup ActionBar with navigation to display titles / up button correctly
            val topLevelDestinations = setOf(
                R.id.homeFragment,
                R.id.notesFragment,
                R.id.summariesFragment,
                R.id.profileFragment
            )
            val appBarConfig = AppBarConfiguration(topLevelDestinations)
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
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
}