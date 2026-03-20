package com.example.myapplication2.presentation.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.Niche
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.AppSettingsRepository
import com.example.myapplication2.domain.repository.CacheRepository
import com.example.myapplication2.domain.repository.UserProfileRepository
import com.example.myapplication2.domain.usecase.GenerateRegulatoryCalendarUseCase
import com.example.myapplication2.domain.usecase.PinCardUseCase
import com.example.myapplication2.domain.usecase.RefreshCalendarUseCase
import com.example.myapplication2.domain.usecase.SaveUserProfileUseCase
import com.example.myapplication2.presentation.components.EmptyStatePanel
import com.example.myapplication2.presentation.components.ErrorStatePanel
import com.example.myapplication2.presentation.components.ModuleTopBar
import com.example.myapplication2.presentation.components.NicheSelectorView
import com.example.myapplication2.presentation.navigation.LocalAppContainer
import com.example.myapplication2.presentation.navigation.PendingCardHolder
import com.example.myapplication2.presentation.navigation.regulationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ViewMode { TIMELINE, CALENDAR, LIST }

enum class CalendarFilter(val label: String) {
    ALL("Усе"),
    REGULATIONS("Регуляції"),
    EVENTS("Події"),
}

private fun DashboardCard.isRegulation(): Boolean {
    val text = "${title.lowercase()} ${subtitle.lowercase()} ${body.lowercase()} ${links.firstOrNull()?.title?.lowercase() ?: ""}"
    val regulationKeywords = listOf("deadline", "mdcg", "eudamed", "mdr", "ivdr", "article", "regulation", "compliance", "nando", "annex", "guidance", "eur-lex", "commission", "legacy", "transition")
    val eventKeywords = listOf("conference", "webinar", "consultation", "workshop", "meeting", "seminar", "event", "summit")
    val hasRegulation = regulationKeywords.any { text.contains(it) }
    val hasEvent = eventKeywords.any { text.contains(it) }
    return when {
        hasRegulation && hasEvent -> true
        hasRegulation -> true
        hasEvent -> false
        else -> true
    }
}

data class CalendarUiState(
    val selectedNiches: List<String> = emptyList(),
    val cards: List<DashboardCard> = emptyList(),
    val isLoading: Boolean = false,
    val loadingStage: String = "",
    val error: String? = null,
    val viewMode: ViewMode = ViewMode.TIMELINE,
    val selectedMonthYear: Pair<Int, Int> = defaultMonthYear(),
    val fromDateMillis: Long = defaultFromMillis(),
    val toDateMillis: Long = defaultToMillis(),
    val filter: CalendarFilter = CalendarFilter.ALL,
)

private fun defaultMonthYear(): Pair<Int, Int> {
    val cal = java.util.Calendar.getInstance()
    return cal.get(java.util.Calendar.MONTH) to cal.get(java.util.Calendar.YEAR)
}

private fun defaultFromMillis(): Long =
    java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -1) }.timeInMillis

private fun defaultToMillis(): Long =
    java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, 3) }.timeInMillis

class CalendarViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val generateRegulatoryCalendarUseCase: GenerateRegulatoryCalendarUseCase,
    private val refreshCalendarUseCase: RefreshCalendarUseCase,
    private val pinCardUseCase: PinCardUseCase,
    private val cacheRepository: CacheRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
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
        _uiState.value = _uiState.value.copy(selectedNiches = current)
        viewModelScope.launch {
            appSettingsRepository.saveSelectedNiches(current)
            userProfileRepository.getUserProfile()?.let { profile ->
                saveUserProfileUseCase(profile.copy(niches = current))
            }
        }
    }

    fun loadCalendar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, loadingStage = "Loading...")
            val profile = userProfileRepository.getUserProfile()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Complete onboarding first.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(loadingStage = "Grok is searching for events...")
            runCatching {
                generateRegulatoryCalendarUseCase(_uiState.value.selectedNiches, profile)
            }.onSuccess { cards ->
                _uiState.value = _uiState.value.copy(cards = cards, isLoading = false, loadingStage = "")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = com.example.myapplication2.core.util.ErrorMessageHelper.userFriendlyMessage(e),
                    loadingStage = "",
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch { cacheRepository.clearAll() }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setSelectedMonthYear(month: Int, year: Int) {
        _uiState.value = _uiState.value.copy(selectedMonthYear = month to year)
    }

    fun setFromDateMillis(millis: Long) {
        _uiState.value = _uiState.value.copy(fromDateMillis = millis)
    }

    fun setToDateMillis(millis: Long) {
        _uiState.value = _uiState.value.copy(toDateMillis = millis)
    }

    fun setFilter(filter: CalendarFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRoute(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = regulationViewModel(),
    onOpenCard: (DashboardCard) -> Unit = {},
) {
    val appContainer = LocalAppContainer.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val profile by appContainer.userProfileRepo.observeUserProfile().collectAsState(initial = null)
    val profileLabel = remember(profile) {
        profile?.role?.split(" ")?.filter { it.isNotBlank() }?.take(2)
            ?.joinToString("") { it.take(1).uppercase() }?.ifBlank { "RA" } ?: "RA"
    }
    val notificationsGranted = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsGranted.value = it }

    var showNicheSheet by remember { mutableStateOf(false) }
    var showPeriodSheet by remember { mutableStateOf(false) }
    var showAllEvents by remember { mutableStateOf(false) }
    val filteredCards = remember(uiState.cards, uiState.filter) {
        when (uiState.filter) {
            CalendarFilter.ALL -> uiState.cards
            CalendarFilter.REGULATIONS -> uiState.cards.filter { it.isRegulation() }
            CalendarFilter.EVENTS -> uiState.cards.filter { !it.isRegulation() }
        }.sortedBy { it.dateMillis }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModuleTopBar(moduleTitle = "Calendar", profileLabel = profileLabel)

        // Disclaimer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "We show only events and regulation changes related to your selected niches (deadlines, MDCG updates, conferences). Select up to 5 niches before generating.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp),
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = { showNicheSheet = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select niche", style = MaterialTheme.typography.labelLarge)
                    Text("${uiState.selectedNiches.size}/5", style = MaterialTheme.typography.titleSmall)
                }
            }
            FilledTonalButton(
                onClick = { viewModel.loadCalendar() },
                enabled = uiState.selectedNiches.isNotEmpty() && !uiState.isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (uiState.isLoading) "..." else "Refresh now", style = MaterialTheme.typography.labelLarge)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { viewModel.clearCache() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear cache", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { showPeriodSheet = true }) {
                Icon(Icons.Default.Tune, contentDescription = "Period settings")
            }
        }

        if (showNicheSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNicheSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Before generating, select up to 5 niches. The calendar shows events only for your selection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NicheSelectorView(
                        selectedNiches = uiState.selectedNiches,
                        onToggleNiche = { viewModel.toggleNiche(it) },
                        title = "Which niches to track?",
                        subtitle = "Tap to select/deselect (max 5).",
                        compact = false,
                        useEnglish = true,
                    )
                    FilledTonalButton(onClick = { showNicheSheet = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }
            }
        }

        if (showPeriodSheet) {
            PeriodSettingsSheet(
                fromDateMillis = uiState.fromDateMillis,
                toDateMillis = uiState.toDateMillis,
                lastRefreshLabel = "—",
                eventCount = uiState.cards.size,
                onFromDateChange = { viewModel.setFromDateMillis(it) },
                onToDateChange = { viewModel.setToDateMillis(it) },
                onDismiss = { showPeriodSheet = false },
            )
        }

        // Notifications
        if (!notificationsGranted.value && uiState.selectedNiches.isNotEmpty()) {
            Card(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔔", style = MaterialTheme.typography.headlineMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifications", fontWeight = FontWeight.SemiBold)
                        Text("Get alerts for critical events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }) { Text("Enable") }
                }
            }
        }

        // Content
        when {
            uiState.selectedNiches.isEmpty() -> {
                EmptyStatePanel(
                    title = "Select niches first",
                    description = "Tap «Select niche» above and choose up to 5 niches. Then tap «Refresh now» to load events.",
                )
            }
            uiState.error != null -> {
                ErrorStatePanel(
                    title = "Error",
                    description = uiState.error!!,
                    actionLabel = "Retry",
                    onAction = { viewModel.loadCalendar() },
                )
            }
            uiState.isLoading -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(uiState.loadingStage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            uiState.cards.isEmpty() -> {
                EmptyStatePanel(
                    title = "No events yet",
                    description = "Tap «Refresh now» to search for regulatory events. Grok will find deadlines, MDCG updates and more.",
                    actionLabel = "Refresh",
                    onAction = { viewModel.loadCalendar() },
                )
            }
            else -> {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.viewMode == ViewMode.TIMELINE,
                        onClick = { viewModel.setViewMode(ViewMode.TIMELINE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    ) { Text("Timeline") }
                    SegmentedButton(
                        selected = uiState.viewMode == ViewMode.CALENDAR,
                        onClick = { viewModel.setViewMode(ViewMode.CALENDAR) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) { Text("Calendar") }
                    SegmentedButton(
                        selected = uiState.viewMode == ViewMode.LIST,
                        onClick = { viewModel.setViewMode(ViewMode.LIST) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    ) { Text("List") }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CalendarFilter.entries.forEachIndexed { idx, f ->
                        val count = when (f) {
                            CalendarFilter.ALL -> uiState.cards.size
                            CalendarFilter.REGULATIONS -> uiState.cards.count { it.isRegulation() }
                            CalendarFilter.EVENTS -> uiState.cards.count { !it.isRegulation() }
                        }
                        SegmentedButton(
                            selected = uiState.filter == f,
                            onClick = { viewModel.setFilter(f) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = CalendarFilter.entries.size),
                        ) { Text("${f.label} ($count)", style = MaterialTheme.typography.labelMedium) }
                    }
                }

                when (uiState.viewMode) {
                    ViewMode.CALENDAR -> {
                        var selectedDayMillis by remember { mutableStateOf<Long?>(null) }
                        val (month, year) = uiState.selectedMonthYear
                        val cal = Calendar.getInstance().apply { set(year, month, 1) }
                        val monthStart = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        val monthEnd = cal.timeInMillis
                        val monthEvents = filteredCards.filter { it.dateMillis in monthStart until monthEnd }
                        val dayStartEnd: (Long) -> Pair<Long, Long> = { dayMs ->
                            val c = Calendar.getInstance().apply { timeInMillis = dayMs }
                            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                            val start = c.timeInMillis
                            c.add(Calendar.DAY_OF_MONTH, 1)
                            start to c.timeInMillis
                        }
                        val selectedDayCards = remember(selectedDayMillis, filteredCards) {
                            selectedDayMillis?.let { dayMs ->
                                val (start, end) = dayStartEnd(dayMs)
                                filteredCards.filter { it.dateMillis in start until end }
                            } ?: emptyList()
                        }
                        CalendarGrid(
                            selectedMonthYear = uiState.selectedMonthYear,
                            events = monthEvents,
                            onMonthChange = { m, y -> viewModel.setSelectedMonthYear(m, y); selectedDayMillis = null },
                            onDayClick = { selectedDayMillis = it },
                        )
                        if (selectedDayCards.isNotEmpty()) {
                            Text("Events", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            selectedDayCards.forEach { card ->
                                EventRowCard(card = card, onClick = { PendingCardHolder.card = card; onOpenCard(card) })
                            }
                        }
                        Text(
                            "All (${filteredCards.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        filteredCards.forEach { card ->
                            EventRowCard(card = card, onClick = { PendingCardHolder.card = card; onOpenCard(card) })
                        }
                    }
                    ViewMode.LIST -> {
                        filteredCards.forEach { card ->
                            EventRowCard(
                                card = card,
                                onClick = { PendingCardHolder.card = card; onOpenCard(card) },
                            )
                        }
                    }
                    ViewMode.TIMELINE -> {
                        TextButton(
                            onClick = { showAllEvents = !showAllEvents },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (showAllEvents) "Group by priority" else "Show all (${filteredCards.size})",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        val criticalCards = filteredCards.filter { it.priority == Priority.CRITICAL }
                        val highCards = filteredCards.filter { it.priority == Priority.HIGH }
                        val otherCards = filteredCards.filter { it.priority == Priority.MEDIUM }
                        if (showAllEvents) {
                            CalendarSection("All", filteredCards, onOpenCard)
                        } else {
                            if (criticalCards.isNotEmpty()) CalendarSection("Critical", criticalCards, onOpenCard)
                            if (highCards.isNotEmpty()) CalendarSection("Important", highCards, onOpenCard)
                            if (otherCards.isNotEmpty()) CalendarSection("Monitoring", otherCards, onOpenCard)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSection(
    title: String,
    cards: List<DashboardCard>,
    onOpenCard: (DashboardCard) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        cards.forEach { card ->
            CalendarEventCard(
                card = card,
                onClick = {
                    PendingCardHolder.card = card
                    onOpenCard(card)
                },
                onOpenLink = { card.links.firstOrNull()?.let { uriHandler.openUri(it.url) } },
            )
        }
    }
}

@Composable
private fun CalendarEventCard(
    card: DashboardCard,
    onClick: () -> Unit,
    onOpenLink: () -> Unit,
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(card.dateMillis))
    val priorityColor = when (card.priority) {
        Priority.CRITICAL -> MaterialTheme.colorScheme.error
        Priority.HIGH -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = priorityColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (card.priority) {
                        Priority.CRITICAL -> "Critical"
                        Priority.HIGH -> "Important"
                        else -> "Monitoring"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.body.isNotBlank()) {
                Text(
                    text = card.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (card.links.isNotEmpty()) {
                Text(
                    text = card.links.first().title.ifBlank { "Open source" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenLink() },
                )
            }
        }
    }
}
