package com.example.myapplication2.domain.repository

import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.domain.model.GenerationSnapshot
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun observePinnedCards(): Flow<List<DashboardCard>>
    fun observeSearchHistory(): Flow<List<DashboardCard>>
    fun observeCard(cardId: String): Flow<DashboardCard?>
    suspend fun getCard(cardId: String): DashboardCard?
    suspend fun saveCards(cards: List<DashboardCard>)
    suspend fun saveCard(card: DashboardCard)
    suspend fun setPinned(cardId: String, pinned: Boolean)
    suspend fun reorderPinnedCards(orderedCardIds: List<String>)
    suspend fun updateCard(card: DashboardCard)
}

interface UserProfileRepository {
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun getUserProfile(): UserProfile?
    suspend fun saveUserProfile(profile: UserProfile)
}

interface AppSettingsRepository {
    fun observeSelectedNiches(): Flow<List<String>>
    suspend fun saveSelectedNiches(niches: List<String>)
    suspend fun getSelectedNiches(): List<String>
    suspend fun getLastCalendarRefreshMillis(): Long
    suspend fun setLastCalendarRefreshMillis(value: Long)
}

interface GenerationRepository {
    suspend fun getSnapshot(): GenerationSnapshot
    suspend fun canGenerate(maxPerWeek: Int = 3): Boolean
    suspend fun recordGeneration(type: GenerationType)
}

interface CacheRepository {
    suspend fun getCachedPayload(key: String): String?
    /** Повертає payload тільки якщо кеш свіжий (вік < maxAgeMillis). */
    suspend fun getCachedPayloadIfFresh(key: String, maxAgeMillis: Long): String?
    suspend fun putCachedPayload(key: String, payload: String, timestampMillis: Long)
    suspend fun clearAll()
}

interface SearchRepository {
    suspend fun search(query: String, userProfile: UserProfile): DashboardCard
}

interface CalendarRepository {
    suspend fun generateCalendar(niches: List<String>, userProfile: UserProfile): List<DashboardCard>
}

interface InsightsRepository {
    suspend fun loadInsights(userProfile: UserProfile): List<DashboardCard>
}

interface StrategyRepository {
    suspend fun loadStrategies(userProfile: UserProfile): List<DashboardCard>
}

interface LearningRepository {
    suspend fun loadLearningModules(userProfile: UserProfile): List<DashboardCard>
}

interface NotificationRepository {
    suspend fun notifyCalendarUpdates(cards: List<DashboardCard>)
}
