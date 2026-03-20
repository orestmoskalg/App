package com.example.myapplication2.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Priority Badge ─────────────────────────────────────────────────────────────
@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when (priority) {
        Priority.CRITICAL -> Triple(CrimsonRed, PureWhite, "CRITICAL")
        Priority.HIGH -> Triple(AmberOrange, PureWhite, "HIGH")
        Priority.MEDIUM -> Triple(MediumPriorityGreen, PureWhite, "MEDIUM")
        Priority.LOW -> Triple(PriorityLowSurface, PriorityLowColor, "LOW")
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ── Card Type Chip ─────────────────────────────────────────────────────────────
@Composable
fun CardTypeChip(type: CardType, modifier: Modifier = Modifier) {
    val (icon, label, color) = when (type) {
        CardType.REGULATORY_EVENT -> Triple(Icons.Filled.Event, "Event", PrimaryGreen)
        CardType.INSIGHT -> Triple(Icons.Filled.Lightbulb, "Insight", EmeraldGreen)
        CardType.STRATEGY -> Triple(Icons.Filled.AutoGraph, "Strategy", AmberOrange)
        CardType.LEARNING_MODULE -> Triple(Icons.Filled.School, "Learning", AppGreen)
        CardType.SEARCH_HISTORY -> Triple(Icons.Filled.AutoAwesome, "AI Search", InfoBlue)
        CardType.ACTION_ITEM -> Triple(Icons.Filled.CheckCircle, "Action", EmeraldGreen)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Dashboard Card Item ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCardItem(
    card: DashboardCard,
    onClick: () -> Unit,
    onPin: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val priorityColor = when (card.priority) {
        Priority.CRITICAL -> CrimsonRed
        Priority.HIGH -> AmberOrange
        Priority.MEDIUM -> MediumPriorityGreen
        Priority.LOW -> PriorityLowColor
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardTypeChip(card.type)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (card.urgencyLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityColor.copy(alpha = 0.1f),
                        ) {
                            Text(
                                card.urgencyLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (onPin != null) {
                        IconButton(onClick = onPin, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (card.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (card.isPinned) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Priority indicator strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(listOf(priorityColor, priorityColor.copy(alpha = 0f)))
                    )
            )

            Spacer(Modifier.height(10.dp))

            Text(
                card.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                card.body.take(150).let { if (card.body.length > 150) "$it…" else it },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (card.actionChecklist.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircleOutline, null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                    Text(
                        "${card.actionChecklist.size} actions to complete",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Module-style cards (same layout as Knowledge / Calendar screens) ──────────

fun accentColorForKnowledgeCardType(type: CardType): Color = when (type) {
    CardType.INSIGHT -> InfoBlue
    CardType.STRATEGY -> WarningAmber
    CardType.LEARNING_MODULE -> SuccessGreen
    else -> PrimaryTeal
}

@Composable
fun KnowledgeModuleCard(
    card: DashboardCard,
    accentColor: Color,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val priorityColor = when (card.priority) {
        Priority.CRITICAL -> ErrorRed
        Priority.HIGH -> WarningAmber
        Priority.MEDIUM -> MediumPriorityGreen
        Priority.LOW -> PriorityLowColor
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth().clickable { onCardClick(card.id) },
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        card.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    if (card.subtitle.isNotBlank()) {
                        Text(
                            card.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            maxLines = 1,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = priorityColor.copy(alpha = 0.12f)) {
                        Text(
                            card.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(
                        onClick = { onPin(card.id, !card.isPinned) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            if (card.isPinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            null,
                            tint = if (card.isPinned) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (card.body.isNotBlank()) {
                Text(
                    card.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (card.analytics?.isNotBlank() == true) {
                Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.07f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.BarChart, null, tint = accentColor, modifier = Modifier.size(13.dp))
                        Text(card.analytics, style = MaterialTheme.typography.labelSmall, color = accentColor)
                    }
                }
            }

            if (card.actionChecklist.isNotEmpty()) {
                Text(
                    "${card.actionChecklist.size} actions to complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Same layout family as [KnowledgeModuleCard] — used on Search screen and Home “Pinned” so design stays consistent. */
@Composable
fun SearchHistoryModuleCard(
    card: DashboardCard,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showManagementActions: Boolean = false,
    onOpenInNew: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val priorityColor = when (card.priority) {
        Priority.CRITICAL -> ErrorRed
        Priority.HIGH -> WarningAmber
        Priority.MEDIUM -> MediumPriorityGreen
        Priority.LOW -> PriorityLowColor
    }
    val accentColor = PrimaryGreen
    val hasFull = remember(card.body) { card.body.startsWith("{") && "executiveSummary" in card.body }
    val bodyPreview = remember(card.body, card.subtitle) {
        val b = card.body.trim()
        if (b.startsWith("{")) card.subtitle.ifBlank { "Tap to open full research" } else b
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth().clickable { onCardClick(card.id) },
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.1f)) {
                Icon(
                    if (hasFull) Icons.Filled.AutoAwesome else Icons.Filled.Search,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    card.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                if (card.subtitle.isNotBlank()) {
                    Text(
                        card.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        maxLines = 1,
                    )
                }
                if (bodyPreview.isNotBlank()) {
                    Text(
                        bodyPreview.take(140).let { if (bodyPreview.length > 140) "$it…" else it },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = priorityColor.copy(alpha = 0.12f)) {
                    Text(
                        card.priority.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = { onPin(card.id, !card.isPinned) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        if (card.isPinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (card.isPinned) "Unpin" else "Pin",
                        tint = if (card.isPinned) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (showManagementActions && onOpenInNew != null) {
                    IconButton(onClick = onOpenInNew, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.OpenInNew, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
                if (showManagementActions && onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegulatoryEventModuleCard(
    card: DashboardCard,
    onClick: () -> Unit,
    onPin: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val today = remember { Calendar.getInstance() }
    val cardCal = Calendar.getInstance().apply { timeInMillis = card.dateMillis }
    val isToday = cardCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
        cardCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    val daysLeft = ((card.dateMillis - System.currentTimeMillis()) / 86400_000L).toInt()
    val isPast = daysLeft < 0
    val isCritical = card.priority == Priority.CRITICAL
    val isCurrentOrUpcoming = !isPast && (isToday || daysLeft <= 14)
    val (dateBg, dateTextColor) = when {
        isPast -> CalendarPastDateBg to SecondaryTextMedium
        isCritical -> CalendarCriticalDateBg to PureWhite
        isCurrentOrUpcoming -> CalendarCurrentDateBg to PureWhite
        else -> CalendarFutureDateBg to PrimaryTextDark
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(14.dp)) {
            Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(10.dp), color = dateBg) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            SimpleDateFormat("MMM", Locale.ENGLISH).format(Date(card.dateMillis)).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = dateTextColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${cardCal.get(Calendar.DAY_OF_MONTH)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = dateTextColor,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                val dayLbl = when {
                    isToday -> "Today"
                    isPast -> "Past"
                    daysLeft == 1 -> "Tomorrow"
                    daysLeft <= 7 -> "$daysLeft d"
                    else -> "${daysLeft / 7}w"
                }
                Text(
                    dayLbl,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isToday -> AccentTealMain
                        isPast -> TertiaryGray
                        daysLeft <= 7 -> CriticalRed
                        else -> SecondaryTextMedium
                    },
                    fontWeight = if (isToday || daysLeft <= 3) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isToday) {
                    Surface(shape = RoundedCornerShape(4.dp), color = AccentTealMain) {
                        Text(
                            "● TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    card.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    color = PrimaryTextDark,
                )
                Text(
                    card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryTextMedium,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (card.actionChecklist.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.CheckCircleOutline, null, tint = AccentTealMain, modifier = Modifier.size(12.dp))
                            Text("${card.actionChecklist.size} actions", style = MaterialTheme.typography.labelSmall, color = AccentTealMain)
                        }
                    }
                    if (card.riskFlags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = CriticalRed, modifier = Modifier.size(12.dp))
                            Text("${card.riskFlags.size} risk", style = MaterialTheme.typography.labelSmall, color = CriticalRed)
                        }
                    }
                    if (card.links.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Link, null, tint = MediumPriorityGreen, modifier = Modifier.size(12.dp))
                            Text("${card.links.size} prep links", style = MaterialTheme.typography.labelSmall, color = MediumPriorityGreen)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onPin != null) {
                    IconButton(onClick = onPin, Modifier.size(24.dp)) {
                        Icon(
                            if (card.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            null,
                            tint = if (card.isPinned) AccentTealMain else SecondaryTextMedium,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                PriorityBadge(card.priority)
            }
        }
    }
}

@Composable
fun PinnedDashboardCard(
    card: DashboardCard,
    onCardClick: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (card.type) {
        CardType.INSIGHT, CardType.STRATEGY, CardType.LEARNING_MODULE -> {
            KnowledgeModuleCard(
                card = card,
                accentColor = accentColorForKnowledgeCardType(card.type),
                onCardClick = onCardClick,
                onPin = onPin,
                modifier = modifier,
            )
        }
        CardType.REGULATORY_EVENT -> {
            RegulatoryEventModuleCard(
                card = card,
                onClick = { onCardClick(card.id) },
                onPin = { onPin(card.id, !card.isPinned) },
                modifier = modifier,
            )
        }
        CardType.SEARCH_HISTORY -> {
            SearchHistoryModuleCard(
                card = card,
                onCardClick = onCardClick,
                onPin = onPin,
                modifier = modifier,
                showManagementActions = false,
            )
        }
        else -> {
            DashboardCardItem(
                card = card,
                onClick = { onCardClick(card.id) },
                onPin = { onPin(card.id, !card.isPinned) },
                modifier = modifier,
            )
        }
    }
}

// ── Loading State ──────────────────────────────────────────────────────────────
@Composable
fun LoadingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.85f).height(18.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(50.dp))
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    var shimmerAlpha by remember { mutableStateOf(0.3f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(800)
            shimmerAlpha = if (shimmerAlpha > 0.5f) 0.3f else 0.7f
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = shimmerAlpha))
    )
}

// ── Empty State ────────────────────────────────────────────────────────────────
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp),
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        action?.invoke()
    }
}

// ── Section Header ─────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

// ── Stat Chip ──────────────────────────────────────────────────────────────────
@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

fun Long.toReadableDate(): String {
    val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return fmt.format(Date(this))
}

fun Long.toDaysFromNow(): Int {
    val diff = this - System.currentTimeMillis()
    return (diff / 86400000L).toInt()
}
