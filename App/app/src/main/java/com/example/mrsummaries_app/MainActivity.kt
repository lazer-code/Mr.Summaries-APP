package com.example.mrsummaries_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import com.example.mrsummaries_app.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

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
}