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
    val expectedSha256: String? = null,
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
    val securityReasons: String? = null,
    val repositoryFullName: String? = null,
    val releaseName: String? = null,
    val releaseUrl: String? = null,
    val assetId: Long? = null,
    val speedBytesPerSecond: Long = 0,
    val etaSeconds: Long? = null,
    val errorMessage: String? = null,
    val fallbackUrl: String? = null,
    val checksumUrl: String? = null
)

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM recent_repositories ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<RepositoryEntity>>
    @Query("SELECT * FROM recent_repositories ORDER BY openedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 100): List<RepositoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(repository: RepositoryEntity)
    @Query("DELETE FROM recent_repositories") suspend fun clear()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(download: DownloadEntity): Long
    @Query("SELECT * FROM downloads WHERE assetId = :assetId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findByAssetId(assetId: Long): DownloadEntity?
    @Query("SELECT * FROM downloads WHERE sourceUrl = :sourceUrl ORDER BY createdAt DESC LIMIT 1")
    suspend fun findBySourceUrl(sourceUrl: String): DownloadEntity?
    @Query("SELECT * FROM downloads WHERE packageName = :packageName AND status IN ('completed', 'installable') ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestCompletedForPackage(packageName: String): DownloadEntity?
    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloaded, totalBytes = :total, localPath = :path, sha256 = CASE WHEN :status IN ('completed', 'installable') THEN :sha ELSE sha256 END, speedBytesPerSecond = :speed, etaSeconds = :eta, errorMessage = :error WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, downloaded: Long, total: Long, path: String?, sha: String?, speed: Long, eta: Long?, error: String?)
    @Query("UPDATE downloads SET packageName = :packageName, versionCode = :versionCode, versionName = :versionName, minSdk = :minSdk, targetSdk = :targetSdk, permissions = :permissions, certificateSha256 = :certificateSha256, signatureSchemes = :signatureSchemes, architectures = :architectures, securityRisk = :securityRisk, securityReasons = :securityReasons WHERE id = :id")
    suspend fun updateSecurity(id: Long, packageName: String, versionCode: Long, versionName: String?, minSdk: Int, targetSdk: Int, permissions: String, certificateSha256: String?, signatureSchemes: String, architectures: String, securityRisk: String, securityReasons: String)
    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?)
    @Query("DELETE FROM downloads WHERE id = :id") suspend fun delete(id: Long)
}

val MIGRATION_1_4 = object : Migration(1, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE downloads ADD COLUMN expectedSha256 TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN packageName TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN versionCode INTEGER")
        database.execSQL("ALTER TABLE downloads ADD COLUMN versionName TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN minSdk INTEGER")
        database.execSQL("ALTER TABLE downloads ADD COLUMN targetSdk INTEGER")
        database.execSQL("ALTER TABLE downloads ADD COLUMN permissions TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN certificateSha256 TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN signatureSchemes TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN architectures TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN securityRisk TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN securityReasons TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN repositoryFullName TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN releaseName TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN releaseUrl TEXT")
        database.execSQL("ALTER TABLE downloads ADD COLUMN assetId INTEGER")
        database.execSQL("ALTER TABLE downloads ADD COLUMN speedBytesPerSecond INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE downloads ADD COLUMN etaSeconds INTEGER")
        database.execSQL("ALTER TABLE downloads ADD COLUMN errorMessage TEXT")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE downloads ADD COLUMN fallbackUrl TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE downloads ADD COLUMN checksumUrl TEXT")
    }
}

@Database(entities = [RepositoryEntity::class, DownloadEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao
    abstract fun downloadDao(): DownloadDao
}
