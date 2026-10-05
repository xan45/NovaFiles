package com.example.data.cloud

import android.content.Context
import com.example.data.model.CloudAccount
import com.example.data.model.CloudFileItem
import com.example.data.model.CloudProvider
import com.example.data.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CloudStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("cloud_storage_prefs", Context.MODE_PRIVATE)

    fun getConnectedAccounts(): List<CloudAccount> {
        return CloudProvider.entries.map { provider ->
            val isConnected = prefs.getBoolean("connected_${provider.name}", false)
            val email = prefs.getString("email_${provider.name}", "") ?: ""
            val used = prefs.getLong("used_${provider.name}", if (isConnected) 4L * 1024 * 1024 * 1024 else 0L)
            val total = prefs.getLong("total_${provider.name}", if (isConnected) 15L * 1024 * 1024 * 1024 else 0L)
            val token = prefs.getString("token_${provider.name}", null)

            CloudAccount(
                provider = provider,
                accountEmail = email,
                isConnected = isConnected,
                usedBytes = used,
                totalBytes = total,
                accessToken = token
            )
        }
    }

    fun connectAccount(provider: CloudProvider, email: String, token: String = "token_${System.currentTimeMillis()}") {
        prefs.edit()
            .putBoolean("connected_${provider.name}", true)
            .putString("email_${provider.name}", email)
            .putString("token_${provider.name}", token)
            .putLong("used_${provider.name}", (2L..8L).random() * 1024 * 1024 * 1024)
            .putLong("total_${provider.name}", if (provider == CloudProvider.DROPBOX) 5L * 1024 * 1024 * 1024 else 15L * 1024 * 1024 * 1024)
            .apply()
    }

    fun disconnectAccount(provider: CloudProvider) {
        prefs.edit()
            .putBoolean("connected_${provider.name}", false)
            .remove("email_${provider.name}")
            .remove("token_${provider.name}")
            .apply()
    }

    suspend fun getCloudFiles(provider: CloudProvider, folderId: String? = null): List<CloudFileItem> = withContext(Dispatchers.IO) {
        // Return files for cloud folder
        if (folderId == null || folderId == "root") {
            listOf(
                CloudFileItem(
                    id = "folder_docs",
                    name = "Documents & Work",
                    size = 0L,
                    isDirectory = true,
                    mimeType = "inode/directory",
                    provider = provider
                ),
                CloudFileItem(
                    id = "folder_photos",
                    name = "Cloud Photos & Camera Backup",
                    size = 0L,
                    isDirectory = true,
                    mimeType = "inode/directory",
                    provider = provider
                ),
                CloudFileItem(
                    id = "folder_projects",
                    name = "Shared Projects",
                    size = 0L,
                    isDirectory = true,
                    mimeType = "inode/directory",
                    provider = provider
                ),
                CloudFileItem(
                    id = "file_annual_report",
                    name = "Annual_Strategy_Report_2026.pdf",
                    size = 4_250_000L,
                    isDirectory = false,
                    mimeType = "application/pdf",
                    provider = provider
                ),
                CloudFileItem(
                    id = "file_dataset",
                    name = "Project_Dataset_Export.csv",
                    size = 1_820_000L,
                    isDirectory = false,
                    mimeType = "text/csv",
                    provider = provider
                ),
                CloudFileItem(
                    id = "file_backup_archive",
                    name = "Cloud_Backup_Archive.zip",
                    size = 38_500_000L,
                    isDirectory = false,
                    mimeType = "application/zip",
                    provider = provider
                )
            )
        } else if (folderId == "folder_docs") {
            listOf(
                CloudFileItem(
                    id = "doc_presentation",
                    name = "Q3_Product_Roadmap.pptx",
                    size = 12_400_000L,
                    isDirectory = false,
                    mimeType = "application/vnd.ms-powerpoint",
                    parentId = folderId,
                    provider = provider
                ),
                CloudFileItem(
                    id = "doc_financials",
                    name = "Budget_Plan_2026.xlsx",
                    size = 850_000L,
                    isDirectory = false,
                    mimeType = "application/vnd.ms-excel",
                    parentId = folderId,
                    provider = provider
                ),
                CloudFileItem(
                    id = "doc_notes",
                    name = "Meeting_Notes.txt",
                    size = 24_000L,
                    isDirectory = false,
                    mimeType = "text/plain",
                    parentId = folderId,
                    provider = provider
                )
            )
        } else if (folderId == "folder_photos") {
            listOf(
                CloudFileItem(
                    id = "photo_1",
                    name = "Mountain_Summit_Panorama.jpg",
                    size = 5_800_000L,
                    isDirectory = false,
                    mimeType = "image/jpeg",
                    parentId = folderId,
                    provider = provider
                ),
                CloudFileItem(
                    id = "photo_2",
                    name = "Team_Celebration.png",
                    size = 3_200_000L,
                    isDirectory = false,
                    mimeType = "image/png",
                    parentId = folderId,
                    provider = provider
                )
            )
        } else {
            emptyList()
        }
    }

    suspend fun downloadCloudFile(item: CloudFileItem, destinationDir: File): File? = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val targetFile = File(destinationDir, item.name)
            // Save file
            FileOutputStream(targetFile).use { fos ->
                val sampleText = "NovaFiles Cloud Download\nProvider: ${item.provider.displayName}\nFile: ${item.name}\nSize: ${item.formattedSize}\nSynced at: ${System.currentTimeMillis()}\n"
                fos.write(sampleText.toByteArray())
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadToCloud(localFile: File, provider: CloudProvider, folderId: String?): Boolean = withContext(Dispatchers.IO) {
        // Simulates upload to provider
        true
    }
}
