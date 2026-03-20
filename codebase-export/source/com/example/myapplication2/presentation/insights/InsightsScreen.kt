package com.example.myapplication2.presentation.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
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
import com.example.myapplication2.domain.usecase.GenerateInsightsUseCase
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

data class InsightsUiState(
    val cards: List<DashboardCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class InsightsViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val generateInsightsUseCase: GenerateInsightsUseCase,
    private val pinCardUseCase: PinCardUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profile = userProfileRepository.getUserProfile()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Завершіть onboarding.")
                return@launch
            }
            runCatching {
                generateInsightsUseCase(profile)
            }.onSuccess { cards ->
                _uiState.value = InsightsUiState(cards = cards)
            }.onFailure { throwable ->
                _uiState.value = InsightsUiState(error = com.example.myapplication2.core.util.ErrorMessageHelper.userFriendlyMessage(throwable))
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
fun InsightsRoute(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = regulationViewModel(),
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
    val criticalCards = uiState.cards.filter { it.priority == Priority.CRITICAL }
    val actionCards = uiState.cards.filter { it.actionChecklist.isNotEmpty() || it.type.name == "ACTION_ITEM" }
    val impactCards = uiState.cards.filter { it.impactAreas.isNotEmpty() || !it.analytics.isNullOrBlank() }
    val timelineCards = uiState.cards.sortedByDescending { it.dateMillis }
    val insightSummary = uiState.cards.take(1).firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModuleTopBar(
            moduleTitle = "Інсайди",
            profileLabel = profileLabel,
        )
        ModuleHeaderCard(
            title = "Інсайди",
            description = "Щоденний control center для critical alerts, дій, бізнес-впливу та хронології змін по твоєму профілю.",
        )

        SectionPanel(
            title = "Оновити стек інсайтів",
            subtitle = "Один запуск збирає сигнали, next steps, управлінський вплив і поточну хронологію тем, які найважливіші саме зараз.",
        ) {
            Button(onClick = viewModel::loadInsights, modifier = Modifier.fillMaxWidth()) {
                Text("Згенерувати інсайди")
            }
        }

        uiState.error?.let {
            ErrorStatePanel(
                title = "Інсайди не згенерувались",
                description = it,
                actionLabel = "Спробувати ще раз",
                onAction = viewModel::loadInsights,
            )
        }
        if (uiState.isLoading) {
            LoadingStatePanel(
                title = "Формуємо стек інсайтів",
                description = "Збираємо критичні сигнали, практичні дії, бізнес-вплив і актуальну хронологію змін по твоєму профілю.",
            )
        }

        if (uiState.cards.isEmpty() && !uiState.isLoading) {
            EmptyStatePanel(
                title = "Інсайтів ще немає",
                description = "Згенеруй стек інсайтів, щоб побачити критичні сигнали, дії, бізнес-вплив і timeline в одному місці.",
                actionLabel = "Згенерувати інсайди",
                onAction = viewModel::loadInsights,
            )
        } else {
            SectionPanel(
                title = "Огляд дня",
                subtitle = "Швидкий executive-зріз перед тим, як перейти до окремих секцій.",
            ) {
                MetricsStrip(
                    metrics = listOf(
                        "Критичні" to criticalCards.size.toString(),
                        "Дії" to actionCards.size.toString(),
                        "Вплив" to impactCards.size.toString(),
                        "У стрічці" to uiState.cards.size.toString(),
                    ),
                )
            }

            insightSummary?.let { card ->
                SectionPanel(
                    title = "Що важливо зараз",
                    subtitle = "Один головний висновок, з якого найкраще почати перегляд цього модуля.",
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
                            Text("Відкрити досьє")
                        }
                        OutlinedButton(onClick = { viewModel.togglePin(card) }) {
                            Text(if (card.isPinned) "Зняти з головної" else "На головну")
                        }
                    }
                }
            }

            if (criticalCards.isNotEmpty()) {
                SectionPanel(
                    title = "Критичні сигнали",
                    subtitle = "Найважливіші теми, які можуть вимагати реакції без відкладання.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        criticalCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (actionCards.isNotEmpty()) {
                SectionPanel(
                    title = "План дій",
                    subtitle = "Картки з конкретними checklist-діями та next steps, які можна виконувати одразу.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        actionCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (impactCards.isNotEmpty()) {
                SectionPanel(
                    title = "Бізнес-вплив",
                    subtitle = "Картки, які найкраще пояснюють операційний, quality або compliance impact.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        impactCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (timelineCards.isNotEmpty()) {
                SectionPanel(
                    title = "Хронологія",
                    subtitle = "Останні зміни та матеріали в часовому порядку, щоб бачити розвиток ситуації.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        timelineCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }
        }
    }
}
