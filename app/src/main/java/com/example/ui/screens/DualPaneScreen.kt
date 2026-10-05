package com.example.ui.screens

import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FileInfo
import com.example.ui.components.DualPaneView
import com.example.ui.viewmodel.ActivePane
import java.io.File

@Composable
fun DualPaneScreen(
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 72.dp)
    ) {
        // Quick Jump Chips for Active Pane
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Target:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val currentSetter = if (activePane == ActivePane.LEFT) onNavigateLeft else onNavigateRight

            AssistChip(
                onClick = { currentSetter(Environment.getExternalStorageDirectory()) },
                label = { Text("Root") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.padding(2.dp)) }
            )
            AssistChip(
                onClick = { currentSetter(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)) },
                label = { Text("Downloads") },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(2.dp)) }
            )
            AssistChip(
                onClick = { currentSetter(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)) },
                label = { Text("Camera") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.padding(2.dp)) }
            )
        }

        DualPaneView(
            leftDirectory = leftDirectory,
            leftFiles = leftFiles,
            leftSelected = leftSelected,
            rightDirectory = rightDirectory,
            rightFiles = rightFiles,
            rightSelected = rightSelected,
            activePane = activePane,
            onSetActivePane = onSetActivePane,
            onNavigateLeft = onNavigateLeft,
            onNavigateRight = onNavigateRight,
            onToggleLeftSelect = onToggleLeftSelect,
            onToggleRightSelect = onToggleRightSelect,
            onTransfer = onTransfer,
            modifier = Modifier.weight(1f)
        )
    }
}
