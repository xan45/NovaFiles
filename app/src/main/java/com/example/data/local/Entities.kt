package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_files")
data class RecentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val mimeType: String,
    val action: String = "OPENED", // OPENED, MODIFIED, SHARED
    val accessedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recycle_bin")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val originalPath: String,
    val trashPath: String,
    val fileName: String,
    val fileSize: Long,
    val isDirectory: Boolean,
    val deletedAt: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(deletedAt))
}

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val totalBytes: Long,
    val usedBytes: Long,
    val imagesBytes: Long,
    val videosBytes: Long,
    val audioBytes: Long,
    val docsBytes: Long,
    val archivesBytes: Long,
    val apksBytes: Long,
    val duplicatesBytes: Long,
    val junkBytes: Long
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Entity(tableName = "cloud_cache")
data class CloudCacheEntity(
    @PrimaryKey val id: String, // "${provider}_${fileId}"
    val fileId: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val mimeType: String,
    val provider: String,
    val parentId: String? = null,
    val cachedFilePath: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "file_tags", primaryKeys = ["path", "tag"])
data class FileTagEntity(
    val path: String,
    val tag: String,
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "folder_customization")
data class FolderCustomizationEntity(
    @PrimaryKey val path: String,
    val colorHex: String? = null,
    val iconName: String? = null,
    val note: String? = null,
    val coverImagePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_tasks")
data class SyncTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sourcePath: String,
    val destPath: String,
    val syncType: String = "ONE_WAY", // ONE_WAY, TWO_WAY, MIRROR
    val intervalHours: Int = 0,
    val lastSyncTime: Long = 0L,
    val lastSyncStatus: String = "IDLE", // SUCCESS, FAILED, RUNNING, IDLE
    val lastSyncMessage: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val action: String, // COPY, MOVE, DELETE, RENAME, EXTRACT, ARCHIVE, RESTORE, SHRED, SYNC
    val sourcePath: String,
    val destPath: String? = null,
    val details: String = "",
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
