package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileInfo
import com.example.data.network.FtpServerConfig
import com.example.data.network.FtpServerState
import com.example.data.network.SmbFileItem
import com.example.data.network.SmbShareConfig
import com.example.data.preferences.AppThemeMode
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.glassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    ftpState: FtpServerState,
    ftpConfig: FtpServerConfig,
    onToggleFtpServer: () -> Unit,
    onUpdateFtpConfig: (FtpServerConfig) -> Unit,
    isSmbConnected: Boolean = false,
    smbFiles: List<SmbFileItem> = emptyList(),
    currentSmbPath: String = "",
    isSmbLoading: Boolean = false,
    smbActiveConfig: SmbShareConfig? = null,
    onConnectSmb: (SmbShareConfig) -> Unit = {},
    onDisconnectSmb: () -> Unit = {},
    onLoadSmbFiles: (String) -> Unit = {},
    onDownloadSmbFile: (SmbFileItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode = LocalAppThemeMode.current
    var showConfigModal by remember { mutableStateOf(false) }

    // SMB Connection Form States
    var smbHost by remember { mutableStateOf("") }
    var smbPort by remember { mutableStateOf("445") }
    var smbShare by remember { mutableStateOf("") }
    var smbUsername by remember { mutableStateOf("") }
    var smbPassword by remember { mutableStateOf("") }
    var smbDomain by remember { mutableStateOf("") }
    var showSmbConnectDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Network & Remote Access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Transfer files over Wi-Fi via built-in FTP or connect to Windows SMBv2/v3 shares",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Wi-Fi FTP Server Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBorder(themeMode, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when (themeMode) {
                        AppThemeMode.FROSTED_GLASS -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        AppThemeMode.CLEAR_TRANSPARENT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        AppThemeMode.SAMSUNG_ONE_UI, AppThemeMode.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (ftpState.isRunning) SecondaryTeal.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = if (ftpState.isRunning) SecondaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Wi-Fi FTP Server",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (ftpState.isRunning) "Server Active • ${ftpState.activeClientsCount} connected" else "Server Stopped",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ftpState.isRunning) SecondaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onToggleFtpServer,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (ftpState.isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (ftpState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (ftpState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (ftpState.isRunning) "Stop" else "Start")
                        }
                    }

                    if (ftpState.isRunning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val serverUri = "ftp://${ftpState.ipAddress}:${ftpState.port}"

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("FTP Address", serverUri))
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Server Address (Tap to copy)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = serverUri,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Port: ${ftpConfig.port} • User: ${if (ftpConfig.isAnonymousAllowed) "Anonymous" else ftpConfig.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { showConfigModal = !showConfigModal },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "FTP Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Expandable FTP Server Config
                    AnimatedVisibility(visible = showConfigModal) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            var portText by remember { mutableStateOf(ftpConfig.port.toString()) }
                            var userText by remember { mutableStateOf(ftpConfig.username) }
                            var passText by remember { mutableStateOf(ftpConfig.password) }
                            var anonymous by remember { mutableStateOf(ftpConfig.isAnonymousAllowed) }
                            var readOnly by remember { mutableStateOf(ftpConfig.isReadOnly) }

                            OutlinedTextField(
                                value = portText,
                                onValueChange = { portText = it },
                                label = { Text("Port (e.g. 2121)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Allow Anonymous Login", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = anonymous, onCheckedChange = { anonymous = it })
                            }

                            if (!anonymous) {
                                OutlinedTextField(
                                    value = userText,
                                    onValueChange = { userText = it },
                                    label = { Text("Username") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = passText,
                                    onValueChange = { passText = it },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Read-Only Mode", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = readOnly, onCheckedChange = { readOnly = it })
                            }

                            Button(
                                onClick = {
                                    val port = portText.toIntOrNull() ?: 2121
                                    onUpdateFtpConfig(
                                        ftpConfig.copy(
                                            port = port,
                                            username = userText,
                                            password = passText,
                                            isAnonymousAllowed = anonymous,
                                            isReadOnly = readOnly
                                        )
                                    )
                                    showConfigModal = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Server Settings")
                            }
                        }
                    }

                    // Live Server Logs
                    if (ftpState.logs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recent Connection Logs:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                ftpState.logs.takeLast(4).forEach { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SMBv2 / SMBv3 Windows Share Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Windows Share (SMBv2 / SMBv3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isSmbConnected) {
                    FilledTonalButton(
                        onClick = onDisconnectSmb,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }

        if (!isSmbConnected) {
            // Connection Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(themeMode, shape = RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lan, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Connect to Windows Share / NAS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Access folders shared across your local Wi-Fi network",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = smbHost,
                            onValueChange = { smbHost = it },
                            label = { Text("Server IP / Hostname (e.g. 192.168.1.100)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = smbShare,
                                onValueChange = { smbShare = it },
                                label = { Text("Share Name (e.g. Shared)") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = smbPort,
                                onValueChange = { smbPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = smbUsername,
                                onValueChange = { smbUsername = it },
                                label = { Text("Username (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = smbPassword,
                                onValueChange = { smbPassword = it },
                                label = { Text("Password (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (smbHost.isNotBlank() && smbShare.isNotBlank()) {
                                    val port = smbPort.toIntOrNull() ?: 445
                                    onConnectSmb(
                                        SmbShareConfig(
                                            host = smbHost.trim(),
                                            port = port,
                                            shareName = smbShare.trim(),
                                            domain = smbDomain.trim(),
                                            username = smbUsername.trim(),
                                            password = smbPassword
                                        )
                                    )
                                }
                            },
                            enabled = !isSmbLoading && smbHost.isNotBlank() && smbShare.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSmbLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect to Share")
                            }
                        }
                    }
                }
            }
        } else {
            // Connected Active Share Browser
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Connected: ${smbActiveConfig?.shareName} on ${smbActiveConfig?.host}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (currentSmbPath.isEmpty()) "Root Folder" else "\\$currentSmbPath",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (currentSmbPath.isNotEmpty()) {
                            IconButton(onClick = {
                                val parent = currentSmbPath.substringBeforeLast("\\", "")
                                onLoadSmbFiles(parent)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Parent Directory")
                            }
                        }
                    }
                }
            }

            if (isSmbLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (smbFiles.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "No files found in this share directory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(smbFiles) { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (item.isDirectory) {
                                    onLoadSmbFiles(item.path)
                                } else {
                                    onDownloadSmbFile(item)
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (item.isDirectory) "Folder" else FileInfo.formatFileSize(item.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!item.isDirectory) {
                                IconButton(onClick = { onDownloadSmbFile(item) }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download File", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
