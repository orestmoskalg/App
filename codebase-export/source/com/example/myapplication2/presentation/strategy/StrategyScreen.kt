package com.example.myapplication2.presentation.strategy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.domain.repository.UserProfileRepository
import com.example.myapplication2.domain.usecase.GenerateStrategiesUseCase
import com.example.myapplication2.domain.usecase.PinCardUseCase
import com.example.myapplication2.presentation.components.DashboardCardItem
import com.example.myapplication2.presentation.components.EmptyStatePanel
import com.example.myapplication2.presentation.components.ErrorStatePanel
import com.example.myapplication2.presentation.components.LoadingStatePanel
import com.example.myapplication2.presentation.components.MetricsStrip
import com.example.myapplication2.presentation.components.ModuleHeaderCard
import com.example.myapplication2.presentation.components.ModuleTopBar
import com.example.myapplication2.presentation.components.SectionPanel
import com.example.myapplication2.presentation.navigation.LocalAppContainer
import com.example.myapplication2.presentation.navigation.regulationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StrategyUiState(
    val cards: List<DashboardCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class StrategyViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val generateStrategiesUseCase: GenerateStrategiesUseCase,
    private val pinCardUseCase: PinCardUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrategyUiState())
    val uiState: StateFlow<StrategyUiState> = _uiState.asStateFlow()

    fun loadStrategies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profile = userProfileRepository.getUserProfile()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Завершіть onboarding.")
                return@launch
            }
            runCatching {
                generateStrategiesUseCase(profile)
            }.onSuccess { cards ->
                _uiState.value = StrategyUiState(cards = cards)
            }.onFailure { throwable ->
                _uiState.value = StrategyUiState(error = com.example.myapplication2.core.util.ErrorMessageHelper.userFriendlyMessage(throwable))
            }
        }
    }

    fun togglePin(card: DashboardCard) {
        viewModelScope.launch {
            pinCardUseCase(card, !card.isPinned)
        }
    }
}

@Composable
fun StrategyRoute(
    modifier: Modifier = Modifier,
    viewModel: StrategyViewModel = regulationViewModel(),
    onOpenCard: (DashboardCard) -> Unit = {},
) {
    val appContainer = LocalAppContainer.current
    val uiState by viewModel.uiState.collectAsState()
    val profile by appContainer.userProfileRepo.observeUserProfile().collectAsState(initial = null)
    val profileLabel = remember(profile) {
        profile?.role
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.take(2)
            ?.joinToString("") { it.take(1).uppercase() }
            ?.ifBlank { "RA" }
            ?: "RA"
    }
    val highPriorityCards = uiState.cards.filter { it.priority == Priority.CRITICAL || it.priority == Priority.HIGH }
    val quickWinCards = uiState.cards.filter { it.resources.size <= 3 }.take(3)
    val payoffCards = uiState.cards.filter { it.impactAreas.isNotEmpty() || !it.analytics.isNullOrBlank() }
    val stackCards = uiState.cards.sortedByDescending { strategyScore(it) }
    val featuredCard = stackCards.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModuleTopBar(
            moduleTitle = "Стратегії",
            profileLabel = profileLabel,
        )
        ModuleHeaderCard(
            title = "Стратегії",
            description = "Готові playbook-сценарії для compliance, ризиків і планування виконання без зайвого аналізу з нуля.",
        )

        SectionPanel(
            title = "Оновити playbooks",
            subtitle = "Один запуск формує сценарії рішень, де видно ризик, очікуваний payoff і наступний крок.",
        ) {
            Button(onClick = viewModel::loadStrategies, modifier = Modifier.fillMaxWidth()) {
                Text("Згенерувати стратегії")
            }
        }

        uiState.error?.let {
            ErrorStatePanel(
                title = "Playbooks не згенерувались",
                description = it,
                actionLabel = "Спробувати ще раз",
                onAction = viewModel::loadStrategies,
            )
        }
        if (uiState.isLoading) {
            LoadingStatePanel(
                title = "Формуємо playbooks",
                description = "Готуємо стратегії з практичними діями, ризиками, очікуваним payoff і сценаріями впровадження.",
            )
        }

        if (uiState.cards.isEmpty() && !uiState.isLoading) {
            EmptyStatePanel(
                title = "Стратегій ще немає",
                description = "Згенеруй playbooks, щоб побачити рекомендовані сценарії дій, ризики і очікуваний payoff по своєму профілю.",
                actionLabel = "Згенерувати стратегії",
                onAction = viewModel::loadStrategies,
            )
        } else {
            SectionPanel(
                title = "Огляд модуля",
                subtitle = "Короткий зріз перед відкриттям окремих playbooks.",
            ) {
                MetricsStrip(
                    metrics = listOf(
                        "Playbooks" to uiState.cards.size.toString(),
                        "Пріоритетні" to highPriorityCards.size.toString(),
                        "Швидкий старт" to quickWinCards.size.toString(),
                        "Вплив" to payoffCards.size.toString(),
                    ),
                )
            }

            featuredCard?.let { card ->
                SectionPanel(
                    title = "Рекомендований сценарій",
                    subtitle = "Найсильніший стартовий playbook на зараз за співвідношенням ризику, впливу і придатності до виконання.",
                ) {
                    Text(
                        text = card.expertOpinion ?: card.body,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { onOpenCard(card) }) {
                            Text("Відкрити playbook")
                        }
                        OutlinedButton(onClick = { viewModel.togglePin(card) }) {
                            Text(if (card.isPinned) "Зняти з головної" else "На головну")
                        }
                    }
                }
            }

            if (highPriorityCards.isNotEmpty()) {
                SectionPanel(
                    title = "Пріоритетні playbooks",
                    subtitle = "Стратегії, які мають найбільшу терміновість або ціну помилки при відкладанні.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        highPriorityCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (quickWinCards.isNotEmpty()) {
                SectionPanel(
                    title = "Швидкий старт",
                    subtitle = "Playbooks, з яких найпростіше почати без великої підготовки команди.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        quickWinCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (payoffCards.isNotEmpty()) {
                SectionPanel(
                    title = "Найбільший вплив",
                    subtitle = "Стратегії, які найкраще закривають бізнес-ризик, execution gap або compliance навантаження.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        payoffCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            SectionPanel(
                title = "Усі playbooks",
                subtitle = "Повний стек стратегій, які можна відкривати детально або pin-ити на головний екран.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stackCards.forEach { card ->
                        DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                    }
                }
            }
        }
    }
}

private fun strategyScore(card: DashboardCard): Int {
    var score = when (card.priority) {
        Priority.CRITICAL -> 70
        Priority.HIGH -> 52
        Priority.MEDIUM -> 34
    }
    score += (card.impactAreas.size.coerceAtMost(4) * 8)
    score += (card.resources.size.coerceAtMost(4) * 4)
    if (!card.analytics.isNullOrBlank()) score += 12
    if (!card.expertOpinion.isNullOrBlank()) score += 10
    return score
}
