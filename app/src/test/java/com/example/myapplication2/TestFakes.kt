package com.example.myapplication2

import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.domain.model.GenerationSnapshot
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.AppSettingsRepository
import com.example.myapplication2.domain.repository.CalendarRepository
import com.example.myapplication2.domain.repository.CardRepository
import com.example.myapplication2.domain.repository.GenerationRepository
import com.example.myapplication2.domain.repository.SearchRepository
import com.example.myapplication2.domain.repository.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeCardRepository : CardRepository {
    val savedCards = mutableListOf<DashboardCard>()
    var lastPinnedUpdate: Pair<String, Boolean>? = null

    override fun observePinnedCards(): Flow<List<DashboardCard>> = flowOf(emptyList())
    override fun observeSearchHistory(): Flow<List<DashboardCard>> = flowOf(emptyList())
    override fun observeCardsByType(type: CardType): Flow<List<DashboardCard>> = flowOf(emptyList())
    override fun observeCard(cardId: String): Flow<DashboardCard?> = flowOf(savedCards.firstOrNull { it.id == cardId })
    override suspend fun getCard(cardId: String): DashboardCard? = savedCards.firstOrNull { it.id == cardId }
    override suspend fun saveCards(cards: List<DashboardCard>) {
        savedCards.addAll(cards)
    }

    override suspend fun saveCard(card: DashboardCard) {
        savedCards.add(card)
    }

    override suspend fun setPinned(cardId: String, pinned: Boolean) {
        lastPinnedUpdate = cardId to pinned
    }

    override suspend fun reorderPinnedCards(orderedCardIds: List<String>) = Unit
    override suspend fun updateCard(card: DashboardCard) = Unit
    override suspend fun deleteCard(cardId: String) = Unit
    override suspend fun clearByType(type: CardType) = Unit
    override suspend fun clearUnpinnedByType(type: CardType) = Unit
    override suspend fun clearSearchHistory() = Unit
    override suspend fun clearAll() = Unit
    override suspend fun getUserStats(): UserStats = UserStats()
}

internal class FakeGenerationRepository(
    private val canGenerate: Boolean,
) : GenerationRepository {
    override suspend fun getSnapshot(): GenerationSnapshot =
        GenerationSnapshot("2026-10", emptyMap())

    override suspend fun canGenerate(type: GenerationType, maxPerWeek: Int): Boolean = canGenerate

    override suspend fun recordGeneration(type: GenerationType) = Unit

    override suspend fun getRemainingGenerations(type: GenerationType, maxPerWeek: Int): Int =
        if (canGenerate) maxPerWeek else 0
}

internal class FakeAppSettingsRepository : AppSettingsRepository {
    var lastCalendarRefreshMillis: Long = 0L
    private val niches = MutableStateFlow<List<String>>(emptyList())

    override fun observeSelectedNiches(): Flow<List<String>> = niches
    override suspend fun saveSelectedNiches(niches: List<String>) {
        this.niches.value = niches
    }

    override suspend fun getSelectedNiches(): List<String> = niches.value
    override suspend fun getLastCalendarRefreshMillis(): Long = lastCalendarRefreshMillis
    override suspend fun setLastCalendarRefreshMillis(value: Long) {
        lastCalendarRefreshMillis = value
    }

    override suspend fun isKnowledgeBaseSeeded(): Boolean = false
    override suspend fun setKnowledgeBaseSeeded(value: Boolean) = Unit
    override suspend fun getKnowledgeBaseSeedVersion(): Int = 0
    override suspend fun setKnowledgeBaseSeedVersion(version: Int) = Unit
    override suspend fun isOnboardingCompleted(): Boolean = false
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun getApiKeyPreview(): String = ""
    override suspend fun saveApiKey(key: String) = Unit
    override suspend fun getApiKey(): String = ""
    override suspend fun getChecklistStates(): Map<String, Boolean> = emptyMap()
    override suspend fun saveChecklistStates(states: Map<String, Boolean>) = Unit
    override suspend fun getNotificationsEnabled(): Boolean = true
    override suspend fun setNotificationsEnabled(value: Boolean) = Unit
    override suspend fun getDeadlineAlertsEnabled(): Boolean = true
    override suspend fun setDeadlineAlertsEnabled(value: Boolean) = Unit
    override suspend fun getWeeklyDigestEnabled(): Boolean = false
    override suspend fun setWeeklyDigestEnabled(value: Boolean) = Unit
    override suspend fun getUrgentOnlyEnabled(): Boolean = false
    override suspend fun setUrgentOnlyEnabled(value: Boolean) = Unit
    override suspend fun resetSessionForOnboarding() = Unit
}

internal class FakeSearchRepository(
    private val block: suspend (String, UserProfile) -> DashboardCard = { q, _ ->
        DashboardCard.create(
            type = CardType.SEARCH_HISTORY,
            title = q,
            subtitle = "subtitle",
            body = "body",
            dateMillis = 1000L,
        )
    },
) : SearchRepository {
    override suspend fun search(query: String, userProfile: UserProfile): DashboardCard =
        block(query, userProfile)
}

internal class FakeCalendarRepository(
    private val generator: suspend (List<String>, UserProfile) -> List<DashboardCard> = { _, _ -> emptyList() },
) : CalendarRepository {
    var callCount: Int = 0

    override suspend fun generateCalendar(niches: List<String>, userProfile: UserProfile): List<DashboardCard> {
        callCount += 1
        return generator(niches, userProfile)
    }
}
