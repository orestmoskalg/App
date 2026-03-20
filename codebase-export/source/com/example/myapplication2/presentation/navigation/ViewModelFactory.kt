package com.example.myapplication2.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication2.RegulationApplication
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.presentation.calendar.CalendarViewModel
import com.example.myapplication2.presentation.dashboard.DashboardViewModel
import com.example.myapplication2.presentation.insights.InsightsViewModel
import com.example.myapplication2.presentation.learning.LearningViewModel
import com.example.myapplication2.presentation.onboarding.OnboardingViewModel
import com.example.myapplication2.presentation.root.AppRootViewModel
import com.example.myapplication2.presentation.search.SearchViewModel
import com.example.myapplication2.presentation.strategy.StrategyViewModel

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}

class RegulationViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            AppRootViewModel::class.java -> AppRootViewModel(appContainer.observeUserProfileUseCase)
            OnboardingViewModel::class.java -> OnboardingViewModel(
                appContainer.saveUserProfileUseCase,
                appContainer.appSettingsRepo,
            )
            DashboardViewModel::class.java -> DashboardViewModel(
                appContainer.observePinnedCardsUseCase,
                appContainer.userProfileRepo,
                appContainer.pinCardUseCase,
                appContainer.reorderPinnedCardsUseCase,
            )
            SearchViewModel::class.java -> SearchViewModel(
                appContainer.observeSearchHistoryUseCase,
                appContainer.appSettingsRepo,
                appContainer.userProfileRepo,
                appContainer.saveUserProfileUseCase,
                appContainer.expertSearchUseCase,
                appContainer.pinCardUseCase,
            )
            CalendarViewModel::class.java -> CalendarViewModel(
                appContainer.appSettingsRepo,
                appContainer.userProfileRepo,
                appContainer.saveUserProfileUseCase,
                appContainer.generateRegulatoryCalendarUseCase,
                appContainer.refreshCalendarUseCase,
                appContainer.pinCardUseCase,
                appContainer.cacheRepo,
            )
            InsightsViewModel::class.java -> InsightsViewModel(
                appContainer.userProfileRepo,
                appContainer.generateInsightsUseCase,
                appContainer.pinCardUseCase,
            )
            StrategyViewModel::class.java -> StrategyViewModel(
                appContainer.userProfileRepo,
                appContainer.generateStrategiesUseCase,
                appContainer.pinCardUseCase,
            )
            LearningViewModel::class.java -> LearningViewModel(
                appContainer.userProfileRepo,
                appContainer.loadLearningModulesUseCase,
                appContainer.pinCardUseCase,
            )
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}

@Composable
inline fun <reified T : ViewModel> regulationViewModel(): T {
    val appContainer = LocalAppContainer.current
    return viewModel(factory = RegulationViewModelFactory(appContainer))
}

@Composable
fun rememberAppContainer(): AppContainer {
    val application = LocalContext.current.applicationContext as RegulationApplication
    return application.appContainer
}
