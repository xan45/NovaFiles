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
    val uri: Uri? = null
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
    LARGE_FILES("Large Files"),
    DUPLICATES("Duplicates")
}

enum class FileSortOption(val displayName: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    SIZE_DESC("Size (Largest first)"),
    SIZE_ASC("Size (Smallest first)"),
    TYPE_ASC("Type (Extension)")
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
    val usedBytes: Long = (totalBytes - freeBytes).coerceAtLeast(0L),
    val isRemovable: Boolean = false
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedTotal: String
        get() = FileInfo.formatFileSize(totalBytes)

    val formattedUsed: String
        get() = FileInfo.formatFileSize(usedBytes)

    val formattedFree: String
        get() = FileInfo.formatFileSize(freeBytes)
}

data class DuplicateGroup(
    val checksum: String,
    val size: Long,
    val files: List<FileInfo>
) {
    val totalWastedBytes: Long
        get() = if (files.size > 1) size * (files.size - 1) else 0L

    val formattedWasted: String
        get() = FileInfo.formatFileSize(totalWastedBytes)
}

data class ArchiveEntryInfo(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val crc: Long
) {
    val formattedSize: String
        get() = FileInfo.formatFileSize(size)
}

data class StorageAnalysisResult(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val imagesBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val archivesBytes: Long = 0L,
    val apksBytes: Long = 0L,
    val othersBytes: Long = 0L,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val largestFiles: List<FileInfo> = emptyList(),
    val emptyFolders: List<FileInfo> = emptyList(),
    val junkFiles: List<FileInfo> = emptyList(),
    val junkTotalBytes: Long = 0L,
    val duplicateTotalBytes: Long = 0L,
    val insights: List<String> = emptyList()
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val apkPath: String,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val permissions: List<String> = emptyList()
) {
    val formattedSize: String
        get() = FileInfo.formatFileSize(apkSize)
}
