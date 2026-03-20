package com.example.myapplication2.domain.usecase

import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.AppSettingsRepository
import com.example.myapplication2.domain.repository.CacheRepository
import com.example.myapplication2.domain.repository.CalendarRepository
import com.example.myapplication2.domain.repository.CardRepository
import com.example.myapplication2.domain.repository.GenerationRepository
import com.example.myapplication2.domain.repository.InsightsRepository
import com.example.myapplication2.domain.repository.LearningRepository
import com.example.myapplication2.domain.repository.NotificationRepository
import com.example.myapplication2.domain.repository.SearchRepository
import com.example.myapplication2.domain.repository.StrategyRepository
import com.example.myapplication2.domain.repository.UserProfileRepository
import javax.inject.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ExpertSearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
    private val cardRepository: CardRepository,
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(query: String, userProfile: UserProfile): DashboardCard {
        require(query.isNotBlank()) {
            "Введіть пошуковий запит."
        }
        check(generationRepository.canGenerate()) {
            "Досягнуто тижневий ліміт генерації."
        }

        val card = searchRepository.search(query, userProfile).copy(type = CardType.SEARCH_HISTORY)
        cardRepository.saveCard(card)
        generationRepository.recordGeneration(GenerationType.SEARCH)
        return card
    }
}

class GenerateRegulatoryCalendarUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val cardRepository: CardRepository,
    private val generationRepository: GenerationRepository,
    private val cacheRepository: CacheRepository,
    private val json: Json,
) {
    suspend operator fun invoke(
        niches: List<String>,
        userProfile: UserProfile,
        forceRefresh: Boolean = false,
    ): List<DashboardCard> {
        require(niches.isNotEmpty()) { "Select at least one niche." }
        require(niches.size <= 5) { "No more than 5 niches are allowed." }
        check(generationRepository.canGenerate()) {
            "Weekly generation limit reached."
        }

        val cacheKey = "calendar_${com.example.myapplication2.data.repository.nichesCacheKey(niches)}"
        if (!forceRefresh) {
            cacheRepository.getCachedPayload(cacheKey)?.let { payload ->
                val cachedCards = runCatching {
                    json.decodeFromString(ListSerializer(DashboardCard.serializer()), payload)
                }.getOrNull()
                if (!cachedCards.isNullOrEmpty()) {
                    cardRepository.saveCards(cachedCards)
                    return cachedCards
                }
            }
        }

        val cards = calendarRepository.generateCalendar(niches, userProfile)
        cardRepository.saveCards(cards)
        cacheRepository.putCachedPayload(
            key = cacheKey,
            payload = json.encodeToString(ListSerializer(DashboardCard.serializer()), cards),
            timestampMillis = System.currentTimeMillis(),
        )
        generationRepository.recordGeneration(GenerationType.CALENDAR)
        return cards
    }
}

class RefreshCalendarUseCase @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val generateRegulatoryCalendarUseCase: GenerateRegulatoryCalendarUseCase,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(force: Boolean = false) {
        val lastRefresh = appSettingsRepository.getLastCalendarRefreshMillis()
        val now = System.currentTimeMillis()
        if (!force && now - lastRefresh < 86_400_000L) return

        val niches = appSettingsRepository.getSelectedNiches()
        val profile = userProfileRepository.getUserProfile() ?: return
        if (niches.isEmpty()) return

        val cards = generateRegulatoryCalendarUseCase(niches, profile, forceRefresh = true)
        notificationRepository.notifyCalendarUpdates(cards.take(3))
        appSettingsRepository.setLastCalendarRefreshMillis(now)
    }
}

class GenerateInsightsUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val cardRepository: CardRepository,
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(userProfile: UserProfile): List<DashboardCard> {
        check(generationRepository.canGenerate()) { "Weekly generation limit reached." }
        val cards = insightsRepository.loadInsights(userProfile)
        cardRepository.saveCards(cards)
        generationRepository.recordGeneration(GenerationType.INSIGHTS)
        return cards
    }
}

class GenerateStrategiesUseCase @Inject constructor(
    private val strategyRepository: StrategyRepository,
    private val cardRepository: CardRepository,
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(userProfile: UserProfile): List<DashboardCard> {
        check(generationRepository.canGenerate()) { "Weekly generation limit reached." }
        val cards = strategyRepository.loadStrategies(userProfile)
        cardRepository.saveCards(cards)
        generationRepository.recordGeneration(GenerationType.STRATEGY)
        return cards
    }
}

class LoadLearningModulesUseCase @Inject constructor(
    private val learningRepository: LearningRepository,
    private val cardRepository: CardRepository,
    private val generationRepository: GenerationRepository,
) {
    suspend operator fun invoke(userProfile: UserProfile): List<DashboardCard> {
        check(generationRepository.canGenerate()) { "Weekly generation limit reached." }
        val cards = learningRepository.loadLearningModules(userProfile)
        cardRepository.saveCards(cards)
        generationRepository.recordGeneration(GenerationType.LEARNING)
        return cards
    }
}
