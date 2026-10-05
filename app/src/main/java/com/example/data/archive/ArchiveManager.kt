package com.example.data.archive

import com.example.data.model.ArchiveEntryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveManager {

    suspend fun listZipEntries(zipFile: File): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArchiveEntryInfo>()
        try {
            ZipFile(zipFile).use { zf ->
                val zipEntries = zf.entries()
                while (zipEntries.hasMoreElements()) {
                    val entry = zipEntries.nextElement()
                    entries.add(
                        ArchiveEntryInfo(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory,
                            crc = entry.crc
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        entries.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    suspend fun extractZip(
        zipFile: File,
        targetDir: File,
        selectedEntryNames: Set<String>? = null,
        onProgress: (extracted: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                var count = 0
                val total = selectedEntryNames?.size ?: 100

                while (entry != null) {
                    val entryName = entry.name

                    // Protect against Zip Slip vulnerability
                    val destFile = File(targetDir, entryName)
                    val canonicalDest = destFile.canonicalPath
                    if (!canonicalDest.startsWith(targetDir.canonicalPath)) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    val shouldExtract = selectedEntryNames == null || selectedEntryNames.contains(entryName)

                    if (shouldExtract) {
                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(destFile)).use { bos ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    bos.write(buffer, 0, len)
                                }
                            }
                        }
                        count++
                        onProgress(count, total)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createZip(
        sourceFiles: List<File>,
        destinationZip: File,
        compressionLevel: Int = ZipOutputStream.DEFLATED // DEFLATED or STORED
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            destinationZip.parentFile?.mkdirs()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
                zos.setLevel(if (compressionLevel == ZipOutputStream.STORED) 0 else 9)
                zos.setMethod(compressionLevel)

                for (source in sourceFiles) {
                    addFileToZip(source, source.name, zos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, relativePath: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val entryName = if (relativePath.endsWith("/")) relativePath else "$relativePath/"
            zos.putNextEntry(ZipEntry(entryName))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, "$entryName${child.name}", zos)
            }
        } else {
            val entry = ZipEntry(relativePath)
            entry.time = file.lastModified()
            zos.putNextEntry(entry)
            BufferedInputStream(FileInputStream(file)).use { bis ->
                val buffer = ByteArray(8192)
                var len: Int
                while (bis.read(buffer).also { len = it } != -1) {
                    zos.write(buffer, 0, len)
                }
            }
            zos.closeEntry()
        }
    }
}
