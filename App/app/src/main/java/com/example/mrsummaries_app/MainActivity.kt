package com.example.mrsummaries_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mrsummaries_app.ui.HomeScreen
import com.example.mrsummaries_app.ui.theme.MrSummariesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MrSummariesAppTheme {
                // New homescreen with side-menu file tree
                HomeScreen()
            }
        }
    }
}