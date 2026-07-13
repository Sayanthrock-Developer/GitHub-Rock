package com.sayanthrock.githubrock.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
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
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM recent_repositories ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<RepositoryEntity>>

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

    /**
     * Updates the status, progress, local path, and checksum of a download.
     *
     * @param id The identifier of the download to update.
     * @param downloaded The number of bytes downloaded.
     * @param total The total number of bytes.
     * @param path The local file path, or `null` if unavailable.
     * @param sha The SHA-256 checksum, or `null` if unavailable.
     */
    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloaded, totalBytes = :total, localPath = :path, sha256 = :sha WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, downloaded: Long, total: Long, path: String?, sha: String?)

    /**
     * Updates the status of a download.
     *
     * @param id The identifier of the download to update.
     * @param status The new status value.
     */
    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    /**
     * Deletes a download record by its identifier.
     *
     * @param id The identifier of the download record to delete.
     */
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [RepositoryEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao
    abstract fun downloadDao(): DownloadDao
}

