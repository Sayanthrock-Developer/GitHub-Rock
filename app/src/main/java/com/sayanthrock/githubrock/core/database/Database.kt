package com.sayanthrock.githubrock.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity(tableName = "recent_repositories")
data class RecentRepositoryEntity(
    @PrimaryKey val id: Long,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val language: String?,
    val stars: Int,
    val lastOpenedAt: Long
)

@Dao
interface RecentRepositoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentRepositoryEntity)

    @Query("SELECT * FROM recent_repositories ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 12): Flow<List<RecentRepositoryEntity>>

    @Query("DELETE FROM recent_repositories WHERE id NOT IN (SELECT id FROM recent_repositories ORDER BY lastOpenedAt DESC LIMIT :keep)")
    suspend fun trim(keep: Int = 30)
}

@Database(
    entities = [RecentRepositoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GitHubRockDatabase : RoomDatabase() {
    abstract fun recentRepositoryDao(): RecentRepositoryDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): GitHubRockDatabase =
        Room.databaseBuilder(
            context,
            GitHubRockDatabase::class.java,
            "github_rock.db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun recentRepositoryDao(database: GitHubRockDatabase): RecentRepositoryDao =
        database.recentRepositoryDao()
}
