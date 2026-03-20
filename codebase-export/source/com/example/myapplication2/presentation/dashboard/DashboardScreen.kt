package com.example.myapplication2.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.domain.repository.UserProfileRepository
import com.example.myapplication2.presentation.components.EmptyStatePanel
import com.example.myapplication2.domain.usecase.ObservePinnedCardsUseCase
import com.example.myapplication2.domain.usecase.PinCardUseCase
import com.example.myapplication2.domain.usecase.ReorderPinnedCardsUseCase
import com.example.myapplication2.presentation.components.DashboardCardItem
import com.example.myapplication2.presentation.components.SectionPanel
import com.example.myapplication2.presentation.navigation.regulationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    observePinnedCardsUseCase: ObservePinnedCardsUseCase,
    userProfileRepository: UserProfileRepository,
    private val pinCardUseCase: PinCardUseCase,
    private val reorderPinnedCardsUseCase: ReorderPinnedCardsUseCase,
) : ViewModel() {

    private val _cards = MutableStateFlow<List<DashboardCard>>(emptyList())
    val cards: StateFlow<List<DashboardCard>> = _cards.asStateFlow()
    private val _profileLabel = MutableStateFlow("RA")
    val profileLabel: StateFlow<String> = _profileLabel.asStateFlow()

    init {
        viewModelScope.launch {
            observePinnedCardsUseCase().collect { _cards.value = it }
        }
        viewModelScope.launch {
            userProfileRepository.observeUserProfile().collect { profile ->
                _profileLabel.value = profile?.role
                    ?.split(" ")
                    ?.filter { it.isNotBlank() }
                    ?.take(2)
                    ?.joinToString("") { it.take(1).uppercase() }
                    ?.ifBlank { "RA" }
                    ?: "RA"
            }
        }
    }

    fun togglePin(card: DashboardCard) {
        viewModelScope.launch {
            pinCardUseCase(card, !card.isPinned)
        }
    }

    fun moveCard(card: DashboardCard, delta: Int) {
        val items = _cards.value.toMutableList()
        val currentIndex = items.indexOfFirst { it.id == card.id }
        val targetIndex = currentIndex + delta
        if (currentIndex == -1 || targetIndex !in items.indices) return
        val removed = items.removeAt(currentIndex)
        items.add(targetIndex, removed)
        _cards.value = items
        viewModelScope.launch {
            reorderPinnedCardsUseCase(items.map { it.id })
        }
    }
}

@Composable
fun DashboardRoute(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = regulationViewModel(),
    onOpenCard: (DashboardCard) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenStrategy: () -> Unit = {},
    onOpenLearning: () -> Unit = {},
) {
    val cards by viewModel.cards.collectAsState()
    val profileLabel by viewModel.profileLabel.collectAsState()
    DashboardScreen(
        cards = cards,
        profileLabel = profileLabel,
        modifier = modifier,
        onPinToggle = viewModel::togglePin,
        onMove = viewModel::moveCard,
        onOpenCard = onOpenCard,
        onOpenSearch = onOpenSearch,
        onOpenCalendar = onOpenCalendar,
        onOpenInsights = onOpenInsights,
        onOpenStrategy = onOpenStrategy,
        onOpenLearning = onOpenLearning,
    )
}

