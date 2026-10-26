package com.example.data.network

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.EnumSet

data class SmbShareConfig(
    val host: String,
    val port: Int = 445,
    val shareName: String,
    val domain: String = "",
    val username: String = "",
    val password: String = ""
)

data class SmbFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

class SmbClientManager {

    private val client = SMBClient()
    private var activeConnection: Connection? = null
    private var activeSession: Session? = null
    private var activeDiskShare: DiskShare? = null

    suspend fun connect(config: SmbShareConfig): Result<DiskShare> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val connection = client.connect(config.host, config.port)
            activeConnection = connection

            val auth = if (config.username.isBlank()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(
                    config.username,
                    config.password.toCharArray(),
                    config.domain.ifBlank { null }
                )
            }

            val session = connection.authenticate(auth)
            activeSession = session

            val share = session.connectShare(config.shareName) as DiskShare
            activeDiskShare = share
            Result.success(share)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    suspend fun listFiles(path: String = ""): Result<List<SmbFileItem>> = withContext(Dispatchers.IO) {
        try {
            val share = activeDiskShare ?: return@withContext Result.failure(IllegalStateException("Not connected to SMB share"))
            val list = mutableListOf<SmbFileItem>()
            val entries = share.list(path)

            for (entry in entries) {
                val name = entry.fileName
                if (name == "." || name == "..") continue
                val isDir = (entry.fileAttributes and 0x10L) != 0L // FILE_ATTRIBUTE_DIRECTORY
                val fullPath = if (path.isBlank()) name else "$path\\$name"
                list.add(
                    SmbFileItem(
                        name = name,
                        path = fullPath,
                        isDirectory = isDir,
                        size = entry.endOfFile,
                        lastModified = entry.changeTime.toEpochMillis()
                    )
                )
            }
            Result.success(list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(remotePath: String, localTarget: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val share = activeDiskShare ?: return@withContext Result.failure(IllegalStateException("Not connected to SMB share"))
            localTarget.parentFile?.mkdirs()

            val smbFile = share.openFile(
                remotePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )

            smbFile.use { sf ->
                sf.inputStream.use { smbIn ->
                    FileOutputStream(localTarget).use { fos ->
                        smbIn.copyTo(fos)
                    }
                }
            }
            Result.success(localTarget)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(localSource: File, remotePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val share = activeDiskShare ?: return@withContext Result.failure(IllegalStateException("Not connected to SMB share"))
            val smbFile = share.openFile(
                remotePath,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            )

            smbFile.use { sf ->
                sf.outputStream.use { smbOut ->
                    localSource.inputStream().use { fis ->
                        fis.copyTo(smbOut)
                    }
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            activeDiskShare?.close()
            activeSession?.close()
            activeConnection?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            activeDiskShare = null
            activeSession = null
            activeConnection = null
        }
    }
}
