package com.example.myapplication2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication2.data.local.entity.CachedPayloadEntity

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_payloads WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CachedPayloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedPayloadEntity)

    @Query("DELETE FROM cached_payloads")
    suspend fun clearAll()
}
