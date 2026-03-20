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
    object Loading : AppState()
    object AlphaDisclaimer : AppState()
    object Onboarding : AppState()
    object Main : AppState()
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
            routeAfterSplash()
        }
    }

    private suspend fun routeAfterSplash() {
        val settings = container.appSettingsRepository
        if (!settings.isAlphaDisclaimerAccepted()) {
            appState = AppState.AlphaDisclaimer
            return
        }
        routeToOnboardingOrMain()
    }

    private suspend fun routeToOnboardingOrMain() {
        val profile = container.userProfileRepository.getUserProfile()
        val settings = container.appSettingsRepository

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

    fun acceptAlphaDisclaimer() {
        viewModelScope.launch {
            container.appSettingsRepository.setAlphaDisclaimerAccepted(true)
            routeToOnboardingOrMain()
        }
    }

    // ── Full app initialization ────────────────────────────────────────────────

    private suspend fun initializeApp(profile: UserProfile) {
        NotificationHelper.createChannels(appContext)
        seedMasterCalendar(profile)
        CalendarRadarWorker.schedule(appContext)
        seedKnowledgeIfNeeded(profile)
    }

    private suspend fun seedMasterCalendar(profile: UserProfile) {
        val existing = runCatching {
            container.cardRepository.observeCardsByType(
                com.example.myapplication2.core.model.CardType.REGULATORY_EVENT,
            ).first()
        }.getOrDefault(emptyList())

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

    fun completeOnboarding(profile: UserProfile) {
        viewModelScope.launch {
            container.saveUserProfileUseCase(profile)
            initializeApp(profile)
            appState = AppState.Main
        }
    }
}
