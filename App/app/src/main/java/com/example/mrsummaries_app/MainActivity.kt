package com.example.mrsummaries_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
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

enum class DrawingTool { WRITE, ERASE }

@Composable
fun DrawingScreen() {
    var paths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var undonePaths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var drawingTool by remember { mutableStateOf(DrawingTool.WRITE) }

    Box(modifier = Modifier.fillMaxSize()) {
        StylusDrawingCanvas(
            modifier = Modifier.fillMaxSize(),
            drawingTool = drawingTool,
            paths = paths,
            currentPath = currentPath,
            onPathAdded = { path ->
                paths = paths + listOf(path)
                undonePaths = emptyList()
                currentPath = emptyList()
            },
            onErasePath = { index ->
                if (index in paths.indices) {
                    undonePaths = undonePaths + paths[index]
                    paths = paths.toMutableList().apply { removeAt(index) }
                }
            }
        )
        HoverBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            drawingTool = drawingTool,
            onToolChange = { drawingTool = it },
            onUndo = {
                if (paths.isNotEmpty()) {
                    undonePaths = undonePaths + paths.last()
                    paths = paths.dropLast(1)
                }
            },
            onRedo = {
                if (undonePaths.isNotEmpty()) {
                    paths = paths + undonePaths.last()
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
        IconButton(
            onClick = { onToolChange(DrawingTool.WRITE) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (drawingTool == DrawingTool.WRITE) Color.LightGray else Color.Transparent
            )
        ) {
            Icon(Icons.Filled.Create, contentDescription = "Write")
        }
        IconButton(
            onClick = { onToolChange(DrawingTool.ERASE) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (drawingTool == DrawingTool.ERASE) Color.LightGray else Color.Transparent
            )
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Erase")
        }
        IconButton(onClick = onUndo) {
            Icon(Icons.Filled.Undo, contentDescription = "Undo")
        }
        IconButton(onClick = onRedo) {
            Icon(Icons.Filled.Redo, contentDescription = "Redo")
        }
    }
}