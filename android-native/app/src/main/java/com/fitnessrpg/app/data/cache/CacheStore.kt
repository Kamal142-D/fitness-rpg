package com.fitnessrpg.app.data.cache

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

/** One persisted screen snapshot: a JSON blob keyed by cache key. */
@Entity(tableName = "screen_cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM screen_cache WHERE key = :key LIMIT 1")
    suspend fun get(key: String): CacheEntity?

    @Upsert
    suspend fun upsert(entity: CacheEntity)

    @Query("DELETE FROM screen_cache WHERE key LIKE :pattern")
    suspend fun deleteLike(pattern: String)

    @Query("DELETE FROM screen_cache")
    suspend fun clearAll()
}

@Database(entities = [CacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
