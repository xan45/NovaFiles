package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        RecentEntity::class,
        TrashEntity::class,
        ScanHistoryEntity::class,
        CloudCacheEntity::class,
        FileTagEntity::class,
        FolderCustomizationEntity::class,
        SyncTaskEntity::class,
        ActivityLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NovaFilesDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun trashDao(): TrashDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun cloudCacheDao(): CloudCacheDao
    abstract fun fileTagDao(): FileTagDao
    abstract fun folderCustomizationDao(): FolderCustomizationDao
    abstract fun syncTaskDao(): SyncTaskDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: NovaFilesDatabase? = null

        fun getInstance(context: Context): NovaFilesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaFilesDatabase::class.java,
                    "nova_files.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
