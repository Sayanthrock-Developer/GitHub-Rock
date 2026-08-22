package com.sayanthrock.githubrock.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recent_repositories")
data class RepositoryEntity(
    @PrimaryKey val id: Long,
    val owner: String,
    val name: String,
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val isPrivate: Boolean,
    val updatedAt: String,
    val openedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val sourceUrl: String,
    val localPath: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val sha256: String? = null,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val repositoryOwner: String? = null,
    val repositoryName: String? = null,
    val releaseTag: String? = null,
    val autoUpdate: Boolean = false
)

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val repositoryOwner: String,
    val repositoryName: String,
    val installedVersionCode: Long,
    val installedVersionName: String? = null,
    val trackedReleaseTag: String? = null,
    val autoUpdate: Boolean = true,
    val lastCheckedAt: Long? = null,
    val lastUpdateAvailableAt: Long? = null
)

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM recent_repositories ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<RepositoryEntity>>

    @Query("SELECT * FROM recent_repositories ORDER BY openedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 100): List<RepositoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(repository: RepositoryEntity)

    @Query("DELETE FROM recent_repositories")
    suspend fun clear()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloaded, totalBytes = :total, localPath = :path, sha256 = :sha WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, downloaded: Long, total: Long, path: String?, sha: String?)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ManagedAppDao {
    @Query("SELECT * FROM managed_apps ORDER BY appName COLLATE NOCASE")
    fun observeAll(): Flow<List<ManagedAppEntity>>

    @Query("SELECT * FROM managed_apps WHERE autoUpdate = 1")
    suspend fun autoUpdateApps(): List<ManagedAppEntity>

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun find(packageName: String): ManagedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: ManagedAppEntity)

    @Query("UPDATE managed_apps SET trackedReleaseTag = :tag, lastCheckedAt = :checkedAt, lastUpdateAvailableAt = :availableAt WHERE packageName = :packageName")
    suspend fun recordCheck(packageName: String, tag: String?, checkedAt: Long, availableAt: Long?)

    @Query("UPDATE managed_apps SET autoUpdate = :enabled WHERE packageName = :packageName")
    suspend fun setAutoUpdate(packageName: String, enabled: Boolean)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Database(
    entities = [RepositoryEntity::class, DownloadEntity::class, ManagedAppEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun managedAppDao(): ManagedAppDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN repositoryOwner TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN repositoryName TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN releaseTag TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN autoUpdate INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS managed_apps (" +
                        "packageName TEXT NOT NULL, " +
                        "appName TEXT NOT NULL, " +
                        "repositoryOwner TEXT NOT NULL, " +
                        "repositoryName TEXT NOT NULL, " +
                        "installedVersionCode INTEGER NOT NULL, " +
                        "installedVersionName TEXT, " +
                        "trackedReleaseTag TEXT, " +
                        "autoUpdate INTEGER NOT NULL DEFAULT 1, " +
                        "lastCheckedAt INTEGER, " +
                        "lastUpdateAvailableAt INTEGER, " +
                        "PRIMARY KEY(packageName)" +
                        ")"
                )
            }
        }
    }
}
