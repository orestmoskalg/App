package com.example.myapplication2.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.Niche
import com.example.myapplication2.core.model.CardLink
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.SearchSection
import com.example.myapplication2.core.model.SearchSectionType
import com.example.myapplication2.core.model.SocialPost
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.AppSettingsRepository
import com.example.myapplication2.domain.repository.UserProfileRepository
import com.example.myapplication2.domain.usecase.SaveUserProfileUseCase
import com.example.myapplication2.domain.usecase.ExpertSearchUseCase
import com.example.myapplication2.domain.usecase.ObserveSearchHistoryUseCase
import com.example.myapplication2.domain.usecase.PinCardUseCase
import com.example.myapplication2.presentation.components.ErrorStatePanel
import com.example.myapplication2.presentation.components.MetricsStrip
import com.example.myapplication2.presentation.components.ModuleTopBar
import com.example.myapplication2.presentation.components.NicheSelectorView
import com.example.myapplication2.presentation.components.SectionPanel
import com.example.myapplication2.presentation.components.StatusBadgeRow
import com.example.myapplication2.presentation.components.shareDashboardCard
import com.example.myapplication2.presentation.navigation.LocalAppContainer
import com.example.myapplication2.presentation.navigation.regulationViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SearchUiState(
    val selectedNiches: List<String> = emptyList(),
    val resultCard: DashboardCard? = null,
    val history: List<DashboardCard> = emptyList(),
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val loadingStage: String = "",
    val error: String? = null,
)

