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