@Composable
private fun DashboardScreen(
    cards: List<DashboardCard>,
    profileLabel: String,
    onPinToggle: (DashboardCard) -> Unit,
    onMove: (DashboardCard, Int) -> Unit,
    onOpenCard: (DashboardCard) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenStrategy: () -> Unit,
    onOpenLearning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val rankedCards = cards.sortedByDescending { todayScore(it, now) }
    val focusCards = rankedCards.take(3)
    val focusIds = focusCards.map { it.id }.toSet()
    val actionCards = rankedCards
        .filter { it.type == CardType.ACTION_ITEM || it.actionChecklist.isNotEmpty() }
        .filterNot { it.id in focusIds }
        .take(4)
    val actionIds = actionCards.map { it.id }.toSet()
    val upcomingDeadlineCards = rankedCards
        .filter { it.type == CardType.REGULATORY_EVENT }
        .filter { it.dateMillis >= now - DAY_IN_MILLIS }
        .sortedBy { kotlin.math.abs(it.dateMillis - now) }
        .take(4)
    val deadlineIds = upcomingDeadlineCards.map { it.id }.toSet()
    val attentionCards = rankedCards
        .filter { it.priority == Priority.CRITICAL || it.priority == Priority.HIGH || it.riskFlags.isNotEmpty() }
        .filterNot { it.id in focusIds || it.id in actionIds || it.id in deadlineIds }
        .take(4)
    val attentionIds = attentionCards.map { it.id }.toSet()
    val resumeCards = rankedCards
        .filterNot {
            it.id in focusIds || it.id in actionIds || it.id in deadlineIds || it.id in attentionIds
        }
        .take(3)
    val savedDossiers = cards
        .filter {
            it.type == CardType.SEARCH_HISTORY ||
                it.type == CardType.INSIGHT ||
                it.type == CardType.STRATEGY ||
                it.type == CardType.LEARNING_MODULE
        }
        .sortedBy { it.orderIndex }
    val criticalCards = cards.filter { it.priority == Priority.CRITICAL || it.priority == Priority.HIGH }
    val searchCount = cards.count { it.type == CardType.SEARCH_HISTORY }
    val calendarCount = cards.count { it.type == CardType.REGULATORY_EVENT }
    val insightsCount = cards.count { it.type == CardType.INSIGHT || it.type == CardType.ACTION_ITEM }
    val strategyCount = cards.count { it.type == CardType.STRATEGY }
    val learningCount = cards.count { it.type == CardType.LEARNING_MODULE }
    val searchCards = rankedCards.filter { it.type == CardType.SEARCH_HISTORY }.take(3)
    val strategyCards = rankedCards.filter { it.type == CardType.STRATEGY }.take(3)
    val calendarCards = rankedCards.filter { it.type == CardType.REGULATORY_EVENT }.take(3)
    val insightCards = rankedCards.filter { it.type == CardType.INSIGHT || it.type == CardType.ACTION_ITEM }.take(2)
    val learningCards = rankedCards.filter { it.type == CardType.LEARNING_MODULE }.take(2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DashboardTopBar(profileLabel = profileLabel)
        DashboardOverviewCard(
            title = "Overview Panel",
            totalCards = cards.size,
            searchCount = searchCount,
            strategyCount = strategyCount,
            calendarCount = calendarCount,
            focusText = focusCards.firstOrNull()?.title ?: "Збережені картки вже зібрані в одному місці для швидкого сканування.",
        )
        SectionPanel(
            title = "Модулі",
            subtitle = "Переходь у потрібний модуль або працюй із збереженими картками прямо тут.",
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DashboardNavButton("Пошук", searchCount, onOpenSearch)
                DashboardNavButton("Стратегія", strategyCount, onOpenStrategy)
                DashboardNavButton("Календар", calendarCount, onOpenCalendar)
                DashboardNavButton("Інсайди", insightsCount, onOpenInsights)
                DashboardNavButton("Навчання", learningCount, onOpenLearning)
            }
        }

        if (cards.isEmpty()) {
            EmptyStatePanel(
                title = "Ще немає збережених карток",
                description = "Додай картки з Пошуку, Календаря, Інсайтів, Стратегій або Навчання, і головна почне працювати як твоя overview panel.",
            )
        } else {
            if (focusCards.isNotEmpty()) {
                SectionPanel(
                    title = "Фокус дня",
                    subtitle = "Найважливіші картки для швидкого старту саме зараз.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        focusCards.forEach { card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                            )
                        }
                    }
                }
            }

            if (searchCards.isNotEmpty()) {
                SectionPanel(
                    title = "Пошукові картки",
                    subtitle = "Збережені досьє з експертного пошуку, які варто тримати під рукою.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        searchCards.forEach { card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                            )
                        }
                    }
                }
            }

            if (strategyCards.isNotEmpty()) {
                SectionPanel(
                    title = "Картки по стратегіям",
                    subtitle = "Playbook-картки з готовими сценаріями дій і рішеннями.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        strategyCards.forEach { card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                            )
                        }
                    }
                }
            }

            if (calendarCards.isNotEmpty()) {
                SectionPanel(
                    title = "Збережені картки з календаря",
                    subtitle = "Події, дедлайни та зміни по нішах, які ти зберіг на головну.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        calendarCards.forEach { card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                            )
                        }
                    }
                }
            }

            if (insightCards.isNotEmpty()) {
                SectionPanel(
                    title = "Інсайди",
                    subtitle = "Сигнали та дії, які залишаються важливими для поточного дня.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        insightCards.forEach { card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                            )
                        }
                    }
                }
            }

            if (learningCards.isNotEmpty()) {
                SectionPanel(
                    title = "Навчання",
                    subtitle = "Модулі, які ти зберіг для швидкого повернення до навчального стеку.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        learningCards.forEach { card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                            )
                        }
                    }
                }
            }

            if (savedDossiers.isNotEmpty()) {
                SectionPanel(
                    title = "Порядок на головній",
                    subtitle = "Пересувай збережені досьє вище або нижче, якщо хочеш вручну змінити порядок.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        savedDossiers.forEachIndexed { index, card ->
                            DashboardCardItem(
                                card = card,
                                onOpenDetail = onOpenCard,
                                onPinToggle = { onPinToggle(card) },
                                onMoveUp = if (index > 0) ({ onMove(card, -1) }) else null,
                                onMoveDown = if (index < savedDossiers.lastIndex) ({ onMove(card, 1) }) else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardNavButton(
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text("$label ($count)")
    }
}

@Composable
private fun DashboardTopBar(
    profileLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DashboardGreenStrong,
                                DashboardGreenSoft,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Regulation",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Data based on saved cards",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashboardTopAction("N")
            DashboardTopAction("+")
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DashboardInk),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profileLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DashboardTopAction(
    label: String,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DashboardOverviewCard(
    title: String,
    totalCards: Int,
    searchCount: Int,
    strategyCount: Int,
    calendarCount: Int,
    focusText: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = DashboardGreenCard),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Account insights",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = DashboardInk,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = focusText,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = DashboardInk,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DashboardOverviewMetric("Усього", totalCards.toString(), Modifier.weight(1f))
                DashboardOverviewMetric("Пошук", searchCount.toString(), Modifier.weight(1f))
                DashboardOverviewMetric("Стратегії", strategyCount.toString(), Modifier.weight(1f))
                DashboardOverviewMetric("Календар", calendarCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardOverviewMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
private val DashboardGreenStrong = Color(0xFF73E48D)
private val DashboardGreenSoft = Color(0xFFA7F4B8)
private val DashboardGreenCard = Color(0xFFD8F9DF)
private val DashboardInk = Color(0xFF171717)

private fun todayScore(card: DashboardCard, now: Long): Int {
    var score = priorityWeight(card.priority) * 100

    if (card.actionChecklist.isNotEmpty()) score += 60
    if (card.riskFlags.isNotEmpty()) score += 40
    if (card.links.isNotEmpty()) score += 10
    if (card.socialInsights.isNotEmpty()) score += 10
    score += (card.impactAreas.size.coerceAtMost(4) * 6)

    score += when (card.type) {
        CardType.ACTION_ITEM -> 90
        CardType.REGULATORY_EVENT -> eventFreshnessScore(card.dateMillis, now)
        CardType.SEARCH_HISTORY -> 35
        CardType.STRATEGY -> 28
        CardType.INSIGHT -> 24
        CardType.LEARNING_MODULE -> 12
    }

    return score
}

private fun priorityWeight(priority: Priority): Int {
    return when (priority) {
        Priority.CRITICAL -> 3
        Priority.HIGH -> 2
        Priority.MEDIUM -> 1
    }
}

private fun eventFreshnessScore(dateMillis: Long, now: Long): Int {
    val delta = dateMillis - now
    return when {
        delta in 0..DAY_IN_MILLIS -> 85
        delta in (DAY_IN_MILLIS + 1)..(3 * DAY_IN_MILLIS) -> 70
        delta in (3 * DAY_IN_MILLIS + 1)..(7 * DAY_IN_MILLIS) -> 50
        delta < 0 && kotlin.math.abs(delta) <= DAY_IN_MILLIS -> 30
        else -> 18
    }
}
