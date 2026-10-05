package com.example.data.model

enum class CloudProvider(val displayName: String, val iconRes: String) {
    GOOGLE_DRIVE("Google Drive", "google_drive"),
    DROPBOX("Dropbox", "dropbox"),
    ONEDRIVE("Microsoft OneDrive", "onedrive")
}

data class CloudAccount(
    val provider: CloudProvider,
    val accountEmail: String = "",
    val isConnected: Boolean = false,
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val accessToken: String? = null
) {
    val formattedUsed: String
        get() = FileInfo.formatFileSize(usedBytes)
    val formattedTotal: String
        get() = FileInfo.formatFileSize(totalBytes)
}

data class CloudFileItem(
    val id: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val mimeType: String,
    val parentId: String? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val provider: CloudProvider,
    val downloadUrl: String? = null,
    val isCachedLocally: Boolean = false,
    val localCachedPath: String? = null
) {
    val formattedSize: String
        get() = FileInfo.formatFileSize(size, isDirectory)
}
