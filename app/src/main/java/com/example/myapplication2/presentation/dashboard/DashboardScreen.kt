package com.example.myapplication2.presentation.dashboard

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.presentation.components.*
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ──────────────────────────────────────────────────────────────────

class DashboardViewModel(private val container: AppContainer) : ViewModel() {

    val pinnedCards: StateFlow<List<DashboardCard>> =
        container.observePinnedCardsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentSearches: StateFlow<List<DashboardCard>> =
        container.observeSearchHistoryUseCase()
            .map { it.take(3) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userProfile: StateFlow<UserProfile?> =
        container.observeUserProfileUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var generationsLeft by mutableIntStateOf(3)
        private set

    init { viewModelScope.launch { refreshGenerationsCount() } }

    private suspend fun refreshGenerationsCount() {
        generationsLeft = container.getRemainingGenerationsUseCase(GenerationType.SEARCH)
    }

    fun pinCard(cardId: String, pinned: Boolean) {
        viewModelScope.launch { container.pinCardUseCase(cardId, pinned) }
    }

    val urgentToday: StateFlow<List<DashboardCard>> =
        container.observeCardsByTypeUseCase(CardType.REGULATORY_EVENT)
            .map { cards ->
                cards.filter {
                    val daysLeft = (it.dateMillis - System.currentTimeMillis()) / 86400_000L
                    daysLeft in 0..14 && (it.priority == Priority.CRITICAL || it.priority == Priority.HIGH)
                }.sortedBy { it.dateMillis }.take(3)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onCardClick: (String) -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateKnowledge: () -> Unit,
    onNavigateGlossary: () -> Unit,
    onNavigateChecklist: () -> Unit,
) {
    val pinnedCards by vm.pinnedCards.collectAsState()
    val recentSearches by vm.recentSearches.collectAsState()
    val urgentToday by vm.urgentToday.collectAsState()
    val profile by vm.userProfile.collectAsState()
    val today = remember { SimpleDateFormat("d MMMM yyyy", Locale("uk")).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {

        // ── Hero Header ──
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(PureWhite),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Regulatory Assistant",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                            )
                            Text(
                                buildString {
                                    profile?.role?.takeIf { it.isNotBlank() }?.let { append(it); append(" • ") }
                                    profile?.sector?.takeIf { it.isNotBlank() }?.let { sk ->
                                        append(SectorCatalog.find(sk)?.label ?: sk)
                                    } ?: append("Set sector in Settings")
                                    profile?.country?.takeIf { it.isNotBlank() }?.let { append(" • "); append(it) }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedLight,
                            )
                            Text(
                                today,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSubtle,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = LightTeal,
                            border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.3f)),
                        ) {
                            Icon(
                                Icons.Filled.Verified, null,
                                tint = PrimaryTeal,
                                modifier = Modifier.padding(12.dp).size(26.dp),
                            )
                        }
                    }

                    // Stats pills
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GreenStatPill("${profile?.niches?.size ?: 0}", "niches", Modifier.weight(1f))
                        GreenStatPill("${pinnedCards.size}", "pinned", Modifier.weight(1f))
                        GreenStatPill("${vm.generationsLeft}", "queries", Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Search Bar ──
        item {
            Surface(
                onClick = onNavigateSearch,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = PrimaryGreen.copy(alpha = 0.1f)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = PrimaryGreen, modifier = Modifier.padding(8.dp).size(18.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("AI regulatory research", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                        Text("Sector-aware analysis for your country & niches", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                }
            }
        }


        // ── Urgent Deadline Banner ──
        if (urgentToday.isNotEmpty()) {
            item {
                val first = urgentToday.first()
                val daysLeft = ((first.dateMillis - System.currentTimeMillis()) / 86400_000L).toInt()
                Surface(
                    onClick = onNavigateCalendar,
                    shape = RoundedCornerShape(14.dp),
                    color = ErrorRed.copy(alpha = 0.09f),
                    border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = ErrorRed.copy(0.2f)) {
                            Icon(Icons.Filled.Warning, null, tint = ErrorRed, modifier = Modifier.padding(8.dp).size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${urgentToday.size} urgent deadline${if(urgentToday.size==1)"" else "s"}",
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ErrorRed,
                            )
                            Text(first.title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = ErrorRed) {
                            Text(
                                if (daysLeft == 0) "Today!" else "$daysLeft d",
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = PureWhite, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Quick Actions ──
        item {
            Text(
                "Quick actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val actions = listOf(
                    QuickAction("Calendar", Icons.Filled.CalendarMonth, PrimaryGreen, onNavigateCalendar),
                    QuickAction("Knowledge", Icons.Filled.School, SuccessGreen, onNavigateKnowledge),
                    QuickAction("Glossary", Icons.Filled.MenuBook, InfoBlue, onNavigateGlossary),
                    QuickAction("Checklist", Icons.Filled.CheckCircle, WarningAmber, onNavigateChecklist),
                )
                items(actions) { a ->
                    Surface(
                        onClick = a.action,
                        shape = RoundedCornerShape(14.dp),
                        color = a.color.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, a.color.copy(alpha = 0.25f)),
                        modifier = Modifier.width(100.dp).height(88.dp),
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(10.dp),
                            Arrangement.Center, Alignment.CenterHorizontally,
                        ) {
                            Icon(a.icon, null, tint = a.color, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(a.label, style = MaterialTheme.typography.labelSmall, color = a.color, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Pinned Cards ──
        if (pinnedCards.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Pinned",
                    subtitle = "${pinnedCards.size} cards",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(pinnedCards, key = { it.id }) { card ->
                PinnedDashboardCard(
                    card = card,
                    onCardClick = onCardClick,
                    onPin = { id, pinned -> vm.pinCard(id, pinned) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // ── Recent Searches ──
        if (recentSearches.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent research",
                    subtitle = "Your AI search",
                    action = {
                        TextButton(onClick = onNavigateSearch) {
                            Text("More", color = PrimaryGreen)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(recentSearches, key = { it.id }) { card ->
                Surface(
                    onClick = { onCardClick(card.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                ) {
                    Row(Modifier.padding(12.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = PrimaryGreen.copy(alpha = 0.1f)) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = PrimaryGreen, modifier = Modifier.padding(6.dp).size(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(card.searchQuery.ifBlank { card.title }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Footer nudge ──
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrimaryGreen.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TipsAndUpdates, null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Tip of the day", style = MaterialTheme.typography.labelLarge, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        Text(
                            "Use AI Search for detailed MDR/IVDR analysis. Grok finds real discussions on LinkedIn, Reddit and RAPS.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private data class QuickAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, val action: () -> Unit)

@Composable
private fun GreenStatPill(value: String, label: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LightTeal,
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 8.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PrimaryTeal)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMutedLight)
        }
    }
}
