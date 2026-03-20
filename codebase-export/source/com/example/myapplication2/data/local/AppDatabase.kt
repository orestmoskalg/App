package com.example.myapplication2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication2.data.local.dao.CacheDao
import com.example.myapplication2.data.local.dao.CardDao
import com.example.myapplication2.data.local.entity.CachedPayloadEntity
import com.example.myapplication2.data.local.entity.StoredCardEntity

@Database(
    entities = [StoredCardEntity::class, CachedPayloadEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun cacheDao(): CacheDao
}
