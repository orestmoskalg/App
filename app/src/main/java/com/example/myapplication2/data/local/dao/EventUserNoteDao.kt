package com.example.myapplication2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication2.data.local.entity.EventUserNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventUserNoteDao {
    @Query("SELECT * FROM event_user_notes ORDER BY pinned DESC, updatedAtMillis DESC")
    fun observeAll(): Flow<List<EventUserNoteEntity>>

    @Query("SELECT * FROM event_user_notes WHERE eventCardId = :cardId ORDER BY updatedAtMillis DESC")
    fun observeForCard(cardId: String): Flow<List<EventUserNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EventUserNoteEntity)

    @Query("DELETE FROM event_user_notes WHERE id = :id")
    suspend fun deleteById(id: String)
}
