package com.example.myapplication2.presentation.insights

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.presentation.components.*
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Insights ────────────────────────────────────────────────────────────────────
class InsightsViewModel(private val container: AppContainer) : ViewModel() {
    val cards: StateFlow<List<DashboardCard>> =
        container.observeCardsByTypeUseCase(CardType.INSIGHT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun refresh() {
        viewModelScope.launch {
            isLoading = true; error = null
            val profile = container.userProfileRepository.getUserProfile() ?: return@launch
            container.generateInsightsUseCase(profile).onFailure { error = it.message }
            isLoading = false
        }
    }
    fun pin(id: String, pinned: Boolean) = viewModelScope.launch { container.pinCardUseCase(id, pinned) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(vm: InsightsViewModel, onCardClick: (String) -> Unit) {
    val cards by vm.cards.collectAsState()
    ContentListScreen(
        title = "Regulatory Insights",
        subtitle = "Current trends and changes",
        icon = Icons.Filled.Lightbulb,
        accentColor = EmeraldGreen,
        cards = cards,
        isLoading = vm.isLoading,
        error = vm.error,
        onRefresh = vm::refresh,
        onCardClick = onCardClick,
        onPin = { id, pinned -> vm.pin(id, pinned) },
        emptyTitle = "No insights",
        emptyBody = "Tap 'Refresh' to get current regulatory insights",
    )
}

// ── Strategy ───────────────────────────────────────────────────────────────────
class StrategyViewModel(private val container: AppContainer) : ViewModel() {
    val cards: StateFlow<List<DashboardCard>> =
        container.observeCardsByTypeUseCase(CardType.STRATEGY)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun refresh() {
        viewModelScope.launch {
            isLoading = true; error = null
            val profile = container.userProfileRepository.getUserProfile() ?: return@launch
            container.generateStrategiesUseCase(profile).onFailure { error = it.message }
            isLoading = false
        }
    }
    fun pin(id: String, pinned: Boolean) = viewModelScope.launch { container.pinCardUseCase(id, pinned) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyScreen(vm: StrategyViewModel, onCardClick: (String) -> Unit) {
    val cards by vm.cards.collectAsState()
    ContentListScreen(
        title = "Strategy",
        subtitle = "Compliance plans and actions",
        icon = Icons.Filled.AutoGraph,
        accentColor = AmberOrange,
        cards = cards,
        isLoading = vm.isLoading,
        error = vm.error,
        onRefresh = vm::refresh,
        onCardClick = onCardClick,
        onPin = { id, pinned -> vm.pin(id, pinned) },
        emptyTitle = "No strategies",
        emptyBody = "Tap 'Refresh' to generate personalized compliance strategies",
    )
}

// ── Learning ──────────────────────────────────────────────────────────────────
class LearningViewModel(private val container: AppContainer) : ViewModel() {
    val cards: StateFlow<List<DashboardCard>> =
        container.observeCardsByTypeUseCase(CardType.LEARNING_MODULE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun refresh() {
        viewModelScope.launch {
            isLoading = true; error = null
            val profile = container.userProfileRepository.getUserProfile() ?: return@launch
            container.loadLearningModulesUseCase(profile).onFailure { error = it.message }
            isLoading = false
        }
    }
    fun pin(id: String, pinned: Boolean) = viewModelScope.launch { container.pinCardUseCase(id, pinned) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(vm: LearningViewModel, onCardClick: (String) -> Unit) {
    val cards by vm.cards.collectAsState()
    ContentListScreen(
        title = "Learning",
        subtitle = "EU MDR/IVDR learning modules",
        icon = Icons.Filled.School,
        accentColor = AppGreen,
        cards = cards,
        isLoading = vm.isLoading,
        error = vm.error,
        onRefresh = vm::refresh,
        onCardClick = onCardClick,
        onPin = { id, pinned -> vm.pin(id, pinned) },
        emptyTitle = "No modules",
        emptyBody = "Tap 'Refresh' to load MDR/IVDR learning materials",
    )
}

// ── Shared ContentListScreen ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentListScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    cards: List<DashboardCard>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    emptyTitle: String,
    emptyBody: String,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = accentColor.copy(alpha = 0.12f)) {
                            Icon(icon, null, tint = accentColor, modifier = Modifier.padding(8.dp).size(20.dp))
                        }
                        Column {
                            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Refresh, "Refresh", tint = accentColor)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        ) {
            error?.let {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = CrimsonRed.copy(alpha = 0.1f),
                    ) { Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = CrimsonRed) }
                }
            }

            if (isLoading) {
                items(4) { LoadingCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            } else if (cards.isEmpty()) {
                item {
                    EmptyState(
                        icon = icon,
                        title = emptyTitle,
                        body = emptyBody,
                        action = {
                            Button(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Generate with AI")
                            }
                        },
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    DashboardCardItem(
                        card = card,
                        onClick = { onCardClick(card.id) },
                        onPin = { onPin(card.id, !card.isPinned) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
