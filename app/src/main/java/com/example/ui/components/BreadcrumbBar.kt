package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun BreadcrumbBar(
    currentDirectory: File,
    onNavigateToDir: (File) -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateUp: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Break current path into segments
    val segments = mutableListOf<File>()
    var curr: File? = currentDirectory
    while (curr != null && curr.canRead()) {
        segments.add(0, curr)
        curr = curr.parentFile
    }

    LaunchedEffect(currentDirectory) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateUp,
            enabled = canNavigateUp,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigate Up",
                tint = if (canNavigateUp) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            segments.forEachIndexed { index, file ->
                val isLast = index == segments.size - 1
                val displayName = when {
                    file.name.isEmpty() -> "Root"
                    file.name == "0" && file.parentFile?.name == "emulated" -> "Internal Storage"
                    else -> file.name
                }

                SuggestionChip(
                    onClick = { onNavigateToDir(file) },
                    label = {
                        Text(
                            text = displayName,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    icon = if (index == 0) {
                        {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else null,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isLast) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else Color.Transparent
                    ),
                    border = null
                )

                if (!isLast) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
