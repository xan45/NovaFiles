package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RecentEntity
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.StorageAnalysisResult
import com.example.data.model.StorageVolumeInfo
import com.example.ui.components.QuickShortcutsGrid
import com.example.ui.components.StorageOverviewCard
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.ui.viewmodel.AppScreen
import java.io.File

@Composable
fun HomeScreen(
    storageVolumes: List<StorageVolumeInfo>,
    recentFiles: List<RecentEntity>,
    analysisResult: StorageAnalysisResult?,
    onNavigateToCategory: (FileCategory) -> Unit,
    onNavigateToScreen: (AppScreen) -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage Overview Card
        item {
            StorageOverviewCard(
                volumes = storageVolumes,
                onCleanStorageClick = { onNavigateToScreen(AppScreen.STORAGE_ANALYZER) }
            )
        }

        // Quick Category Shortcuts Grid
        item {
            QuickShortcutsGrid(
                onCategoryClick = onNavigateToCategory
            )
        }

        // Quick Tools Row
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pro Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolShortcutCard(
                        title = "Cloud Storage",
                        subtitle = "Drive & Cloud",
                        icon = Icons.Default.Cloud,
                        color = PrimaryBlue,
                        onClick = { onNavigateToScreen(AppScreen.CLOUD_STORAGE) },
                        modifier = Modifier.weight(1f)
                    )
                    ToolShortcutCard(
                        title = "Dual Pane",
                        subtitle = "Two Folders",
                        icon = Icons.Default.CompareArrows,
                        color = SecondaryTeal,
                        onClick = { onNavigateToScreen(AppScreen.DUAL_PANE) },
                        modifier = Modifier.weight(1f)
                    )
                    ToolShortcutCard(
                        title = "Analyzer",
                        subtitle = "Charts & Dups",
                        icon = Icons.Default.PieChart,
                        color = TertiaryAmber,
                        onClick = { onNavigateToScreen(AppScreen.STORAGE_ANALYZER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // AI Storage Insights Card
        if (analysisResult?.insights?.isNotEmpty() == true) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToScreen(AppScreen.STORAGE_ANALYZER) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Storage Insights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        analysisResult.insights.take(2).forEach { insight ->
                            Text(
                                text = "• $insight",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Files Section
        if (recentFiles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(recentFiles.take(8)) { recent ->
                val file = File(recent.path)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (file.exists()) onOpenFile(file) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recent.name.substringAfterLast(".", "").take(3).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recent.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${FileInfo.formatFileSize(recent.size)} • ${recent.action}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Secondary Shortcuts: Favorites, Vault, Recycle Bin
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryToolCard(
                    title = "APKs",
                    icon = Icons.Default.Widgets,
                    color = SecondaryTeal,
                    onClick = { onNavigateToScreen(AppScreen.APK_MANAGER) },
                    modifier = Modifier.weight(1f)
                )
                SecondaryToolCard(
                    title = "Favorites",
                    icon = Icons.Default.Star,
                    color = TertiaryAmber,
                    onClick = { onNavigateToScreen(AppScreen.FAVORITES) },
                    modifier = Modifier.weight(1f)
                )
                SecondaryToolCard(
                    title = "Vault",
                    icon = Icons.Default.Lock,
                    color = PrimaryBlue,
                    onClick = { onNavigateToScreen(AppScreen.VAULT) },
                    modifier = Modifier.weight(1f)
                )
                SecondaryToolCard(
                    title = "Bin",
                    icon = Icons.Default.DeleteSweep,
                    color = Color(0xFFEF4444),
                    onClick = { onNavigateToScreen(AppScreen.RECYCLE_BIN) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ToolShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SecondaryToolCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
