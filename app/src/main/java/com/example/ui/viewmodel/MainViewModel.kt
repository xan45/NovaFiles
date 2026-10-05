package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.archive.ArchiveManager
import com.example.data.local.FavoriteEntity
import com.example.data.local.RecentEntity
import com.example.data.local.ScanHistoryEntity
import com.example.data.local.TrashEntity
import com.example.data.model.ArchiveEntryInfo
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import com.example.data.model.FileSortOption
import com.example.data.model.FileViewMode
import com.example.data.model.InstalledAppInfo
import com.example.data.model.StorageAnalysisResult
import com.example.data.model.StorageVolumeInfo
import com.example.data.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.cloud.CloudStorageManager
import com.example.data.model.CloudAccount
import com.example.data.model.CloudFileItem
import com.example.data.model.CloudProvider
import java.io.File
import java.util.zip.ZipOutputStream

enum class AppScreen {
    HOME,
    BROWSER,
    DUAL_PANE,
    STORAGE_ANALYZER,
    APK_MANAGER,
    RECYCLE_BIN,
    FAVORITES,
    VAULT,
    SETTINGS,
    ONBOARDING,
    CLOUD_STORAGE
}

enum class ActivePane { LEFT, RIGHT }
enum class ClipboardOp { COPY, CUT }

enum class StorageSubTab(val title: String) {
    OVERVIEW("Overview"),
    DUPLICATES("Duplicates"),
    LARGE_FILES("Large Files"),
    EMPTY_FOLDERS("Empty Folders"),
    JUNK_CLEANER("Junk Cleaner"),
    INSIGHTS("AI Insights"),
    HISTORY("Scan History")
}

