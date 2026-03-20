package com.example.myapplication2.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppRootState {
    data object Loading : AppRootState
    data object Onboarding : AppRootState
    data class Ready(val profile: UserProfile) : AppRootState
}

class AppRootViewModel(
    observeUserProfileUseCase: ObserveUserProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<AppRootState>(AppRootState.Loading)
    val state: StateFlow<AppRootState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeUserProfileUseCase().collect { profile ->
                _state.value = if (profile == null) {
                    AppRootState.Onboarding
                } else {
                    AppRootState.Ready(profile)
                }
            }
        }
    }
}
