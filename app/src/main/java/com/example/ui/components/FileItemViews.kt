package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.ui.theme.CategoryApks
import com.example.ui.theme.CategoryArchives
import com.example.ui.theme.CategoryAudio
import com.example.ui.theme.CategoryDocs
import com.example.ui.theme.CategoryImages
import com.example.ui.theme.CategoryVideos
import com.example.ui.theme.PrimaryBlue

@Composable
fun getFileIconAndColor(fileInfo: FileInfo): Pair<ImageVector, Color> {
    if (fileInfo.isDirectory) {
        return Pair(Icons.Rounded.Folder, PrimaryBlue)
    }
    return when (fileInfo.category) {
        FileCategory.IMAGES -> Pair(Icons.Default.Image, CategoryImages)
        FileCategory.VIDEOS -> Pair(Icons.Default.Movie, CategoryVideos)
        FileCategory.AUDIO -> Pair(Icons.Default.Audiotrack, CategoryAudio)
        FileCategory.DOCUMENTS -> Pair(Icons.Default.Description, CategoryDocs)
        FileCategory.ARCHIVES -> Pair(Icons.Default.FolderZip, CategoryArchives)
        FileCategory.APKS -> Pair(Icons.Default.Android, CategoryApks)
        else -> Pair(Icons.Default.Description, Color.Gray)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileInfo: FileInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCompress: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val (icon, tint) = getFileIconAndColor(fileInfo)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onSelectToggle() else onClick()
                },
                onLongClick = onLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox or Thumbnail
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (fileInfo.category == FileCategory.IMAGES && !fileInfo.isDirectory) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fileInfo.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = fileInfo.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Name & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fileInfo.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = fileInfo.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Menu
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "File Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FileActionDropdownMenu(
                        expanded = showMenu,
                        fileInfo = fileInfo,
                        onDismiss = { showMenu = false },
                        onOpen = onOpen,
                        onRename = onRename,
                        onDelete = onDelete,
                        onCopy = onCopy,
                        onMove = onMove,
                        onDuplicate = onDuplicate,
                        onShare = onShare,
                        onToggleFavorite = onToggleFavorite,
                        onCompress = onCompress,
                        onDetails = onDetails
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    fileInfo: FileInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = getFileIconAndColor(fileInfo)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (isSelectionMode) onSelectToggle() else onClick() },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (fileInfo.category == FileCategory.IMAGES && !fileInfo.isDirectory) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fileInfo.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = fileInfo.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(40.dp)
                    )
                }

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = fileInfo.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = fileInfo.formattedSize,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FileActionDropdownMenu(
    expanded: Boolean,
    fileInfo: FileInfo,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCompress: () -> Unit,
    onDetails: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Open") },
            onClick = { onDismiss(); onOpen() }
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = { onDismiss(); onCopy() }
        )
        DropdownMenuItem(
            text = { Text("Move / Cut") },
            onClick = { onDismiss(); onMove() }
        )
        DropdownMenuItem(
            text = { Text("Duplicate") },
            onClick = { onDismiss(); onDuplicate() }
        )
        if (!fileInfo.isDirectory) {
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { onDismiss(); onShare() }
            )
        }
        DropdownMenuItem(
            text = { Text("Add to Favorites") },
            onClick = { onDismiss(); onToggleFavorite() }
        )
        DropdownMenuItem(
            text = { Text("Compress to ZIP") },
            onClick = { onDismiss(); onCompress() }
        )
        DropdownMenuItem(
            text = { Text("Details & Checksum") },
            onClick = { onDismiss(); onDetails() }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}
