package com.example.myapplication2.presentation.knowledge

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.data.repository.SeedContentFactory
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.presentation.components.*
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════
//  VIEWMODEL
// ════════════════════════════════════════════════════════════

class KnowledgeViewModel(private val container: AppContainer) : ViewModel() {

    private val profileFlow = container.observeUserProfileUseCase()

    val userProfile: StateFlow<UserProfile?> =
        profileFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedInsightNiche = MutableStateFlow<String?>(null)
    private val _selectedStrategyNiche = MutableStateFlow<String?>(null)
    private val _selectedLearningNiche = MutableStateFlow<String?>(null)

    val selectedInsightNicheState = _selectedInsightNiche.asStateFlow()
    val selectedStrategyNicheState = _selectedStrategyNiche.asStateFlow()
    val selectedLearningNicheState = _selectedLearningNiche.asStateFlow()

    private fun filterKnowledge(
        cards: List<DashboardCard>,
        profile: UserProfile?,
        selectedNicheKey: String?,
    ): List<DashboardCard> {
        if (profile == null) return emptyList()
        return cards.filter { card ->
            CountryRegulatoryContext.knowledgeJurisdictionMatches(card.jurisdictionKey, profile.country) &&
                CountryRegulatoryContext.knowledgeSectorMatches(card, profile) &&
                CountryRegulatoryContext.knowledgeNicheMatches(card, profile, selectedNicheKey)
        }
    }

