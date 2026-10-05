package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.archive.ArchiveManager
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import com.example.data.local.NovaFilesDatabase
import com.example.data.local.RecentDao
import com.example.data.local.RecentEntity
import com.example.data.local.ScanHistoryDao
import com.example.data.local.ScanHistoryEntity
import com.example.data.local.TrashDao
import com.example.data.local.TrashEntity
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.FileSortOption
import com.example.data.model.InstalledAppInfo
import com.example.data.model.StorageAnalysisResult
import com.example.data.model.StorageVolumeInfo
import com.example.data.storage.StorageAnalyzerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileRepository(private val context: Context) {

    private val db = NovaFilesDatabase.getInstance(context)
    val favoriteDao: FavoriteDao = db.favoriteDao()
    val recentDao: RecentDao = db.recentDao()
    val trashDao: TrashDao = db.trashDao()
    val scanHistoryDao: ScanHistoryDao = db.scanHistoryDao()
    val fileTagDao: FileTagDao = db.fileTagDao()
    val folderCustomizationDao: FolderCustomizationDao = db.folderCustomizationDao()
    val syncTaskDao: SyncTaskDao = db.syncTaskDao()
    val activityLogDao: ActivityLogDao = db.activityLogDao()
    val syncEngine = com.example.data.sync.FolderSyncEngine(syncTaskDao, activityLogDao)

    private val storageEngine = StorageAnalyzerEngine(context)

    private val trashDirectory: File by lazy {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, ".recycle_bin")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val backupDirectory: File by lazy {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "NovaFiles/Backups")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun getStorageVolumes(): List<StorageVolumeInfo> = storageEngine.getStorageVolumes()

    suspend fun getFiles(
        directory: File,
        sortOption: FileSortOption = FileSortOption.NAME_ASC,
        showHidden: Boolean = false,
        searchQuery: String = ""
    ): List<FileInfo> = withContext(Dispatchers.IO) {
        val children = directory.listFiles() ?: return@withContext emptyList()
        val list = mutableListOf<FileInfo>()

        for (file in children) {
            val isHidden = file.isHidden || file.name.startsWith(".")
            if (!showHidden && isHidden) continue

            if (searchQuery.isNotBlank() && !file.name.contains(searchQuery, ignoreCase = true)) {
                continue
            }

            list.add(FileInfo(file))
        }

        sortFiles(list, sortOption)
    }

    suspend fun getFilesByCategory(
        category: FileCategory,
        sortOption: FileSortOption = FileSortOption.DATE_DESC
    ): List<FileInfo> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val results = mutableListOf<FileInfo>()

        when (category) {
            FileCategory.DOWNLOADS -> {
                val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (dlDir.exists()) {
                    dlDir.listFiles()?.forEach { if (!it.isHidden) results.add(FileInfo(it)) }
                }
            }
            FileCategory.ALL -> {
                return@withContext getFiles(root, sortOption)
            }
            else -> {
                // Collect files matching category from standard directories
                val candidateDirs = when (category) {
                    FileCategory.IMAGES -> listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    )
                    FileCategory.VIDEOS -> listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    )
                    FileCategory.AUDIO -> listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    )
                    FileCategory.DOCUMENTS -> listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    )
                    else -> listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        root
                    )
                }

                fun scan(dir: File, depth: Int = 0) {
                    if (depth > 4) return
                    val files = dir.listFiles() ?: return
                    for (f in files) {
                        if (f.name.startsWith(".")) continue
                        if (f.isDirectory) {
                            if (!f.name.equals("Android", ignoreCase = true)) {
                                scan(f, depth + 1)
                            }
                        } else {
                            val info = FileInfo(f)
                            if (info.category == category) {
                                results.add(info)
                            }
                        }
                    }
                }

                for (dir in candidateDirs) {
                    if (dir.exists()) scan(dir)
                }
            }
        }

        sortFiles(results, sortOption)
    }

    private fun sortFiles(files: List<FileInfo>, sortOption: FileSortOption): List<FileInfo> {
        val dirs = files.filter { it.isDirectory }
        val nonDirs = files.filter { !it.isDirectory }

        val comparator: Comparator<FileInfo> = when (sortOption) {
            FileSortOption.NAME_ASC -> compareBy { it.name.lowercase(Locale.ROOT) }
            FileSortOption.NAME_DESC -> compareByDescending { it.name.lowercase(Locale.ROOT) }
            FileSortOption.DATE_DESC -> compareByDescending { it.lastModified }
            FileSortOption.DATE_ASC -> compareBy { it.lastModified }
            FileSortOption.SIZE_DESC -> compareByDescending { it.size }
            FileSortOption.SIZE_ASC -> compareBy { it.size }
            FileSortOption.TYPE_ASC -> compareBy({ it.extension }, { it.name.lowercase(Locale.ROOT) })
        }

        return dirs.sortedWith(comparator) + nonDirs.sortedWith(comparator)
    }

    suspend fun createFolder(parent: File, name: String): Boolean = withContext(Dispatchers.IO) {
        val newFolder = File(parent, name)
        if (!newFolder.exists()) newFolder.mkdirs() else false
    }

    suspend fun createFile(parent: File, name: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(parent, name)
            if (!file.exists()) {
                file.createNewFile()
                if (content.isNotEmpty()) {
                    file.writeText(content)
                }
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun rename(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(file.parentFile, newName)
            if (target.exists()) return@withContext false
            file.renameTo(target)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun copy(source: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val dest = File(targetDir, source.name)
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun move(source: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val dest = File(targetDir, source.name)
            if (source.renameTo(dest)) {
                true
            } else {
                // Cross-device fallback
                val copied = if (source.isDirectory) {
                    source.copyRecursively(dest, overwrite = true)
                } else {
                    source.copyTo(dest, overwrite = true)
                    true
                }
                if (copied) {
                    source.deleteRecursively()
                    true
                } else false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun duplicate(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val parent = file.parentFile ?: return@withContext false
            val nameWithoutExt = file.nameWithoutExtension
            val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
            var copyIndex = 1
            var candidate = File(parent, "${nameWithoutExt}_copy$ext")
            while (candidate.exists()) {
                copyIndex++
                candidate = File(parent, "${nameWithoutExt}_copy$copyIndex$ext")
            }
            if (file.isDirectory) {
                file.copyRecursively(candidate)
            } else {
                file.copyTo(candidate)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun delete(file: File, moveToTrash: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        try {
            if (moveToTrash) {
                val trashName = "${System.currentTimeMillis()}_${file.name}"
                val trashFile = File(trashDirectory, trashName)
                val moved = file.renameTo(trashFile) || run {
                    val copied = if (file.isDirectory) file.copyRecursively(trashFile) else {
                        file.copyTo(trashFile)
                        true
                    }
                    if (copied) file.deleteRecursively() else false
                }
                if (moved) {
                    trashDao.insertTrash(
                        TrashEntity(
                            originalPath = file.absolutePath,
                            trashPath = trashFile.absolutePath,
                            fileName = file.name,
                            fileSize = if (trashFile.isDirectory) 0L else trashFile.length(),
                            isDirectory = trashFile.isDirectory
                        )
                    )
                    true
                } else false
            } else {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreTrash(trash: TrashEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(trash.trashPath)
            val originalFile = File(trash.originalPath)
            originalFile.parentFile?.mkdirs()
            val restored = trashFile.renameTo(originalFile) || run {
                val copied = if (trashFile.isDirectory) trashFile.copyRecursively(originalFile) else {
                    trashFile.copyTo(originalFile)
                    true
                }
                if (copied) trashFile.deleteRecursively() else false
            }
            if (restored) {
                trashDao.deleteTrashById(trash.id)
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        try {
            trashDirectory.deleteRecursively()
            trashDirectory.mkdirs()
            trashDao.clearTrash()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun runStorageAnalysis(): StorageAnalysisResult = withContext(Dispatchers.IO) {
        val result = storageEngine.analyzeStorage()
        // Save scan history
        scanHistoryDao.insertScan(
            ScanHistoryEntity(
                totalBytes = result.totalBytes,
                usedBytes = result.usedBytes,
                imagesBytes = result.imagesBytes,
                videosBytes = result.videosBytes,
                audioBytes = result.audioBytes,
                docsBytes = result.documentsBytes,
                archivesBytes = result.archivesBytes,
                apksBytes = result.apksBytes,
                duplicatesBytes = result.duplicateTotalBytes,
                junkBytes = result.junkTotalBytes
            )
        )
        result
    }

    suspend fun cleanJunkFiles(junkFiles: List<FileInfo>): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        for (f in junkFiles) {
            val size = f.file.length()
            if (f.file.delete()) {
                reclaimed += size
            }
        }
        reclaimed
    }

    suspend fun deleteEmptyFolders(emptyFolders: List<FileInfo>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (f in emptyFolders) {
            if (f.file.isDirectory && (f.file.listFiles()?.isEmpty() == true)) {
                if (f.file.delete()) deletedCount++
            }
        }
        deletedCount
    }

    suspend fun readTextPreview(file: File, maxLines: Int = 1000): Pair<String, Int> = withContext(Dispatchers.IO) {
        try {
            val lines = mutableListOf<String>()
            file.bufferedReader().useLines { sequence ->
                for (line in sequence.take(maxLines)) {
                    lines.add(line)
                }
            }
            Pair(lines.joinToString("\n"), lines.size)
        } catch (e: Exception) {
            Pair("Unable to preview text: ${e.message}", 0)
        }
    }

    suspend fun getExifMetadata(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val meta = mutableMapOf<String, String>()
        try {
            val exif = ExifInterface(file.absolutePath)
            meta["File Name"] = file.name
            meta["Size"] = FileInfo.formatFileSize(file.length())
            meta["Path"] = file.absolutePath

            val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (!date.isNullOrBlank()) meta["Date Taken"] = date

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            if (!make.isNullOrBlank() || !model.isNullOrBlank()) {
                meta["Camera"] = "${make.orEmpty()} ${model.orEmpty()}".trim()
            }

            val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            if (!width.isNullOrBlank() && !height.isNullOrBlank()) {
                meta["Dimensions"] = "${width} x ${height} px"
            }

            val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
            if (!iso.isNullOrBlank()) meta["ISO"] = iso

            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
            if (!fNumber.isNullOrBlank()) meta["Aperture"] = "f/$fNumber"

            val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            if (!exposure.isNullOrBlank()) meta["Exposure Time"] = "${exposure}s"

            val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            if (!focal.isNullOrBlank()) meta["Focal Length"] = "${focal}mm"
        } catch (e: Exception) {
            meta["Error"] = "EXIF information unavailable: ${e.localizedMessage}"
        }
        meta
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = mutableListOf<InstalledAppInfo>()
        try {
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val sourceDir = appInfo.sourceDir
                val apkFile = File(sourceDir)
                val appName = pm.getApplicationLabel(appInfo).toString()

                apps.add(
                    InstalledAppInfo(
                        appName = appName,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        apkPath = sourceDir,
                        apkSize = if (apkFile.exists()) apkFile.length() else 0L,
                        isSystemApp = isSystem,
                        permissions = pkg.requestedPermissions?.toList() ?: emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        apps.sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase(Locale.ROOT) }))
    }

    suspend fun extractApk(app: InstalledAppInfo): File? = withContext(Dispatchers.IO) {
        try {
            val src = File(app.apkPath)
            if (!src.exists()) return@withContext null
            val safeName = app.appName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val dest = File(backupDirectory, "${safeName}_v${app.versionName}.apk")
            src.copyTo(dest, overwrite = true)
            dest
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun getMimeType(file: File): String {
        return when (file.extension.lowercase(Locale.ROOT)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }

    suspend fun logRecentFile(file: File, action: String = "OPENED") {
        withContext(Dispatchers.IO) {
            recentDao.insertRecent(
                RecentEntity(
                    path = file.absolutePath,
                    name = file.name,
                    size = if (file.isDirectory) 0L else file.length(),
                    isDirectory = file.isDirectory,
                    mimeType = getMimeType(file),
                    action = action
                )
            )
        }
    }

    // --- Smart Collections ---
    suspend fun getFilesForCollection(collection: com.example.data.model.SmartCollection): List<FileInfo> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        when (collection) {
            com.example.data.model.SmartCollection.RECENT_DOWNLOADS -> {
                val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (dlDir.exists()) {
                    val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                    dlDir.listFiles()?.filter { it.isFile && it.lastModified() >= sevenDaysAgo }
                        ?.map { FileInfo(it) }?.sortedByDescending { it.lastModified } ?: emptyList()
                } else emptyList()
            }
            com.example.data.model.SmartCollection.LARGE_FILES -> {
                val threshold = 100L * 1024 * 1024 // 100MB
                val list = mutableListOf<FileInfo>()
                root.walkTopDown().maxDepth(6).filter { it.isFile && it.length() >= threshold }.take(100).forEach {
                    list.add(FileInfo(it))
                }
                list.sortedByDescending { it.size }
            }
            com.example.data.model.SmartCollection.APK_BACKUPS -> {
                val list = mutableListOf<FileInfo>()
                root.walkTopDown().maxDepth(5).filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }.take(80).forEach {
                    list.add(FileInfo(it))
                }
                list.sortedByDescending { it.lastModified }
            }
            com.example.data.model.SmartCollection.SCREENSHOTS -> {
                val list = mutableListOf<FileInfo>()
                val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                listOf(File(picDir, "Screenshots"), File(dcimDir, "Screenshots")).forEach { sDir ->
                    if (sDir.exists()) {
                        sDir.listFiles()?.filter { it.isFile }?.forEach { list.add(FileInfo(it)) }
                    }
                }
                list.sortedByDescending { it.lastModified }
            }
            com.example.data.model.SmartCollection.VIDEOS -> {
                getFilesByCategory(com.example.data.model.FileCategory.VIDEOS)
            }
            com.example.data.model.SmartCollection.DOCUMENTS -> {
                getFilesByCategory(com.example.data.model.FileCategory.DOCUMENTS)
            }
            com.example.data.model.SmartCollection.FAVORITES -> {
                val favs = favoriteDao.getAllFavorites()
                // Return currently pinned favorites as FileInfo
                emptyList()
            }
        }
    }

    // --- Tags ---
    suspend fun addTagToFile(path: String, tag: String, colorHex: String = "#3B82F6") {
        withContext(Dispatchers.IO) {
            fileTagDao.insertTag(com.example.data.local.FileTagEntity(path, tag, colorHex))
        }
    }

    suspend fun removeTagFromFile(path: String, tag: String) {
        withContext(Dispatchers.IO) {
            fileTagDao.deleteTag(path, tag)
        }
    }

    // --- Folder Customization ---
    suspend fun setFolderCustomization(path: String, colorHex: String?, iconName: String?, note: String?, coverImagePath: String?) {
        withContext(Dispatchers.IO) {
            folderCustomizationDao.setCustomization(
                com.example.data.local.FolderCustomizationEntity(path, colorHex, iconName, note, coverImagePath)
            )
        }
    }

    suspend fun removeFolderCustomization(path: String) {
        withContext(Dispatchers.IO) {
            folderCustomizationDao.removeCustomization(path)
        }
    }

    // --- Activity Logging ---
    suspend fun logActivity(action: String, src: String, dst: String? = null, details: String = "", success: Boolean = true) {
        withContext(Dispatchers.IO) {
            activityLogDao.insertLog(
                com.example.data.local.ActivityLogEntity(
                    action = action,
                    sourcePath = src,
                    destPath = dst,
                    details = details,
                    isSuccess = success
                )
            )
        }
    }
}
