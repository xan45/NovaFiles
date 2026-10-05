package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileInfo
import com.example.ui.viewmodel.ActivePane
import java.io.File

@Composable
fun DualPaneView(
    leftDirectory: File,
    leftFiles: List<FileInfo>,
    leftSelected: Set<FileInfo>,
    rightDirectory: File,
    rightFiles: List<FileInfo>,
    rightSelected: Set<FileInfo>,
    activePane: ActivePane,
    onSetActivePane: (ActivePane) -> Unit,
    onNavigateLeft: (File) -> Unit,
    onNavigateRight: (File) -> Unit,
    onToggleLeftSelect: (FileInfo) -> Unit,
    onToggleRightSelect: (FileInfo) -> Unit,
    onTransfer: (fromLeft: Boolean, isMove: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 650.dp

        if (isWideScreen) {
            // Horizontal split (Left & Right side by side)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Left Pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    SinglePanePanel(
                        title = "Left Panel",
                        directory = leftDirectory,
                        files = leftFiles,
                        selectedFiles = leftSelected,
                        isActive = activePane == ActivePane.LEFT,
                        onActivate = { onSetActivePane(ActivePane.LEFT) },
                        onNavigate = onNavigateLeft,
                        onToggleSelect = onToggleLeftSelect
                    )
                }

                // Middle Transfer Toolbar
                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilledTonalButton(
                        onClick = { onTransfer(true, false) },
                        enabled = leftSelected.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Copy to Right")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { onTransfer(true, true) },
                        enabled = leftSelected.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move to Right", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = { onTransfer(false, false) },
                        enabled = rightSelected.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Copy to Left")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { onTransfer(false, true) },
                        enabled = rightSelected.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move to Left", modifier = Modifier.size(16.dp))
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Right Pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    SinglePanePanel(
                        title = "Right Panel",
                        directory = rightDirectory,
                        files = rightFiles,
                        selectedFiles = rightSelected,
                        isActive = activePane == ActivePane.RIGHT,
                        onActivate = { onSetActivePane(ActivePane.RIGHT) },
                        onNavigate = onNavigateRight,
                        onToggleSelect = onToggleRightSelect
                    )
                }
            }
        } else {
            // Compact Phone: Tabbed view with dual status
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = if (activePane == ActivePane.LEFT) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = activePane == ActivePane.LEFT,
                        onClick = { onSetActivePane(ActivePane.LEFT) },
                        text = {
                            Text(
                                "Left: ${leftDirectory.name.ifEmpty { "Root" }} (${leftSelected.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = activePane == ActivePane.RIGHT,
                        onClick = { onSetActivePane(ActivePane.RIGHT) },
                        text = {
                            Text(
                                "Right: ${rightDirectory.name.ifEmpty { "Root" }} (${rightSelected.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                // Active Panel View
                Box(modifier = Modifier.weight(1f)) {
                    if (activePane == ActivePane.LEFT) {
                        SinglePanePanel(
                            title = "Left Panel",
                            directory = leftDirectory,
                            files = leftFiles,
                            selectedFiles = leftSelected,
                            isActive = true,
                            onActivate = {},
                            onNavigate = onNavigateLeft,
                            onToggleSelect = onToggleLeftSelect
                        )
                    } else {
                        SinglePanePanel(
                            title = "Right Panel",
                            directory = rightDirectory,
                            files = rightFiles,
                            selectedFiles = rightSelected,
                            isActive = true,
                            onActivate = {},
                            onNavigate = onNavigateRight,
                            onToggleSelect = onToggleRightSelect
                        )
                    }
                }

                // Transfer Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isLeftActive = activePane == ActivePane.LEFT
                        val activeSelected = if (isLeftActive) leftSelected else rightSelected
                        val otherPaneName = if (isLeftActive) "Right" else "Left"

                        Button(
                            onClick = { onTransfer(isLeftActive, false) },
                            enabled = activeSelected.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy -> $otherPaneName")
                        }

                        Button(
                            onClick = { onTransfer(isLeftActive, true) },
                            enabled = activeSelected.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Move -> $otherPaneName")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SinglePanePanel(
    title: String,
    directory: File,
    files: List<FileInfo>,
    selectedFiles: Set<FileInfo>,
    isActive: Boolean,
    onActivate: () -> Unit,
    onNavigate: (File) -> Unit,
    onToggleSelect: (FileInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Directory name and up button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .clickable { onActivate() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val parent = directory.parentFile
                IconButton(
                    onClick = { if (parent != null && parent.canRead()) onNavigate(parent) },
                    enabled = parent != null && parent.canRead(),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = directory.name.ifEmpty { "Storage Root" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${files.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // File listing
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                items(files, key = { it.path }) { fileInfo ->
                    val isSelected = selectedFiles.contains(fileInfo)
                    val (icon, tint) = getFileIconAndColor(fileInfo)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (fileInfo.isDirectory) {
                                    onNavigate(fileInfo.file)
                                } else {
                                    onToggleSelect(fileInfo)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(tint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileInfo.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = fileInfo.formattedSize,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Selection tick
                        IconButton(
                            onClick = { onToggleSelect(fileInfo) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
