package com.example.myapplication2.presentation.carddetail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.domain.usecase.ObserveCardUseCase
import com.example.myapplication2.presentation.components.EmptyStatePanel
import com.example.myapplication2.presentation.components.SectionPanel
import com.example.myapplication2.presentation.components.StatusBadgeRow
import com.example.myapplication2.presentation.components.shareDashboardCard
import com.example.myapplication2.presentation.navigation.LocalAppContainer
import com.example.myapplication2.presentation.navigation.PendingCardHolder
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CardDetailUiState(
    val card: DashboardCard? = null,
)

class CardDetailViewModel(
    cardId: String,
    observeCardUseCase: ObserveCardUseCase,
) : ViewModel() {
    val uiState: StateFlow<CardDetailUiState> = observeCardUseCase(cardId)
        .map { card -> CardDetailUiState(card = card) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CardDetailUiState(),
        )
}

@Composable
fun CardDetailRoute(
    cardId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContainer = LocalAppContainer.current
    val pendingCard = remember(cardId) {
        PendingCardHolder.card?.takeIf { it.id == cardId }?.also {
            PendingCardHolder.card = null
        }
    }
    val viewModel: CardDetailViewModel = viewModel(
        key = "card_detail_$cardId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CardDetailViewModel(cardId, appContainer.observeCardUseCase) as T
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val card = pendingCard ?: uiState.card

    card?.let { c ->
        CardDetailScreen(
            card = c,
            onBack = onBack,
            modifier = modifier,
        )
    } ?: EmptyStateCardDetail(
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun CardDetailScreen(
    card: DashboardCard,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    if (card.type == CardType.SEARCH_HISTORY) {
        SearchHistoryDetailScreen(
            card = card,
            onBack = onBack,
            onShare = { shareDashboardCard(context, card) },
            onOpenFirstSource = { card.links.firstOrNull()?.let { uriHandler.openUri(it.url) } },
            onCopySuggestedQuestion = { clipboardManager.setText(AnnotatedString(it)) },
            modifier = modifier,
        )
        return
    }
    if (card.type == CardType.REGULATORY_EVENT) {
        CalendarEventDetailScreen(
            card = card,
            onBack = onBack,
            onShare = { shareDashboardCard(context, card) },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionPanel(
            title = card.title,
            subtitle = card.subtitle,
        ) {
            StatusBadgeRow(
                items = listOfNotNull(
                    detailSource(card),
                    "Дата: ${detailDate(card.dateMillis)}",
                    "Пріоритет: ${priorityLabel(card)}",
                    card.niche.takeIf { it.isNotBlank() }?.let { "Ніша: $it" },
                ),
            )
            DetailTopSummary(card = card)
            DetailMetricStrip(card = card)
            StatusBadgeRow(
                items = listOfNotNull(
                    card.confidenceLabel.takeIf { it.isNotBlank() }?.let { "Впевненість: $it" },
                    card.urgencyLabel.takeIf { it.isNotBlank() }?.let { "Терміновість: $it" },
                ),
            )
            Text(card.body, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack) {
                    Text("Назад")
                }
                OutlinedButton(onClick = { shareDashboardCard(context, card) }) {
                    Text("Розшарити")
                }
                if (card.links.isNotEmpty()) {
                    OutlinedButton(onClick = { uriHandler.openUri(card.links.first().url) }) {
                        Text("Відкрити джерело")
                    }
                }
            }
        }

        card.expertOpinion?.takeIf { it.isNotBlank() }?.let {
            SectionPanel(
                title = "Експертна думка",
                subtitle = null,
            ) {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        card.analytics?.takeIf { it.isNotBlank() }?.let {
            SectionPanel(
                title = "Аналітика",
                subtitle = null,
            ) {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (card.actionChecklist.isNotEmpty()) {
            BulletSection(
                title = "План дій",
                subtitle = null,
                items = card.actionChecklist,
            )
        }

        if (card.riskFlags.isNotEmpty()) {
            BulletSection(
                title = "Ризики",
                subtitle = null,
                items = card.riskFlags,
                useWarningSurface = true,
            )
        }

        if (card.impactAreas.isNotEmpty()) {
            SectionPanel(
                title = "Зони впливу",
                subtitle = null,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.impactAreas.forEach { area ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                        ) {
                            Text(
                                text = area,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        if (card.resources.isNotEmpty()) {
            BulletSection(
                title = "Що перевірити",
                subtitle = null,
                items = card.resources,
            )
        }

        if (card.links.isNotEmpty()) {
            DetailLinksSection(
                title = "Джерела",
                subtitle = null,
                links = card.links,
            )
        }

        if (card.detailedSections.isNotEmpty()) {
            card.detailedSections.forEach { section ->
                SectionPanel(
                    title = detailSectionTitle(section.type, section.title),
                    subtitle = null,
                ) {
                    Text(section.content, style = MaterialTheme.typography.bodyMedium)
                    if (section.resources.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            section.resources.forEach { resource ->
                                Text("• $resource", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (section.links.isNotEmpty()) {
                        DetailLinksSection(
                            title = "Джерела секції",
                            subtitle = null,
                            links = section.links,
                        )
                    }
                }
            }
        }

        if (card.socialInsights.isNotEmpty()) {
            SectionPanel(
                title = "Обговорення",
                subtitle = null,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    card.socialInsights.forEach { post ->
                        SocialInsightCard(
                            platform = post.platform,
                            author = post.author,
                            text = post.text,
                            onOpen = { uriHandler.openUri(post.url) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryDetailScreen(
    card: DashboardCard,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenFirstSource: () -> Unit,
    onCopySuggestedQuestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val originalQuery = card.searchQuery.ifBlank { card.title }
    val rankedLinks = rankedDetailLinks(card.links)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("← Назад") }
            TextButton(onClick = onShare) { Text("Поділитися") }
        }

        HeroArticleCard(
            query = originalQuery,
            body = card.body,
            confidenceLabel = card.confidenceLabel,
            urgencyLabel = card.urgencyLabel,
            firstLink = card.links.firstOrNull(),
            onLinkClick = { uriHandler.openUri(it) },
        )

        if (rankedLinks.isNotEmpty()) {
            HorizontalSection(
                title = "Пов'язані джерела",
                items = rankedLinks.take(12),
                content = { link ->
                    SourceCard(
                        title = link.title.ifBlank { link.url },
                        sourceLabel = link.sourceLabel,
                        onClick = { uriHandler.openUri(link.url) },
                    )
                },
            )
        }

        if (card.impactAreas.isNotEmpty()) {
            LightBulletBlock(title = "Ключові факти", items = card.impactAreas.take(6))
        }

        card.analytics?.takeIf { it.isNotBlank() }?.let { analytics ->
            LightTextBlock(title = "Для вашої ролі", text = analytics)
        }

        if (card.actionChecklist.isNotEmpty()) {
            LightBulletBlock(title = "Дії", items = card.actionChecklist.take(6))
        }

        if (card.riskFlags.isNotEmpty()) {
            LightBulletBlock(title = "Ризики", items = card.riskFlags, accent = true)
        }

        card.expertOpinion?.takeIf { it.isNotBlank() }?.let { note ->
            LightTextBlock(title = "Експертна позиція", text = note)
        }

        if (card.socialInsights.isNotEmpty()) {
            HorizontalSection(
                title = "Обговорення",
                items = card.socialInsights.take(8),
                content = { post ->
                    SocialCard(
                        platform = post.platform,
                        author = post.author,
                        text = post.text,
                        onClick = { uriHandler.openUri(post.url) },
                    )
                },
            )
        }

        if (card.resources.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Що дослідити далі",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                card.resources.take(3).forEach { question ->
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onCopySuggestedQuestion(question) },
                    )
                }
            }
        }

        if (rankedLinks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Усі джерела",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                rankedLinks.take(20).forEach { link ->
                    Text(
                        text = link.title.ifBlank { link.url },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { uriHandler.openUri(link.url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroArticleCard(
    query: String,
    body: String,
    confidenceLabel: String,
    urgencyLabel: String,
    firstLink: com.example.myapplication2.core.model.CardLink?,
    onLinkClick: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = query,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            firstLink?.let { link ->
                Text(
                    text = link.title.ifBlank { "Відкрити джерело" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onLinkClick(link.url) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (confidenceLabel.isNotBlank()) {
                    Text(
                        text = confidenceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (urgencyLabel.isNotBlank()) {
                    Text(
                        text = urgencyLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEventDetailScreen(
    card: DashboardCard,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(card.dateMillis))
    val officialLink = card.links.firstOrNull()
    val checkedItems = remember(card.id) { mutableStateMapOf<Int, Boolean>().apply {
        card.actionChecklist.indices.forEach { put(it, false) }
    }}

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("← Назад") }
            TextButton(onClick = onShare) { Text("Поділитися") }
        }

        LightTextBlock(title = card.title, text = "$dateStr · ${card.subtitle.ifBlank { "Подія" }}")

        if (card.body.isNotBlank()) {
            LightTextBlock(title = "Опис", text = card.body)
        }

        card.analytics?.takeIf { it.isNotBlank() }?.let {
            LightTextBlock(title = "Вплив на бізнес", text = it)
        }

        if (card.impactAreas.isNotEmpty()) {
            LightTextBlock(
                title = "Оцінка часу та ресурсів",
                text = card.impactAreas.joinToString("\n\n"),
            )
        }

        if (card.niche.isNotBlank() || card.impactAreas.any { it.contains("class") || it.contains("Class") || it.contains("клас") }) {
            val deviceInfo = buildString {
                if (card.niche.isNotBlank()) append("Ніша: ${card.niche}")
                val classHints = card.impactAreas.filter { it.contains("class", ignoreCase = true) || it.contains("клас", ignoreCase = true) }
                if (classHints.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("Класи пристроїв: ").append(classHints.joinToString("; "))
                }
            }
            if (deviceInfo.isNotBlank()) {
                LightTextBlock(title = "Зачіпає класи пристроїв", text = deviceInfo)
            }
        }

        if (card.links.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Точне посилання на статтю / офіційний документ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                officialLink?.let { link ->
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(link.url) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(link.title.ifBlank { link.url }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                card.links.drop(1).take(11).forEach { link ->
                    Text(
                        text = link.title.ifBlank { link.url },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { uriHandler.openUri(link.url) },
                    )
                }
            }
        }

        if (card.actionChecklist.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Чек-ліст (відміть виконане)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    card.actionChecklist.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkedItems[index] = !(checkedItems[index] ?: false) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(
                                checked = checkedItems[index] ?: false,
                                onCheckedChange = { checkedItems[index] = it },
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        FilledTonalButton(
            onClick = {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, card.title)
                    putExtra(CalendarContract.Events.DESCRIPTION, card.body)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, card.dateMillis)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, card.dateMillis + 3600_000L)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Додати в Google Calendar")
        }

        officialLink?.let { link ->
            OutlinedButton(
                onClick = { uriHandler.openUri(link.url) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Відкрити офіційний документ")
            }
        }

        card.expertOpinion?.takeIf { it.isNotBlank() }?.let {
            LightTextBlock(title = "Аналіз", text = it)
        }
        if (card.riskFlags.isNotEmpty()) {
            LightBulletBlock(title = "Ризики", items = card.riskFlags, accent = true)
        }
    }
}

@Composable
private fun <T> HorizontalSection(
    title: String,
    items: List<T>,
    content: @Composable (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEach { content(it) }
        }
    }
}

@Composable
private fun SourceCard(
    title: String,
    sourceLabel: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (sourceLabel.isNotBlank()) {
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SocialCard(
    platform: String,
    author: String,
    text: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$platform • $author",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LightBulletBlock(
    title: String,
    items: List<String>,
    accent: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (accent) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LightTextBlock(
    title: String,
    text: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EmptyStateCardDetail(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EmptyStatePanel(
            title = "Картку не знайдено",
            description = "Ймовірно, дані були оновлені або цей елемент більше недоступний у локальному сховищі.",
            actionLabel = "Повернутись назад",
            onAction = onBack,
        )
    }
}

private fun detailDate(value: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return formatter.format(Date(value))
}

@Composable
private fun DetailTopSummary(card: DashboardCard) {
    val summary = when {
        !card.expertOpinion.isNullOrBlank() -> card.expertOpinion
        !card.analytics.isNullOrBlank() -> card.analytics
        card.riskFlags.isNotEmpty() -> card.riskFlags.first()
        else -> card.body
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Що важливо зараз",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailMetricStrip(card: DashboardCard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailMetricCard(
            label = "Дії",
            value = card.actionChecklist.size.toString(),
        )
        DetailMetricCard(
            label = "Ризики",
            value = card.riskFlags.size.toString(),
        )
        DetailMetricCard(
            label = "Джерела",
            value = card.links.size.toString(),
        )
        DetailMetricCard(
            label = "Соцсигнали",
            value = card.socialInsights.size.toString(),
        )
    }
}

@Composable
private fun SearchTextSection(
    title: String,
    subtitle: String? = null,
    content: String,
    resources: List<String>,
    links: List<com.example.myapplication2.core.model.CardLink>,
) {
    if (content.isBlank() && resources.isEmpty() && links.isEmpty()) return
    ResearchSectionPanel(
        title = title,
        subtitle = subtitle,
    ) {
        content.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SearchSupportBlock(resources = resources, links = links)
    }
}

@Composable
private fun DetailMetricCard(
    label: String,
    value: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(10.dp),
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BulletSection(
    title: String,
    subtitle: String? = null,
    items: List<String>,
    resources: List<String> = emptyList(),
    links: List<com.example.myapplication2.core.model.CardLink> = emptyList(),
    useWarningSurface: Boolean = false,
) {
    ResearchSectionPanel(
        title = title,
        subtitle = subtitle,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (useWarningSurface) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        SearchSupportBlock(resources = resources, links = links)
    }
}

@Composable
private fun SearchSupportBlock(
    resources: List<String>,
    links: List<com.example.myapplication2.core.model.CardLink>,
) {
    val uriHandler = LocalUriHandler.current
    val normalizedResources = resources.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(4)
    val normalizedLinks = links.distinctBy { it.url }.take(4)
    if (normalizedResources.isEmpty() && normalizedLinks.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (normalizedResources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    normalizedResources.forEach { resource ->
                        Text(
                            text = "• $resource",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (normalizedLinks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    normalizedLinks.forEach { link ->
                        OutlinedButton(
                            onClick = { uriHandler.openUri(link.url) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
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
        }
    }
}

@Composable
private fun DetailLinksSection(
    title: String,
    subtitle: String?,
    links: List<com.example.myapplication2.core.model.CardLink>,
) {
    val uriHandler = LocalUriHandler.current
    val rankedLinks = rankedDetailLinks(links)
    ResearchSectionPanel(
        title = title,
        subtitle = subtitle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rankedLinks.forEachIndexed { index, link ->
                OutlinedButton(
                    onClick = { uriHandler.openUri(link.url) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}. ${link.title.ifBlank { link.url }}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(detailLinkTierLabel(link), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialInsightCard(
    platform: String,
    author: String,
    text: String,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("$platform • $author", fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(10.dp)) {
                Text("Відкрити джерело")
            }
        }
    }
}

@Composable
private fun ResearchSectionPanel(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

private fun detailSource(card: DashboardCard): String {
    return when (card.type) {
        CardType.SEARCH_HISTORY -> "Модуль: Пошук"
        CardType.REGULATORY_EVENT -> "Модуль: Календар"
        CardType.INSIGHT -> "Модуль: Інсайди"
        CardType.STRATEGY -> "Модуль: Стратегія"
        CardType.LEARNING_MODULE -> "Модуль: Навчання"
        CardType.ACTION_ITEM -> "Модуль: Інсайди / Дії"
    }
}

private fun priorityLabel(card: DashboardCard): String {
    return when (card.priority) {
        com.example.myapplication2.core.model.Priority.CRITICAL -> "Критичний"
        com.example.myapplication2.core.model.Priority.HIGH -> "Високий"
        com.example.myapplication2.core.model.Priority.MEDIUM -> "Середній"
    }
}

private fun detailSectionTitle(
    type: com.example.myapplication2.core.model.SearchSectionType,
    fallback: String,
): String {
    if (fallback.isNotBlank()) return fallback
    return when (type) {
        com.example.myapplication2.core.model.SearchSectionType.QUERY_BRIEFING -> "Опис запиту"
        com.example.myapplication2.core.model.SearchSectionType.RELATED_EVENTS -> "Пов'язані події"
        com.example.myapplication2.core.model.SearchSectionType.EXPERT_ANALYTICS -> "Експертна аналітика"
        com.example.myapplication2.core.model.SearchSectionType.STRATEGIC_FOCUS -> "Стратегічний фокус"
        com.example.myapplication2.core.model.SearchSectionType.SOCIAL_DISCUSSION -> "Соціальні обговорення"
    }
}

private fun searchSection(
    card: DashboardCard,
    type: com.example.myapplication2.core.model.SearchSectionType,
): com.example.myapplication2.core.model.SearchSection? {
    return card.detailedSections.firstOrNull { it.type == type }
}

private fun supportingLinks(
    primary: List<com.example.myapplication2.core.model.CardLink>,
    fallback: List<com.example.myapplication2.core.model.CardLink>,
    maxItems: Int,
): List<com.example.myapplication2.core.model.CardLink> {
    return (primary + fallback)
        .distinctBy { it.url }
        .sortedWith(
            compareByDescending<com.example.myapplication2.core.model.CardLink> { detailLinkScore(it) }
                .thenBy { it.title },
        )
        .take(maxItems)
}

private fun rankedDetailLinks(
    links: List<com.example.myapplication2.core.model.CardLink>,
): List<com.example.myapplication2.core.model.CardLink> {
    return links
        .distinctBy { it.url }
        .sortedWith(
            compareByDescending<com.example.myapplication2.core.model.CardLink> { detailLinkScore(it) }
                .thenBy { it.title },
        )
}

private fun detailLinkScore(link: com.example.myapplication2.core.model.CardLink): Int {
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

private fun detailLinkTier(link: com.example.myapplication2.core.model.CardLink): String {
    val url = link.url.lowercase()
    return when {
        url.contains("eur-lex.europa.eu") || url.contains("health.ec.europa.eu") || url.contains("webgate.ec.europa.eu") || url.contains("ec.europa.eu/tools/eudamed") || url.contains("ec.europa.eu/docsroom") -> "Official"
        url.contains("linkedin.com") || url.contains("x.com") || url.contains("twitter.com") -> "Community"
        detailLinkScore(link) >= 80 -> "Strong evidence"
        else -> "Supporting"
    }
}

private fun detailLinkTierLabel(link: com.example.myapplication2.core.model.CardLink): String {
    return when (detailLinkTier(link)) {
        "Official" -> "Офіційне"
        "Community" -> "Практичний контекст"
        "Strong evidence" -> "Ключове джерело"
        else -> "Додаткове"
    }
}
