package com.example.mrsummaries_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.mrsummaries_app.ui.theme.MrSummariesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MrSummariesAppTheme {
                DrawingScreen()
            }
        }
    }
}

@Composable
fun DrawingScreen() {
    var paths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var undonePaths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }

    // persistent on-screen Erase toggle
    var eraseToggled by remember { mutableStateOf(false) }
    // SPen physical button state reported by StylusDrawingCanvas
    var spenPressed by remember { mutableStateOf(false) }

    // effective tool: ERASE while SPen pressed or when erase toggled
    val drawingTool = if (spenPressed || eraseToggled) DrawingTool.ERASE else DrawingTool.WRITE

    Box(modifier = Modifier.fillMaxSize()) {
        StylusDrawingCanvas(
            modifier = Modifier.fillMaxSize(),
            drawingTool = drawingTool,
            paths = paths,
            currentPath = currentPath,
            onCurrentPathChange = { currentPath = it },
            onPathAdded = { path ->
                paths = paths + listOf<List<Offset>>(path)
                undonePaths = emptyList()
                currentPath = emptyList()
            },
            onErasePath = { index ->
                if (index in paths.indices) {
                    undonePaths = undonePaths + listOf<List<Offset>>(paths[index])
                    paths = paths.toMutableList().apply { removeAt(index) }
                }
            },
            onStylusButtonChange = { pressed ->
                spenPressed = pressed
            }
        )

        HoverBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            drawingTool = drawingTool,
            // Explicitly annotate the lambda parameter type to avoid "Cannot infer type" errors
            onToolChange = { tool: DrawingTool ->
                if (tool == DrawingTool.ERASE) {
                    // toggle persistent erase when user taps on-screen Erase button
                    eraseToggled = !eraseToggled
                } else {
                    // Write button clears persistent erase
                    eraseToggled = false
                }
            },
            onUndo = {
                if (paths.isNotEmpty()) {
                    undonePaths = undonePaths + listOf<List<Offset>>(paths.last())
                    paths = paths.dropLast(1)
                }
            },
            onRedo = {
                if (undonePaths.isNotEmpty()) {
                    paths = paths + listOf<List<Offset>>(undonePaths.last())
                    undonePaths = undonePaths.dropLast(1)
                }
            }
        )
    }
}

@Composable
fun HoverBar(
    modifier: Modifier = Modifier,
    drawingTool: DrawingTool,
    onToolChange: (DrawingTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xCCFFFFFF),
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { onToolChange(DrawingTool.WRITE) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (drawingTool == DrawingTool.WRITE) Color.LightGray else Color.Transparent
            )
        ) { Text("Write") }

        Button(
            onClick = { onToolChange(DrawingTool.ERASE) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (drawingTool == DrawingTool.ERASE) Color.LightGray else Color.Transparent
            )
        ) { Text("Erase") }

        Button(onClick = onUndo) { Text("Undo") }
        Button(onClick = onRedo) { Text("Redo") }
    }
}