package com.example.data.cloud

import android.content.Context
import com.example.data.local.CloudCacheEntity
import com.example.data.local.NovaFilesDatabase
import com.example.data.model.CloudAccount
import com.example.data.model.CloudFileItem
import com.example.data.model.CloudProvider
import com.example.data.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CloudStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("cloud_storage_prefs", Context.MODE_PRIVATE)
    private val db = NovaFilesDatabase.getInstance(context)
    private val cloudCacheDao = db.cloudCacheDao()

    private val cacheDirectory: File by lazy {
        val dir = File(context.cacheDir, "cloud_cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

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
        // Check Room local cache first
        val cachedEntities = cloudCacheDao.getCachedFiles(provider.name, folderId)

        if (cachedEntities.isNotEmpty()) {
            return@withContext cachedEntities.map { entity ->
                val localFile = entity.cachedFilePath?.let { File(it) }
                val isCached = localFile != null && localFile.exists()

                CloudFileItem(
                    id = entity.fileId,
                    name = entity.name,
                    size = entity.size,
                    isDirectory = entity.isDirectory,
                    mimeType = entity.mimeType,
                    parentId = entity.parentId,
                    lastModified = entity.lastAccessedAt,
                    provider = provider,
                    isCachedLocally = isCached,
                    localCachedPath = if (isCached) localFile?.absolutePath else null
                )
            }
        }

        // Generate baseline cloud files and persist to Room cache
        val rawItems = getRemoteTemplateFiles(provider, folderId)

        val entitiesToCache = rawItems.map { item ->
            // Pre-cache small documents & text files for instant offline availability
            val cachedFile = if (!item.isDirectory && (item.name.endsWith(".txt") || item.name.endsWith(".csv") || item.name.endsWith(".pdf"))) {
                ensureCachedFileContent(provider, item)
            } else null

            CloudCacheEntity(
                id = "${provider.name}_${item.id}",
                fileId = item.id,
                name = item.name,
                size = item.size,
                isDirectory = item.isDirectory,
                mimeType = item.mimeType,
                provider = provider.name,
                parentId = folderId,
                cachedFilePath = cachedFile?.absolutePath,
                cachedAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis()
            )
        }

        cloudCacheDao.insertAll(entitiesToCache)

        entitiesToCache.map { entity ->
            val localFile = entity.cachedFilePath?.let { File(it) }
            val isCached = localFile != null && localFile.exists()

            CloudFileItem(
                id = entity.fileId,
                name = entity.name,
                size = entity.size,
                isDirectory = entity.isDirectory,
                mimeType = entity.mimeType,
                parentId = entity.parentId,
                lastModified = entity.lastAccessedAt,
                provider = provider,
                isCachedLocally = isCached,
                localCachedPath = if (isCached) localFile?.absolutePath else null
            )
        }
    }

    private fun getRemoteTemplateFiles(provider: CloudProvider, folderId: String?): List<CloudFileItem> {
        return if (folderId == null || folderId == "root") {
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

    private fun ensureCachedFileContent(provider: CloudProvider, item: CloudFileItem): File? {
        return try {
            val providerDir = File(cacheDirectory, provider.name.lowercase())
            if (!providerDir.exists()) providerDir.mkdirs()
            val cachedFile = File(providerDir, "${item.id}_${item.name}")
            if (!cachedFile.exists()) {
                FileOutputStream(cachedFile).use { fos ->
                    val content = when {
                        item.name.endsWith(".txt") ->
                            "=== NovaFiles Cloud Synced Document ===\nService: ${provider.displayName}\nTitle: ${item.name}\nSynced for offline reading.\n\nProject Notes:\n1. Synchronize storage quotas\n2. Dual-pane transfer verified\n3. Offline caching enabled\n"
                        item.name.endsWith(".csv") ->
                            "Month,Category,Allocated,Used\nJan,Compute,15000,12000\nFeb,Storage,8000,7500\nMar,Bandwidth,4500,3200\n"
                        else ->
                            "NovaFiles Cached Document: ${item.name}\nSource: ${provider.displayName}\nOffline Cache Active\n"
                    }
                    fos.write(content.toByteArray())
                }
            }
            cachedFile
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCachedLocalFile(item: CloudFileItem): File? = withContext(Dispatchers.IO) {
        val compositeId = "${item.provider.name}_${item.id}"
        val entity = cloudCacheDao.getById(compositeId)
        if (entity?.cachedFilePath != null) {
            val f = File(entity.cachedFilePath)
            if (f.exists()) {
                cloudCacheDao.updateCachedPath(compositeId, f.absolutePath, System.currentTimeMillis())
                return@withContext f
            }
        }
        // Cache now
        val cached = ensureCachedFileContent(item.provider, item)
        if (cached != null) {
            cloudCacheDao.updateCachedPath(compositeId, cached.absolutePath, System.currentTimeMillis())
        }
        cached
    }

    suspend fun downloadCloudFile(item: CloudFileItem, destinationDir: File): File? = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val targetFile = File(destinationDir, item.name)

            // Write target file
            val cachedSource = getCachedLocalFile(item)
            if (cachedSource != null && cachedSource.exists()) {
                cachedSource.copyTo(targetFile, overwrite = true)
            } else {
                FileOutputStream(targetFile).use { fos ->
                    val sampleText = "NovaFiles Cloud Download\nProvider: ${item.provider.displayName}\nFile: ${item.name}\nSize: ${item.formattedSize}\nSynced at: ${System.currentTimeMillis()}\n"
                    fos.write(sampleText.toByteArray())
                }
            }

            // Also update Room cache reference
            val compositeId = "${item.provider.name}_${item.id}"
            cloudCacheDao.updateCachedPath(compositeId, targetFile.absolutePath, System.currentTimeMillis())

            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun uploadToCloud(localFile: File, provider: CloudProvider, folderId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val newId = "file_${System.currentTimeMillis()}"
            val compositeId = "${provider.name}_$newId"

            // Save in cache
            val providerDir = File(cacheDirectory, provider.name.lowercase())
            if (!providerDir.exists()) providerDir.mkdirs()
            val cachedFile = File(providerDir, "${newId}_${localFile.name}")
            localFile.copyTo(cachedFile, overwrite = true)

            val entity = CloudCacheEntity(
                id = compositeId,
                fileId = newId,
                name = localFile.name,
                size = localFile.length(),
                isDirectory = localFile.isDirectory,
                mimeType = if (localFile.isDirectory) "inode/directory" else "application/octet-stream",
                provider = provider.name,
                parentId = folderId,
                cachedFilePath = cachedFile.absolutePath,
                cachedAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis()
            )
            cloudCacheDao.insertAll(listOf(entity))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        cacheDirectory.listFiles()?.forEach { f ->
            reclaimed += f.length()
            f.deleteRecursively()
        }
        cloudCacheDao.clearAll()
        reclaimed
    }
}
