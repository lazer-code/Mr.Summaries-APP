package com.example.mrsummaries_app.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.util.PreferenceManager

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 1500L // 1.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide the action bar
        supportActionBar?.hide()

        // Delay and then navigate to the appropriate screen
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }

    private fun navigateToNextScreen() {
        val preferenceManager = PreferenceManager(this)

        // Check if user is logged in (for a real app with authentication)
        // For now, we'll go straight to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the splash activity
    }
}