class SearchViewModel(
    observeSearchHistoryUseCase: ObserveSearchHistoryUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val expertSearchUseCase: ExpertSearchUseCase,
    private val pinCardUseCase: PinCardUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var progressJob: Job? = null

    init {
        viewModelScope.launch {
            observeSearchHistoryUseCase().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
        viewModelScope.launch {
            appSettingsRepository.observeSelectedNiches().collect { niches ->
                _uiState.value = _uiState.value.copy(selectedNiches = niches)
            }
        }
    }

    fun toggleNiche(niche: Niche) {
        val key = niche.promptKey
        val current = _uiState.value.selectedNiches.toMutableList()
        if (current.contains(key)) current.remove(key)
        else if (current.size < 5) current.add(key)
        viewModelScope.launch {
            appSettingsRepository.saveSelectedNiches(current)
            userProfileRepository.getUserProfile()?.let { profile: UserProfile ->
                saveUserProfileUseCase(profile.copy(niches = current))
            }
        }
    }

    fun performSearch(query: String) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) return@launch
            if (_uiState.value.selectedNiches.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "Спочатку оберіть нішу для пошуку.",
                )
                return@launch
            }

            val profile = userProfileRepository.getUserProfile()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Завершіть onboarding.",
                    progress = 0f,
                    loadingStage = "",
                )
                return@launch
            }
            val profileWithNiches = profile.copy(niches = _uiState.value.selectedNiches)

            startTimeBasedProgress()
            _uiState.value = _uiState.value.copy(
                resultCard = null,
                isLoading = true,
                error = null,
                progress = 0.05f,
                loadingStage = "Готуємо запит і контекст",
            )

            val startMillis = System.currentTimeMillis()
            runCatching {
                expertSearchUseCase(trimmedQuery, profileWithNiches)
            }.onSuccess { card ->
                progressJob?.cancel()
                val elapsedSec = (System.currentTimeMillis() - startMillis) / 1000
                _uiState.value = _uiState.value.copy(
                    resultCard = card,
                    isLoading = false,
                    progress = 1f,
                    loadingStage = "Готово за ${elapsedSec} с",
                )
            }.onFailure { throwable ->
                progressJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    resultCard = null,
                    isLoading = false,
                    error = com.example.myapplication2.core.util.ErrorMessageHelper.userFriendlyMessage(throwable),
                    progress = 0f,
                    loadingStage = "",
                )
            }
        }
    }

    fun togglePin(card: DashboardCard) {
        viewModelScope.launch {
            pinCardUseCase(card, !card.isPinned)
        }
    }

    private fun startTimeBasedProgress() {
        progressJob?.cancel()
        val startMillis = System.currentTimeMillis()
        val stages = listOf(
            "Аналізуємо запит",
            "Формуємо план",
            "Збираємо докази",
            "Підбираємо джерела",
            "Формуємо відповідь",
            "Перевіряємо твердження",
            "Ранжуємо ресурси",
            "Фінальний розбір",
        )
        progressJob = viewModelScope.launch {
            var lastStageIndex = 0
            while (isActive && _uiState.value.isLoading) {
                delay(400)
                if (!_uiState.value.isLoading) break
                val elapsedMs = System.currentTimeMillis() - startMillis
                val elapsedSec = elapsedMs / 1000f
                val progress = (elapsedSec / 30f).coerceIn(0.05f, 0.95f)
                val stageIndex = (elapsedSec / 2.2f).toInt().coerceIn(0, stages.lastIndex)
                if (stageIndex != lastStageIndex) {
                    lastStageIndex = stageIndex
                }
                _uiState.value = _uiState.value.copy(
                    progress = progress,
                    loadingStage = stages[stageIndex],
                )
            }
        }
    }
}

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = regulationViewModel(),
    onOpenCard: (DashboardCard) -> Unit = {},
) {
    val appContainer = LocalAppContainer.current
    val uiState by viewModel.uiState.collectAsState()
    val profile by appContainer.userProfileRepo.observeUserProfile().collectAsState(initial = null)
    var query by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val historyWithoutActiveResult = remember(uiState.history, uiState.resultCard?.id) {
        uiState.history.filterNot { it.id == uiState.resultCard?.id }
    }
    val profileLabel = remember(profile) {
        profile?.role
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.take(2)
            ?.joinToString("") { it.take(1).uppercase() }
            ?.ifBlank { "RA" }
            ?: "RA"
    }

    LaunchedEffect(uiState.resultCard?.id) {
        if (uiState.resultCard != null) {
            scrollState.animateScrollTo(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModuleTopBar(
            moduleTitle = "Пошук",
            profileLabel = profileLabel,
        )

        NicheSelectorView(
            selectedNiches = uiState.selectedNiches,
            onToggleNiche = { viewModel.toggleNiche(it) },
            title = "По якій ніші шукати?",
            subtitle = "Оберіть нішу для пошуку подій та змін. Цей вибір використовується для пошуку та календаря.",
            compact = true,
        )

        if (uiState.selectedNiches.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Спочатку оберіть нішу",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Щоб шукати події та зміни, оберіть хоча б одну нішу вище. Цей вибір також використовується для календаря.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SearchEntryHeader(
            query = query,
            onQueryChange = { query = it },
            onSearch = { viewModel.performSearch(query) },
            isLoading = uiState.isLoading,
            searchEnabled = uiState.selectedNiches.isNotEmpty(),
        )

        uiState.error?.let {
            ErrorStatePanel(
                title = "Пошук не завершився",
                description = it,
                actionLabel = if (query.isNotBlank()) "Спробувати ще раз" else null,
                onAction = if (query.isNotBlank()) {
                    { viewModel.performSearch(query) }
                } else {
                    null
                },
            )
        }
        if (uiState.isLoading) {
            SearchLoadingPanel(
                progress = uiState.progress,
                stage = uiState.loadingStage,
            )
        }
        uiState.resultCard?.let { card ->
            AnswerFirstSearchResult(
                card = card,
                onOpenDetail = { onOpenCard(card) },
            )
        }

        if (historyWithoutActiveResult.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Пошукові запити",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy((-36).dp),
                ) {
                    historyWithoutActiveResult.forEachIndexed { index, card ->
                        SearchQueryPreviewCard(
                            card = card,
                            isHighlighted = index == 0,
                            modifier = Modifier.zIndex(index.toFloat()),
                            onOpenDetail = { onOpenCard(card) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEntryHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    searchEnabled: Boolean = true,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                label = { Text("Пошук") },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Button(
                onClick = onSearch,
                enabled = searchEnabled && query.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (isLoading) "Пошук..." else "Пошук",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AnswerFirstSearchResult(
    card: DashboardCard,
    onOpenDetail: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    SectionPanel(
        title = card.title.ifBlank { card.searchQuery.ifBlank { "Результат" } },
        subtitle = card.subtitle.takeIf { it.isNotBlank() },
    ) {
        StatusBadgeRow(
            items = listOfNotNull(
                card.confidenceLabel.takeIf { it.isNotBlank() }?.let { "Впевненість: $it" },
                card.urgencyLabel.takeIf { it.isNotBlank() }?.let { "Терміновість: $it" },
            ),
        )
        Text(
            text = card.body,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        CompactMetricsBlock(card = card)
        card.analytics?.takeIf { it.isNotBlank() }?.let {
            HighlightedTextBlock(title = "Для вас", text = it)
        }
        card.expertOpinion?.takeIf { it.isNotBlank() }?.let {
            HighlightedTextBlock(title = "Коментар", text = it)
        }
        if (card.impactAreas.isNotEmpty()) {
            BulletInsightBlock(title = "Ключові факти", items = card.impactAreas.take(4))
        }
        NextActionsPanel(card = card)
        if (card.links.isNotEmpty()) {
            CompactLinksBlock(links = card.links.take(4))
        }
        if (card.socialInsights.isNotEmpty()) {
            SocialDiscussionBlock(posts = card.socialInsights.take(2), compact = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onOpenDetail,
                modifier = Modifier.weight(1f),
            ) {
                Text("Повна відповідь")
            }
            if (card.links.isNotEmpty()) {
                OutlinedButton(
                    onClick = { uriHandler.openUri(card.links.first().url) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Перше джерело")
                }
            }
            OutlinedButton(
                onClick = { shareDashboardCard(context, card) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Розшарити")
            }
        }
    }
}

@Composable
private fun SearchQueryPreviewCard(
    card: DashboardCard,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    onOpenDetail: () -> Unit,
) {
    val queryTitle = card.searchQuery.ifBlank { card.title }
    val description = when {
        card.body.isNotBlank() -> card.body
        card.searchQuery.isNotBlank() && card.title.isNotBlank() && card.title != card.searchQuery -> card.title
        else -> card.subtitle
    }
    val accentColor = if (isHighlighted) SearchReferenceAccent else MaterialTheme.colorScheme.surface
    val accentText = if (isHighlighted) SearchReferenceText else MaterialTheme.colorScheme.onSurface
    val accentSubtle = if (isHighlighted) SearchReferenceText.copy(alpha = 0.68f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 22.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .offset(x = 10.dp, y = 18.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
            ),
        ) {}

        Card(
            modifier = Modifier
                .fillMaxWidth(0.985f)
                .offset(x = 5.dp, y = 9.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.46f),
            ),
        ) {}

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isHighlighted) 0.dp else 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(24.dp),
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = accentColor),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = queryTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = accentText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = accentSubtle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenDetail,
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = CircleShape,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isHighlighted) {
                                    SearchReferenceText.copy(alpha = 0.32f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = if (isHighlighted) SearchReferenceText else MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("...", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Text(
                            text = compactSearchDate(card.dateMillis),
                            style = MaterialTheme.typography.labelMedium,
                            color = accentSubtle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMetricsBlock(card: DashboardCard) {
    val rankedLinks = rankedResearchLinks(card.links)
    MetricsStrip(
        metrics = listOf(
            "Офіц." to rankedLinks.count { researchLinkTier(it) == "Official" }.toString(),
            "Дії" to card.actionChecklist.size.toString(),
            "Ризики" to card.riskFlags.size.toString(),
        ),
    )
}

@Composable
private fun CompactLinksBlock(links: List<CardLink>) {
    val uriHandler = LocalUriHandler.current
    val ranked = rankedResearchLinks(links)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ranked.forEach { link ->
            OutlinedButton(
                onClick = { uriHandler.openUri(link.url) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = link.title.ifBlank { link.url },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RiskSignalPanel(card: DashboardCard) {
    val risk = riskScore(card)
    val riskLabel = when {
        risk >= 75 -> "Високий"
        risk >= 45 -> "Середній"
        else -> "Контрольований"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Оцінка ризику", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        MetricLine(label = "Поточний рівень ризику", value = "$risk / 100 • $riskLabel")
        LinearProgressIndicator(
            progress = { risk / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NextActionsPanel(card: DashboardCard) {
    val nextActions = card.actionChecklist.take(3).ifEmpty {
        buildList {
            if (card.riskFlags.isNotEmpty()) add("Перевірити ризики та слабкі місця у повному досьє")
            if (card.links.isNotEmpty()) add("Відкрити ключові джерела і звірити позицію команди")
            if (card.analytics?.isNotBlank() == true) add("Переглянути аналітику і сформувати рішення по темі")
        }
    }
    if (nextActions.isEmpty()) return

    BulletInsightBlock(
        title = "Дії",
        items = nextActions,
    )
}

@Composable
private fun SearchLoadingPanel(
    progress: Float,
    stage: String,
) {
    SectionPanel(
        title = "Підбираємо матеріали",
        subtitle = null,
    ) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(stage.ifBlank { "Уточнюємо відповідь і джерела..." }, style = MaterialTheme.typography.bodyMedium)
    }
}

private val SearchReferenceAccent = Color(0xFFDDFE28)
private val SearchReferenceText = Color(0xFF111111)

private fun compactSearchDate(value: Long): String {
    return SimpleDateFormat("dd MMM", Locale.US).format(Date(value))
}

@Composable
private fun ExpertSearchResultCard(
    card: DashboardCard,
    onPinToggle: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(card.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(card.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(card.body, style = MaterialTheme.typography.bodyLarge)

            SearchSummaryMetrics(card = card)
            ConfidenceUrgencyBlock(card = card)

            card.expertOpinion?.takeIf { it.isNotBlank() }?.let {
                HighlightedTextBlock(
                    title = "Експертний висновок",
                    text = it,
                )
            }

            card.analytics?.takeIf { it.isNotBlank() }?.let {
                HighlightedTextBlock(
                    title = "Ключова аналітика",
                    text = it,
                )
            }

            if (card.resources.isNotEmpty()) {
                SectionResources(resources = card.resources)
            }

            if (card.actionChecklist.isNotEmpty()) {
                BulletInsightBlock(
                    title = "Killer feature: Action checklist",
                    items = card.actionChecklist,
                )
            }

            if (card.riskFlags.isNotEmpty()) {
                BulletInsightBlock(
                    title = "Killer feature: Risk flags",
                    items = card.riskFlags,
                )
            }

            if (card.impactAreas.isNotEmpty()) {
                ImpactAreasBlock(areas = card.impactAreas)
            }

            if (card.links.isNotEmpty()) {
                VerifiedLinksBlock(title = "Основні джерела", links = card.links)
            }

            card.detailedSections.forEach { section ->
                SearchSectionCard(section = section)
            }

            if (card.socialInsights.isNotEmpty()) {
                SocialDiscussionBlock(posts = card.socialInsights)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPinToggle) {
                    Text(if (card.isPinned) "Зняти з головної" else "Додати на головну")
                }
                OutlinedButton(onClick = { shareDashboardCard(context, card) }) {
                    Text("Розшарити")
                }
                if (card.links.isNotEmpty()) {
                    OutlinedButton(onClick = { uriHandler.openUri(card.links.first().url) }) {
                        Text("Відкрити перше джерело")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSummaryMetrics(card: DashboardCard) {
    val rankedLinks = rankedResearchLinks(card.links)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Швидкий огляд", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        MetricsStrip(
            metrics = listOf(
                "Офіційні" to rankedLinks.count { researchLinkTier(it) == "Official" }.toString(),
                "Ключові" to rankedLinks.count { researchLinkScore(it) >= 80 }.toString(),
                "Дії" to card.actionChecklist.size.toString(),
                "Ризики" to card.riskFlags.size.toString(),
                "Соцзгадки" to card.socialInsights.size.toString(),
            ),
        )
    }
}

@Composable
private fun MetricLine(
    label: String,
    value: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConfidenceUrgencyBlock(card: DashboardCard) {
    if (card.confidenceLabel.isBlank() && card.urgencyLabel.isBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Контекст рішення", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        StatusBadgeRow(
            items = listOfNotNull(
                card.confidenceLabel.takeIf { it.isNotBlank() }?.let { "Впевненість: $it" },
                card.urgencyLabel.takeIf { it.isNotBlank() }?.let { "Терміновість: $it" },
            ),
        )
    }
}

@Composable
private fun HighlightedTextBlock(
    title: String,
    text: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BulletInsightBlock(
    title: String,
    items: List<String>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            items.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ImpactAreasBlock(areas: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Impact areas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        areas.forEach { area ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Text(
                    text = area,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SearchSectionCard(section: SearchSection) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(sectionTitle(section.type, section.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(section.content, style = MaterialTheme.typography.bodyMedium)
            if (section.resources.isNotEmpty()) {
                SectionResources(resources = section.resources)
            }
            if (section.links.isNotEmpty()) {
                VerifiedLinksBlock(title = "Ресурси секції", links = section.links)
            }
        }
    }
}

@Composable
private fun SectionResources(resources: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Що варто врахувати", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        resources.forEach { resource ->
            Text("• $resource", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VerifiedLinksBlock(
    title: String,
    links: List<CardLink>,
) {
    val uriHandler = LocalUriHandler.current
    val rankedLinks = rankedResearchLinks(links)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        rankedLinks.forEachIndexed { index, link ->
            OutlinedButton(
                onClick = { uriHandler.openUri(link.url) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("${index + 1}. ${link.title}")
                    StatusBadgeRow(
                        items = listOfNotNull(
                            researchLinkTierLabel(link),
                            "Сила: ${researchLinkScore(link)}",
                            link.sourceLabel.takeIf { it.isNotBlank() },
                            link.takeIf { it.isVerified }?.let { "URL доступний" },
                        ),
                    )
                    Text(
                        text = buildString {
                            append(
                                when (researchLinkTier(link)) {
                                    "Official" -> "Ключове офіційне джерело"
                                    "Community" -> "Практичний контекст"
                                    "Strong evidence" -> "Ключове джерело для орієнтації"
                                    else -> "Додаткове джерело"
                                },
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialDiscussionBlock(posts: List<SocialPost>, compact: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!compact) {
            Text("Обговорення в соцмережах", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        posts.forEach { post ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("${post.platform} • ${post.author}", fontWeight = FontWeight.Medium)
                    Text(post.text, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { uriHandler.openUri(post.url) }) {
                        Text(if (compact) "Відкрити" else "Відкрити джерело")
                    }
                }
            }
        }
    }
}

private fun sectionTitle(type: SearchSectionType, fallback: String): String {
    return fallback.ifBlank {
        when (type) {
            SearchSectionType.QUERY_BRIEFING -> "1. Повний опис запиту"
            SearchSectionType.RELATED_EVENTS -> "2. Події, пов'язані із запитом"
            SearchSectionType.EXPERT_ANALYTICS -> "3. Експертна аналітика"
            SearchSectionType.STRATEGIC_FOCUS -> "4. На що звернути увагу / Стратегія"
            SearchSectionType.SOCIAL_DISCUSSION -> "5. LinkedIn / X / Facebook обговорення"
        }
    }
}

private fun rankedResearchLinks(links: List<CardLink>): List<CardLink> {
    return links
        .distinctBy { it.url }
        .sortedWith(
            compareByDescending<CardLink> { researchLinkScore(it) }
                .thenBy { it.title },
        )
}

private fun researchLinkScore(link: CardLink): Int {
    val url = link.url.lowercase()
    val source = link.sourceLabel.lowercase()
    var score = 20
    if (url.contains("eur-lex.europa.eu") || url.contains("health.ec.europa.eu") || url.contains("webgate.ec.europa.eu") || url.contains("ec.europa.eu/tools/eudamed") || url.contains("ec.europa.eu/docsroom")) {
        score += 55
    }
    if (link.isVerified) score += 15
    if (source.contains("official") || source.contains("regulation") || source.contains("commission") || source.contains("guidance")) {
        score += 10
    }
    if (url.contains("linkedin.com") || url.contains("x.com") || url.contains("twitter.com")) {
        score -= 10
    }
    return score.coerceIn(0, 100)
}

private fun researchLinkTier(link: CardLink): String {
    val url = link.url.lowercase()
    return when {
        url.contains("eur-lex.europa.eu") || url.contains("health.ec.europa.eu") || url.contains("webgate.ec.europa.eu") || url.contains("ec.europa.eu/tools/eudamed") || url.contains("ec.europa.eu/docsroom") -> "Official"
        url.contains("linkedin.com") || url.contains("x.com") || url.contains("twitter.com") -> "Community"
        researchLinkScore(link) >= 80 -> "Strong evidence"
        else -> "Supporting"
    }
}

private fun researchLinkTierLabel(link: CardLink): String {
    return when (researchLinkTier(link)) {
        "Official" -> "Офіційне"
        "Community" -> "Практичний контекст"
        "Strong evidence" -> "Ключове джерело"
        else -> "Додаткове"
    }
}

private fun riskScore(card: DashboardCard): Int {
    var score = 20

    score += when (card.priority) {
        com.example.myapplication2.core.model.Priority.CRITICAL -> 36
        com.example.myapplication2.core.model.Priority.HIGH -> 24
        com.example.myapplication2.core.model.Priority.MEDIUM -> 12
    }

    score += (card.riskFlags.size.coerceAtMost(4) * 8)
    score += (card.actionChecklist.size.coerceAtMost(4) * 4)

    val urgencyBonus = when {
        card.urgencyLabel.contains("high", ignoreCase = true) -> 16
        card.urgencyLabel.contains("critical", ignoreCase = true) -> 20
        card.urgencyLabel.contains("medium", ignoreCase = true) -> 8
        card.urgencyLabel.contains("вис", ignoreCase = true) -> 16
        card.urgencyLabel.contains("термін", ignoreCase = true) -> 16
        else -> 0
    }

    return (score + urgencyBonus).coerceIn(0, 100)
}
