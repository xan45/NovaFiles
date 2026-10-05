package com.example.data.model

import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileInfo(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val extension: String = file.extension.lowercase(),
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val isDirectory: Boolean = file.isDirectory,
    val isHidden: Boolean = file.isHidden || file.name.startsWith("."),
    val itemCount: Int = if (file.isDirectory) (file.listFiles()?.size ?: 0) else 0,
    val uri: Uri? = null,
    val tags: List<String> = emptyList(),
    val tagColors: List<String> = emptyList(),
    val folderColorHex: String? = null,
    val folderIconName: String? = null,
    val folderNote: String? = null,
    val folderCoverImage: String? = null
) {
    val formattedSize: String
        get() = formatFileSize(size, isDirectory, itemCount)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val category: FileCategory
        get() = when {
            isDirectory -> FileCategory.ALL
            extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg") -> FileCategory.IMAGES
            extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "wmv") -> FileCategory.VIDEOS
            extension in listOf("mp3", "flac", "wav", "m4a", "ogg", "aac", "wma", "opus") -> FileCategory.AUDIO
            extension in listOf("pdf", "doc", "docx", "txt", "xlsx", "xls", "pptx", "ppt", "json", "xml", "csv", "md") -> FileCategory.DOCUMENTS
            extension in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> FileCategory.ARCHIVES
            extension in listOf("apk", "xapk", "apks") -> FileCategory.APKS
            else -> FileCategory.ALL
        }

    companion object {
        fun formatFileSize(bytes: Long, isDirectory: Boolean = false, itemCount: Int = 0): String {
            if (isDirectory) {
                return if (itemCount == 1) "1 item" else "$itemCount items"
            }
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, clampedGroup.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[clampedGroup])
        }
    }
}

enum class FileCategory(val label: String) {
    ALL("All Files"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    ARCHIVES("Archives"),
    APKS("APKs"),
    DOWNLOADS("Downloads"),
    TRASH("Recycle Bin")
}

enum class SmartCollection(val title: String, val description: String) {
    RECENT_DOWNLOADS("Recent Downloads", "Files saved in the last 7 days"),
    LARGE_FILES("Large Files", "Files consuming over 100 MB"),
    APK_BACKUPS("APK Backups", "Extracted and standalone application packages"),
    SCREENSHOTS("Screenshots", "Captured device screens & snapshots"),
    VIDEOS("Videos", "All video recordings and media"),
    DOCUMENTS("Documents", "PDFs, spreadsheets, text and notes"),
    FAVORITES("Favorites", "Starred and pinned folders & files")
}

enum class FileSortOption(val displayName: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    SIZE_DESC("Size (Largest first)"),
    SIZE_ASC("Size (Smallest first)"),
    TYPE("File Type")
}

enum class FileViewMode {
    LIST,
    GRID
}

data class StorageVolumeInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean = false
) {
    val usedBytes: Long get() = totalBytes - freeBytes
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
    val formattedTotal: String get() = FileInfo.formatFileSize(totalBytes)
    val formattedUsed: String get() = FileInfo.formatFileSize(usedBytes)
    val formattedFree: String get() = FileInfo.formatFileSize(freeBytes)
}

data class StorageAnalysisResult(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val imagesBytes: Long,
    val videosBytes: Long,
    val audioBytes: Long,
    val documentsBytes: Long,
    val archivesBytes: Long,
    val apksBytes: Long,
    val othersBytes: Long,
    val duplicateGroups: List<DuplicateGroup>,
    val largestFiles: List<FileInfo>,
    val emptyFolders: List<FileInfo>,
    val junkFiles: List<FileInfo>,
    val junkTotalBytes: Long,
    val duplicateTotalBytes: Long,
    val insights: List<String>
)

data class DuplicateGroup(
    val checksum: String,
    val files: List<FileInfo>
) {
    val singleFileSize: Long get() = files.firstOrNull()?.size ?: 0L
    val totalWastedBytes: Long get() = if (files.size > 1) singleFileSize * (files.size - 1) else 0L
    val formattedWasted: String get() = FileInfo.formatFileSize(totalWastedBytes)
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val apkPath: String
) {
    val formattedSize: String get() = FileInfo.formatFileSize(apkSize)
}

data class ArchiveEntryInfo(
    val name: String,
    val isDirectory: Boolean,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val lastModified: Long
)
