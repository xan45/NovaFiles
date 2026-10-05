package com.example.data.network

data class NetworkConnection(
    val id: String,
    val name: String,
    val protocol: NetworkProtocol,
    val host: String,
    val port: Int = protocol.defaultPort,
    val username: String = "",
    val password: String = "",
    val path: String = "/",
    val isAnonymous: Boolean = false,
    val lastConnected: Long = 0L
)

enum class NetworkProtocol(val displayName: String, val defaultPort: Int, val description: String) {
    FTP("FTP", 21, "File Transfer Protocol (Local & Remote)"),
    SFTP("SFTP", 22, "SSH File Transfer Protocol (Encrypted)"),
    WEBDAV("WebDAV", 80, "HTTP-based distributed authoring"),
    SMB("SMB / LAN", 445, "Windows Network Shares & NAS")
}

data class RemoteFileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long = System.currentTimeMillis()
)
