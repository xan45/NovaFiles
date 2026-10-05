package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileInfo
import com.example.data.tools.FileComparisonResult
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.glassBorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileCompareScreen(
    fileA: File?,
    fileB: File?,
    comparisonResult: FileComparisonResult?,
    modifier: Modifier = Modifier
) {
    val themeMode = LocalAppThemeMode.current
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "File Comparison Tool",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Side-by-side metadata and cryptographic hash verification",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (fileA == null || fileB == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(themeMode, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select two files to compare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select files from the file browser and tap Compare",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (comparisonResult != null) {
            // Overall Verdict Banner
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (comparisonResult.isExactContentMatch) SecondaryTeal.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(themeMode, shape = RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (comparisonResult.isExactContentMatch) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (comparisonResult.isExactContentMatch) SecondaryTeal else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (comparisonResult.isExactContentMatch) "Exact Content Match!" else "Files Are Different",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (comparisonResult.isExactContentMatch) SecondaryTeal else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (comparisonResult.isExactContentMatch) "SHA-256 and MD5 hashes match 100% identically."
                                else "File contents or sizes do not match.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Comparison Table
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(themeMode, shape = RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Properties Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // File Names
                        CompareRow(
                            label = "File Name",
                            valA = fileA.name,
                            valB = fileB.name,
                            matches = comparisonResult.nameMatch
                        )

                        // Sizes
                        CompareRow(
                            label = "File Size",
                            valA = "${FileInfo.formatFileSize(comparisonResult.sizeA)} (${comparisonResult.sizeA} bytes)",
                            valB = "${FileInfo.formatFileSize(comparisonResult.sizeB)} (${comparisonResult.sizeB} bytes)",
                            matches = comparisonResult.sizeMatch
                        )

                        // Modified Dates
                        CompareRow(
                            label = "Date Modified",
                            valA = sdf.format(Date(comparisonResult.dateA)),
                            valB = sdf.format(Date(comparisonResult.dateB)),
                            matches = comparisonResult.dateA == comparisonResult.dateB
                        )

                        // MD5
                        CompareRow(
                            label = "MD5 Hash",
                            valA = comparisonResult.md5A.ifEmpty { "N/A" },
                            valB = comparisonResult.md5B.ifEmpty { "N/A" },
                            matches = comparisonResult.md5Match,
                            isMonospace = true
                        )

                        // SHA-256
                        CompareRow(
                            label = "SHA-256 Hash",
                            valA = comparisonResult.sha256A.ifEmpty { "N/A" },
                            valB = comparisonResult.sha256B.ifEmpty { "N/A" },
                            matches = comparisonResult.sha256Match,
                            isMonospace = true
                        )

                        if (comparisonResult.isTextDiffAvailable) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Text Diff Analysis:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = comparisonResult.textDiffSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    valA: String,
    valB: String,
    matches: Boolean,
    isMonospace: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (matches) SecondaryTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (matches) "MATCH" else "DIFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (matches) SecondaryTeal else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "File A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = valA,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (isMonospace) 10.sp else 12.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "File B", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = valB,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                        fontSize = if (isMonospace) 10.sp else 12.sp
                    )
                }
            }
        }
    }
}
