package com.example.mrsummaries_app.filesystem.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mrsummaries_app.filesystem.model.FileSystemItem
import com.example.mrsummaries_app.filesystem.model.FileSystemItemType
import com.example.mrsummaries_app.filesystem.model.Folder
import com.example.mrsummaries_app.filesystem.viewmodel.FileSystemViewModel

@Composable
fun FolderTree(
    onFolderSelected: (String) -> Unit,
    viewModel: FileSystemViewModel = viewModel()
) {
    val allItems by viewModel.allItems.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()

    // Build folder hierarchy for tree view
    val rootItems = remember(allItems) {
        buildFolderHierarchy(allItems)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Folders",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // Root folder
        item {
            FolderTreeItem(
                folder = Folder(id = "root", name = "My Files"),
                isSelected = currentFolderId == "root",
                depth = 0,
                expandedFolders = viewModel.expandedFolders,
                children = rootItems,
                onFolderClick = onFolderSelected,
                onExpandToggle = { viewModel.toggleFolderExpansion(it) }
            )
        }
    }
}

@Composable
fun FolderTreeItem(
    folder: Folder,
    isSelected: Boolean,
    depth: Int,
    expandedFolders: Map<String, Boolean>,
    children: List<FileSystemItem>,
    onFolderClick: (String) -> Unit,
    onExpandToggle: (String) -> Unit
) {
    val isExpanded = expandedFolders[folder.id] ?: false
    val hasChildren = children.isNotEmpty()
    val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

    Column {
        // Folder row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFolderClick(folder.id) }
                .padding(
                    start = (depth * 16).dp + 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse chevron
            if (hasChildren) {
                IconButton(
                    onClick = { onExpandToggle(folder.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Folder icon
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Folder name
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Child folders (if expanded)
        AnimatedVisibility(
            visible = isExpanded && hasChildren,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                children.forEach { child ->
                    if (child is Folder) {
                        // Recursive call for nested folders
                        val grandChildren = buildFolderHierarchy(
                            items = buildFolderHierarchy(items = children),
                            parentId = child.id
                        )
                        FolderTreeItem(
                            folder = child,
                            isSelected = isSelected,
                            depth = depth + 1,
                            expandedFolders = expandedFolders,
                            children = grandChildren,
                            onFolderClick = onFolderClick,
                            onExpandToggle = onExpandToggle
                        )
                    }
                }
            }
        }
    }
}

// Helper function to build folder hierarchy for tree view
private fun buildFolderHierarchy(
    items: List<FileSystemItem>,
    parentId: String? = null
): List<FileSystemItem> {
    return items.filter {
        it.parentId == parentId && (it.type == FileSystemItemType.FOLDER)
    }
}