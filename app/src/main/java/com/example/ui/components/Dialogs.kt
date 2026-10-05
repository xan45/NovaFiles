package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream

@Composable
fun FileDetailsDialog(
    file: File,
    onDismiss: () -> Unit
) {
    var md5Hash by remember { mutableStateOf<String?>(null) }
    var isCalculatingHash by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("File Properties", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "Name", value = file.name)
                DetailRow(label = "Type", value = if (file.isDirectory) "Directory" else "${file.extension.uppercase()} File")
                DetailRow(
                    label = "Size",
                    value = FileInfo.formatFileSize(file.length(), file.isDirectory, file.listFiles()?.size ?: 0)
                )
                DetailRow(label = "Location", value = file.parent ?: "Root")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                DetailRow(label = "Modified", value = sdf.format(Date(file.lastModified())))
                DetailRow(label = "Readable", value = if (file.canRead()) "Yes" else "No")
                DetailRow(label = "Writable", value = if (file.canWrite()) "Yes" else "No")

                if (!file.isDirectory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (md5Hash != null) {
                        DetailRow(label = "MD5 Hash", value = md5Hash ?: "")
                    } else {
                        FilledTonalButton(
                            onClick = {
                                isCalculatingHash = true
                                scope.launch(Dispatchers.IO) {
                                    val hash = try {
                                        val md = MessageDigest.getInstance("MD5")
                                        val buffer = ByteArray(8192)
                                        FileInputStream(file).use { fis ->
                                            var read: Int
                                            while (fis.read(buffer).also { read = it } != -1) {
                                                md.update(buffer, 0, read)
                                            }
                                        }
                                        md.digest().joinToString("") { "%02x".format(it) }
                                    } catch (e: Exception) {
                                        "Error calculating hash"
                                    }
                                    withContext(Dispatchers.Main) {
                                        md5Hash = hash
                                        isCalculatingHash = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCalculatingHash) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Calculate Checksum (MD5)")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (label == "MD5 Hash" || label == "Location") FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, content: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("New Text File") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("File Name (e.g. notes.txt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Initial Content (optional)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), content) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    file: File,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(file.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && name != file.name) onConfirm(name.trim()) },
                enabled = name.isNotBlank() && name != file.name
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateZipDialog(
    sourceFiles: List<File>,
    onDismiss: () -> Unit,
    onConfirm: (zipName: String, compressionLevel: Int) -> Unit
) {
    val defaultName = if (sourceFiles.size == 1) "${sourceFiles.first().nameWithoutExtension}.zip" else "Archive.zip"
    var zipName by remember { mutableStateOf(defaultName) }
    var compressionLevel by remember { mutableIntStateOf(ZipOutputStream.DEFLATED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Create ZIP Archive") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Compression Mode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = compressionLevel == ZipOutputStream.DEFLATED,
                        onClick = { compressionLevel = ZipOutputStream.DEFLATED }
                    )
                    Text("Standard (Deflated - Smaller size)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = compressionLevel == ZipOutputStream.STORED,
                        onClick = { compressionLevel = ZipOutputStream.STORED }
                    )
                    Text("Store (No compression - Faster)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (zipName.isNotBlank()) onConfirm(zipName.trim(), compressionLevel) },
                enabled = zipName.isNotBlank()
            ) {
                Text("Compress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    files: List<File>,
    permanent: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(if (permanent) "Permanent Delete" else "Move to Recycle Bin") },
        text = {
            Text(
                if (permanent) "Are you sure you want to permanently delete ${files.size} items? This action cannot be undone."
                else "Move ${files.size} items to the Recycle Bin? You can restore them anytime within 30 days."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PinDialog(
    title: String,
    buttonText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("4-6 Digit Security PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        error = "PIN must be at least 4 digits"
                    } else {
                        val ok = onConfirm(pin)
                        if (!ok) {
                            error = "Incorrect PIN"
                        }
                    }
                }
            ) {
                Text(buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
