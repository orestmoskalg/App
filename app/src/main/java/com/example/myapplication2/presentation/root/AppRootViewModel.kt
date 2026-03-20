package com.example.myapplication2.presentation.root

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NotificationHelper
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.data.repository.CalendarRadarWorker
import com.example.myapplication2.data.repository.MasterCalendarSeed
import com.example.myapplication2.data.repository.SeedContentFactory
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AppState {
    object Loading    : AppState()
    object Onboarding : AppState()
    object Main       : AppState()
}

class AppRootViewModel(
    private val container: AppContainer,
    private val appContext: Context,
) : ViewModel() {

    var appState by mutableStateOf<AppState>(AppState.Loading)
        private set

    /** Draft profile when showing onboarding (resume / incomplete setup). */
    var onboardingInitialProfile by mutableStateOf<UserProfile?>(null)
        private set

    init {
        viewModelScope.launch {
            val profile = container.userProfileRepository.getUserProfile()
            val settings = container.appSettingsRepository

            // Legacy installs: profile already complete before this flag existed
            if (profile != null && profile.isComplete && !settings.isOnboardingCompleted()) {
                settings.setOnboardingCompleted(true)
            }

            val onboardingDone = settings.isOnboardingCompleted()
            val profileReady = profile != null && profile.isComplete

            if (profileReady && onboardingDone) {
                onboardingInitialProfile = null
                appState = AppState.Main
                initializeApp(profile!!)
            } else {
                onboardingInitialProfile = profile
                appState = AppState.Onboarding
            }
        }
    }

    // ── Full app initialization ────────────────────────────────────────────────

    private suspend fun initializeApp(profile: UserProfile) {
        // API key is embedded via BuildConfig (secrets.properties → OPENAI_API_KEY).

        // 1. Create notification channels (idempotent)
        NotificationHelper.createChannels(appContext)

        // 2. Seed master calendar (100+ real regulatory events)
        seedMasterCalendar(profile)

        // 3. Start daily radar worker
        CalendarRadarWorker.schedule(appContext)

        // 4. Seed Knowledge Base if first launch
        seedKnowledgeIfNeeded(profile)
    }

    // ── Master Calendar Seed ───────────────────────────────────────────────────

    private suspend fun seedMasterCalendar(profile: UserProfile) {
        val existing = runCatching {
            container.cardRepository.observeCardsByType(
                com.example.myapplication2.core.model.CardType.REGULATORY_EVENT
            ).first()
        }.getOrDefault(emptyList())

        // Only fill from static seed when there are no events yet.
        // After API calendar refresh, titles differ from master → re-merging would stack duplicates.
        if (existing.isNotEmpty()) return

        val jk = CountryRegulatoryContext.forCountry(profile.country).jurisdictionKey
        val allSeedEvents = MasterCalendarSeed.allEvents() +
            MasterCalendarSeed.nicheEvents() +
            SeedContentFactory.calendarCards(profile.niches, jk)

        val toAdd = allSeedEvents.distinctBy { it.title.trim().lowercase() }

        if (toAdd.isNotEmpty()) {
            container.cardRepository.saveCards(toAdd)
        }
    }

    // ── Knowledge Base Seed ────────────────────────────────────────────────────

    private suspend fun seedKnowledgeIfNeeded(profile: UserProfile) {
        val settings = container.appSettingsRepository
        val storedVersion = settings.getKnowledgeBaseSeedVersion()
        val targetVersion = SeedContentFactory.KNOWLEDGE_SEED_VERSION
        val seededFlag = settings.isKnowledgeBaseSeeded()

        if (seededFlag && storedVersion >= targetVersion) return

        if (seededFlag && storedVersion < targetVersion) {
            container.cardRepository.clearByType(CardType.INSIGHT)
            container.cardRepository.clearByType(CardType.STRATEGY)
            container.cardRepository.clearByType(CardType.LEARNING_MODULE)
        }

        val jk = CountryRegulatoryContext.forCountry(profile.country).jurisdictionKey
        val knowledgeCards = SeedContentFactory.knowledgeSeedCards(profile, jk)
        container.cardRepository.saveCards(knowledgeCards)
        settings.setKnowledgeBaseSeeded(true)
        settings.setKnowledgeBaseSeedVersion(targetVersion)
    }

    // ── Onboarding completion ──────────────────────────────────────────────────

    fun completeOnboarding(profile: UserProfile) {
        viewModelScope.launch {
            container.saveUserProfileUseCase(profile)
            initializeApp(profile)
            appState = AppState.Main
        }
    }
}
