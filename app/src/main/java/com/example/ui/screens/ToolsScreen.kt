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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.ui.theme.glassBorder
import com.example.ui.viewmodel.AppScreen

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val targetScreen: AppScreen
)

@Composable
fun ToolsScreen(
    onNavigateToScreen: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode = LocalAppThemeMode.current

    val toolsList = listOf(
        ToolItem(
            title = "Batch Rename Tool",
            description = "Find & replace, numbering, prefix/suffix, extension rules",
            icon = Icons.Default.DriveFileRenameOutline,
            iconColor = PrimaryBlue,
            targetScreen = AppScreen.BATCH_RENAME
        ),
        ToolItem(
            title = "File Comparison Tool",
            description = "Compare size, modified date, MD5 and SHA-256 hashes",
            icon = Icons.Default.Compare,
            iconColor = SecondaryTeal,
            targetScreen = AppScreen.FILE_COMPARE
        ),
        ToolItem(
            title = "Folder Synchronization",
            description = "One-way, two-way, and mirror synchronization tasks",
            icon = Icons.Default.Sync,
            iconColor = TertiaryAmber,
            targetScreen = AppScreen.FOLDER_SYNC
        ),
        ToolItem(
            title = "Activity History Log",
            description = "Complete audit trail of all copy, move, rename, delete actions",
            icon = Icons.Default.History,
            iconColor = Color(0xFF8B5CF6),
            targetScreen = AppScreen.ACTIVITY_LOGS
        ),
        ToolItem(
            title = "Wi-Fi FTP & Network",
            description = "Access phone storage from PC over Wi-Fi without cables",
            icon = Icons.Default.Wifi,
            iconColor = Color(0xFF06B6D4),
            targetScreen = AppScreen.NETWORK
        ),
        ToolItem(
            title = "APK & App Manager",
            description = "Inspect installed apps, extract APK backups, view details",
            icon = Icons.Default.Android,
            iconColor = Color(0xFF10B981),
            targetScreen = AppScreen.APK_MANAGER
        ),
        ToolItem(
            title = "Secure Vault",
            description = "Encrypted storage locked with PIN or biometric fingerprint",
            icon = Icons.Default.Lock,
            iconColor = Color(0xFFE11D48),
            targetScreen = AppScreen.VAULT
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Power Tools & Utilities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Advanced productivity and organization tools inspired by MiXplorer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(toolsList.size) { index ->
            val tool = toolsList[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onNavigateToScreen(tool.targetScreen) }
                    .glassBorder(themeMode, shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tool.iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = tool.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
