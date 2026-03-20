package com.example.myapplication2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication2.data.local.dao.CardDao
import com.example.myapplication2.data.local.dao.CacheDao
import com.example.myapplication2.data.local.dao.EventUserNoteDao
import com.example.myapplication2.data.local.dao.GenerationLogDao
import com.example.myapplication2.data.local.entity.CardEntity
import com.example.myapplication2.data.local.entity.CacheEntity
import com.example.myapplication2.data.local.entity.EventUserNoteEntity
import com.example.myapplication2.data.local.entity.GenerationLogEntity

@Database(
    entities = [CardEntity::class, CacheEntity::class, GenerationLogEntity::class, EventUserNoteEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun cacheDao(): CacheDao
    abstract fun generationLogDao(): GenerationLogDao
    abstract fun eventUserNoteDao(): EventUserNoteDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS generation_log")
            }
        }

        /** Recreate generation_log after v4 (removed in 3→4). */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS generation_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        weekKey TEXT NOT NULL,
                        generationType TEXT NOT NULL,
                        timestampMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "regulation_db",
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
