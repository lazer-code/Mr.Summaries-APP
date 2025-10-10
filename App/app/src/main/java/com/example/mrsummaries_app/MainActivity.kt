package com.example.mrsummaries_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.mrsummaries_app.ui.HomeScreen
import com.example.mrsummaries_app.ui.theme.MrSummariesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep edge-to-edge but also hide the status bar for an immersive editor experience
        enableEdgeToEdge()
        // Let the app draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide status bar (use swipe to reveal transiently)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            MrSummariesAppTheme {
                // New homescreen with side-menu file tree
                HomeScreen()
            }
        }
    }
}