package com.example.data.root

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.model.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class RootEnvironmentStatus(
    val hasRootAccess: Boolean,
    val isShizukuAvailable: Boolean,
    val isShizukuAuthorized: Boolean
)

object RootStorageManager {

    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isShizukuAuthorized(): Boolean {
        return try {
            if (!isShizukuAvailable()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun getEnvironmentStatus(): RootEnvironmentStatus {
        return RootEnvironmentStatus(
            hasRootAccess = isRootAvailable(),
            isShizukuAvailable = isShizukuAvailable(),
            isShizukuAuthorized = isShizukuAuthorized()
        )
    }

    suspend fun executeElevatedCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = if (isRootAvailable()) {
                ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            } else {
                ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            Result.success(output.toString().trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listProtectedDirectory(path: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val cleanPath = if (path.endsWith("/")) path.dropLast(1) else path
        val command = "ls -la \"$cleanPath\""
        val result = executeElevatedCommand(command)

        val files = mutableListOf<FileInfo>()
        result.onSuccess { output ->
            val lines = output.lines()
            for (line in lines) {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 8 && !parts[0].startsWith("total")) {
                    val permissions = parts[0]
                    val isDir = permissions.startsWith("d")
                    val name = parts.drop(7).joinToString(" ")
                    if (name == "." || name == "..") continue

                    val fullPath = if (cleanPath == "/") "/$name" else "$cleanPath/$name"
                    val fileObj = File(fullPath)
                    files.add(FileInfo(fileObj))
                }
            }
        }
        files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
}
