package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.FileSortOption
import com.example.data.model.FileViewMode
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.FileGridItem
import com.example.ui.components.FileListItem
import com.example.ui.components.MultiSelectActionBar
import java.io.File

@Composable
fun BrowserScreen(
    currentDirectory: File,
    files: List<FileInfo>,
    selectedFiles: Set<FileInfo>,
    isLoading: Boolean,
    viewMode: FileViewMode,
    sortOption: FileSortOption,
    showHidden: Boolean,
    searchQuery: String,
    activeCategory: FileCategory?,
    onNavigateToDir: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenFile: (FileInfo) -> Unit,
    onToggleSelect: (FileInfo) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSetSortOption: (FileSortOption) -> Unit,
    onToggleShowHidden: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearCategory: () -> Unit,
    onRename: (File) -> Unit,
    onDelete: (List<File>) -> Unit,
    onCopy: (List<FileInfo>) -> Unit,
    onCut: (List<FileInfo>) -> Unit,
    onDuplicate: (File) -> Unit,
    onShare: (List<FileInfo>) -> Unit,
    onToggleFavorite: (FileInfo) -> Unit,
    onCompress: (List<File>) -> Unit,
    onDetails: (File) -> Unit,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    val isSelectionMode = selectedFiles.isNotEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (!isSelectionMode && activeCategory == null) {
                Column(horizontalAlignment = Alignment.End) {
                    if (isFabExpanded) {
                        SmallFloatingActionButton(
                            onClick = { isFabExpanded = false; onCreateFile() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New File", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        SmallFloatingActionButton(
                            onClick = { isFabExpanded = false; onCreateFolder() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Folder", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add"
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MultiSelectActionBar(
                    selectedCount = selectedFiles.size,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll,
                    onCopy = { onCopy(selectedFiles.toList()) },
                    onCut = { onCut(selectedFiles.toList()) },
                    onShare = { onShare(selectedFiles.toList()) },
                    onZip = { onCompress(selectedFiles.map { it.file }) },
                    onDelete = { onDelete(selectedFiles.map { it.file }) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Filter Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search files & folders...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // View Mode Toggle
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (viewMode == FileViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Switch View"
                    )
                }

                // Sort Menu
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        FileSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                trailingIcon = if (sortOption == option) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    onSetSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (showHidden) "Hide Hidden Files" else "Show Hidden Files") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onToggleShowHidden()
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Category filter badge if active
            if (activeCategory != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category: ${activeCategory.label}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onClearCategory,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Interactive Breadcrumb Bar
                BreadcrumbBar(
                    currentDirectory = currentDirectory,
                    onNavigateToDir = onNavigateToDir,
                    onNavigateUp = onNavigateUp,
                    canNavigateUp = currentDirectory.parentFile?.canRead() == true
                )
            }

            // File Listing Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching files found" else "This folder is empty",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    if (viewMode == FileViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            items(files, key = { it.path }) { fileInfo ->
                                val isSelected = selectedFiles.contains(fileInfo)
                                FileListItem(
                                    fileInfo = fileInfo,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = { onOpenFile(fileInfo) },
                                    onLongClick = { onToggleSelect(fileInfo) },
                                    onSelectToggle = { onToggleSelect(fileInfo) },
                                    onOpen = { onOpenFile(fileInfo) },
                                    onRename = { onRename(fileInfo.file) },
                                    onDelete = { onDelete(listOf(fileInfo.file)) },
                                    onCopy = { onCopy(listOf(fileInfo)) },
                                    onMove = { onCut(listOf(fileInfo)) },
                                    onDuplicate = { onDuplicate(fileInfo.file) },
                                    onShare = { onShare(listOf(fileInfo)) },
                                    onToggleFavorite = { onToggleFavorite(fileInfo) },
                                    onCompress = { onCompress(listOf(fileInfo.file)) },
                                    onDetails = { onDetails(fileInfo.file) }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(files, key = { it.path }) { fileInfo ->
                                val isSelected = selectedFiles.contains(fileInfo)
                                FileGridItem(
                                    fileInfo = fileInfo,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = { onOpenFile(fileInfo) },
                                    onLongClick = { onToggleSelect(fileInfo) },
                                    onSelectToggle = { onToggleSelect(fileInfo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
