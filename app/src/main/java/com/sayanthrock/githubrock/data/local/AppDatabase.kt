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
    val createdAt: Long = System.currentTimeMillis(),
    val packageName: String? = null,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
    val permissions: String? = null,
    val certificateSha256: String? = null,
    val signatureSchemes: String? = null,
    val architectures: String? = null,
    val securityRisk: String? = null,
    val securityReasons: String? = null
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

    @Query("SELECT * FROM downloads WHERE packageName = :packageName AND status = 'completed' ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestCompletedForPackage(packageName: String): DownloadEntity?

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloaded, totalBytes = :total, localPath = :path, sha256 = :sha WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, downloaded: Long, total: Long, path: String?, sha: String?)

    @Query("UPDATE downloads SET packageName = :packageName, versionCode = :versionCode, versionName = :versionName, minSdk = :minSdk, targetSdk = :targetSdk, permissions = :permissions, certificateSha256 = :certificateSha256, signatureSchemes = :signatureSchemes, architectures = :architectures, securityRisk = :securityRisk, securityReasons = :securityReasons WHERE id = :id")
    suspend fun updateSecurity(
        id: Long,
        packageName: String,
        versionCode: Long,
        versionName: String?,
        minSdk: Int,
        targetSdk: Int,
        permissions: String,
        certificateSha256: String?,
        signatureSchemes: String,
        architectures: String,
        securityRisk: String,
        securityReasons: String
    )

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [RepositoryEntity::class, DownloadEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao
    abstract fun downloadDao(): DownloadDao
}
