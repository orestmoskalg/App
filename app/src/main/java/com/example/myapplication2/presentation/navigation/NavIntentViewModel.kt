package com.example.myapplication2.presentation.navigation

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class PendingNotificationRoute {
    data class CardDetail(val cardId: String) : PendingNotificationRoute()
    data object CalendarTab : PendingNotificationRoute()
}

class NavIntentViewModel : ViewModel() {

    private val _pendingRoute = MutableStateFlow<PendingNotificationRoute?>(null)
    val pendingRoute: StateFlow<PendingNotificationRoute?> = _pendingRoute.asStateFlow()

    fun applyIntent(intent: Intent?) {
        if (intent == null) return
        val cardId = intent.getStringExtra("navigate_card_id")
        val tab = intent.getStringExtra("navigate_tab")
        _pendingRoute.update {
            when {
                !cardId.isNullOrBlank() -> PendingNotificationRoute.CardDetail(cardId)
                tab == "calendar" -> PendingNotificationRoute.CalendarTab
                else -> null
            }
        }
    }

    fun consumePendingRoute() {
        _pendingRoute.value = null
    }
}
