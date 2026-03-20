package com.example.myapplication2.domain.usecase

import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.domain.repository.CardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePinnedCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    operator fun invoke(): Flow<List<DashboardCard>> = cardRepository.observePinnedCards()
}

class ObserveSearchHistoryUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    operator fun invoke(): Flow<List<DashboardCard>> = cardRepository.observeSearchHistory()
}

class ObserveCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    operator fun invoke(cardId: String): Flow<DashboardCard?> = cardRepository.observeCard(cardId)
}

class PinCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(card: DashboardCard, pinned: Boolean) {
        cardRepository.saveCard(card.copy(isPinned = pinned))
        cardRepository.setPinned(card.id, pinned)
    }
}

class ReorderPinnedCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) {
    suspend operator fun invoke(orderedCardIds: List<String>) {
        cardRepository.reorderPinnedCards(orderedCardIds)
    }
}
