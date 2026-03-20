package com.example.myapplication2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myapplication2.data.local.entity.StoredCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM stored_cards WHERE isPinned = 1 ORDER BY orderIndex ASC, dateMillis DESC")
    fun observePinnedCards(): Flow<List<StoredCardEntity>>

    @Query("SELECT * FROM stored_cards WHERE isSearchHistory = 1 ORDER BY dateMillis DESC")
    fun observeSearchHistory(): Flow<List<StoredCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StoredCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<StoredCardEntity>)

    @Update
    suspend fun update(entity: StoredCardEntity)

    @Query("SELECT * FROM stored_cards WHERE id = :cardId LIMIT 1")
    suspend fun getById(cardId: String): StoredCardEntity?

    @Query("SELECT * FROM stored_cards WHERE id = :cardId LIMIT 1")
    fun observeById(cardId: String): Flow<StoredCardEntity?>

    @Query("UPDATE stored_cards SET isPinned = :pinned WHERE id = :cardId")
    suspend fun setPinned(cardId: String, pinned: Boolean)

    @Query("SELECT * FROM stored_cards WHERE isPinned = 1 ORDER BY orderIndex ASC, dateMillis DESC")
    suspend fun getPinnedCardsSnapshot(): List<StoredCardEntity>

    @Transaction
    suspend fun updatePinnedOrder(cardIds: List<String>) {
        cardIds.forEachIndexed { index, id ->
            updateOrderIndex(id, index)
        }
    }

    @Query("UPDATE stored_cards SET orderIndex = :index WHERE id = :cardId")
    suspend fun updateOrderIndex(cardId: String, index: Int)
}
