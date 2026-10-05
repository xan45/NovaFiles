package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.FileInfo
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.DarkModeOption
import com.example.ui.components.ArchiveViewerModal
import com.example.ui.components.AudioPlayerModal
import com.example.ui.components.CreateFileDialog
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.CreateZipDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DocumentViewerModal
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FloatingNavigationBar
import com.example.ui.components.ImageViewerModal
import com.example.ui.components.PinDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.ApkManagerScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.CloudStorageScreen
import com.example.ui.screens.DualPaneScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageAnalyzerScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.NovaFilesTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DialogType
import com.example.ui.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val currentThemeMode by viewModel.themeMode.collectAsState()
            val currentDarkMode by viewModel.darkModeOption.collectAsState()

            NovaFilesTheme(
                themeMode = currentThemeMode,
                darkModeOption = currentDarkMode
            ) {
                MainAppScreen(
                    viewModel = viewModel,
                    currentThemeMode = currentThemeMode,
                    currentDarkMode = currentDarkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    currentThemeMode: AppThemeMode,
    currentDarkMode: DarkModeOption
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val currentScreen by viewModel.currentScreen.collectAsState()
    val storageVolumes by viewModel.storageVolumes.collectAsState()
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoadingFiles by viewModel.isLoadingFiles.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val showHidden by viewModel.showHiddenFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeCategory by viewModel.activeCategory.collectAsState()

    // Dual Pane states
    val dualLeftDir by viewModel.dualLeftDirectory.collectAsState()
    val dualLeftFiles by viewModel.dualLeftFiles.collectAsState()
    val dualLeftSelected by viewModel.dualLeftSelected.collectAsState()
    val dualRightDir by viewModel.dualRightDirectory.collectAsState()
    val dualRightFiles by viewModel.dualRightFiles.collectAsState()
    val dualRightSelected by viewModel.dualRightSelected.collectAsState()
    val activePane by viewModel.activePane.collectAsState()

    // Storage Analyzer states
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val analysisSubTab by viewModel.analysisSubTab.collectAsState()
    val historyScans by viewModel.scanHistory.collectAsState()

    // Tools & Database states
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val appSearchQuery by viewModel.appSearchQuery.collectAsState()
    val filterSystemApps by viewModel.filterSystemApps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val trashFiles by viewModel.trashFiles.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultFiles by viewModel.vaultFiles.collectAsState()

    // Active Previews & Modals
    val previewImage by viewModel.previewImage.collectAsState()
    val imageExif by viewModel.imageExif.collectAsState()
    val previewVideo by viewModel.previewVideo.collectAsState()
    val previewAudio by viewModel.previewAudio.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val audioProgress by viewModel.audioProgress.collectAsState()
    val audioDuration by viewModel.audioDuration.collectAsState()
    val previewDoc by viewModel.previewDoc.collectAsState()
    val previewArchive by viewModel.previewArchive.collectAsState()
    val archiveEntries by viewModel.archiveEntries.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    // Storage Permission handling
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
                viewModel.refreshStorageVolumes()
                viewModel.loadCurrentFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission launcher for pre-Android 11
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.values.all { it }
        viewModel.refreshStorageVolumes()
        viewModel.loadCurrentFiles()
    }

    // Handle Snackbars
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back handling
    BackHandler(enabled = currentScreen != AppScreen.HOME || activeCategory != null || selectedFiles.isNotEmpty()) {
        when {
            selectedFiles.isNotEmpty() -> viewModel.clearSelection()
            activeCategory != null -> viewModel.setCategory(null)
            currentScreen == AppScreen.BROWSER -> {
                if (!viewModel.navigateUp()) {
                    viewModel.navigateTo(AppScreen.HOME)
                }
            }
            currentScreen == AppScreen.ONBOARDING -> {
                viewModel.completeOnboarding()
            }
            else -> viewModel.navigateTo(AppScreen.HOME)
        }
    }

    if (currentScreen == AppScreen.ONBOARDING) {
        OnboardingScreen(
            onFinish = { viewModel.completeOnboarding() }
        )
        return
    }

    val cloudAccounts by viewModel.cloudAccounts.collectAsState()
    val selectedCloudProvider by viewModel.selectedCloudProvider.collectAsState()
    val cloudFiles by viewModel.cloudFiles.collectAsState()
    val cloudFolderId by viewModel.cloudFolderId.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentScreen != AppScreen.HOME && currentScreen != AppScreen.BROWSER) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                AppScreen.DUAL_PANE -> "Dual Pane Explorer"
                                AppScreen.STORAGE_ANALYZER -> "Storage Analyzer"
                                AppScreen.APK_MANAGER -> "APK & App Manager"
                                AppScreen.RECYCLE_BIN -> "Recycle Bin"
                                AppScreen.FAVORITES -> "Favorites"
                                AppScreen.VAULT -> "Secure Vault"
                                AppScreen.CLOUD_STORAGE -> "Cloud Storage"
                                AppScreen.SETTINGS -> "Settings"
                                else -> "NovaFiles"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            FloatingNavigationBar(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Storage permission banner if not granted
                if (!hasStoragePermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All Files Access Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "NovaFiles requires storage access permissions to browse, organize, duplicate, and clean files across internal and external storage.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                            context.startActivity(intent)
                                        }
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // Primary Screen Router
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            storageVolumes = storageVolumes,
                            recentFiles = recentFiles,
                            analysisResult = analysisResult,
                            onNavigateToCategory = { viewModel.setCategory(it) },
                            onNavigateToScreen = { viewModel.navigateTo(it) },
                            onOpenFile = { file -> viewModel.openFile(FileInfo(file), context) }
                        )

                        AppScreen.BROWSER -> BrowserScreen(
                            currentDirectory = currentDirectory,
                            files = files,
                            selectedFiles = selectedFiles,
                            isLoading = isLoadingFiles,
                            viewMode = viewMode,
                            sortOption = sortOption,
                            showHidden = showHidden,
                            searchQuery = searchQuery,
                            activeCategory = activeCategory,
                            onNavigateToDir = { viewModel.navigateToDirectory(it) },
                            onNavigateUp = { viewModel.navigateUp() },
                            onOpenFile = { viewModel.openFile(it, context) },
                            onToggleSelect = { viewModel.toggleFileSelection(it) },
                            onSelectAll = { viewModel.selectAllFiles() },
                            onClearSelection = { viewModel.clearSelection() },
                            onToggleViewMode = { viewModel.toggleViewMode() },
                            onSetSortOption = { viewModel.setSortOption(it) },
                            onToggleShowHidden = { viewModel.toggleShowHidden() },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onClearCategory = { viewModel.setCategory(null) },
                            onRename = { viewModel.showDialog(DialogType.Rename(it)) },
                            onDelete = { viewModel.showDialog(DialogType.DeleteConfirm(it, permanent = false)) },
                            onCopy = { viewModel.copySelectedToClipboard(cut = false) },
                            onCut = { viewModel.copySelectedToClipboard(cut = true) },
                            onDuplicate = { viewModel.duplicateFile(it) },
                            onShare = { viewModel.shareFiles(it, context) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onCompress = { viewModel.showDialog(DialogType.CreateZip(it, currentDirectory)) },
                            onDetails = { viewModel.showDialog(DialogType.FileDetails(it)) },
                            onCreateFolder = { viewModel.showDialog(DialogType.CreateFolder(currentDirectory)) },
                            onCreateFile = { viewModel.showDialog(DialogType.CreateFile(currentDirectory)) }
                        )

                        AppScreen.DUAL_PANE -> DualPaneScreen(
                            leftDirectory = dualLeftDir,
                            leftFiles = dualLeftFiles,
                            leftSelected = dualLeftSelected,
                            rightDirectory = dualRightDir,
                            rightFiles = dualRightFiles,
                            rightSelected = dualRightSelected,
                            activePane = activePane,
                            onSetActivePane = { viewModel.setActivePane(it) },
                            onNavigateLeft = { viewModel.setDualLeftDirectory(it) },
                            onNavigateRight = { viewModel.setDualRightDirectory(it) },
                            onToggleLeftSelect = { viewModel.toggleDualSelection(com.example.ui.viewmodel.ActivePane.LEFT, it) },
                            onToggleRightSelect = { viewModel.toggleDualSelection(com.example.ui.viewmodel.ActivePane.RIGHT, it) },
                            onTransfer = { fromLeft, isMove -> viewModel.transferBetweenPanes(fromLeft, isMove) }
                        )

                        AppScreen.STORAGE_ANALYZER -> StorageAnalyzerScreen(
                            isAnalyzing = isAnalyzing,
                            progress = analysisProgress,
                            analysisResult = analysisResult,
                            activeSubTab = analysisSubTab,
                            historyScans = historyScans,
                            onSelectSubTab = { viewModel.setAnalysisSubTab(it) },
                            onRunAnalysis = { viewModel.runStorageAnalysis() },
                            onCleanJunk = { viewModel.cleanJunkFiles() },
                            onDeleteEmptyFolders = { viewModel.deleteEmptyFolders() },
                            onDeleteDuplicateCopy = { viewModel.deleteDuplicateCopy(it) },
                            onDeleteLargeFile = { viewModel.deleteFiles(listOf(it), permanent = false) }
                        )

                        AppScreen.APK_MANAGER -> ApkManagerScreen(
                            installedApps = installedApps,
                            isLoading = isLoadingApps,
                            searchQuery = appSearchQuery,
                            filterSystemApps = filterSystemApps,
                            onSearchChange = { viewModel.setAppSearchQuery(it) },
                            onToggleFilterSystem = { viewModel.toggleFilterSystemApps() },
                            onExtractApk = { viewModel.extractApk(it) },
                            onRefresh = { viewModel.loadInstalledApps() }
                        )

                        AppScreen.RECYCLE_BIN -> RecycleBinScreen(
                            trashFiles = trashFiles,
                            onRestore = { viewModel.restoreTrash(it) },
                            onEmptyBin = { viewModel.emptyRecycleBin() }
                        )

                        AppScreen.FAVORITES -> FavoritesScreen(
                            favorites = favorites,
                            onOpenFile = { file -> viewModel.openFile(FileInfo(file), context) },
                            onRemoveFavorite = { viewModel.toggleFavorite(it) }
                        )

                        AppScreen.VAULT -> VaultScreen(
                            isUnlocked = isVaultUnlocked,
                            hasPin = viewModel.hasVaultPin,
                            vaultFiles = vaultFiles,
                            onPromptSetPin = { viewModel.showDialog(DialogType.SetPin(isChange = false)) },
                            onPromptUnlock = { viewModel.showDialog(DialogType.UnlockVault) },
                            onLockVault = { viewModel.lockVault() },
                            onRestoreFile = { viewModel.restoreFromVault(it) },
                            onOpenFile = { viewModel.openFile(it, context) }
                        )

                        AppScreen.CLOUD_STORAGE -> CloudStorageScreen(
                            accounts = cloudAccounts,
                            selectedProvider = selectedCloudProvider,
                            cloudFiles = cloudFiles,
                            currentFolderId = cloudFolderId,
                            onSelectProvider = { viewModel.selectCloudProvider(it) },
                            onConnectAccount = { viewModel.connectCloudAccount(it) },
                            onDisconnectAccount = { viewModel.disconnectCloudAccount(it) },
                            onNavigateCloudFolder = { viewModel.navigateCloudFolder(it) },
                            onNavigateCloudUp = { viewModel.navigateCloudUp() },
                            onDownloadCloudFile = { viewModel.downloadCloudFile(it) },
                            onOpenFile = { item -> viewModel.openCloudFile(item, context) },
                            onUploadFile = { viewModel.showSnackbar("Select local file to upload") },
                            onClearCache = { viewModel.clearCloudCache() }
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            currentThemeMode = currentThemeMode,
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            currentDarkMode = currentDarkMode,
                            onDarkModeChange = { viewModel.setDarkModeOption(it) },
                            showHidden = showHidden,
                            onToggleShowHidden = { viewModel.toggleShowHidden() },
                            onChangePin = { viewModel.showDialog(DialogType.SetPin(isChange = true)) },
                            onClearRecents = {
                                viewModel.showSnackbar("Recent history cleared")
                            },
                            onRevisitOnboarding = { viewModel.navigateTo(AppScreen.ONBOARDING) },
                            onOpenCloudStorage = { viewModel.navigateTo(AppScreen.CLOUD_STORAGE) }
                        )
                        AppScreen.ONBOARDING -> {}
                    }
                }
            }

            // Audio Player Floating Bottom Card
            if (previewAudio != null) {
                AudioPlayerModal(
                    audioFile = previewAudio!!,
                    isPlaying = isAudioPlaying,
                    progress = audioProgress,
                    durationMs = audioDuration,
                    onTogglePlay = { viewModel.toggleAudioPlayPause() },
                    onSeek = { viewModel.seekAudio(it) },
                    onClose = { viewModel.stopAudioPlayback() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Modal Overlays
    previewImage?.let { file ->
        ImageViewerModal(
            imageFile = file,
            exifData = imageExif,
            onClose = { viewModel.closePreview() }
        )
    }

    previewVideo?.let { file ->
        VideoPlayerModal(
            videoFile = file,
            onClose = { viewModel.closePreview() }
        )
    }

    previewDoc?.let { (file, content) ->
        DocumentViewerModal(
            file = file,
            content = content,
            onClose = { viewModel.closePreview() }
        )
    }

    previewArchive?.let { file ->
        ArchiveViewerModal(
            archiveFile = file,
            entries = archiveEntries,
            onExtractAll = {
                viewModel.extractArchive(file)
                viewModel.closePreview()
            },
            onClose = { viewModel.closePreview() }
        )
    }

    // Dialogs
    when (val dialog = activeDialog) {
        is DialogType.CreateFolder -> {
            CreateFolderDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name ->
                    viewModel.createFolder(name, dialog.parentDir)
                    viewModel.dismissDialog()
                }
            )
        }
        is DialogType.CreateFile -> {
            CreateFileDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, content ->
                    viewModel.createFile(name, dialog.parentDir)
                    viewModel.dismissDialog()
                }
            )
        }
        is DialogType.Rename -> {
            RenameDialog(
                file = dialog.file,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName ->
                    viewModel.renameFile(dialog.file, newName)
                    viewModel.dismissDialog()
                }
            )
        }
        is DialogType.CreateZip -> {
            CreateZipDialog(
                sourceFiles = dialog.sourceFiles,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { zipName, compressionLevel ->
                    viewModel.createZipArchive(dialog.sourceFiles, dialog.parentDir, zipName, compressionLevel)
                    viewModel.dismissDialog()
                }
            )
        }
        is DialogType.FileDetails -> {
            FileDetailsDialog(
                file = dialog.file,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is DialogType.DeleteConfirm -> {
            DeleteConfirmDialog(
                files = dialog.files,
                permanent = dialog.permanent,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = {
                    viewModel.deleteFiles(dialog.files, permanent = dialog.permanent)
                    viewModel.dismissDialog()
                }
            )
        }
        is DialogType.SetPin -> {
            PinDialog(
                title = if (dialog.isChange) "Change Vault PIN" else "Set Security PIN",
                buttonText = "Save PIN",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { pin ->
                    viewModel.setVaultPin(pin)
                    viewModel.dismissDialog()
                    true
                }
            )
        }
        is DialogType.UnlockVault -> {
            PinDialog(
                title = "Unlock Secure Vault",
                buttonText = "Unlock",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { pin ->
                    val success = viewModel.unlockVault(pin)
                    if (success) {
                        viewModel.dismissDialog()
                    }
                    success
                }
            )
        }
        null -> {}
    }
}
