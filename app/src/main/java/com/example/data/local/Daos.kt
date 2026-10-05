package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
