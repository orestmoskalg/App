package com.example.myapplication2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.ui.theme.AppHeroGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardCardItem(
    card: DashboardCard,
    modifier: Modifier = Modifier,
    onOpenDetail: ((DashboardCard) -> Unit)? = null,
    onPinToggle: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .offset(x = 20.dp, y = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)),
            shape = RoundedCornerShape(26.dp),
        ) {}
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .offset(x = 10.dp, y = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(24.dp),
        ) {}

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .clickable(enabled = onOpenDetail != null) { onOpenDetail?.invoke(card) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 22.dp))
                            .background(AppHeroGradient)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            previewTypeLabel(card.type),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = dateLabel(card.dateMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = card.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PriorityBadge(priority = card.priority)
                }

                Text(
                    text = sourceLabel(card),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                cardPreviewSignal(card)?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = card.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val metricBadges = buildList {
                    card.confidenceLabel.takeIf { it.isNotBlank() }?.let { add("Впевненість: $it") }
                    card.urgencyLabel.takeIf { it.isNotBlank() }?.let { add("Терміновість: $it") }
                    if (card.impactAreas.isNotEmpty()) add("${card.impactAreas.size} зон впливу")
                    if (card.actionChecklist.isNotEmpty()) add("${card.actionChecklist.size} дій")
                    if (card.riskFlags.isNotEmpty()) add("${card.riskFlags.size} ризиків")
                }
                if (metricBadges.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        metricBadges.forEach { badge ->
                            AssistChip(
                                onClick = { onOpenDetail?.invoke(card) },
                                label = { Text(badge) },
                            )
                        }
                    }
                }

                Text(
                    text = "Натисни на картку, щоб відкрити повну сторінку з усією інформацією.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { shareDashboardCard(context, card) },
                    ) {
                        Text("Розшарити")
                    }
                    onPinToggle?.let {
                        OutlinedButton(onClick = it) {
                            Text(if (card.isPinned) "Зняти з головної" else "На головну")
                        }
                    }
                    onMoveUp?.let {
                        OutlinedButton(onClick = it) {
                            Text("Вище")
                        }
                    }
                    onMoveDown?.let {
                        OutlinedButton(onClick = it) {
                            Text("Нижче")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    AssistChip(
        onClick = {},
        label = { Text(priorityLabel(priority)) },
    )
}

@Composable
fun CardList(
    cards: List<DashboardCard>,
    modifier: Modifier = Modifier,
    onOpenDetail: ((DashboardCard) -> Unit)? = null,
    onPinToggle: (DashboardCard) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            DashboardCardItem(
                card = card,
                onOpenDetail = onOpenDetail,
                onPinToggle = { onPinToggle(card) },
            )
        }
    }
}

private fun dateLabel(value: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return formatter.format(Date(value))
}

private fun sourceLabel(card: DashboardCard): String {
    return when (card.type) {
        CardType.SEARCH_HISTORY -> "Модуль: Пошук"
        CardType.REGULATORY_EVENT -> "Модуль: Календар • ${dateLabel(card.dateMillis)}"
        CardType.INSIGHT -> "Модуль: Інсайди"
        CardType.STRATEGY -> "Модуль: Стратегія"
        CardType.LEARNING_MODULE -> "Модуль: Навчання"
        CardType.ACTION_ITEM -> "Модуль: Інсайди / Дії"
    }
}

private fun previewTypeLabel(type: CardType): String {
    return when (type) {
        CardType.SEARCH_HISTORY -> "Пошук"
        CardType.REGULATORY_EVENT -> "Подія"
        CardType.INSIGHT -> "Інсайт"
        CardType.STRATEGY -> "Playbook"
        CardType.LEARNING_MODULE -> "Модуль"
        CardType.ACTION_ITEM -> "Дія"
    }
}

private fun priorityLabel(priority: Priority): String {
    return when (priority) {
        Priority.CRITICAL -> "Критично"
        Priority.HIGH -> "Високий"
        Priority.MEDIUM -> "Середній"
    }
}

private fun cardPreviewSignal(card: DashboardCard): String? {
    return when {
        !card.expertOpinion.isNullOrBlank() -> card.expertOpinion
        !card.analytics.isNullOrBlank() -> card.analytics
        card.riskFlags.isNotEmpty() -> card.riskFlags.first()
        card.actionChecklist.isNotEmpty() -> card.actionChecklist.first()
        else -> null
    }
}
