package com.example.myapplication2.domain.usecase

import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.ResearchResult
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.*
import kotlinx.coroutines.flow.Flow

// ── Card Use Cases ────────────────────────────────────────────────────────────

class ObservePinnedCardsUseCase(private val repo: CardRepository) {
    operator fun invoke(): Flow<List<DashboardCard>> = repo.observePinnedCards()
}
class ObserveSearchHistoryUseCase(private val repo: CardRepository) {
    operator fun invoke(): Flow<List<DashboardCard>> = repo.observeSearchHistory()
}
class ObserveCardsByTypeUseCase(private val repo: CardRepository) {
    operator fun invoke(type: CardType): Flow<List<DashboardCard>> = repo.observeCardsByType(type)
}
class ObserveCardUseCase(private val repo: CardRepository) {
    operator fun invoke(cardId: String): Flow<DashboardCard?> = repo.observeCard(cardId)
}
class PinCardUseCase(private val repo: CardRepository) {
    suspend operator fun invoke(cardId: String, pinned: Boolean) = repo.setPinned(cardId, pinned)
}
class ReorderPinnedCardsUseCase(private val repo: CardRepository) {
    suspend operator fun invoke(orderedIds: List<String>) = repo.reorderPinnedCards(orderedIds)
}
class DeleteCardUseCase(private val repo: CardRepository) {
    suspend operator fun invoke(cardId: String) = repo.deleteCard(cardId)
}

// ── Content Use Cases ─────────────────────────────────────────────────────────

class ExpertSearchUseCase(
    private val searchRepo: SearchRepository,
    private val cardRepo: CardRepository,
    private val generationRepo: GenerationRepository,
) {
    suspend operator fun invoke(query: String, userProfile: UserProfile): Result<DashboardCard> =
        runCatching {
            val q = query.trim()
            require(q.isNotEmpty()) { "Enter a search query" }
            require(generationRepo.getRemainingGenerations(GenerationType.SEARCH) > 0) {
                "Weekly generation limit reached"
            }
            val card = searchRepo.search(q, userProfile)
            cardRepo.saveCard(card)
            generationRepo.recordGeneration(GenerationType.SEARCH)
            card
        }
}

/**
 * Rich research use case — returns full ResearchResult.
 * Calls research() ONCE. Builds card from the ResearchResult directly
 * without a second API call. The cache in RemoteSearchRepository ensures
 * the subsequent search() call (legacy path) is free.
 */
class ResearchUseCase(
    private val researchRepo: ResearchRepository,
    private val cardRepo: CardRepository,
    private val generationRepo: GenerationRepository,
) {
    suspend operator fun invoke(query: String, userProfile: UserProfile): ResearchResult {
        val result = researchRepo.research(query, userProfile)
        runCatching {
            val card = researchRepo.search(query, userProfile)
            cardRepo.saveCard(card)
        }
        runCatching { generationRepo.recordGeneration(GenerationType.SEARCH) }
        return result
    }
}

class GenerateInsightsUseCase(
    private val insightsRepo: InsightsRepository,
    private val cardRepo: CardRepository,
    private val generationRepo: GenerationRepository,
) {
    suspend operator fun invoke(
        userProfile: UserProfile,
        nicheOverride: String? = null,
    ): Result<List<DashboardCard>> = runCatching {
        val cards = insightsRepo.loadInsights(userProfile, nicheOverride)
        cardRepo.clearUnpinnedByType(CardType.INSIGHT)
        cardRepo.saveCards(cards)
        generationRepo.recordGeneration(GenerationType.INSIGHTS)
        cards
    }
}

class GenerateStrategiesUseCase(
    private val strategyRepo: StrategyRepository,
    private val cardRepo: CardRepository,
    private val generationRepo: GenerationRepository,
) {
    suspend operator fun invoke(userProfile: UserProfile, nicheOverride: String? = null): Result<List<DashboardCard>> =
        runCatching {
            val cards = strategyRepo.loadStrategies(userProfile, nicheOverride)
            cardRepo.clearUnpinnedByType(CardType.STRATEGY)
            cardRepo.saveCards(cards)
            generationRepo.recordGeneration(GenerationType.STRATEGY)
            cards
        }
}

class LoadLearningModulesUseCase(
    private val learningRepo: LearningRepository,
    private val cardRepo: CardRepository,
    private val generationRepo: GenerationRepository,
) {
    suspend operator fun invoke(
        userProfile: UserProfile,
        nicheOverride: String? = null,
    ): Result<List<DashboardCard>> = runCatching {
        val cards = learningRepo.loadLearningModules(userProfile, nicheOverride)
        cardRepo.clearUnpinnedByType(CardType.LEARNING_MODULE)
        cardRepo.saveCards(cards)
        generationRepo.recordGeneration(GenerationType.LEARNING)
        cards
    }
}

class GenerateRegulatoryCalendarUseCase(
    private val calendarRepo: CalendarRepository,
    private val cardRepo: CardRepository,
    private val settingsRepo: AppSettingsRepository,
    private val generationRepo: GenerationRepository,
) {
    suspend operator fun invoke(niches: List<String>, userProfile: UserProfile): Result<List<DashboardCard>> =
        runCatching {
            val cards = calendarRepo.generateCalendar(niches, userProfile)
            cardRepo.clearByType(CardType.REGULATORY_EVENT)
            cardRepo.saveCards(cards)
            settingsRepo.setLastCalendarRefreshMillis(System.currentTimeMillis())
            generationRepo.recordGeneration(GenerationType.CALENDAR)
            cards
        }
}

// ── User Profile Use Cases ────────────────────────────────────────────────────

class ObserveUserProfileUseCase(private val repo: UserProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = repo.observeUserProfile()
}

class SaveUserProfileUseCase(
    private val userRepo: UserProfileRepository,
    private val settingsRepo: AppSettingsRepository,
) {
    suspend operator fun invoke(profile: UserProfile) {
        userRepo.saveUserProfile(profile)
        settingsRepo.saveSelectedNiches(profile.niches)
        settingsRepo.setOnboardingCompleted(profile.isComplete)
    }
}

class GetRemainingGenerationsUseCase(private val repo: GenerationRepository) {
    suspend operator fun invoke(type: GenerationType, maxPerWeek: Int = 3): Int =
        repo.getRemainingGenerations(type, maxPerWeek)
}
