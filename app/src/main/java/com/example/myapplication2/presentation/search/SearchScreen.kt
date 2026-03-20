@file:OptIn(ExperimentalLayoutApi::class)
package com.example.myapplication2.presentation.search

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.common.NicheQueryCatalog
import com.example.myapplication2.core.model.*
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.presentation.components.SearchHistoryModuleCard
import com.example.myapplication2.presentation.components.SectionHeader
import com.example.myapplication2.presentation.components.ShimmerBox
import com.example.myapplication2.presentation.components.toReadableDate
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Markdown cleanup ───────────────────────────────────────────────────────────
private fun String.cleanMarkdown(): String = this
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("\\*(.+?)\\*"), "$1")
    .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("`(.+?)`"), "$1")
    .trim()

// ── ViewModel ──────────────────────────────────────────────────────────────────

class SearchViewModel(private val container: AppContainer) : ViewModel() {

    var query by mutableStateOf("")
        private set
    var isSearching by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var currentResearch by mutableStateOf<ResearchResult?>(null)
        private set
    var activeTab by mutableIntStateOf(0)
        private set
    var profile by mutableStateOf<UserProfile?>(null)
        private set

    val history: StateFlow<List<DashboardCard>> = container.observeSearchHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            profile = container.userProfileRepository.getUserProfile()
        }
    }

    fun onQueryChange(value: String) { query = value }

    fun onTabChange(tab: Int) { activeTab = tab }

    fun clearError() { error = null }

    /** Start a research job, cancelling any in-flight one first */
    fun search(overrideQuery: String? = null) {
        val q = (overrideQuery ?: query).trim().takeIf { it.isNotBlank() } ?: return
        if (overrideQuery != null) query = overrideQuery

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            isSearching = true
            error = null
            currentResearch = null
            activeTab = 0

            val p = profile
                ?: container.userProfileRepository.getUserProfile()?.also { profile = it }
                ?: run {
                    error = "Profile not set up. Complete onboarding."
                    isSearching = false
                    return@launch
                }

            runCatching {
                container.researchUseCase(q, p)
            }.fold(
                onSuccess = { currentResearch = it },
                onFailure = { error = friendlyError(it) },
            )
            isSearching = false
        }
    }

    fun clearResearch() {
        searchJob?.cancel()
        currentResearch = null
        query = ""
        error = null
    }

    fun deleteHistory(cardId: String) {
        viewModelScope.launch { container.deleteCardUseCase(cardId) }
    }

    fun pinCard(cardId: String, pinned: Boolean) {
        viewModelScope.launch { container.pinCardUseCase(cardId, pinned) }
    }

    fun loadFromCard(card: DashboardCard) {
        val parsed = runCatching {
            AppJson.decodeFromString<ResearchResult>(card.body)
        }.getOrNull()
        if (parsed != null) {
            currentResearch = parsed
            query = card.searchQuery
            activeTab = 0
        }
    }

    private fun friendlyError(e: Throwable): String = when (e) {
        is com.example.myapplication2.data.remote.GrokError.Unauthorized ->
            "Invalid or missing OpenAI API key. Key is set in secrets.properties (OPENAI_API_KEY)."
        is com.example.myapplication2.data.remote.GrokError.RateLimited ->
            "OpenAI API rate limit exceeded. Wait a few minutes and try again."
        is com.example.myapplication2.data.remote.GrokError.NetworkError ->
            "No internet connection. Check your network."
        is com.example.myapplication2.data.remote.GrokError.EmptyResponse ->
            "Server returned empty response. Try again."
        is com.example.myapplication2.data.remote.GrokError.ParseError ->
            "Failed to parse response. Try rephrasing your query."
        is com.example.myapplication2.data.remote.GrokError.ServerError ->
            "OpenAI server error. Try again in a few minutes."
        else -> when {
            e.message?.contains("cancel", ignoreCase = true) == true -> ""
            else -> e.message?.take(100) ?: "Unknown error"
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(vm: SearchViewModel, onCardClick: (String) -> Unit) {
    val history by vm.history.collectAsState()
    val focusManager = LocalFocusManager.current

    val autocomplete = remember(vm.profile, vm.query) {
        if (vm.query.length < 2) return@remember emptyList()
        NicheQueryCatalog
            .getAutocompleteSuggestions(
                vm.profile?.niches ?: emptyList(),
                vm.profile?.country ?: "",
                vm.profile?.sector ?: "",
            )
            .filter { it.contains(vm.query, ignoreCase = true) }
            .take(5)
    }

    val queryGroups = remember(vm.profile) {
        NicheQueryCatalog.getQueriesForProfile(
            vm.profile?.niches ?: emptyList(),
            vm.profile?.country ?: "",
            vm.profile?.sector ?: "",
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(ContentBg)) {

        SearchBarSection(
            query = vm.query,
            onQueryChange = vm::onQueryChange,
            isSearching = vm.isSearching,
            hasResult = vm.currentResearch != null,
            onSearch = { focusManager.clearFocus(); vm.search() },
            onClear = vm::clearResearch,
            jurisdictionName = CountryRegulatoryContext.forCountry(vm.profile?.country ?: "").jurisdictionName,
        )

        // Autocomplete
        AnimatedVisibility(
            autocomplete.isNotEmpty() && !vm.isSearching && vm.currentResearch == null && vm.query.length >= 2,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Surface(shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Column {
                    autocomplete.forEach { s ->
                        val idx = s.indexOf(vm.query, ignoreCase = true)
                        ListItem(
                            headlineContent = {
                                Text(buildAnnotatedString {
                                    if (idx >= 0) {
                                        append(s.take(idx))
                                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryGreen))
                                        append(s.substring(idx, idx + vm.query.length))
                                        pop()
                                        append(s.substring(idx + vm.query.length))
                                    } else append(s)
                                })
                            },
                            leadingContent = { Icon(Icons.Filled.Search, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp)) },
                            trailingContent = { Icon(Icons.Filled.NorthWest, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.clickable { focusManager.clearFocus(); vm.search(s) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }

        // Error banner
        val errMsg = vm.error
        if (!errMsg.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = CrimsonRed.copy(alpha = 0.1f),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                    Text(errMsg, style = MaterialTheme.typography.bodySmall, color = CrimsonRed, modifier = Modifier.weight(1f))
                    IconButton(onClick = vm::clearError, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        when {
            vm.isSearching -> SearchLoadingState(
                query = vm.query,
                jurisdictionName = CountryRegulatoryContext.forCountry(vm.profile?.country ?: "").jurisdictionName,
            )
            vm.currentResearch != null -> ResearchResultView(
                research = vm.currentResearch!!,
                activeTab = vm.activeTab,
                onTabChange = vm::onTabChange,
                onRelatedQuery = { focusManager.clearFocus(); vm.search(it) },
            )
            else -> SuggestionsAndHistory(
                queryGroups = queryGroups,
                history = history,
                onQueryClick = { focusManager.clearFocus(); vm.search(it) },
                onHistoryClick = { card ->
                    vm.loadFromCard(card)
                    if (vm.currentResearch == null) onCardClick(card.id)
                },
                onDeleteHistory = vm::deleteHistory,
                onPin = vm::pinCard,
            )
        }
    }
}

// ── Search Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun SearchBarSection(
    query: String, onQueryChange: (String) -> Unit,
    isSearching: Boolean, hasResult: Boolean,
    onSearch: () -> Unit, onClear: () -> Unit,
    jurisdictionName: String,
) {
    Surface(shape = RectangleShape, color = PureWhite, shadowElevation = 2.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!hasResult) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    Text("AI Research", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Full $jurisdictionName analysis — any niche, any question",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (hasResult) "New query..." else "E.g.: SaMD Rule 11 classification...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    if (isSearching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                    else Icon(Icons.Filled.Search, null, tint = PrimaryGreen)
                },
                trailingIcon = {
                    if (hasResult || query.isNotBlank()) {
                        IconButton(onClick = if (hasResult) onClear else ({ onQueryChange("") })) {
                            Icon(if (hasResult) Icons.Filled.Close else Icons.Filled.Clear, null)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TabSelected,
                    unfocusedBorderColor = BorderGreen,
                    cursorColor = TabSelected,
                    focusedTextColor = TextOnLight,
                    unfocusedTextColor = TextOnLight,
                    focusedLeadingIconColor = SearchBarIcon,
                    unfocusedLeadingIconColor = SearchBarIcon,
                    focusedContainerColor = SearchBarBg,
                    unfocusedContainerColor = SearchBarBg,
                ),
            )
            AnimatedVisibility(!hasResult && !isSearching && query.isNotBlank()) {
                Button(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Research with AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Loading State ──────────────────────────────────────────────────────────────

@Composable
private fun SearchLoadingState(query: String, jurisdictionName: String) {
    val stages = listOf(
        "Analyzing regulatory context...",
        "Searching $jurisdictionName articles and guidance...",
        "Assessing risks and impacts...",
        "Searching LinkedIn, Reddit, RAPS discussions...",
        "Building action plan...",
    )
    var stageIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_400)
            stageIdx = (stageIdx + 1) % stages.size
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(44.dp), strokeWidth = 3.dp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Researching:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("\"$query\"", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            AnimatedContent(targetState = stages[stageIdx], transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }) { stage ->
                Text(stage, style = MaterialTheme.typography.bodyMedium, color = PrimaryGreen)
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { i ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(if (i == 1) 0.6f else 0.82f).height(10.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.95f).height(8.dp))
                        if (i == 0) ShimmerBox(Modifier.fillMaxWidth(0.7f).height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Research Result View ───────────────────────────────────────────────────────

private data class TabInfo(val label: String, val icon: ImageVector)

@Composable
private fun ResearchResultView(
    research: ResearchResult,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onRelatedQuery: (String) -> Unit,
) {
    val tabs = buildList {
        add(TabInfo("Overview", Icons.Filled.Dashboard))
        add(TabInfo("Documents", Icons.Filled.Article))
        add(TabInfo("Risks", Icons.Filled.Shield))
        add(TabInfo("Actions", Icons.Filled.Checklist))
        if (research.socialSummary != null) add(TabInfo("Social", Icons.Filled.Forum))
        add(TabInfo("Related", Icons.Filled.AccountTree))
    }

    // After loading another card, Social may disappear — clamp tab index to avoid illegal branches
    SideEffect {
        if (activeTab > tabs.lastIndex) onTabChange(tabs.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeTab.coerceAtMost(tabs.lastIndex),
            edgePadding = 16.dp,
            containerColor = PureWhite,
            divider = { HorizontalDivider(color = BorderGreen.copy(alpha = 0.3f)) },
        ) {
            tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = activeTab == i,
                    onClick = { onTabChange(i) },
                    icon = { Icon(tab.icon, null, Modifier.size(16.dp), tint = if (activeTab == i) TabSelected else TabUnselected) },
                    text = {
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (activeTab == i) TabSelected else TabUnselected,
                            fontWeight = if (activeTab == i) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.background(ContentBg),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ResearchHeader(research) }

            // Map tab index → content, skipping social if absent
            val hasSocial = research.socialSummary != null
            val safeTab = activeTab.coerceIn(0, if (hasSocial) 5 else 4)
            when (safeTab) {
                0 -> overviewItems(research)
                1 -> documentsItems(research)
                2 -> risksItems(research)
                3 -> actionsItems(research)
                4 -> if (hasSocial) socialItems(research) else relatedItems(research, onRelatedQuery)
                5 -> relatedItems(research, onRelatedQuery)
            }
        }
    }
}

// ── Research Header ────────────────────────────────────────────────────────────

@Composable
private fun ResearchHeader(r: ResearchResult) {
    val scoreColor = when {
        r.confidenceScore >= 85 -> AccuracyText
        r.confidenceScore >= 70 -> AmberOrange
        else -> RequiredRed
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBgWhite,
        border = BorderStroke(1.dp, BorderGreen.copy(alpha = 0.3f)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("\"${r.query}\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (r.timestampMillis > 0L) Text(r.timestampMillis.toReadableDate(), style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = if (r.confidenceScore >= 85) AccuracyBg else scoreColor.copy(alpha = 0.12f)) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${r.confidenceScore}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (r.confidenceScore >= 85) AccuracyText else scoreColor)
                        Text("accuracy", style = MaterialTheme.typography.labelSmall, color = if (r.confidenceScore >= 85) AccuracyText else scoreColor)
                    }
                }
            }
            if (r.complianceDeadline.isNotBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = RequiredBg) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = RequiredRed, modifier = Modifier.size(14.dp))
                        Text("Deadline: ${r.complianceDeadline}", style = MaterialTheme.typography.labelMedium, color = RequiredRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (r.affectedNiches.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.affectedNiches.take(3).forEach { n ->
                        Surface(shape = RoundedCornerShape(6.dp), color = TagTealBg) {
                            Text(n.take(24), style = MaterialTheme.typography.labelSmall, color = TagTealText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Shared card composable ─────────────────────────────────────────────────────

@Composable
private fun ResearchBox(
    icon: ImageVector,
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
            content()
        }
    }
}

// ── Tab: Overview ──────────────────────────────────────────────────────────────

private fun LazyListScope.overviewItems(r: ResearchResult) {
    if (r.executiveSummary.isNotBlank()) {
        item { ResearchBox(Icons.Filled.Summarize, "Summary", PrimaryGreen) { Text(r.executiveSummary, style = MaterialTheme.typography.bodyLarge) } }
    }
    if (r.regulatoryContext.isNotBlank()) {
        item { ResearchBox(Icons.Filled.Gavel, "Regulatory context", AppGreen) { Text(r.regulatoryContext, style = MaterialTheme.typography.bodyMedium) } }
    }
    if (r.keyFindings.isNotEmpty()) {
        item {
            ResearchBox(Icons.Filled.Lightbulb, "Key findings", EmeraldGreen) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    r.keyFindings.forEachIndexed { i, f ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Surface(shape = CircleShape, color = EmeraldGreen.copy(alpha = 0.15f), modifier = Modifier.size(22.dp)) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                            }
                            Text(f, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (r.expertInsight.isNotBlank()) {
        item { ResearchBox(Icons.Filled.Person, "Expert insight", AmberOrange) { Text(r.expertInsight, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic) } }
    }
    if (r.marketContext.isNotBlank()) {
        item { ResearchBox(Icons.Filled.TrendingUp, "Market context 2025", PrimaryGreen) { Text(r.marketContext, style = MaterialTheme.typography.bodyMedium) } }
    }
}

// ── Tab: Documents ─────────────────────────────────────────────────────────────

private fun LazyListScope.documentsItems(r: ResearchResult) {
    val grouped = r.applicableDocuments.groupBy { it.type }
    val typeOrder = listOf(DocType.REGULATION, DocType.GUIDANCE, DocType.STANDARD, DocType.TEMPLATE, DocType.DATABASE, DocType.DECISION)
    typeOrder.forEach { type ->
        val docs = grouped[type] ?: return@forEach
        item(key = "dtype_$type") {
            val (label, icon, color) = when (type) {
                DocType.REGULATION -> Triple("EU Regulation — Required", Icons.Filled.Gavel, RequiredRed)
                DocType.GUIDANCE -> Triple("MDCG Guidance", Icons.Filled.Description, TagTealText)
                DocType.STANDARD -> Triple("ISO/IEC/EN Standards", Icons.Filled.Verified, EmeraldGreen)
                DocType.TEMPLATE -> Triple("Templates & forms", Icons.Filled.TableChart, AmberOrange)
                DocType.DATABASE -> Triple("Databases & registries", Icons.Filled.Storage, AppGreen)
                DocType.DECISION -> Triple("Decisions & precedents", Icons.Filled.AccountBalance, TagTealText)
            }
            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            }
        }
        docs.forEach { doc -> item(key = "doc_${doc.code}") { DocumentCard(doc) } }
    }
    if (r.sources.isNotEmpty()) {
        item {
            ResearchBox(Icons.Filled.Book, "Sources", MaterialTheme.colorScheme.onSurfaceVariant) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    r.sources.forEach { s ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(s, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(doc: RegulatoryDocument) {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBgWhite,
        border = BorderStroke(1.dp, if (doc.isBinding) RequiredBorder else BorderGreen.copy(alpha = 0.25f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(doc.code, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TagTealText)
                Text(doc.title, style = MaterialTheme.typography.bodyMedium, color = TextOnLight)
                if (doc.isBinding) {
                    Surface(shape = RoundedCornerShape(4.dp), color = RequiredBg) {
                        Text("Required", style = MaterialTheme.typography.labelSmall, color = RequiredRed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            if (doc.url.isNotBlank()) {
                IconButton(onClick = { runCatching { uriHandler.openUri(doc.url) } }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.OpenInNew, null, tint = LinkIconGrey, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Tab: Risks ─────────────────────────────────────────────────────────────────

private fun LazyListScope.risksItems(r: ResearchResult) {
    if (r.riskMatrix.isEmpty()) {
        item { EmptyTabPlaceholder(Icons.Filled.Shield, "No risks identified") }; return
    }
    item {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceEvenly) {
                listOf(
                    Triple("Critical", r.riskMatrix.count { it.severity == RiskLevel.HIGH }, CrimsonRed),
                    Triple("Medium", r.riskMatrix.count { it.severity == RiskLevel.MEDIUM }, AmberOrange),
                    Triple("Low", r.riskMatrix.count { it.severity == RiskLevel.LOW }, EmeraldGreen),
                ).forEach { (l, c, col) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$c", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = col)
                        Text(l, style = MaterialTheme.typography.labelSmall, color = col)
                    }
                }
            }
        }
    }
    r.riskMatrix.forEachIndexed { i, risk ->
        item(key = "risk_$i") {
            val sc = risk.severity.color()
            Surface(shape = RoundedCornerShape(12.dp), color = sc.copy(alpha = 0.06f), border = BorderStroke(1.dp, sc.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = sc, modifier = Modifier.size(16.dp))
                        Text(risk.scenario, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = sc.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = sc, modifier = Modifier.size(14.dp))
                            Text(risk.consequence, style = MaterialTheme.typography.bodySmall, color = sc, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RiskPill("Likelihood", risk.likelihood)
                        RiskPill("Severity", risk.severity)
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskPill(label: String, level: RiskLevel) {
    val col = level.color()
    Surface(shape = RoundedCornerShape(6.dp), color = col.copy(alpha = 0.1f)) {
        Text("$label: ${level.label()}", style = MaterialTheme.typography.labelSmall, color = col, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun RiskLevel.color() = when (this) {
    RiskLevel.HIGH -> CrimsonRed
    RiskLevel.MEDIUM -> AmberOrange
    RiskLevel.LOW -> EmeraldGreen
}
private fun RiskLevel.label() = when (this) {
    RiskLevel.HIGH -> "High"; RiskLevel.MEDIUM -> "Medium"; RiskLevel.LOW -> "Low"
}

// ── Tab: Actions ───────────────────────────────────────────────────────────────

private fun LazyListScope.actionsItems(r: ResearchResult) {
    if (r.actionPlan.isEmpty()) {
        item { EmptyTabPlaceholder(Icons.Filled.Checklist, "No action plan identified") }; return
    }
    val blocking = r.actionPlan.count { it.isBlocking }
    if (blocking > 0) {
        item {
            Surface(shape = RoundedCornerShape(12.dp), color = EmeraldGreen.copy(alpha = 0.07f), border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.2f))) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Text("$blocking critical steps — required for compliance", style = MaterialTheme.typography.bodySmall, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    r.actionPlan.forEachIndexed { i, step ->
        item(key = "step_${step.step}") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = if (step.isBlocking) EmeraldGreen else PrimaryGreen.copy(alpha = 0.2f), modifier = Modifier.size(32.dp)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("${step.step}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (step.isBlocking) Color.White else PrimaryGreen)
                        }
                    }
                    if (i < r.actionPlan.lastIndex) {
                        Box(Modifier.width(2.dp).height(16.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (step.isBlocking) EmeraldGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (step.isBlocking) EmeraldGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f).padding(bottom = if (i < r.actionPlan.lastIndex) 12.dp else 0.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                            Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (step.isBlocking) {
                                Surface(shape = RoundedCornerShape(4.dp), color = CrimsonRed.copy(alpha = 0.1f)) {
                                    Text("Critical", style = MaterialTheme.typography.labelSmall, color = CrimsonRed, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, null, tint = AmberOrange, modifier = Modifier.size(12.dp))
                                Text(step.timeframe, style = MaterialTheme.typography.labelSmall, color = AmberOrange)
                            }
                            if (step.owner.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Text(step.owner, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tab: Social ────────────────────────────────────────────────────────────────

private fun LazyListScope.socialItems(r: ResearchResult) {
    val social = r.socialSummary ?: return

    if (social.overallSentiment.isNotBlank()) {
        item {
            ResearchBox(Icons.Filled.RecordVoiceOver, "Community sentiment", AppGreen) {
                Text(social.overallSentiment.cleanMarkdown(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (social.topConcerns.isNotEmpty() || social.topInsights.isNotEmpty()) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (social.topConcerns.isNotEmpty()) {
                    PillColumn("Concerns", Icons.Filled.ReportProblem, CrimsonRed, social.topConcerns, Modifier.weight(1f))
                }
                if (social.topInsights.isNotEmpty()) {
                    PillColumn("Insights", Icons.Filled.TipsAndUpdates, EmeraldGreen, social.topInsights, Modifier.weight(1f))
                }
            }
        }
    }

    item {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Discussions (${social.discussions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(8.dp), color = PrimaryGreen.copy(alpha = 0.08f)) {
                Text("LinkedIn · Reddit · RAPS · Forums", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }

    social.discussions.forEachIndexed { i, d ->
        item(key = "disc_$i") { SocialCard(d) }
    }
}

@Composable
private fun PillColumn(title: String, icon: ImageVector, color: Color, items: List<String>, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(12.dp), color = color.copy(alpha = 0.06f), border = BorderStroke(1.dp, color.copy(alpha = 0.15f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
            }
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(4.dp).padding(top = 6.dp).clip(CircleShape).background(color.copy(alpha = 0.6f)))
                    Text(item.cleanMarkdown(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SocialCard(d: SocialDiscussion) {
    val uriHandler = LocalUriHandler.current
    val (platformLabel, platformColor, platformIcon) = platformMeta(d.platform)
    val sentimentColor = d.sentiment.color()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, platformColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = platformColor.copy(alpha = 0.1f)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(platformIcon, null, tint = platformColor, modifier = Modifier.size(13.dp))
                        Text(platformLabel, style = MaterialTheme.typography.labelSmall, color = platformColor, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = sentimentColor.copy(alpha = 0.1f)) {
                        Text(d.sentiment.label(), style = MaterialTheme.typography.labelSmall, color = sentimentColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (d.engagement.isNotBlank()) Text(d.engagement, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(d.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (d.snippet.isNotBlank()) Text(d.snippet.cleanMarkdown(), style = MaterialTheme.typography.bodySmall)

            if (d.keyTakeaway.isNotBlank()) {
                HorizontalDivider(color = platformColor.copy(alpha = 0.2f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.FormatQuote, null, tint = platformColor, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Text(d.keyTakeaway.cleanMarkdown(), style = MaterialTheme.typography.bodySmall, color = platformColor, fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f))
                }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    if (d.author.isNotBlank()) Text(d.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (d.postedDate.isNotBlank()) Text(d.postedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                if (d.url.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { runCatching { uriHandler.openUri(d.url) } },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = platformColor.copy(alpha = 0.12f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.OpenInNew, null, tint = platformColor, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open", style = MaterialTheme.typography.labelSmall, color = platformColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscussionSentiment.color() = when (this) {
    DiscussionSentiment.CONCERN, DiscussionSentiment.URGENT -> CrimsonRed
    DiscussionSentiment.POSITIVE -> EmeraldGreen
    DiscussionSentiment.DEBATE -> AmberOrange
    DiscussionSentiment.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}
private fun DiscussionSentiment.label() = when (this) {
    DiscussionSentiment.CONCERN -> "Concern"
    DiscussionSentiment.URGENT -> "Urgent"
    DiscussionSentiment.POSITIVE -> "Positive"
    DiscussionSentiment.DEBATE -> "Debate"
    DiscussionSentiment.NEUTRAL -> "Info"
}

@Composable
private fun platformMeta(p: SocialPlatform): Triple<String, Color, ImageVector> = when (p) {
    SocialPlatform.LINKEDIN -> Triple("LinkedIn", AppGreen, Icons.Filled.Work)
    SocialPlatform.REDDIT -> Triple("Reddit", AppGreen, Icons.Filled.Forum)
    SocialPlatform.TWITTER_X -> Triple("X / Twitter", AppGreen, Icons.Filled.AlternateEmail)
    SocialPlatform.RAPS -> Triple("RAPS", AppGreen, Icons.Filled.MenuBook)
    SocialPlatform.MEDTECH_FORUM -> Triple("MedTech Forum", AppGreen, Icons.Filled.Biotech)
    SocialPlatform.ELSEVIERCONNECT -> Triple("Elsevier", AppGreen, Icons.Filled.Science)
    SocialPlatform.MDR_NEWS -> Triple("MDR News", AppGreen, Icons.Filled.Newspaper)
    SocialPlatform.REGULATORY_FOCUS -> Triple("Regulatory Focus", AppGreen, Icons.Filled.Article)
    SocialPlatform.OTHER -> Triple("Forum", AppGreen, Icons.Filled.Language)
}

// ── Tab: Related ───────────────────────────────────────────────────────────────

private fun LazyListScope.relatedItems(r: ResearchResult, onRelatedQuery: (String) -> Unit) {
    if (r.relatedQueries.isNotEmpty()) {
        item { Text("Related research", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        r.relatedQueries.forEachIndexed { i, q ->
            item(key = "rq_$i") {
                OutlinedCard(onClick = { onRelatedQuery(q) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = PrimaryGreen.copy(alpha = 0.1f)) {
                            Icon(Icons.Filled.Search, null, tint = PrimaryGreen, modifier = Modifier.padding(6.dp).size(16.dp))
                        }
                        Text(q, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowForward, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
    if (r.affectedNiches.isNotEmpty()) {
        item {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(4.dp))
                Text("Affected niches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    r.affectedNiches.forEach { n ->
                        Surface(shape = RoundedCornerShape(20.dp), color = PrimaryGreen.copy(alpha = 0.1f), border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))) {
                            Text(n, style = MaterialTheme.typography.labelMedium, color = PrimaryGreen, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Suggestions & History ──────────────────────────────────────────────────────

@Composable
private fun SuggestionsAndHistory(
    queryGroups: List<NicheQueryCatalog.QueryGroup>,
    history: List<DashboardCard>,
    onQueryClick: (String) -> Unit,
    onHistoryClick: (DashboardCard) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        if (history.isNotEmpty()) {
            item {
                SectionHeader("Previous research", "${history.size} saved", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
            items(history.take(3), key = { it.id }) { card ->
                SearchHistoryModuleCard(
                    card = card,
                    onCardClick = { _ -> onHistoryClick(card) },
                    onPin = onPin,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    showManagementActions = true,
                    onOpenInNew = { onHistoryClick(card) },
                    onDelete = { onDeleteHistory(card.id) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        queryGroups.forEach { group ->
            item(key = "qg_${group.category}") { QueryGroupSection(group, onQueryClick) }
        }
    }
}

@Composable
private fun QueryGroupSection(group: NicheQueryCatalog.QueryGroup, onQueryClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 10.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(group.icon, style = MaterialTheme.typography.titleLarge)
                Text(group.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(expanded) {
            Column {
                group.queries.forEach { q ->
                    ListItem(
                        headlineContent = { Text(q, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Filled.TrendingUp, null, tint = PrimaryGreen.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) },
                        trailingContent = { Icon(Icons.Filled.NorthWest, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                        modifier = Modifier.clickable { onQueryClick(q) },
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    }
}

// ── Utility composables ────────────────────────────────────────────────────────

@Composable
private fun EmptyTabPlaceholder(icon: ImageVector, message: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
