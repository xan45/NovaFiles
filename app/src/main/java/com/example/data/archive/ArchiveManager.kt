package com.example.data.archive

import com.example.data.model.ArchiveEntryInfo
import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveManager {

    suspend fun listEntries(archiveFile: File): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        val ext = archiveFile.extension.lowercase()
        when (ext) {
            "7z" -> list7zEntries(archiveFile)
            "rar" -> listRarEntries(archiveFile)
            "tar", "tgz" -> listTarEntries(archiveFile)
            else -> listZipEntries(archiveFile)
        }
    }

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
                            isDirectory = entry.isDirectory,
                            compressedSize = entry.compressedSize,
                            uncompressedSize = entry.size,
                            lastModified = entry.time,
                            crc = entry.crc.toLong()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        entries.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    private fun list7zEntries(sevenZFile: File): List<ArchiveEntryInfo> {
        val entries = mutableListOf<ArchiveEntryInfo>()
        try {
            SevenZFile.builder().setFile(sevenZFile).get().use { sz ->
                var entry = sz.nextEntry
                while (entry != null) {
                    entries.add(
                        ArchiveEntryInfo(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            compressedSize = entry.size,
                            uncompressedSize = entry.size,
                            lastModified = entry.lastModifiedDate?.time ?: 0L,
                            crc = entry.crc.toLong()
                        )
                    )
                    entry = sz.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entries.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    private fun listRarEntries(rarFile: File): List<ArchiveEntryInfo> {
        val entries = mutableListOf<ArchiveEntryInfo>()
        try {
            Archive(rarFile).use { archive ->
                for (header in archive.fileHeaders) {
                    entries.add(
                        ArchiveEntryInfo(
                            name = header.fileName,
                            isDirectory = header.isDirectory,
                            compressedSize = header.fullPackSize,
                            uncompressedSize = header.fullUnpackSize,
                            lastModified = header.mTime?.time ?: 0L,
                            crc = header.fileCRC.toLong()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entries.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    private fun listTarEntries(tarFile: File): List<ArchiveEntryInfo> {
        val entries = mutableListOf<ArchiveEntryInfo>()
        try {
            val isGz = tarFile.name.lowercase().endsWith(".tar.gz") || tarFile.extension.lowercase() == "tgz"
            val rawStream = FileInputStream(tarFile)
            val inputStream = if (isGz) GZIPInputStream(rawStream) else rawStream
            TarArchiveInputStream(BufferedInputStream(inputStream)).use { tis ->
                var entry = tis.nextEntry
                while (entry != null) {
                    entries.add(
                        ArchiveEntryInfo(
                            name = entry.name,
                            isDirectory = entry.isDirectory,
                            compressedSize = entry.size,
                            uncompressedSize = entry.size,
                            lastModified = entry.modTime.time,
                            crc = 0L
                        )
                    )
                    entry = tis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entries.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    suspend fun extractArchive(
        archiveFile: File,
        targetDir: File,
        selectedEntryNames: Set<String>? = null,
        onProgress: (extracted: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val ext = archiveFile.extension.lowercase()
        when (ext) {
            "7z" -> extract7z(archiveFile, targetDir, onProgress)
            "rar" -> extractRar(archiveFile, targetDir, onProgress)
            else -> extractZip(archiveFile, targetDir, selectedEntryNames, onProgress)
        }
    }

    private fun extract7z(
        sevenZFile: File,
        targetDir: File,
        onProgress: (extracted: Int, total: Int) -> Unit
    ): Boolean {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            SevenZFile.builder().setFile(sevenZFile).get().use { sz ->
                var entry = sz.nextEntry
                var count = 0
                val total = 100
                while (entry != null) {
                    val destFile = File(targetDir, entry.name)
                    if (!destFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        entry = sz.nextEntry
                        continue
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (sz.read(buffer).also { len = it } != -1) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    count++
                    onProgress(count, total)
                    entry = sz.nextEntry
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun extractRar(
        rarFile: File,
        targetDir: File,
        onProgress: (extracted: Int, total: Int) -> Unit
    ): Boolean {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            Archive(rarFile).use { archive ->
                var count = 0
                val total = archive.fileHeaders.size.coerceAtLeast(1)
                for (header in archive.fileHeaders) {
                    val destFile = File(targetDir, header.fileName)
                    if (!destFile.canonicalPath.startsWith(targetDir.canonicalPath)) continue
                    if (header.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            archive.extractFile(header, fos)
                        }
                    }
                    count++
                    onProgress(count, total)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun extractZip(
        zipFile: File,
        targetDir: File,
        selectedEntryNames: Set<String>? = null,
        onProgress: (extracted: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                var count = 0
                val total = selectedEntryNames?.size ?: 100

                while (entry != null) {
                    val entryName = entry.name
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
        compressionLevel: Int = ZipOutputStream.DEFLATED
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
