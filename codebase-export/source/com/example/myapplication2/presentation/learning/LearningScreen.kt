package com.example.myapplication2.presentation.learning

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
import com.example.myapplication2.domain.repository.UserProfileRepository
import com.example.myapplication2.domain.usecase.LoadLearningModulesUseCase
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

data class LearningUiState(
    val cards: List<DashboardCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LearningViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val loadLearningModulesUseCase: LoadLearningModulesUseCase,
    private val pinCardUseCase: PinCardUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    fun loadModules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val profile = userProfileRepository.getUserProfile()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Завершіть onboarding.")
                return@launch
            }
            runCatching {
                loadLearningModulesUseCase(profile)
            }.onSuccess { cards ->
                _uiState.value = LearningUiState(cards = cards)
            }.onFailure { throwable ->
                _uiState.value = LearningUiState(error = com.example.myapplication2.core.util.ErrorMessageHelper.userFriendlyMessage(throwable))
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
fun LearningRoute(
    modifier: Modifier = Modifier,
    viewModel: LearningViewModel = regulationViewModel(),
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
    val quickStartCards = uiState.cards.filter { learningLevel(it) == "Базовий" }.take(3)
    val practicalCards = uiState.cards.filter { it.resources.isNotEmpty() || !it.expertOpinion.isNullOrBlank() }
    val deepDiveCards = uiState.cards.filter { !it.analytics.isNullOrBlank() }
    val sortedCards = uiState.cards.sortedByDescending { learningScore(it) }
    val featuredCard = sortedCards.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModuleTopBar(
            moduleTitle = "Навчання",
            profileLabel = profileLabel,
        )
        ModuleHeaderCard(
            title = "Навчання",
            description = "Практичний модуль щоденного навчання: швидкий старт, key takeaways, ресурси і матеріали для глибшого опрацювання.",
        )

        SectionPanel(
            title = "Оновити навчальний стек",
            subtitle = "Один запуск підтягує модулі, які допомагають швидше орієнтуватися в ніші та закривати реальні робочі питання.",
        ) {
            Button(onClick = viewModel::loadModules, modifier = Modifier.fillMaxWidth()) {
                Text("Згенерувати модулі")
            }
        }

        uiState.error?.let {
            ErrorStatePanel(
                title = "Навчальні модулі не згенерувались",
                description = it,
                actionLabel = "Спробувати ще раз",
                onAction = viewModel::loadModules,
            )
        }
        if (uiState.isLoading) {
            LoadingStatePanel(
                title = "Формуємо навчальний стек",
                description = "Підтягуємо модулі з короткими summary, practical takeaways, ресурсами й орієнтирами для наступного кроку.",
            )
        }

        if (uiState.cards.isEmpty() && !uiState.isLoading) {
            EmptyStatePanel(
                title = "Модулі ще не згенеровано",
                description = "Запусти навчальний стек, щоб отримати модулі з key takeaways, ресурсами і можливістю додати потрібне на головну.",
                actionLabel = "Згенерувати модулі",
                onAction = viewModel::loadModules,
            )
        } else {
            SectionPanel(
                title = "Огляд навчання",
                subtitle = "Короткий зріз перед тим, як відкривати окремі модулі.",
            ) {
                MetricsStrip(
                    metrics = listOf(
                        "Модулі" to uiState.cards.size.toString(),
                        "Швидкий старт" to quickStartCards.size.toString(),
                        "Практичні" to practicalCards.size.toString(),
                        "Deep dive" to deepDiveCards.size.toString(),
                    ),
                )
            }

            featuredCard?.let { card ->
                SectionPanel(
                    title = "Рекомендований модуль",
                    subtitle = "Найкраща точка входу на зараз за цінністю, простотою входу та практичністю.",
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
                            Text("Відкрити модуль")
                        }
                        OutlinedButton(onClick = { viewModel.togglePin(card) }) {
                            Text(if (card.isPinned) "Зняти з головної" else "На головну")
                        }
                    }
                }
            }

            if (quickStartCards.isNotEmpty()) {
                SectionPanel(
                    title = "Швидкий старт",
                    subtitle = "Модулі, з яких найпростіше почати без довгої підготовки.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        quickStartCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (practicalCards.isNotEmpty()) {
                SectionPanel(
                    title = "Практичні takeaway",
                    subtitle = "Модулі, де є найбільше прикладної користі, ресурсів і підказок для роботи.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        practicalCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            if (deepDiveCards.isNotEmpty()) {
                SectionPanel(
                    title = "Поглиблене навчання",
                    subtitle = "Модулі для глибшого опрацювання теми, коли потрібен не лише summary, а й глибше розуміння.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        deepDiveCards.forEach { card ->
                            DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                        }
                    }
                }
            }

            SectionPanel(
                title = "Усі модулі",
                subtitle = "Повний стек навчальних карток, які можна відкривати детально або pin-ити на головну.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sortedCards.forEach { card ->
                        DashboardCardItem(card = card, onOpenDetail = onOpenCard, onPinToggle = { viewModel.togglePin(card) })
                    }
                }
            }
        }
    }
}

private fun learningLevel(card: DashboardCard): String {
    return when {
        card.analytics.isNullOrBlank() && card.resources.size <= 3 -> "Базовий"
        card.analytics.isNullOrBlank() -> "Робочий"
        else -> "Поглиблений"
    }
}

private fun learningScore(card: DashboardCard): Int {
    var score = card.resources.size.coerceAtMost(4) * 8
    if (!card.expertOpinion.isNullOrBlank()) score += 18
    if (!card.analytics.isNullOrBlank()) score += 14
    if (card.links.isNotEmpty()) score += 10
    score += when (card.priority) {
        com.example.myapplication2.core.model.Priority.CRITICAL -> 24
        com.example.myapplication2.core.model.Priority.HIGH -> 18
        com.example.myapplication2.core.model.Priority.MEDIUM -> 12
    }
    return score
}
