package com.example.myapplication2.presentation.components

import android.content.Context
import android.content.Intent
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun shareDashboardCard(context: Context, card: DashboardCard) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, card.title)
        putExtra(Intent.EXTRA_TEXT, buildCardShareText(card))
    }

    val chooser = Intent.createChooser(shareIntent, "Розшарити картку")
    context.startActivity(chooser)
}

fun buildCardShareText(card: DashboardCard): String {
    return buildString {
        appendLine(card.title)
        appendLine(card.subtitle)
        appendLine()
        appendLine("Модуль: ${cardSource(card.type)}")
        appendLine("Пріоритет: ${card.priority.name}")
        appendLine("Дата: ${shareDate(card.dateMillis)}")
        if (card.niche.isNotBlank()) appendLine("Ніша: ${card.niche}")
        if (card.confidenceLabel.isNotBlank()) appendLine("Confidence: ${card.confidenceLabel}")
        if (card.urgencyLabel.isNotBlank()) appendLine("Urgency: ${card.urgencyLabel}")
        appendLine()
        appendLine("Опис")
        appendLine(card.body)

        card.expertOpinion?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("Експертна думка")
            appendLine(it)
        }

        card.analytics?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("Аналітика")
            appendLine(it)
        }

        if (card.actionChecklist.isNotEmpty()) {
            appendLine()
            appendLine("Action checklist")
            card.actionChecklist.forEach { appendLine("• $it") }
        }

        if (card.riskFlags.isNotEmpty()) {
            appendLine()
            appendLine("Risk flags")
            card.riskFlags.forEach { appendLine("• $it") }
        }

        if (card.impactAreas.isNotEmpty()) {
            appendLine()
            appendLine("Impact areas")
            card.impactAreas.forEach { appendLine("• $it") }
        }

        if (card.resources.isNotEmpty()) {
            appendLine()
            appendLine("Resources")
            card.resources.forEach { appendLine("• $it") }
        }

        if (card.links.isNotEmpty()) {
            appendLine()
            appendLine("Посилання")
            card.links.forEach { link ->
                appendLine("• ${link.title}")
                if (link.sourceLabel.isNotBlank()) appendLine("  Джерело: ${link.sourceLabel}")
                appendLine("  URL: ${link.url}")
                appendLine("  Статус: ${if (link.isVerified) "Перевірено" else "Без перевірки"}")
            }
        }

        if (card.detailedSections.isNotEmpty()) {
            appendLine()
            appendLine("Секції досьє")
            card.detailedSections.forEach { section ->
                appendLine()
                appendLine(section.title.ifBlank { section.type.name })
                appendLine(section.content)
                if (section.resources.isNotEmpty()) {
                    appendLine("Ресурси секції:")
                    section.resources.forEach { appendLine("• $it") }
                }
                if (section.links.isNotEmpty()) {
                    appendLine("Лінки секції:")
                    section.links.forEach { appendLine("• ${it.title}: ${it.url}") }
                }
            }
        }

        if (card.socialInsights.isNotEmpty()) {
            appendLine()
            appendLine("Соціальні обговорення")
            card.socialInsights.forEach { post ->
                appendLine("• ${post.platform} — ${post.author}")
                appendLine("  ${post.text}")
                appendLine("  ${post.url}")
            }
        }
    }.trim()
}

private fun shareDate(value: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
    return formatter.format(Date(value))
}

private fun cardSource(type: CardType): String {
    return when (type) {
        CardType.SEARCH_HISTORY -> "Пошук"
        CardType.REGULATORY_EVENT -> "Календар"
        CardType.INSIGHT -> "Інсайди"
        CardType.STRATEGY -> "Стратегія"
        CardType.LEARNING_MODULE -> "Навчання"
        CardType.ACTION_ITEM -> "Інсайди / Дії"
    }
}
