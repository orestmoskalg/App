package com.example.myapplication2.domain.repository

import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.ResearchResult
import com.example.myapplication2.domain.model.GenerationSnapshot
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

data class UserStats(
    val searchCount: Int = 0,
    val calendarCount: Int = 0,
    val pinnedCount: Int = 0,
    val totalCards: Int = 0,
)

interface CardRepository {
    fun observePinnedCards(): Flow<List<DashboardCard>>
    fun observeSearchHistory(): Flow<List<DashboardCard>>
    fun observeCardsByType(type: com.example.myapplication2.core.model.CardType): Flow<List<DashboardCard>>
    fun observeCard(cardId: String): Flow<DashboardCard?>
    suspend fun getCard(cardId: String): DashboardCard?
    suspend fun saveCards(cards: List<DashboardCard>)
    suspend fun saveCard(card: DashboardCard)
    suspend fun setPinned(cardId: String, pinned: Boolean)
    suspend fun reorderPinnedCards(orderedCardIds: List<String>)
    suspend fun updateCard(card: DashboardCard)
    suspend fun deleteCard(cardId: String)
    suspend fun clearByType(type: com.example.myapplication2.core.model.CardType)
    /** Removes only unpinned cards of this type (preserves pins on regenerate). */
    suspend fun clearUnpinnedByType(type: com.example.myapplication2.core.model.CardType)
    suspend fun clearSearchHistory()
    suspend fun clearAll()
    suspend fun getUserStats(): UserStats
}

interface UserProfileRepository {
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun getUserProfile(): UserProfile?
    suspend fun saveUserProfile(profile: UserProfile)
    /** Removes saved profile (first-run / reset onboarding). */
    suspend fun clearUserProfile()
}

interface AppSettingsRepository {
    fun observeSelectedNiches(): Flow<List<String>>
    suspend fun saveSelectedNiches(niches: List<String>)
    suspend fun getSelectedNiches(): List<String>
    suspend fun getLastCalendarRefreshMillis(): Long
    suspend fun setLastCalendarRefreshMillis(value: Long)
    /** True after default knowledge/strategy/learning cards were seeded (separate from calendar API refresh). */
    suspend fun isKnowledgeBaseSeeded(): Boolean
    suspend fun setKnowledgeBaseSeeded(value: Boolean)
    /** Stored KB seed generation; bump `SeedContentFactory.KNOWLEDGE_SEED_VERSION` to force one-time re-seed. */
    suspend fun getKnowledgeBaseSeedVersion(): Int
    suspend fun setKnowledgeBaseSeedVersion(version: Int)
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
    /** One-time acknowledgement of alpha / AI disclaimer before onboarding or main. */
    suspend fun isAlphaDisclaimerAccepted(): Boolean
    suspend fun setAlphaDisclaimerAccepted(value: Boolean)
    // Checklist persistence
    suspend fun getChecklistStates(): Map<String, Boolean>
    suspend fun saveChecklistStates(states: Map<String, Boolean>)
    // Notification settings
    suspend fun getNotificationsEnabled(): Boolean
    suspend fun setNotificationsEnabled(value: Boolean)
    suspend fun getDeadlineAlertsEnabled(): Boolean
    suspend fun setDeadlineAlertsEnabled(value: Boolean)
    suspend fun getWeeklyDigestEnabled(): Boolean
    suspend fun setWeeklyDigestEnabled(value: Boolean)
    suspend fun getUrgentOnlyEnabled(): Boolean
    suspend fun setUrgentOnlyEnabled(value: Boolean)
    /** Clears onboarding + KB/calendar flags so the next launch runs setup from scratch. */
    suspend fun resetSessionForOnboarding()
}

interface GenerationRepository {
    suspend fun getSnapshot(): GenerationSnapshot
    suspend fun canGenerate(type: GenerationType, maxPerWeek: Int = 3): Boolean
    suspend fun recordGeneration(type: GenerationType)
    suspend fun getRemainingGenerations(type: GenerationType, maxPerWeek: Int = 3): Int
}

interface CacheRepository {
    suspend fun getCachedPayload(key: String): String?
    suspend fun getCachedPayloadIfFresh(key: String, maxAgeMillis: Long): String?
    suspend fun putCachedPayload(key: String, payload: String, timestampMillis: Long)
    suspend fun clearAll()
}

/** Legacy interface — kept for ExpertSearchUseCase compatibility */
interface SearchRepository {
    suspend fun search(query: String, userProfile: UserProfile): DashboardCard
}

/** Rich research interface — implemented by RemoteSearchRepository */
interface ResearchRepository : SearchRepository {
    suspend fun research(query: String, userProfile: UserProfile): ResearchResult
}

interface CalendarRepository {
    suspend fun generateCalendar(niches: List<String>, userProfile: UserProfile): List<DashboardCard>
}

interface InsightsRepository {
    suspend fun loadInsights(userProfile: UserProfile, nicheOverride: String? = null): List<DashboardCard>
}

interface StrategyRepository {
    suspend fun loadStrategies(userProfile: UserProfile, nicheOverride: String? = null): List<DashboardCard>
}

interface LearningRepository {
    suspend fun loadLearningModules(userProfile: UserProfile, nicheOverride: String? = null): List<DashboardCard>
}