sealed class DialogType {
    data class CreateFolder(val parentDir: File) : DialogType()
    data class CreateFile(val parentDir: File) : DialogType()
    data class Rename(val file: File) : DialogType()
    data class CreateZip(val sourceFiles: List<File>, val parentDir: File) : DialogType()
    data class FileDetails(val file: File) : DialogType()
    data class DeleteConfirm(val files: List<File>, val permanent: Boolean) : DialogType()
    data class SetPin(val isChange: Boolean = false) : DialogType()
    data object UnlockVault : DialogType()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)
    private val prefs = application.getSharedPreferences("novafiles_prefs", Context.MODE_PRIVATE)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Storage Volumes
    private val _storageVolumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val storageVolumes: StateFlow<List<StorageVolumeInfo>> = _storageVolumes.asStateFlow()

    // Browser State
    private val _currentDirectory = MutableStateFlow(Environment.getExternalStorageDirectory())
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _files = MutableStateFlow<List<FileInfo>>(emptyList())
    val files: StateFlow<List<FileInfo>> = _files.asStateFlow()

    private val _isLoadingFiles = MutableStateFlow(false)
    val isLoadingFiles: StateFlow<Boolean> = _isLoadingFiles.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<FileInfo>>(emptySet())
    val selectedFiles: StateFlow<Set<FileInfo>> = _selectedFiles.asStateFlow()

    private val _sortOption = MutableStateFlow(FileSortOption.NAME_ASC)
    val sortOption: StateFlow<FileSortOption> = _sortOption.asStateFlow()

    private val _viewMode = MutableStateFlow(FileViewMode.LIST)
    val viewMode: StateFlow<FileViewMode> = _viewMode.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean("show_hidden", false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeCategory = MutableStateFlow<FileCategory?>(null)
    val activeCategory: StateFlow<FileCategory?> = _activeCategory.asStateFlow()

    // Clipboard
    private val _clipboardFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val clipboardFiles: StateFlow<List<FileInfo>> = _clipboardFiles.asStateFlow()
    private val _clipboardOp = MutableStateFlow<ClipboardOp?>(null)
    val clipboardOp: StateFlow<ClipboardOp?> = _clipboardOp.asStateFlow()

    // Dual Pane State
    private val _dualLeftDirectory = MutableStateFlow(Environment.getExternalStorageDirectory())
    val dualLeftDirectory: StateFlow<File> = _dualLeftDirectory.asStateFlow()

    private val _dualLeftFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val dualLeftFiles: StateFlow<List<FileInfo>> = _dualLeftFiles.asStateFlow()

    private val _dualLeftSelected = MutableStateFlow<Set<FileInfo>>(emptySet())
    val dualLeftSelected: StateFlow<Set<FileInfo>> = _dualLeftSelected.asStateFlow()

    private val _dualRightDirectory = MutableStateFlow(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).let {
            if (it.exists()) it else Environment.getExternalStorageDirectory()
        }
    )
    val dualRightDirectory: StateFlow<File> = _dualRightDirectory.asStateFlow()

    private val _dualRightFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val dualRightFiles: StateFlow<List<FileInfo>> = _dualRightFiles.asStateFlow()

    private val _dualRightSelected = MutableStateFlow<Set<FileInfo>>(emptySet())
    val dualRightSelected: StateFlow<Set<FileInfo>> = _dualRightSelected.asStateFlow()

    private val _activePane = MutableStateFlow(ActivePane.LEFT)
    val activePane: StateFlow<ActivePane> = _activePane.asStateFlow()

    // Storage Analyzer State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisProgress = MutableStateFlow(Pair(0, ""))
    val analysisProgress: StateFlow<Pair<Int, String>> = _analysisProgress.asStateFlow()

    private val _analysisResult = MutableStateFlow<StorageAnalysisResult?>(null)
    val analysisResult: StateFlow<StorageAnalysisResult?> = _analysisResult.asStateFlow()

    private val _analysisSubTab = MutableStateFlow(StorageSubTab.OVERVIEW)
    val analysisSubTab: StateFlow<StorageSubTab> = _analysisSubTab.asStateFlow()

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = repository.scanHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites & Recents & Trash
    val favorites: StateFlow<List<FavoriteEntity>> = repository.favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<RecentEntity>> = repository.recentDao.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashFiles: StateFlow<List<TrashEntity>> = repository.trashDao.getAllTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // APK Manager State
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    private val _filterSystemApps = MutableStateFlow(false)
    val filterSystemApps: StateFlow<Boolean> = _filterSystemApps.asStateFlow()

    // Vault State
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _vaultFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val vaultFiles: StateFlow<List<FileInfo>> = _vaultFiles.asStateFlow()

    val hasVaultPin: Boolean
        get() = prefs.getString("vault_pin", null) != null

    // Previews & Players
    private val _previewImage = MutableStateFlow<File?>(null)
    val previewImage: StateFlow<File?> = _previewImage.asStateFlow()
    private val _imageExif = MutableStateFlow<Map<String, String>>(emptyMap())
    val imageExif: StateFlow<Map<String, String>> = _imageExif.asStateFlow()

    private val _previewVideo = MutableStateFlow<File?>(null)
    val previewVideo: StateFlow<File?> = _previewVideo.asStateFlow()

    private val _previewAudio = MutableStateFlow<File?>(null)
    val previewAudio: StateFlow<File?> = _previewAudio.asStateFlow()
    private var mediaPlayer: MediaPlayer? = null
    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()
    private val _audioProgress = MutableStateFlow(0f)
    val audioProgress: StateFlow<Float> = _audioProgress.asStateFlow()
    private val _audioDuration = MutableStateFlow(0)
    val audioDuration: StateFlow<Int> = _audioDuration.asStateFlow()

    private val _previewDoc = MutableStateFlow<Pair<File, String>?>(null)
    val previewDoc: StateFlow<Pair<File, String>?> = _previewDoc.asStateFlow()

    private val _previewArchive = MutableStateFlow<File?>(null)
    val previewArchive: StateFlow<File?> = _previewArchive.asStateFlow()
    private val _archiveEntries = MutableStateFlow<List<ArchiveEntryInfo>>(emptyList())
    val archiveEntries: StateFlow<List<ArchiveEntryInfo>> = _archiveEntries.asStateFlow()

    // Active Dialog
    private val _activeDialog = MutableStateFlow<DialogType?>(null)
    val activeDialog: StateFlow<DialogType?> = _activeDialog.asStateFlow()

    // Toast / Feedback message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Cloud Storage
    private val cloudManager = CloudStorageManager(application)
    private val _cloudAccounts = MutableStateFlow<List<CloudAccount>>(emptyList())
    val cloudAccounts: StateFlow<List<CloudAccount>> = _cloudAccounts.asStateFlow()

    private val _selectedCloudProvider = MutableStateFlow<CloudProvider?>(null)
    val selectedCloudProvider: StateFlow<CloudProvider?> = _selectedCloudProvider.asStateFlow()

    private val _cloudFiles = MutableStateFlow<List<CloudFileItem>>(emptyList())
    val cloudFiles: StateFlow<List<CloudFileItem>> = _cloudFiles.asStateFlow()

    private val _cloudFolderId = MutableStateFlow<String?>(null)
    val cloudFolderId: StateFlow<String?> = _cloudFolderId.asStateFlow()

    init {
        if (!prefs.getBoolean("onboarding_completed", false)) {
            _currentScreen.value = AppScreen.ONBOARDING
        }
        refreshStorageVolumes()
        loadCurrentFiles()
        loadDualFiles(ActivePane.LEFT)
        loadDualFiles(ActivePane.RIGHT)
        loadCloudAccounts()
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _currentScreen.value = AppScreen.HOME
    }

    fun loadCloudAccounts() {
        _cloudAccounts.value = cloudManager.getConnectedAccounts()
    }

    fun connectCloudAccount(provider: CloudProvider) {
        val email = when (provider) {
            CloudProvider.GOOGLE_DRIVE -> "user@gmail.com"
            CloudProvider.DROPBOX -> "user@dropbox.com"
            CloudProvider.ONEDRIVE -> "user@outlook.com"
        }
        cloudManager.connectAccount(provider, email)
        loadCloudAccounts()
        showSnackbar("Connected to ${provider.displayName}")
    }

    fun disconnectCloudAccount(provider: CloudProvider) {
        cloudManager.disconnectAccount(provider)
        if (_selectedCloudProvider.value == provider) {
            _selectedCloudProvider.value = null
        }
        loadCloudAccounts()
        showSnackbar("Disconnected from ${provider.displayName}")
    }

    fun selectCloudProvider(provider: CloudProvider?) {
        _selectedCloudProvider.value = provider
        _cloudFolderId.value = null
        if (provider != null) {
            loadCloudFiles(provider, null)
        }
    }

    fun navigateCloudFolder(item: CloudFileItem) {
        _cloudFolderId.value = item.id
        val prov = _selectedCloudProvider.value ?: return
        loadCloudFiles(prov, item.id)
    }

    fun navigateCloudUp() {
        if (_cloudFolderId.value != null) {
            _cloudFolderId.value = null
            val prov = _selectedCloudProvider.value ?: return
            loadCloudFiles(prov, null)
        } else {
            _selectedCloudProvider.value = null
        }
    }

    private fun loadCloudFiles(provider: CloudProvider, folderId: String?) {
        viewModelScope.launch {
            _cloudFiles.value = cloudManager.getCloudFiles(provider, folderId)
        }
    }

    fun downloadCloudFile(item: CloudFileItem) {
        viewModelScope.launch {
            val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloaded = cloudManager.downloadCloudFile(item, destDir)
            if (downloaded != null) {
                showSnackbar("Downloaded ${item.name} to Downloads")
                loadCurrentFiles()
            } else {
                showSnackbar("Download failed")
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.APK_MANAGER && _installedApps.value.isEmpty()) {
            loadInstalledApps()
        }
        if (screen == AppScreen.STORAGE_ANALYZER && _analysisResult.value == null && !_isAnalyzing.value) {
            runStorageAnalysis()
        }
        if (screen == AppScreen.VAULT && _isVaultUnlocked.value) {
            loadVaultFiles()
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun refreshStorageVolumes() {
        _storageVolumes.value = repository.getStorageVolumes()
    }

    fun setSortOption(option: FileSortOption) {
        _sortOption.value = option
        loadCurrentFiles()
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == FileViewMode.LIST) FileViewMode.GRID else FileViewMode.LIST
    }

    fun toggleShowHidden() {
        val newVal = !_showHiddenFiles.value
        _showHiddenFiles.value = newVal
        prefs.edit().putBoolean("show_hidden", newVal).apply()
        loadCurrentFiles()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadCurrentFiles()
    }

    fun setCategory(category: FileCategory?) {
        _activeCategory.value = category
        _selectedFiles.value = emptySet()
        if (category != null) {
            _currentScreen.value = AppScreen.BROWSER
            viewModelScope.launch {
                _isLoadingFiles.value = true
                _files.value = repository.getFilesByCategory(category, _sortOption.value)
                _isLoadingFiles.value = false
            }
        } else {
            loadCurrentFiles()
        }
    }

    fun navigateToDirectory(directory: File) {
        _activeCategory.value = null
        _currentDirectory.value = directory
        _selectedFiles.value = emptySet()
        loadCurrentFiles()
    }

    fun navigateUp(): Boolean {
        if (_activeCategory.value != null) {
            _activeCategory.value = null
            loadCurrentFiles()
            return true
        }
        val current = _currentDirectory.value
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            _currentDirectory.value = parent
            _selectedFiles.value = emptySet()
            loadCurrentFiles()
            return true
        }
        return false
    }

    fun loadCurrentFiles() {
        viewModelScope.launch {
            _isLoadingFiles.value = true
            val category = _activeCategory.value
            _files.value = if (category != null) {
                repository.getFilesByCategory(category, _sortOption.value)
            } else {
                repository.getFiles(
                    _currentDirectory.value,
                    _sortOption.value,
                    _showHiddenFiles.value,
                    _searchQuery.value
                )
            }
            _isLoadingFiles.value = false
        }
    }

    fun toggleFileSelection(file: FileInfo) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(file)) {
            current.remove(file)
        } else {
            current.add(file)
        }
        _selectedFiles.value = current
    }

    fun selectAllFiles() {
        _selectedFiles.value = _files.value.toSet()
    }

    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    // File Operations
    fun openFile(fileInfo: FileInfo, context: Context) {
        val file = fileInfo.file
        viewModelScope.launch {
            repository.logRecentFile(file, "OPENED")
        }

        if (file.isDirectory) {
            navigateToDirectory(file)
            return
        }

        when (fileInfo.category) {
            FileCategory.IMAGES -> {
                _previewImage.value = file
                viewModelScope.launch {
                    _imageExif.value = repository.getExifMetadata(file)
                }
            }
            FileCategory.AUDIO -> {
                startAudioPlayback(file)
            }
            FileCategory.VIDEOS -> {
                _previewVideo.value = file
            }
            FileCategory.DOCUMENTS -> {
                val ext = fileInfo.extension
                if (ext in listOf("txt", "json", "xml", "md", "csv", "html", "js", "kt", "java", "py", "c", "cpp")) {
                    viewModelScope.launch {
                        val (content, _) = repository.readTextPreview(file)
                        _previewDoc.value = Pair(file, content)
                    }
                } else {
                    launchExternalIntent(file, context)
                }
            }
            FileCategory.ARCHIVES -> {
                if (fileInfo.extension == "zip") {
                    _previewArchive.value = file
                    viewModelScope.launch {
                        _archiveEntries.value = ArchiveManager.listZipEntries(file)
                    }
                } else {
                    launchExternalIntent(file, context)
                }
            }
            else -> {
                launchExternalIntent(file, context)
            }
        }
    }

    private fun launchExternalIntent(file: File, context: Context) {
        try {
            val uri = repository.getUriForFile(file)
            val mime = repository.getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showSnackbar("No application found to open this file")
        }
    }

    fun shareFiles(files: List<FileInfo>, context: Context) {
        try {
            if (files.isEmpty()) return
            if (files.size == 1) {
                val f = files.first().file
                val uri = repository.getUriForFile(f)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = repository.getMimeType(f)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share file"))
            } else {
                val uris = ArrayList<Uri>()
                for (f in files) {
                    if (!f.isDirectory) {
                        uris.add(repository.getUriForFile(f.file))
                    }
                }
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share files"))
            }
            viewModelScope.launch {
                files.forEach { repository.logRecentFile(it.file, "SHARED") }
            }
        } catch (e: Exception) {
            showSnackbar("Failed to share: ${e.message}")
        }
    }

    fun copySelectedToClipboard(cut: Boolean = false) {
        _clipboardFiles.value = _selectedFiles.value.toList()
        _clipboardOp.value = if (cut) ClipboardOp.CUT else ClipboardOp.COPY
        clearSelection()
        showSnackbar(if (cut) "${_clipboardFiles.value.size} items ready to move" else "${_clipboardFiles.value.size} items copied")
    }

    fun pasteClipboard(targetDir: File = _currentDirectory.value) {
        val filesToPaste = _clipboardFiles.value
        val op = _clipboardOp.value ?: return
        if (filesToPaste.isEmpty()) return

        viewModelScope.launch {
            var count = 0
            for (f in filesToPaste) {
                val ok = if (op == ClipboardOp.CUT) {
                    repository.move(f.file, targetDir)
                } else {
                    repository.copy(f.file, targetDir)
                }
                if (ok) count++
            }
            if (op == ClipboardOp.CUT) {
                _clipboardFiles.value = emptyList()
                _clipboardOp.value = null
            }
            showSnackbar(if (op == ClipboardOp.CUT) "Moved $count items" else "Pasted $count items")
            loadCurrentFiles()
        }
    }

    fun duplicateFile(file: File) {
        viewModelScope.launch {
            if (repository.duplicate(file)) {
                showSnackbar("Created duplicate of ${file.name}")
                loadCurrentFiles()
            } else {
                showSnackbar("Failed to duplicate")
            }
        }
    }

    fun deleteFiles(files: List<File>, permanent: Boolean = false) {
        viewModelScope.launch {
            var count = 0
            for (f in files) {
                if (repository.delete(f, moveToTrash = !permanent)) count++
            }
            showSnackbar(if (permanent) "Permanently deleted $count items" else "Moved $count items to Recycle Bin")
            clearSelection()
            loadCurrentFiles()
            refreshStorageVolumes()
        }
    }

    fun createFolder(name: String, parentDir: File = _currentDirectory.value) {
        viewModelScope.launch {
            if (repository.createFolder(parentDir, name)) {
                showSnackbar("Folder created")
                loadCurrentFiles()
            } else {
                showSnackbar("Failed to create folder")
            }
        }
    }

    fun createFile(name: String, parentDir: File = _currentDirectory.value) {
        viewModelScope.launch {
            if (repository.createFile(parentDir, name)) {
                showSnackbar("File created")
                loadCurrentFiles()
            } else {
                showSnackbar("Failed to create file")
            }
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            if (repository.rename(file, newName)) {
                showSnackbar("Renamed to $newName")
                loadCurrentFiles()
            } else {
                showSnackbar("Failed to rename file")
            }
        }
    }

    fun toggleFavorite(file: FileInfo) {
        viewModelScope.launch {
            val isFav = repository.favoriteDao.isFavorite(file.path)
            if (isFav) {
                repository.favoriteDao.removeFavorite(file.path)
                showSnackbar("Removed from Favorites")
            } else {
                repository.favoriteDao.addFavorite(
                    FavoriteEntity(
                        path = file.path,
                        name = file.name,
                        isDirectory = file.isDirectory
                    )
                )
                showSnackbar("Added to Favorites")
            }
        }
    }

    fun restoreTrash(trash: TrashEntity) {
        viewModelScope.launch {
            if (repository.restoreTrash(trash)) {
                showSnackbar("Restored ${trash.fileName}")
            } else {
                showSnackbar("Failed to restore file")
            }
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            if (repository.emptyTrash()) {
                showSnackbar("Recycle Bin emptied")
                refreshStorageVolumes()
            }
        }
    }

    // Dual Pane Operations
    fun setActivePane(pane: ActivePane) {
        _activePane.value = pane
    }

    fun setDualLeftDirectory(dir: File) {
        _dualLeftDirectory.value = dir
        _dualLeftSelected.value = emptySet()
        loadDualFiles(ActivePane.LEFT)
    }

    fun setDualRightDirectory(dir: File) {
        _dualRightDirectory.value = dir
        _dualRightSelected.value = emptySet()
        loadDualFiles(ActivePane.RIGHT)
    }

    fun loadDualFiles(pane: ActivePane) {
        viewModelScope.launch {
            val dir = if (pane == ActivePane.LEFT) _dualLeftDirectory.value else _dualRightDirectory.value
            val list = repository.getFiles(dir, FileSortOption.NAME_ASC, _showHiddenFiles.value)
            if (pane == ActivePane.LEFT) {
                _dualLeftFiles.value = list
            } else {
                _dualRightFiles.value = list
            }
        }
    }

    fun toggleDualSelection(pane: ActivePane, file: FileInfo) {
        val target = if (pane == ActivePane.LEFT) _dualLeftSelected else _dualRightSelected
        val set = target.value.toMutableSet()
        if (set.contains(file)) set.remove(file) else set.add(file)
        target.value = set
    }

    fun transferBetweenPanes(fromLeft: Boolean, isMove: Boolean) {
        val sourceFiles = if (fromLeft) _dualLeftSelected.value.toList() else _dualRightSelected.value.toList()
        val targetDir = if (fromLeft) _dualRightDirectory.value else _dualLeftDirectory.value
        if (sourceFiles.isEmpty()) {
            showSnackbar("No items selected to transfer")
            return
        }
        viewModelScope.launch {
            var count = 0
            for (f in sourceFiles) {
                val ok = if (isMove) repository.move(f.file, targetDir) else repository.copy(f.file, targetDir)
                if (ok) count++
            }
            val op = if (isMove) "Moved" else "Copied"
            showSnackbar("$op $count items to ${targetDir.name}")
            if (fromLeft) _dualLeftSelected.value = emptySet() else _dualRightSelected.value = emptySet()
            loadDualFiles(ActivePane.LEFT)
            loadDualFiles(ActivePane.RIGHT)
        }
    }

    // Storage Analyzer
    fun runStorageAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = repository.runStorageAnalysis()
            _isAnalyzing.value = false
            refreshStorageVolumes()
        }
    }

    fun setAnalysisSubTab(tab: StorageSubTab) {
        _analysisSubTab.value = tab
    }

    fun cleanJunkFiles() {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            val reclaimed = repository.cleanJunkFiles(result.junkFiles)
            showSnackbar("Cleaned ${FileInfo.formatFileSize(reclaimed)} of junk files!")
            runStorageAnalysis()
        }
    }

    fun deleteEmptyFolders() {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            val count = repository.deleteEmptyFolders(result.emptyFolders)
            showSnackbar("Deleted $count empty folders")
            runStorageAnalysis()
        }
    }

    fun deleteDuplicateCopy(file: File) {
        viewModelScope.launch {
            if (file.delete()) {
                showSnackbar("Removed duplicate copy")
                runStorageAnalysis()
            }
        }
    }

    // APK Manager
    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = repository.getInstalledApps()
            _isLoadingApps.value = false
        }
    }

    fun setAppSearchQuery(q: String) {
        _appSearchQuery.value = q
    }

    fun toggleFilterSystemApps() {
        _filterSystemApps.value = !_filterSystemApps.value
    }

    fun extractApk(app: InstalledAppInfo) {
        viewModelScope.launch {
            val extracted = repository.extractApk(app)
            if (extracted != null) {
                showSnackbar("Extracted to: Documents/NovaFiles/Backups/${extracted.name}")
            } else {
                showSnackbar("Failed to extract APK")
            }
        }
    }

    // Archive Management
    fun createZipArchive(files: List<File>, targetDir: File, zipName: String, compressionLevel: Int = ZipOutputStream.DEFLATED) {
        viewModelScope.launch {
            val destFile = File(targetDir, if (zipName.endsWith(".zip")) zipName else "$zipName.zip")
            if (ArchiveManager.createZip(files, destFile, compressionLevel)) {
                showSnackbar("Created archive ${destFile.name}")
                loadCurrentFiles()
            } else {
                showSnackbar("Failed to create archive")
            }
        }
    }

    fun extractArchive(zipFile: File, targetDir: File = zipFile.parentFile ?: _currentDirectory.value) {
        viewModelScope.launch {
            showSnackbar("Extracting archive...")
            if (ArchiveManager.extractZip(zipFile, targetDir)) {
                showSnackbar("Extracted archive to ${targetDir.name}")
                loadCurrentFiles()
            } else {
                showSnackbar("Extraction failed")
            }
        }
    }

    // Audio Playback
    private fun startAudioPlayback(file: File) {
        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                _audioDuration.value = duration
                _isAudioPlaying.value = true
                setOnCompletionListener {
                    _isAudioPlaying.value = false
                    _audioProgress.value = 0f
                }
            }
            _previewAudio.value = file
            startAudioProgressTracker()
        } catch (e: Exception) {
            showSnackbar("Cannot play audio: ${e.message}")
        }
    }

    private fun startAudioProgressTracker() {
        viewModelScope.launch {
            while (_previewAudio.value != null && mediaPlayer != null) {
                val mp = mediaPlayer ?: break
                if (mp.isPlaying) {
                    val current = mp.currentPosition
                    val total = mp.duration
                    if (total > 0) {
                        _audioProgress.value = current.toFloat() / total.toFloat()
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun toggleAudioPlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _isAudioPlaying.value = false
        } else {
            mp.start()
            _isAudioPlaying.value = true
        }
    }

    fun seekAudio(progressFraction: Float) {
        val mp = mediaPlayer ?: return
        val pos = (progressFraction * mp.duration).toInt()
        mp.seekTo(pos)
        _audioProgress.value = progressFraction
    }

    fun stopAudioPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _previewAudio.value = null
        _isAudioPlaying.value = false
        _audioProgress.value = 0f
    }

    // Dialogs & Modals
    fun showDialog(dialog: DialogType) {
        _activeDialog.value = dialog
    }

    fun dismissDialog() {
        _activeDialog.value = null
    }

    fun closePreview() {
        _previewImage.value = null
        _previewVideo.value = null
        _previewDoc.value = null
        _previewArchive.value = null
        stopAudioPlayback()
    }

    // Vault
    fun setVaultPin(pin: String) {
        prefs.edit().putString("vault_pin", pin).apply()
        _isVaultUnlocked.value = true
        showSnackbar("Vault PIN set successfully")
        loadVaultFiles()
    }

    fun unlockVault(pin: String): Boolean {
        val saved = prefs.getString("vault_pin", null)
        return if (saved == pin) {
            _isVaultUnlocked.value = true
            loadVaultFiles()
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _vaultFiles.value = emptyList()
    }

    private val vaultDir: File by lazy {
        val dir = File(getApplication<Application>().filesDir, ".secure_vault")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun loadVaultFiles() {
        viewModelScope.launch {
            val list = vaultDir.listFiles()?.map { FileInfo(it) } ?: emptyList()
            _vaultFiles.value = list
        }
    }

    fun moveToVault(file: File) {
        viewModelScope.launch {
            val dest = File(vaultDir, file.name)
            if (file.renameTo(dest)) {
                showSnackbar("Moved ${file.name} to Secure Vault")
                loadCurrentFiles()
                loadVaultFiles()
            } else {
                showSnackbar("Failed to move file to Vault")
            }
        }
    }

    fun restoreFromVault(file: File, targetDir: File = Environment.getExternalStorageDirectory()) {
        viewModelScope.launch {
            val dest = File(targetDir, file.name)
            if (file.renameTo(dest)) {
                showSnackbar("Restored ${file.name} from Vault")
                loadVaultFiles()
                loadCurrentFiles()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
