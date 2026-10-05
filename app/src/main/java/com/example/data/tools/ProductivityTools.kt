package com.example.data.tools

import com.example.data.local.ActivityLogDao
import com.example.data.local.ActivityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.SecureRandom

// --- Batch Rename ---
data class BatchRenameConfig(
    val findText: String = "",
    val replaceText: String = "",
    val isRegex: Boolean = false,
    val isCaseSensitive: Boolean = false,
    val prefix: String = "",
    val suffix: String = "",
    val useSequentialNumbering: Boolean = false,
    val numberStart: Int = 1,
    val numberStep: Int = 1,
    val numberDigits: Int = 3, // e.g. 001
    val numberPosition: NumberPosition = NumberPosition.SUFFIX,
    val extensionChange: ExtensionChange = ExtensionChange.KEEP
)

enum class NumberPosition {
    PREFIX, SUFFIX
}

enum class ExtensionChange {
    KEEP, LOWERCASE, UPPERCASE, CUSTOM
}

data class RenameItem(
    val originalFile: File,
    val newName: String,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

object BatchRenameEngine {
    fun computePreview(files: List<File>, config: BatchRenameConfig): List<RenameItem> {
        var currentNumber = config.numberStart

        return files.mapIndexed { index, file ->
            val ext = file.extension
            var baseName = file.nameWithoutExtension

            // 1. Find & Replace
            if (config.findText.isNotEmpty()) {
                baseName = try {
                    if (config.isRegex) {
                        val options = if (config.isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                        baseName.replace(Regex(config.findText, options), config.replaceText)
                    } else {
                        baseName.replace(config.findText, config.replaceText, ignoreCase = !config.isCaseSensitive)
                    }
                } catch (e: Exception) {
                    baseName
                }
            }

            // 2. Sequential numbering
            val numberStr = if (config.useSequentialNumbering) {
                String.format("%0${config.numberDigits}d", currentNumber)
            } else ""

            if (config.useSequentialNumbering) {
                currentNumber += config.numberStep
            }

            // 3. Prefix & Suffix assembly
            var resultBase = baseName
            if (config.prefix.isNotEmpty()) {
                resultBase = "${config.prefix}$resultBase"
            }
            if (config.suffix.isNotEmpty()) {
                resultBase = "$resultBase${config.suffix}"
            }
            if (config.useSequentialNumbering) {
                resultBase = when (config.numberPosition) {
                    NumberPosition.PREFIX -> "$numberStr-$resultBase"
                    NumberPosition.SUFFIX -> "${resultBase}_$numberStr"
                }
            }

            // 4. Extension handling
            val newExt = when (config.extensionChange) {
                ExtensionChange.KEEP -> ext
                ExtensionChange.LOWERCASE -> ext.lowercase()
                ExtensionChange.UPPERCASE -> ext.uppercase()
                ExtensionChange.CUSTOM -> ext
            }

            val finalName = if (newExt.isNotEmpty() && !file.isDirectory) "$resultBase.$newExt" else resultBase
            val isValid = finalName.isNotBlank() && !finalName.contains("/") && !finalName.contains("\\")

            RenameItem(
                originalFile = file,
                newName = finalName,
                isValid = isValid,
                errorMessage = if (!isValid) "Invalid characters in filename" else null
            )
        }
    }

    suspend fun applyBatchRename(
        items: List<RenameItem>,
        activityLogDao: ActivityLogDao
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0

        for (item in items) {
            if (!item.isValid || item.newName == item.originalFile.name) continue
            val parent = item.originalFile.parentFile ?: continue
            val target = File(parent, item.newName)

            if (item.originalFile.renameTo(target)) {
                success++
                activityLogDao.insertLog(
                    ActivityLogEntity(
                        action = "RENAME",
                        sourcePath = item.originalFile.absolutePath,
                        destPath = target.absolutePath,
                        details = "Batch rename: ${item.originalFile.name} -> ${item.newName}",
                        isSuccess = true
                    )
                )
            } else {
                failed++
            }
        }
        Pair(success, failed)
    }
}

// --- File Comparison Tool ---
data class FileComparisonResult(
    val fileA: File,
    val fileB: File,
    val nameMatch: Boolean,
    val sizeMatch: Boolean,
    val sizeA: Long,
    val sizeB: Long,
    val dateA: Long,
    val dateB: Long,
    val md5A: String,
    val md5B: String,
    val md5Match: Boolean,
    val sha256A: String,
    val sha256B: String,
    val sha256Match: Boolean,
    val isExactContentMatch: Boolean,
    val isTextDiffAvailable: Boolean = false,
    val textDiffSummary: String = ""
)

object FileCompareEngine {
    suspend fun compareFiles(fileA: File, fileB: File): FileComparisonResult = withContext(Dispatchers.IO) {
        val md5A = ChecksumTool.calculateHash(fileA, "MD5")
        val md5B = ChecksumTool.calculateHash(fileB, "MD5")
        val sha256A = ChecksumTool.calculateHash(fileA, "SHA-256")
        val sha256B = ChecksumTool.calculateHash(fileB, "SHA-256")

        val isText = (fileA.extension in listOf("txt", "json", "xml", "md", "csv", "log") &&
                      fileB.extension in listOf("txt", "json", "xml", "md", "csv", "log")) &&
                      fileA.length() < 500_000 && fileB.length() < 500_000

        var diffSummary = ""
        if (isText) {
            val linesA = runCatching { fileA.readLines() }.getOrDefault(emptyList())
            val linesB = runCatching { fileB.readLines() }.getOrDefault(emptyList())
            val diffCount = Math.abs(linesA.size - linesB.size)
            diffSummary = "File A: ${linesA.size} lines | File B: ${linesB.size} lines | Size diff: ${Math.abs(fileA.length() - fileB.length())} bytes"
        }

        FileComparisonResult(
            fileA = fileA,
            fileB = fileB,
            nameMatch = fileA.name == fileB.name,
            sizeMatch = fileA.length() == fileB.length(),
            sizeA = fileA.length(),
            sizeB = fileB.length(),
            dateA = fileA.lastModified(),
            dateB = fileB.lastModified(),
            md5A = md5A,
            md5B = md5B,
            md5Match = md5A == md5B && md5A.isNotEmpty(),
            sha256A = sha256A,
            sha256B = sha256B,
            sha256Match = sha256A == sha256B && sha256A.isNotEmpty(),
            isExactContentMatch = sha256A == sha256B && sha256A.isNotEmpty(),
            isTextDiffAvailable = isText,
            textDiffSummary = diffSummary
        )
    }
}

// --- Checksum & Hashes ---
object ChecksumTool {
    fun calculateHash(file: File, algorithm: String): String {
        if (!file.exists() || file.isDirectory) return ""
        return try {
            val md = MessageDigest.getInstance(algorithm)
            val buffer = ByteArray(16384)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun verifyChecksum(computed: String, target: String): Boolean {
        return computed.isNotBlank() && computed.trim().equals(target.trim(), ignoreCase = true)
    }
}

// --- Secure File Shredder ---
object FileShredder {
    suspend fun shredFile(
        file: File,
        passes: Int = 3,
        activityLogDao: ActivityLogDao
    ): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false

        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { shredFile(it, passes, activityLogDao) }
                return@withContext file.delete()
            }

            val length = file.length()
            val random = SecureRandom()
            val buffer = ByteArray(8192)

            RandomAccessFile(file, "rws").use { raf ->
                for (pass in 1..passes) {
                    raf.seek(0)
                    var written: Long = 0
                    while (written < length) {
                        val toWrite = Math.min(buffer.size.toLong(), length - written).toInt()
                        when (pass) {
                            1 -> buffer.fill(0x00.toByte())
                            2 -> buffer.fill(0xFF.toByte())
                            else -> random.nextBytes(buffer)
                        }
                        raf.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                }
                raf.setLength(0)
            }

            val deleted = file.delete()
            activityLogDao.insertLog(
                ActivityLogEntity(
                    action = "SHRED",
                    sourcePath = file.absolutePath,
                    details = "Securely shredded with $passes overwrites (DoD 5220.22-M)",
                    isSuccess = deleted
                )
            )
            deleted
        } catch (e: Exception) {
            false
        }
    }
}
