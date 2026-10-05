package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileInfo
import com.example.data.model.StorageAnalysisResult
import com.example.ui.theme.CategoryApks
import com.example.ui.theme.CategoryArchives
import com.example.ui.theme.CategoryAudio
import com.example.ui.theme.CategoryDocs
import com.example.ui.theme.CategoryImages
import com.example.ui.theme.CategoryOthers
import com.example.ui.theme.CategoryVideos

data class ChartSlice(
    val label: String,
    val bytes: Long,
    val color: Color
)

@Composable
fun StorageCategoryPieChart(
    result: StorageAnalysisResult,
    modifier: Modifier = Modifier
) {
    val totalBytes = (result.imagesBytes + result.videosBytes + result.audioBytes +
            result.documentsBytes + result.archivesBytes + result.apksBytes + result.othersBytes).coerceAtLeast(1L)

    val slices = listOf(
        ChartSlice("Videos", result.videosBytes, CategoryVideos),
        ChartSlice("Images", result.imagesBytes, CategoryImages),
        ChartSlice("Audio", result.audioBytes, CategoryAudio),
        ChartSlice("Documents", result.documentsBytes, CategoryDocs),
        ChartSlice("Archives", result.archivesBytes, CategoryArchives),
        ChartSlice("APKs", result.apksBytes, CategoryApks),
        ChartSlice("Others", result.othersBytes, CategoryOthers)
    ).filter { it.bytes > 0 }

    var selectedSlice by remember { mutableStateOf<ChartSlice?>(null) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Pie/Donut Canvas
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.size(170.dp)) {
                var currentAngle = -90f
                val strokeWidth = 28.dp.toPx()

                for (slice in slices) {
                    val sweep = (slice.bytes.toFloat() / totalBytes.toFloat()) * 360f
                    if (sweep > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = currentAngle,
                            sweepAngle = sweep - 2f, // Subtle gap between slices
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    currentAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val displaySlice = selectedSlice ?: slices.maxByOrNull { it.bytes }
                Text(
                    text = displaySlice?.label ?: "Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = FileInfo.formatFileSize(displaySlice?.bytes ?: totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-segment stacked horizontal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            for (slice in slices) {
                val weight = (slice.bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.005f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(weight)
                        .height(12.dp)
                        .background(slice.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chunked = slices.chunked(2)
            for (row in chunked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (slice in row) {
                        LegendItem(
                            slice = slice,
                            totalBytes = totalBytes,
                            isSelected = selectedSlice == slice,
                            onClick = { selectedSlice = if (selectedSlice == slice) null else slice },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    slice: ChartSlice,
    totalBytes: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = (slice.bytes * 100 / totalBytes).toInt()

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(slice.color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${FileInfo.formatFileSize(slice.bytes)} ($pct%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StorageTreemapCard(
    result: StorageAnalysisResult,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        ChartSlice("Videos", result.videosBytes, CategoryVideos),
        ChartSlice("Images", result.imagesBytes, CategoryImages),
        ChartSlice("Audio", result.audioBytes, CategoryAudio),
        ChartSlice("Docs", result.documentsBytes, CategoryDocs),
        ChartSlice("Archives", result.archivesBytes, CategoryArchives),
        ChartSlice("APKs", result.apksBytes, CategoryApks),
        ChartSlice("Others", result.othersBytes, CategoryOthers)
    ).filter { it.bytes > 0 }.sortedByDescending { it.bytes }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Treemap Representation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2-row simulated treemap layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val primaryCat = categories.firstOrNull()
                if (primaryCat != null) {
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryCat.color.copy(alpha = 0.85f))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(
                                text = primaryCat.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = FileInfo.formatFileSize(primaryCat.bytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val second = categories.getOrNull(1)
                    val third = categories.getOrNull(2)

                    if (second != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(second.color.copy(alpha = 0.85f))
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    text = second.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = FileInfo.formatFileSize(second.bytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (third != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(third.color.copy(alpha = 0.85f))
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    text = third.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = FileInfo.formatFileSize(third.bytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
