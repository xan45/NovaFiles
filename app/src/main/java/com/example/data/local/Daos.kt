package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE path = :path)")
    fun isFavoriteFlow(path: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun removeFavorite(path: String)
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_files ORDER BY accessedAt DESC LIMIT 50")
    fun getRecentFiles(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentEntity)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun deleteRecentByPath(path: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearAllRecents()
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllTrash(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(trash: TrashEntity): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteTrashById(id: Long)

    @Query("SELECT * FROM recycle_bin WHERE id = :id")
    suspend fun getTrashById(id: Long): TrashEntity?

    @Query("DELETE FROM recycle_bin")
    suspend fun clearTrash()
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 20")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestScan(): ScanHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity): Long
}

@Dao
interface CloudCacheDao {
    @Query("SELECT * FROM cloud_cache WHERE provider = :provider AND ((:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId) ORDER BY isDirectory DESC, name ASC")
    fun getCachedFilesFlow(provider: String, parentId: String?): Flow<List<CloudCacheEntity>>

    @Query("SELECT * FROM cloud_cache WHERE provider = :provider AND ((:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId) ORDER BY isDirectory DESC, name ASC")
    suspend fun getCachedFiles(provider: String, parentId: String?): List<CloudCacheEntity>

    @Query("SELECT * FROM cloud_cache WHERE cachedFilePath IS NOT NULL ORDER BY lastAccessedAt DESC")
    fun getAllOfflineFilesFlow(): Flow<List<CloudCacheEntity>>

    @Query("SELECT * FROM cloud_cache WHERE id = :id")
    suspend fun getById(id: String): CloudCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CloudCacheEntity>)

    @Query("UPDATE cloud_cache SET cachedFilePath = :localPath, lastAccessedAt = :time WHERE id = :id")
    suspend fun updateCachedPath(id: String, localPath: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM cloud_cache WHERE provider = :provider")
    suspend fun clearProvider(provider: String)

    @Query("DELETE FROM cloud_cache")
    suspend fun clearAll()
}

@Dao
interface FileTagDao {
    @Query("SELECT * FROM file_tags WHERE path = :path")
    fun getTagsForPathFlow(path: String): Flow<List<FileTagEntity>>

    @Query("SELECT * FROM file_tags WHERE path = :path")
    suspend fun getTagsForPath(path: String): List<FileTagEntity>

    @Query("SELECT * FROM file_tags")
    fun getAllTagsFlow(): Flow<List<FileTagEntity>>

    @Query("SELECT DISTINCT tag FROM file_tags ORDER BY tag ASC")
    fun getAllDistinctTags(): Flow<List<String>>

    @Query("SELECT * FROM file_tags WHERE tag = :tag")
    suspend fun getFilesWithTag(tag: String): List<FileTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: FileTagEntity)

    @Query("DELETE FROM file_tags WHERE path = :path AND tag = :tag")
    suspend fun deleteTag(path: String, tag: String)

    @Query("DELETE FROM file_tags WHERE path = :path")
    suspend fun deleteAllTagsForPath(path: String)
}

@Dao
interface FolderCustomizationDao {
    @Query("SELECT * FROM folder_customization WHERE path = :path")
    fun getCustomizationFlow(path: String): Flow<FolderCustomizationEntity?>

    @Query("SELECT * FROM folder_customization WHERE path = :path")
    suspend fun getCustomization(path: String): FolderCustomizationEntity?

    @Query("SELECT * FROM folder_customization")
    fun getAllCustomizationsFlow(): Flow<List<FolderCustomizationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCustomization(entity: FolderCustomizationEntity)

    @Query("DELETE FROM folder_customization WHERE path = :path")
    suspend fun removeCustomization(path: String)
}

@Dao
interface SyncTaskDao {
    @Query("SELECT * FROM sync_tasks ORDER BY createdAt DESC")
    fun getAllSyncTasksFlow(): Flow<List<SyncTaskEntity>>

    @Query("SELECT * FROM sync_tasks WHERE isEnabled = 1")
    suspend fun getEnabledTasks(): List<SyncTaskEntity>

    @Query("SELECT * FROM sync_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): SyncTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: SyncTaskEntity): Long

    @Update
    suspend fun updateTask(task: SyncTaskEntity)

    @Query("UPDATE sync_tasks SET lastSyncTime = :time, lastSyncStatus = :status, lastSyncMessage = :msg WHERE id = :id")
    suspend fun updateStatus(id: Long, time: Long, status: String, msg: String)

    @Query("DELETE FROM sync_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getActivityLogsFlow(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE action = :action ORDER BY timestamp DESC LIMIT 50")
    fun getLogsByActionFlow(action: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}
