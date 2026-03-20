package com.example.myapplication2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication2.data.local.dao.CardDao
import com.example.myapplication2.data.local.dao.CacheDao
import com.example.myapplication2.data.local.dao.GenerationLogDao
import com.example.myapplication2.data.local.entity.CardEntity
import com.example.myapplication2.data.local.entity.CacheEntity
import com.example.myapplication2.data.local.entity.GenerationLogEntity

@Database(
    entities = [CardEntity::class, CacheEntity::class, GenerationLogEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun cacheDao(): CacheDao
    abstract fun generationLogDao(): GenerationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "regulation_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