    val insights: StateFlow<List<DashboardCard>> = combine(
        container.observeCardsByTypeUseCase(CardType.INSIGHT),
        profileFlow,
        _selectedInsightNiche,
    ) { cards, profile, nicheKey ->
        filterKnowledge(cards, profile, nicheKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val strategies: StateFlow<List<DashboardCard>> = combine(
        container.observeCardsByTypeUseCase(CardType.STRATEGY),
        profileFlow,
        _selectedStrategyNiche,
    ) { cards, profile, nicheKey ->
        filterKnowledge(cards, profile, nicheKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val learning: StateFlow<List<DashboardCard>> = combine(
        container.observeCardsByTypeUseCase(CardType.LEARNING_MODULE),
        profileFlow,
        _selectedLearningNiche,
    ) { cards, profile, nicheKey ->
        filterKnowledge(cards, profile, nicheKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var activeTab by mutableIntStateOf(0)

    var isLoading by mutableStateOf(false); private set
    var error     by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch {
            val profile = container.userProfileRepository.getUserProfile() ?: return@launch
            val first = profile.niches.firstOrNull()
            _selectedInsightNiche.value = first
            _selectedStrategyNiche.value = first
            _selectedLearningNiche.value = first
            val ins = filterKnowledge(
                container.observeCardsByTypeUseCase(CardType.INSIGHT).first(),
                profile, first,
            )
            val str = filterKnowledge(
                container.observeCardsByTypeUseCase(CardType.STRATEGY).first(),
                profile, first,
            )
            val lrn = filterKnowledge(
                container.observeCardsByTypeUseCase(CardType.LEARNING_MODULE).first(),
                profile, first,
            )
            if (ins.isEmpty()) loadInsights(first)
            if (str.isEmpty()) loadStrategies(first)
            if (lrn.isEmpty()) loadLearning(first)
        }
    }

    // ── Insights ─────────────────────────────────────────────────

    fun loadInsights(nicheKey: String?) {
        _selectedInsightNiche.value = nicheKey
        viewModelScope.launch {
            isLoading = true; error = null
            val profile = container.userProfileRepository.getUserProfile() ?: run {
                error = "Profile not found"; isLoading = false; return@launch
            }
            container.generateInsightsUseCase(profile, nicheOverride = nicheKey)
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    // ── Strategies ───────────────────────────────────────────────

    fun loadStrategies(nicheKey: String? = null) {
        _selectedStrategyNiche.value = nicheKey
        viewModelScope.launch {
            isLoading = true; error = null
            val profile = container.userProfileRepository.getUserProfile() ?: run {
                error = "Profile not found"; isLoading = false; return@launch
            }
            container.generateStrategiesUseCase(profile, nicheOverride = nicheKey).onFailure { error = it.message }
            isLoading = false
        }
    }

    // ── Learning ─────────────────────────────────────────────────

    fun loadLearning(nicheKey: String?) {
        _selectedLearningNiche.value = nicheKey
        viewModelScope.launch {
            isLoading = true; error = null
            val profile = container.userProfileRepository.getUserProfile() ?: run {
                error = "Profile not found"; isLoading = false; return@launch
            }
            container.loadLearningModulesUseCase(profile, nicheOverride = nicheKey)
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun pinCard(id: String, pinned: Boolean) = viewModelScope.launch { container.pinCardUseCase(id, pinned) }
    fun dismissError() { error = null }
}

// ════════════════════════════════════════════════════════════
//  SCREEN
// ════════════════════════════════════════════════════════════

private val TAB_LABELS = listOf("Insights", "Strategy", "Learning")
private val TAB_ICONS  = listOf(Icons.Filled.Insights, Icons.Filled.AutoGraph, Icons.Filled.School)

@Composable
fun KnowledgeScreen(vm: KnowledgeViewModel, onCardClick: (String) -> Unit) {
    val insights   by vm.insights.collectAsState()
    val strategies by vm.strategies.collectAsState()
    val learning   by vm.learning.collectAsState()
    val selectedInsight by vm.selectedInsightNicheState.collectAsState()
    val selectedStrategy by vm.selectedStrategyNicheState.collectAsState()
    val selectedLearning by vm.selectedLearningNicheState.collectAsState()
    val userProfile by vm.userProfile.collectAsState()

    val jurisdictionLabel = CountryRegulatoryContext.forCountry(userProfile?.country ?: "").jurisdictionName
    val sectorLabel = SectorCatalog.labelOrKey(userProfile?.sector.orEmpty().ifBlank { SectorCatalog.DEFAULT_KEY })

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Header ─────────────────────────────────────────────────
        Box(Modifier.fillMaxWidth().background(PureWhite)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Knowledge Base", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        Text(
                            "$jurisdictionLabel • $sectorLabel • ≥${SeedContentFactory.MIN_KNOWLEDGE_ITEMS} items per tab after Refresh (matches country, sector & niche)",
                            style = MaterialTheme.typography.bodySmall, color = TextMutedLight,
                        )
                    }
                    FilledIconButton(
                        onClick  = {
                            when (vm.activeTab) {
                                0 -> vm.loadInsights(selectedInsight)
                                1 -> vm.loadStrategies(selectedStrategy)
                                2 -> vm.loadLearning(selectedLearning)
                            }
                        },
                        enabled  = !vm.isLoading,
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LightTeal, contentColor = PrimaryTeal),
                    ) {
                        if (vm.isLoading)
                            CircularProgressIndicator(Modifier.size(18.dp), color = PrimaryTeal, strokeWidth = 2.dp)
                        else
                            Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Tab row
                ScrollableTabRow(
                    selectedTabIndex = vm.activeTab,
                    containerColor   = Color.Transparent,
                    contentColor     = PrimaryTeal,
                    edgePadding      = 0.dp,
                    indicator        = { tabs ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabs[vm.activeTab]),
                            color = PrimaryTeal,
                        )
                    },
                    divider = {},
                ) {
                    TAB_LABELS.forEachIndexed { i, label ->
                        Tab(
                            selected  = vm.activeTab == i,
                            onClick   = { vm.activeTab = i },
                            text      = { Text(label, style = MaterialTheme.typography.labelLarge) },
                            icon      = { Icon(TAB_ICONS[i], null, Modifier.size(16.dp)) },
                            selectedContentColor   = PrimaryTeal,
                            unselectedContentColor = TextSubtle,
                        )
                    }
                }
            }
        }

        // ── Error ───────────────────────────────────────────────────
        vm.error?.let { msg ->
            if (msg.isNotBlank()) {
                Surface(
                    color  = ErrorRed.copy(0.1f),
                    border = BorderStroke(1.dp, ErrorRed.copy(0.3f)),
                    shape  = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    Row(Modifier.padding(10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = vm::dismissError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // ── Content ─────────────────────────────────────────────────
        when (vm.activeTab) {
            0 -> InsightsTab(
                cards         = insights,
                selectedNiche = selectedInsight,
                isLoading     = vm.isLoading,
                onSelectNiche = vm::loadInsights,
                onCardClick   = onCardClick,
                onPin         = vm::pinCard,
            )
            1 -> StrategiesTab(
                cards         = strategies,
                selectedNiche = selectedStrategy,
                isLoading     = vm.isLoading,
                onSelectNiche = vm::loadStrategies,
                onCardClick   = onCardClick,
                onPin         = vm::pinCard,
            )
            2 -> LearningTab(
                cards         = learning,
                selectedNiche = selectedLearning,
                isLoading     = vm.isLoading,
                onSelectNiche = vm::loadLearning,
                onCardClick   = onCardClick,
                onPin         = vm::pinCard,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  INSIGHTS TAB  — with niche selection
// ════════════════════════════════════════════════════════════

@Composable
private fun InsightsTab(
    cards: List<DashboardCard>,
    selectedNiche: String?,
    isLoading: Boolean,
    onSelectNiche: (String?) -> Unit,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Niche picker chips
        item {
            NichePicker(
                label         = "Niche for insights:",
                selectedKey   = selectedNiche,
                onSelectNiche = onSelectNiche,
            )
        }

        if (isLoading && cards.isEmpty()) {
            items(5) { ShimmerBox(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 4.dp)) }
        } else if (cards.isEmpty()) {
            item { EmptyKnowledgeState("No insights", "Select a niche and tap Refresh", Icons.Filled.Insights) }
        } else {
            item {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("${cards.size} insights", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(cards, key = { it.id }) { card ->
                KnowledgeModuleCard(card = card, accentColor = InfoBlue, onCardClick = onCardClick, onPin = onPin)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  STRATEGIES TAB
// ════════════════════════════════════════════════════════════

@Composable
private fun StrategiesTab(
    cards: List<DashboardCard>,
    selectedNiche: String?,
    isLoading: Boolean,
    onSelectNiche: (String?) -> Unit,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            NichePicker(
                label         = "Niche for strategies:",
                selectedKey   = selectedNiche,
                onSelectNiche = onSelectNiche,
            )
        }

        if (isLoading && cards.isEmpty()) {
            items(3) { ShimmerBox(Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 4.dp)) }
        } else if (cards.isEmpty()) {
            item { EmptyKnowledgeState("No strategies", "Select a niche and tap Refresh to generate", Icons.Filled.AutoGraph) }
        } else {
            item {
                Text("${cards.size} strategies (mixed horizons)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
            items(cards, key = { it.id }) { card ->
                KnowledgeModuleCard(card = card, accentColor = WarningAmber, onCardClick = onCardClick, onPin = onPin)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  LEARNING TAB  — with niche selection
// ════════════════════════════════════════════════════════════

@Composable
private fun LearningTab(
    cards: List<DashboardCard>,
    selectedNiche: String?,
    isLoading: Boolean,
    onSelectNiche: (String?) -> Unit,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            NichePicker(
                label         = "Niche for learning:",
                selectedKey   = selectedNiche,
                onSelectNiche = onSelectNiche,
            )
        }

        if (isLoading && cards.isEmpty()) {
            items(5) { ShimmerBox(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 4.dp)) }
        } else if (cards.isEmpty()) {
            item { EmptyKnowledgeState("No modules", "Select a niche and tap Refresh", Icons.Filled.School) }
        } else {
            item {
                Text("${cards.size} learning modules",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
            items(cards, key = { it.id }) { card ->
                KnowledgeModuleCard(card = card, accentColor = SuccessGreen, onCardClick = onCardClick, onPin = onPin)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  NICHE PICKER  — horizontal scroll chips
// ════════════════════════════════════════════════════════════

@Composable
private fun NichePicker(
    label:         String,
    selectedKey:   String?,
    onSelectNiche: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding        = PaddingValues(horizontal = 4.dp),
        ) {
            // "All niches" chip
            item {
                FilterChip(
                    selected = selectedKey == null,
                    onClick  = { onSelectNiche(null) },
                    label    = { Text("🌍 General", style = MaterialTheme.typography.labelMedium) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor     = PrimaryGreen,
                    ),
                )
            }
            items(NicheCatalog.all) { niche ->
                FilterChip(
                    selected = selectedKey == niche.promptKey,
                    onClick  = { onSelectNiche(niche.promptKey) },
                    label    = { Text("${niche.icon} ${niche.nameEn}", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (selectedKey == niche.promptKey) {
                        { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                    } else null,
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                        selectedLabelColor     = PrimaryGreen,
                    ),
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  EMPTY STATE
// ════════════════════════════════════════════════════════════

@Composable
private fun EmptyKnowledgeState(title: String, subtitle: String, icon: ImageVector) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = PrimaryGreen.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
