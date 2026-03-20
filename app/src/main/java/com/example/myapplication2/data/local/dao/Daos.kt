package com.example.myapplication2.data.local.dao

import androidx.room.*
import com.example.myapplication2.data.local.entity.CardEntity
import com.example.myapplication2.data.local.entity.CacheEntity
import com.example.myapplication2.data.local.entity.GenerationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isPinned = 1 ORDER BY orderIndex ASC")
    fun observePinnedCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE type = 'SEARCH_HISTORY' ORDER BY dateMillis DESC")
    fun observeSearchHistory(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE type = :type ORDER BY dateMillis DESC")
    fun observeCardsByType(type: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun observeCard(id: String): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCard(id: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Query("UPDATE cards SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE cards SET orderIndex = :order WHERE id = :id")
    suspend fun setOrderIndex(id: String, order: Int)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: String)

    @Query("DELETE FROM cards WHERE type = :type")
    suspend fun deleteByType(type: String)

    /** Keeps pinned cards on Home when regenerating Knowledge content. */
    @Query("DELETE FROM cards WHERE type = :type AND isPinned = 0")
    suspend fun deleteUnpinnedByType(type: String)

    @Query("DELETE FROM cards")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cards WHERE type = 'SEARCH_HISTORY'")
    suspend fun countSearchHistory(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE type = 'REGULATORY_EVENT'")
    suspend fun countCalendarEvents(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE isPinned = 1")
    suspend fun countPinned(): Int

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun countAll(): Int

    @Query("SELECT * FROM cards WHERE type = 'SEARCH_HISTORY' ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecentSearchHistory(limit: Int): Flow<List<CardEntity>>
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CacheEntity)

    @Query("DELETE FROM cache")
    suspend fun clearAll()
}

@Dao
interface GenerationLogDao {
    @Query("SELECT * FROM generation_log WHERE weekKey = :weekKey")
    suspend fun getByWeek(weekKey: String): List<GenerationLogEntity>

    @Query("SELECT COUNT(*) FROM generation_log WHERE weekKey = :weekKey AND generationType = :type")
    suspend fun countByWeekAndType(weekKey: String, type: String): Int

    @Insert
    suspend fun insert(log: GenerationLogEntity)
}
