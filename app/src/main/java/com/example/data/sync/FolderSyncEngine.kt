package com.example.data.sync

import com.example.data.local.ActivityLogDao
import com.example.data.local.ActivityLogEntity
import com.example.data.local.SyncTaskDao
import com.example.data.local.SyncTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SyncProgress(
    val taskId: Long = 0L,
    val isRunning: Boolean = false,
    val currentFile: String = "",
    val itemsProcessed: Int = 0,
    val totalItems: Int = 0,
    val bytesProcessed: Long = 0L
)

data class SyncResult(
    val isSuccess: Boolean,
    val filesCopied: Int,
    val filesDeleted: Int,
    val errorCount: Int,
    val message: String
)

class FolderSyncEngine(
    private val syncTaskDao: SyncTaskDao,
    private val activityLogDao: ActivityLogDao
) {
    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    suspend fun runSync(task: SyncTaskEntity): SyncResult = withContext(Dispatchers.IO) {
        val srcDir = File(task.sourcePath)
        val dstDir = File(task.destPath)

        if (!srcDir.exists() || !srcDir.isDirectory) {
            val msg = "Source folder does not exist: ${task.sourcePath}"
            syncTaskDao.updateStatus(task.id, System.currentTimeMillis(), "FAILED", msg)
            return@withContext SyncResult(false, 0, 0, 1, msg)
        }

        if (!dstDir.exists()) {
            dstDir.mkdirs()
        }

        _progress.value = SyncProgress(taskId = task.id, isRunning = true)
        syncTaskDao.updateStatus(task.id, System.currentTimeMillis(), "RUNNING", "Synchronizing files...")

        var copied = 0
        var deleted = 0
        var errors = 0

        try {
            when (task.syncType) {
                "ONE_WAY" -> {
                    // Copy new / updated files from src to dst
                    val srcFiles = srcDir.walkTopDown().filter { it.isFile }.toList()
                    _progress.value = _progress.value.copy(totalItems = srcFiles.size)

                    for (file in srcFiles) {
                        val relPath = file.relativeTo(srcDir).path
                        val targetFile = File(dstDir, relPath)
                        _progress.value = _progress.value.copy(
                            currentFile = file.name,
                            itemsProcessed = copied + errors
                        )

                        if (!targetFile.exists() || file.lastModified() > targetFile.lastModified()) {
                            try {
                                targetFile.parentFile?.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                                copied++
                            } catch (e: Exception) {
                                errors++
                            }
                        }
                    }
                }
                "TWO_WAY" -> {
                    // Bidirectional sync
                    val srcFiles = srcDir.walkTopDown().filter { it.isFile }.toList()
                    val dstFiles = dstDir.walkTopDown().filter { it.isFile }.toList()
                    _progress.value = _progress.value.copy(totalItems = srcFiles.size + dstFiles.size)

                    // 1. Src -> Dst
                    for (file in srcFiles) {
                        val relPath = file.relativeTo(srcDir).path
                        val targetFile = File(dstDir, relPath)
                        _progress.value = _progress.value.copy(currentFile = file.name)
                        if (!targetFile.exists() || file.lastModified() > targetFile.lastModified()) {
                            try {
                                targetFile.parentFile?.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                                copied++
                            } catch (e: Exception) {
                                errors++
                            }
                        }
                    }

                    // 2. Dst -> Src
                    for (file in dstFiles) {
                        val relPath = file.relativeTo(dstDir).path
                        val targetFile = File(srcDir, relPath)
                        _progress.value = _progress.value.copy(currentFile = file.name)
                        if (!targetFile.exists() || file.lastModified() > targetFile.lastModified()) {
                            try {
                                targetFile.parentFile?.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                                copied++
                            } catch (e: Exception) {
                                errors++
                            }
                        }
                    }
                }
                "MIRROR" -> {
                    // Exact clone of src into dst. Delete destination items not in source
                    val srcFiles = srcDir.walkTopDown().filter { it.isFile }.toList()
                    val srcRelPaths = srcFiles.map { it.relativeTo(srcDir).path }.toSet()
                    val dstFiles = dstDir.walkTopDown().filter { it.isFile }.toList()
                    _progress.value = _progress.value.copy(totalItems = srcFiles.size + dstFiles.size)

                    // Delete extra files in destination
                    for (file in dstFiles) {
                        val relPath = file.relativeTo(dstDir).path
                        if (relPath !in srcRelPaths) {
                            if (file.delete()) deleted++
                        }
                    }

                    // Copy all files from src
                    for (file in srcFiles) {
                        val relPath = file.relativeTo(srcDir).path
                        val targetFile = File(dstDir, relPath)
                        _progress.value = _progress.value.copy(currentFile = file.name)
                        if (!targetFile.exists() || file.lastModified() > targetFile.lastModified() || file.length() != targetFile.length()) {
                            try {
                                targetFile.parentFile?.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                                copied++
                            } catch (e: Exception) {
                                errors++
                            }
                        }
                    }
                }
            }

            val summaryMsg = "Synced $copied files, deleted $deleted, errors: $errors"
            val status = if (errors == 0) "SUCCESS" else "COMPLETED_WITH_ERRORS"
            syncTaskDao.updateStatus(task.id, System.currentTimeMillis(), status, summaryMsg)

            activityLogDao.insertLog(
                ActivityLogEntity(
                    action = "SYNC",
                    sourcePath = task.sourcePath,
                    destPath = task.destPath,
                    details = "Mode: ${task.syncType} | $summaryMsg",
                    isSuccess = errors == 0
                )
            )

            SyncResult(errors == 0, copied, deleted, errors, summaryMsg)
        } catch (e: Exception) {
            val err = "Sync failed: ${e.message}"
            syncTaskDao.updateStatus(task.id, System.currentTimeMillis(), "FAILED", err)
            SyncResult(false, copied, deleted, errors + 1, err)
        } finally {
            _progress.value = SyncProgress(taskId = task.id, isRunning = false)
        }
    }
}
