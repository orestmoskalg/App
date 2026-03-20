package com.example.myapplication2.di

import android.content.Context
import com.example.myapplication2.BuildConfig
import com.example.myapplication2.core.config.RegulationConfigLoader
import com.example.myapplication2.data.local.AppDatabase
import com.example.myapplication2.data.remote.GrokApi
import com.example.myapplication2.data.repository.*
import com.example.myapplication2.domain.repository.*
import com.example.myapplication2.domain.usecase.*

class AppContainer(val context: Context) {

    val appContext: Context get() = context.applicationContext

    private val db: AppDatabase by lazy { AppDatabase.getInstance(context) }

    val cardRepository: CardRepository by lazy { CardRepositoryImpl(db.cardDao()) }
    val cacheRepository: CacheRepository by lazy { CacheRepositoryImpl(db.cacheDao()) }
    val generationRepository: GenerationRepository by lazy { GenerationRepositoryImpl(db.generationLogDao()) }
    val appSettingsRepository: AppSettingsRepository by lazy { AppSettingsRepositoryImpl(context) }
    val userProfileRepository: UserProfileRepository by lazy { UserProfileRepositoryImpl(context) }

    val eventNotesRepository by lazy { EventNotesRepository(db.eventUserNoteDao()) }

    val regulationConfig by lazy { RegulationConfigLoader.load(appContext) }

    // ── API instance — key only from build (secrets.properties → BuildConfig), not shown in UI ────
    // Prefer OPENAI_API_KEY. Fallback GROK_API_KEY. For production, prefer a backend proxy instead of embedding keys.
    private val _grokApi: GrokApi by lazy {
        val openAiKey = runCatching { BuildConfig.OPENAI_API_KEY }.getOrDefault("").trim()
        val grokKey = runCatching { BuildConfig.GROK_API_KEY }.getOrDefault("").trim()
        val buildKey = if (openAiKey.isNotBlank()) openAiKey else grokKey
        GrokApi(buildKey)
    }

    fun getGrokApi(): GrokApi = _grokApi

    // ── Repositories — all share the same GrokApi instance ────────────────────
    val searchRepository: ResearchRepository by lazy { RemoteSearchRepository(_grokApi) }
    val insightsRepository: InsightsRepository by lazy { RemoteInsightsRepository(_grokApi) }
    val strategyRepository: StrategyRepository by lazy { RemoteStrategyRepository(_grokApi) }
    val learningRepository: LearningRepository by lazy { RemoteLearningRepository(_grokApi) }
    val calendarRepository: CalendarRepository by lazy { RemoteCalendarRepository(_grokApi) }

    val observePinnedCardsUseCase by lazy { ObservePinnedCardsUseCase(cardRepository) }
    val observeSearchHistoryUseCase by lazy { ObserveSearchHistoryUseCase(cardRepository) }
    val observeCardsByTypeUseCase by lazy { ObserveCardsByTypeUseCase(cardRepository) }
    val observeCardUseCase by lazy { ObserveCardUseCase(cardRepository) }
    val pinCardUseCase by lazy { PinCardUseCase(cardRepository) }
    val reorderPinnedCardsUseCase by lazy { ReorderPinnedCardsUseCase(cardRepository) }
    val deleteCardUseCase by lazy { DeleteCardUseCase(cardRepository) }

    val expertSearchUseCase by lazy {
        ExpertSearchUseCase(searchRepository, cardRepository, generationRepository)
    }
    val researchUseCase by lazy {
        ResearchUseCase(searchRepository, cardRepository, generationRepository)
    }
    val generateInsightsUseCase by lazy {
        GenerateInsightsUseCase(insightsRepository, cardRepository, generationRepository)
    }
    val generateStrategiesUseCase by lazy {
        GenerateStrategiesUseCase(strategyRepository, cardRepository, generationRepository)
    }
    val loadLearningModulesUseCase by lazy {
        LoadLearningModulesUseCase(learningRepository, cardRepository, generationRepository)
    }
    val generateCalendarUseCase by lazy {
        GenerateRegulatoryCalendarUseCase(calendarRepository, cardRepository, appSettingsRepository, generationRepository)
    }

    val observeUserProfileUseCase by lazy { ObserveUserProfileUseCase(userProfileRepository) }
    val saveUserProfileUseCase by lazy { SaveUserProfileUseCase(userProfileRepository, appSettingsRepository) }
    val getRemainingGenerationsUseCase by lazy { GetRemainingGenerationsUseCase(generationRepository) }
}

