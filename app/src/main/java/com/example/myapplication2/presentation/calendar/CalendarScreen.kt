package com.example.myapplication2.presentation.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.Niche
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.data.repository.CalendarRadarWorker
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.presentation.components.*
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel(private val container: AppContainer) : ViewModel() {
    val profile = container.observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<DashboardCard>> =
        container.observeCardsByTypeUseCase(CardType.REGULATORY_EVENT)
            .map { it.sortedBy { c -> c.dateMillis } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var isLoading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var selectedFilter by mutableStateOf<Priority?>(null)
    var selectedNiche by mutableStateOf<String?>(null)
    private var refreshJob: Job? = null

    /**
     * Call when the calendar tab becomes visible. Runs API refresh only if never refreshed
     * or last successful refresh was at least [CALENDAR_AUTO_REFRESH_INTERVAL_MS] ago.
     * Manual [refresh] ignores this interval.
     */
    fun onCalendarScreenEntered() {
        viewModelScope.launch {
            val last = container.appSettingsRepository.getLastCalendarRefreshMillis()
            val now = System.currentTimeMillis()
            if (last > 0L && now - last < CALENDAR_AUTO_REFRESH_INTERVAL_MS) return@launch
            enqueueRefresh(nicheKey = null, runRadarWorker = true)
        }
    }

    /** Niche chips: filter only — no network (calendar sync is first visit + daily). */
    fun selectNiche(nicheKey: String?) {
        selectedNiche = nicheKey
    }

    /** User-initiated full reload; always runs and updates last refresh time on success (use case). */
    fun refresh(nicheKey: String? = null) {
        enqueueRefresh(nicheKey = nicheKey, runRadarWorker = true)
    }

    private fun enqueueRefresh(nicheKey: String?, runRadarWorker: Boolean) {
        if (nicheKey != null) selectedNiche = nicheKey
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                isLoading = true
                error = null
                val profile = container.userProfileRepository.getUserProfile() ?: run {
                    error = "Profile not found. Complete settings."
                    return@launch
                }
                if (runRadarWorker) {
                    runCatching { CalendarRadarWorker.scheduleImmediate(container.appContext) }
                }
                val niches = (nicheKey ?: selectedNiche)?.let { listOf(it) } ?: profile.niches
                container.generateCalendarUseCase(niches, profile).onFailure { err -> error = err.message }
            } finally {
                isLoading = false
            }
        }
    }

    fun pin(cardId: String, pinned: Boolean) = viewModelScope.launch { container.pinCardUseCase(cardId, pinned) }
    fun dismissError() { error = null }
    /** Country (jurisdiction) + profile niches; optional single-niche chip narrows further. */
    fun filteredEvents(events: List<DashboardCard>, profile: UserProfile?): List<DashboardCard> {
        val byJurisdiction = events.filter { e ->
            if (profile == null) return@filter true
            CountryRegulatoryContext.calendarEventMatchesProfile(e.jurisdictionKey, profile.country)
        }
        val byNiche = when {
            selectedNiche != null -> byJurisdiction.filter { card ->
                card.niche.equals(selectedNiche, ignoreCase = true) || (selectedNiche!! in card.niche)
            }
            profile != null -> byJurisdiction.filter { CountryRegulatoryContext.matchesProfileNiches(it, profile) }
            else -> byJurisdiction
        }
        return if (selectedFilter == null) byNiche else byNiche.filter { it.priority == selectedFilter }
    }
    fun criticalCount(events: List<DashboardCard>) = events.count { it.priority == Priority.CRITICAL }
    fun upcomingCount(events: List<DashboardCard>) = events.count { it.dateMillis >= System.currentTimeMillis() }
    fun thisMonthCount(events: List<DashboardCard>): Int {
        val cal = Calendar.getInstance()
        return events.count {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            c.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && c.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
        }
    }

    private companion object {
        const val CALENDAR_AUTO_REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel, onCardClick: (String) -> Unit) {
    LaunchedEffect(Unit) { vm.onCalendarScreenEntered() }
    val events by vm.events.collectAsState()
    val profile by vm.profile.collectAsState()
    val jurisdictionName = CountryRegulatoryContext.forCountry(profile?.country ?: "").jurisdictionName
    val filtered = vm.filteredEvents(events, profile)
    val nicheChips = remember(profile) {
        val keys = profile?.niches.orEmpty()
        if (keys.isEmpty()) NicheCatalog.all
        else keys.map { key ->
            NicheCatalog.findByKeyOrName(key)
                ?: Niche(SectorCatalog.DEFAULT_KEY, key, key, key)
        }
    }
    val today = remember { Calendar.getInstance() }
    val grouped = remember(filtered) {
        filtered.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}"
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Header (reference: light background, dark text)
        Box(Modifier.fillMaxWidth().background(PureWhite)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Regulatory Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PrimaryTextDark)
                        Text("$jurisdictionName deadlines and events", style = MaterialTheme.typography.bodySmall, color = SecondaryTextMedium)
                        Text(
                            "Events: ${filtered.size} · ${profile?.niches?.size ?: 0} niches in profile",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryTextMedium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    FilledIconButton(onClick = { vm.refresh(vm.selectedNiche) }, enabled = !vm.isLoading,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = LightTealBg, contentColor = AccentTealMain)) {
                        CalendarRefreshGlyph(loading = vm.isLoading)
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Niche selector
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // All niches
                    item {
                        val selected = vm.selectedNiche == null
                        FilterChip(
                            selected = selected,
                            onClick = { vm.selectNiche(null) },
                            label = { Text("🌍 All niches", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTealMain,
                                selectedLabelColor = PureWhite,
                                containerColor = PureWhite,
                                labelColor = PrimaryTextDark,
                            ),
                        )
                    }
                    items(nicheChips, key = { it.promptKey }) { niche ->
                        val selected = vm.selectedNiche == niche.promptKey
                        FilterChip(
                            selected = selected,
                            onClick = { vm.selectNiche(if (selected) null else niche.promptKey) },
                            label = {
                                Text(
                                    "${niche.icon} ${niche.nameEn}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTealMain,
                                selectedLabelColor = PureWhite,
                                containerColor = PureWhite,
                                labelColor = PrimaryTextDark,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Priority filter
                LazyRow(contentPadding = PaddingValues(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = vm.selectedFilter == null, onClick = { vm.selectedFilter = null },
                            label = { Text("All", color = if (vm.selectedFilter == null) PureWhite else PrimaryTextDark) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentTealMain, containerColor = PureWhite, selectedLabelColor = PureWhite, labelColor = PrimaryTextDark))
                    }
                    items(Priority.values().toList()) { p ->
                        val (lbl, col) = when(p) {
                            Priority.CRITICAL -> "Critical" to CriticalRed
                            Priority.HIGH -> "High" to FutureEventOrange
                            Priority.MEDIUM -> "Medium" to MediumPriorityGreen
                            Priority.LOW -> "Low" to PriorityLowColor
                        }
                        val sel = vm.selectedFilter == p
                        FilterChip(selected = sel, onClick = { vm.selectedFilter = if (sel) null else p },
                            label = { Text(lbl, color = if (sel) PureWhite else PrimaryTextDark, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = col, containerColor = PureWhite, selectedLabelColor = PureWhite, labelColor = PrimaryTextDark))
                    }
                }
            }
        }

        AnimatedVisibility(vm.error != null, enter = expandVertically(), exit = shrinkVertically()) {
            Surface(Modifier.fillMaxWidth().padding(12.dp), RoundedCornerShape(12.dp), color = ErrorRed.copy(0.1f)) {
                Row(Modifier.padding(12.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Text(vm.error ?: "", style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
                    IconButton(onClick = vm::dismissError, Modifier.size(20.dp)) { Icon(Icons.Filled.Close, null, Modifier.size(14.dp)) }
                }
            }
        }

        if (vm.isLoading && events.isEmpty()) {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(5) { LoadingCard() }
            }
        } else if (filtered.isEmpty() && !vm.isLoading) {
            EmptyState(icon = Icons.Filled.CalendarMonth, title = "No events",
                body = "The calendar syncs automatically on first open and at most once per 24 hours. Tap Refresh or Load to update immediately.",
                action = {
                    Button(
                        onClick = { vm.refresh(vm.selectedNiche) },
                        enabled = !vm.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(PrimaryGreen),
                    ) {
                        CalendarRefreshGlyph(loading = vm.isLoading, size = 18.dp, tint = PureWhite)
                        Spacer(Modifier.width(8.dp))
                        Text(if (vm.isLoading) "Loading…" else "Load")
                    }
                }, modifier = Modifier.padding(top = 40.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)) {
                val urgents = filtered.filter { it.dateMillis in System.currentTimeMillis()..(System.currentTimeMillis() + 7*24*3600_000L) && (it.priority == Priority.CRITICAL || it.priority == Priority.HIGH) }
                if (urgents.isNotEmpty()) {
                    item("urgent") {
                        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=8.dp), shape = RoundedCornerShape(14.dp),
                            color = ErrorRed.copy(0.09f), border = BorderStroke(1.5.dp, ErrorRed.copy(0.35f))) {
                            Row(Modifier.padding(14.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = ErrorRed.copy(0.2f)) { Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.padding(8.dp).size(18.dp)) }
                                Column(Modifier.weight(1f)) {
                                    Text("${urgents.size} urgent deadline${if(urgents.size==1)"" else "s"}!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ErrorRed)
                                    Text(urgents.first().title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                                val d = ((urgents.first().dateMillis - System.currentTimeMillis()) / 86400_000L).toInt()
                                Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed) {
                                    Text(if(d<=0) "Today!" else "$d d", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PureWhite, modifier = Modifier.padding(horizontal=8.dp, vertical=4.dp))
                                }
                            }
                        }
                    }
                }
                grouped.forEach { (monthKey, monthEvents) ->
                    val parts = monthKey.split("-")
                    val mCal = Calendar.getInstance().apply { set(Calendar.YEAR, parts[0].toInt()); set(Calendar.MONTH, parts[1].toInt()) }
                    val isCurrent = mCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) && mCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    val monthStr = SimpleDateFormat("LLLL yyyy", Locale.ENGLISH).format(mCal.time).replaceFirstChar { it.uppercase() }
                    item("hdr_$monthKey") {
                        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=14.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                            Box(Modifier.size(4.dp,28.dp).clip(RoundedCornerShape(2.dp)).background(if(isCurrent) AccentTealMain else BorderGray))
                            Text(monthStr, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PrimaryTextDark)
                            if (isCurrent) Surface(shape = RoundedCornerShape(6.dp), color = AccentTealMain) { Text("Current", style = MaterialTheme.typography.labelSmall, color = PureWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal=7.dp, vertical=3.dp)) }
                            Spacer(Modifier.weight(1f))
                            Text("${monthEvents.size}", style = MaterialTheme.typography.labelMedium, color = TertiaryGray, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(monthEvents, key = { it.id }) { card ->
                        RegulatoryEventModuleCard(
                            card = card,
                            onClick = { onCardClick(card.id) },
                            onPin = { vm.pin(card.id, !card.isPinned) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Rotating refresh arrows while [loading]; static icon when idle (no endless animation in background). */
@Composable
private fun CalendarRefreshGlyph(loading: Boolean, size: Dp = 22.dp, tint: Color = AccentTealMain) {
    key(loading) {
        if (loading) {
            val transition = rememberInfiniteTransition(label = "calendarRefresh")
            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
                label = "rotation",
            )
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(size).rotate(rotation),
                tint = tint,
            )
        } else {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(size), tint = tint)
        }
    }
}

