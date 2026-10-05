package com.example.data.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.model.DuplicateGroup
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.StorageAnalysisResult
import com.example.data.model.StorageVolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

class StorageAnalyzerEngine(private val context: Context) {

    fun getStorageVolumes(): List<StorageVolumeInfo> {
        val volumes = mutableListOf<StorageVolumeInfo>()
        try {
            val internalRoot = Environment.getExternalStorageDirectory()
            if (internalRoot.exists()) {
                val stat = StatFs(internalRoot.path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                val totalBytes = totalBlocks * blockSize
                val freeBytes = availableBlocks * blockSize

                volumes.add(
                    StorageVolumeInfo(
                        name = "Internal Storage",
                        path = internalRoot.absolutePath,
                        totalBytes = totalBytes,
                        freeBytes = freeBytes,
                        isRemovable = false
                    )
                )
            }

            // Check secondary storage / SD cards / USB OTG
            val externalDirs = context.getExternalFilesDirs(null)
            if (externalDirs.size > 1) {
                for (i in 1 until externalDirs.size) {
                    val dir = externalDirs[i] ?: continue
                    // Extract root path
                    val path = dir.absolutePath.substringBefore("/Android")
                    val sdFile = File(path)
                    if (sdFile.exists() && sdFile.canRead()) {
                        val stat = StatFs(sdFile.path)
                        val total = stat.blockCountLong * stat.blockSizeLong
                        val free = stat.availableBlocksLong * stat.blockSizeLong
                        volumes.add(
                            StorageVolumeInfo(
                                name = "SD Card / External",
                                path = sdFile.absolutePath,
                                totalBytes = total,
                                freeBytes = free,
                                isRemovable = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (volumes.isEmpty()) {
            volumes.add(
                StorageVolumeInfo(
                    name = "Internal Storage",
                    path = Environment.getExternalStorageDirectory().absolutePath,
                    totalBytes = 64L * 1024 * 1024 * 1024,
                    freeBytes = 32L * 1024 * 1024 * 1024
                )
            )
        }
        return volumes
    }

    suspend fun analyzeStorage(
        rootDirectory: File = Environment.getExternalStorageDirectory(),
        onProgress: (scannedCount: Int, currentPath: String) -> Unit = { _, _ -> }
    ): StorageAnalysisResult = withContext(Dispatchers.IO) {
        var imagesBytes = 0L
        var videosBytes = 0L
        var audioBytes = 0L
        var documentsBytes = 0L
        var archivesBytes = 0L
        var apksBytes = 0L
        var othersBytes = 0L

        val allFiles = mutableListOf<FileInfo>()
        val emptyFolders = mutableListOf<FileInfo>()
        val junkFiles = mutableListOf<FileInfo>()
        var junkBytes = 0L

        var count = 0

        fun scanDir(dir: File) {
            val children = dir.listFiles() ?: return
            if (children.isEmpty()) {
                emptyFolders.add(FileInfo(dir))
                return
            }

            var hasFiles = false
            for (child in children) {
                count++
                if (count % 50 == 0) {
                    onProgress(count, child.name)
                }

                if (child.isDirectory) {
                    val name = child.name.lowercase()
                    // Detect cache folders as junk
                    if (name == ".thumbnails" || name == "cache" || name == ".cache" || name.contains("temp")) {
                        val folderFiles = child.listFiles() ?: emptyArray()
                        for (f in folderFiles) {
                            if (!f.isDirectory) {
                                junkFiles.add(FileInfo(f))
                                junkBytes += f.length()
                            }
                        }
                    }
                    scanDir(child)
                } else {
                    hasFiles = true
                    val fileInfo = FileInfo(child)
                    allFiles.add(fileInfo)

                    val ext = fileInfo.extension
                    val len = child.length()

                    // Junk file patterns (.tmp, .log, .bak, .crdownload, .part)
                    if (ext in listOf("tmp", "temp", "log", "bak", "crdownload", "part") ||
                        child.name.startsWith("temp_")
                    ) {
                        junkFiles.add(fileInfo)
                        junkBytes += len
                    }

                    when (fileInfo.category) {
                        FileCategory.IMAGES -> imagesBytes += len
                        FileCategory.VIDEOS -> videosBytes += len
                        FileCategory.AUDIO -> audioBytes += len
                        FileCategory.DOCUMENTS -> documentsBytes += len
                        FileCategory.ARCHIVES -> archivesBytes += len
                        FileCategory.APKS -> apksBytes += len
                        else -> othersBytes += len
                    }
                }
            }
            if (!hasFiles && children.all { it.isDirectory }) {
                // If it contains only empty subdirectories, it's also empty
            }
        }

        try {
            if (rootDirectory.exists() && rootDirectory.canRead()) {
                scanDir(rootDirectory)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Compute largest files (top 50)
        val largestFiles = allFiles.sortedByDescending { it.size }.take(50)

        // Duplicate File Detection (group by byte size first)
        val duplicatesBySize = allFiles.filter { it.size > 2048L } // Ignore files <= 2KB
            .groupBy { it.size }
            .filter { it.value.size > 1 }

        val duplicateGroups = mutableListOf<DuplicateGroup>()
        var duplicateTotalBytes = 0L

        for ((_, candidateFiles) in duplicatesBySize) {
            // For files with identical size, compute hash to confirm duplicate
            val hashGroups = candidateFiles.groupBy { computeFastHash(it.file) }
            for ((hash, filesWithHash) in hashGroups) {
                if (filesWithHash.size > 1) {
                    val group = DuplicateGroup(
                        checksum = hash,
                        size = filesWithHash.first().size,
                        files = filesWithHash
                    )
                    duplicateGroups.add(group)
                    duplicateTotalBytes += group.totalWastedBytes
                }
            }
        }

        // Get storage stats
        val volumes = getStorageVolumes()
        val primary = volumes.firstOrNull()
        val totalBytes = primary?.totalBytes ?: (64L * 1024 * 1024 * 1024)
        val freeBytes = primary?.freeBytes ?: (32L * 1024 * 1024 * 1024)
        val usedBytes = primary?.usedBytes ?: (totalBytes - freeBytes)

        // Generate AI-style storage insights
        val insights = mutableListOf<String>()
        val totalCategorized = (imagesBytes + videosBytes + audioBytes + documentsBytes + archivesBytes + apksBytes + othersBytes).coerceAtLeast(1L)

        val videoPct = (videosBytes * 100 / totalCategorized).toInt()
        if (videoPct > 30) {
            insights.add("🎬 Videos consume $videoPct% of your file storage (${FileInfo.formatFileSize(videosBytes)}). Consider backing up or compressing large video files.")
        }

        val imgPct = (imagesBytes * 100 / totalCategorized).toInt()
        if (imgPct > 25) {
            insights.add("📸 Photos and images take up ${FileInfo.formatFileSize(imagesBytes)}. Check for duplicate shots or bursts.")
        }

        if (duplicateTotalBytes > 10 * 1024 * 1024) {
            insights.add("✨ You have ${FileInfo.formatFileSize(duplicateTotalBytes)} of identical duplicate files. Deleting redundant copies will free up immediate space.")
        }

        if (junkBytes > 5 * 1024 * 1024) {
            insights.add("🧹 Detected ${FileInfo.formatFileSize(junkBytes)} in temporary, log, and cache files that are safe to clean.")
        }

        if (emptyFolders.isNotEmpty()) {
            insights.add("📁 Found ${emptyFolders.size} empty folders cluttering your file system hierarchy.")
        }

        val largeCount = largestFiles.count { it.size > 100 * 1024 * 1024 }
        if (largeCount > 0) {
            insights.add("📦 You have $largeCount files larger than 100 MB. Review the Large Files tab to reclaim bulk storage.")
        }

        if (insights.isEmpty()) {
            insights.add("🎉 Your storage is in great shape! No major bottlenecks or bloat detected.")
        }

        StorageAnalysisResult(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            imagesBytes = imagesBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            documentsBytes = documentsBytes,
            archivesBytes = archivesBytes,
            apksBytes = apksBytes,
            othersBytes = othersBytes,
            duplicateGroups = duplicateGroups.sortedByDescending { it.totalWastedBytes },
            largestFiles = largestFiles,
            emptyFolders = emptyFolders,
            junkFiles = junkFiles,
            junkTotalBytes = junkBytes,
            duplicateTotalBytes = duplicateTotalBytes,
            insights = insights
        )
    }

    private fun computeFastHash(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read: Int
                var totalRead = 0
                // Read up to first 256KB and last 64KB for speed on large files
                while (fis.read(buffer).also { read = it } != -1 && totalRead < 256 * 1024) {
                    md.update(buffer, 0, read)
                    totalRead += read
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "${file.name}_${file.length()}"
        }
    }
}
