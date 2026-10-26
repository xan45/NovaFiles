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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RecentEntity
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.StorageAnalysisResult
import com.example.data.model.StorageVolumeInfo
import com.example.data.preferences.AppThemeMode
import com.example.ui.components.QuickShortcutsGrid
import com.example.ui.components.StorageOverviewCard
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.ui.theme.glassBorder
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
    val themeMode = LocalAppThemeMode.current
    val cardCorner = if (themeMode == AppThemeMode.SAMSUNG_ONE_UI) 26.dp else 20.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 95.dp),
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

        // Primary Feature Hub: Core Power Modules
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Core Power Engines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 2x3 Grid of core power modules
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PowerModuleCard(
                            title = "Dual Pane",
                            subtitle = "Side-by-side browsing",
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            color = SecondaryTeal,
                            onClick = { onNavigateToScreen(AppScreen.DUAL_PANE) },
                            modifier = Modifier.weight(1f),
                            corner = cardCorner
                        )
                        PowerModuleCard(
                            title = "Storage Analyzer",
                            subtitle = "3-Point SHA-256 cleaner",
                            icon = Icons.Default.PieChart,
                            color = TertiaryAmber,
                            onClick = { onNavigateToScreen(AppScreen.STORAGE_ANALYZER) },
                            modifier = Modifier.weight(1f),
                            corner = cardCorner
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PowerModuleCard(
                            title = "Secure Vault",
                            subtitle = "Hardware/PIN AES-256",
                            icon = Icons.Default.Lock,
                            color = Color(0xFFEF4444),
                            onClick = { onNavigateToScreen(AppScreen.VAULT) },
                            modifier = Modifier.weight(1f),
                            corner = cardCorner
                        )
                        PowerModuleCard(
                            title = "Network & SMB",
                            subtitle = "Wi-Fi FTP & SMBv2/v3",
                            icon = Icons.Default.Lan,
                            color = PrimaryBlue,
                            onClick = { onNavigateToScreen(AppScreen.NETWORK) },
                            modifier = Modifier.weight(1f),
                            corner = cardCorner
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PowerModuleCard(
                            title = "Root & Shizuku",
                            subtitle = "Privileged system shell",
                            icon = Icons.Default.Security,
                            color = Color(0xFF8B5CF6),
                            onClick = { onNavigateToScreen(AppScreen.ROOT_EXPLORER) },
                            modifier = Modifier.weight(1f),
                            corner = cardCorner
                        )
                        PowerModuleCard(
                            title = "Power Tools",
                            subtitle = "Batch rename, sync & shred",
                            icon = Icons.Default.Build,
                            color = Color(0xFF10B981),
                            onClick = { onNavigateToScreen(AppScreen.TOOLS) },
                            modifier = Modifier.weight(1f),
                            corner = cardCorner
                        )
                    }
                }
            }
        }

        // Quick Utilities Strip: APKs, Favorites, Bin, Sync
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickUtilityChip(
                    title = "APKs",
                    icon = Icons.Default.Android,
                    color = Color(0xFF10B981),
                    onClick = { onNavigateToScreen(AppScreen.APK_MANAGER) },
                    modifier = Modifier.weight(1f)
                )
                QuickUtilityChip(
                    title = "Favorites",
                    icon = Icons.Default.Star,
                    color = TertiaryAmber,
                    onClick = { onNavigateToScreen(AppScreen.FAVORITES) },
                    modifier = Modifier.weight(1f)
                )
                QuickUtilityChip(
                    title = "Recycle Bin",
                    icon = Icons.Default.DeleteSweep,
                    color = Color(0xFFF43F5E),
                    onClick = { onNavigateToScreen(AppScreen.RECYCLE_BIN) },
                    modifier = Modifier.weight(1f)
                )
                QuickUtilityChip(
                    title = "Folder Sync",
                    icon = Icons.Default.Sync,
                    color = SecondaryTeal,
                    onClick = { onNavigateToScreen(AppScreen.FOLDER_SYNC) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Real Storage Insights (when available from analysis)
        if (analysisResult?.insights?.isNotEmpty() == true) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToScreen(AppScreen.STORAGE_ANALYZER) },
                    shape = RoundedCornerShape(cardCorner),
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
                                text = "Intelligent Storage Insights",
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

            items(recentFiles.take(6)) { recent ->
                val file = File(recent.path)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { if (file.exists()) onOpenFile(file) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
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
    }
}

@Composable
fun PowerModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 20.dp
) {
    val themeMode = LocalAppThemeMode.current
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .clickable { onClick() }
            .glassBorder(themeMode, shape = RoundedCornerShape(corner)),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickUtilityChip(
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}
