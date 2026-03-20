package com.example.myapplication2.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Priority { CRITICAL, HIGH, MEDIUM, LOW }

@Serializable
enum class CardType {
    SEARCH_HISTORY, REGULATORY_EVENT, INSIGHT,
    STRATEGY, LEARNING_MODULE, ACTION_ITEM
}

@Serializable
data class CardLink(
    val title: String,
    val url: String,
    val sourceLabel: String = "",
    val isVerified: Boolean = false,
)

@Serializable
data class SocialPost(
    val platform: String,
    val author: String,
    val text: String,
    val dateMillis: Long,
    val url: String,
)

@Serializable
enum class SearchSectionType {
    QUERY_BRIEFING, RELATED_EVENTS, EXPERT_ANALYTICS,
    STRATEGIC_FOCUS, SOCIAL_DISCUSSION
}

@Serializable
data class SearchSection(
    val type: SearchSectionType,
    val title: String,
    val content: String,
    val resources: List<String> = emptyList(),
    val links: List<CardLink> = emptyList(),
)

@Serializable
data class DashboardCard(
    val id: String,
    val type: CardType,
    val searchQuery: String = "",
    val title: String,
    val subtitle: String,
    val body: String,
    val expertOpinion: String? = null,
    val analytics: String? = null,
    val actionChecklist: List<String> = emptyList(),
    val riskFlags: List<String> = emptyList(),
    val impactAreas: List<String> = emptyList(),
    val confidenceLabel: String = "",
    val urgencyLabel: String = "",
    val links: List<CardLink> = emptyList(),
    val resources: List<String> = emptyList(),
    val socialInsights: List<SocialPost> = emptyList(),
    val detailedSections: List<SearchSection> = emptyList(),
    val niche: String = "",
    /** ISO-like key from [CountryRegulatoryContext.Context.jurisdictionKey] (e.g. eu, usa, ukraine). */
    val jurisdictionKey: String = "",
    val dateMillis: Long,
    val priority: Priority = Priority.MEDIUM,
    val isPinned: Boolean = false,
    val orderIndex: Int = 0,
) {
    companion object {
        fun create(
            type: CardType,
            title: String,
            subtitle: String,
            body: String,
            dateMillis: Long,
            niche: String = "",
            searchQuery: String = "",
            priority: Priority = Priority.MEDIUM,
            expertOpinion: String? = null,
            analytics: String? = null,
            actionChecklist: List<String> = emptyList(),
            riskFlags: List<String> = emptyList(),
            impactAreas: List<String> = emptyList(),
            confidenceLabel: String = "",
            urgencyLabel: String = "",
            links: List<CardLink> = emptyList(),
            resources: List<String> = emptyList(),
            socialInsights: List<SocialPost> = emptyList(),
            detailedSections: List<SearchSection> = emptyList(),
            jurisdictionKey: String = "",
        ): DashboardCard = DashboardCard(
            id = UUID.randomUUID().toString(),
            type = type, searchQuery = searchQuery, title = title, subtitle = subtitle,
            body = body, expertOpinion = expertOpinion, analytics = analytics,
            actionChecklist = actionChecklist, riskFlags = riskFlags,
            impactAreas = impactAreas, confidenceLabel = confidenceLabel,
            urgencyLabel = urgencyLabel, links = links, resources = resources,
            socialInsights = socialInsights, detailedSections = detailedSections,
            niche = niche, jurisdictionKey = jurisdictionKey, dateMillis = dateMillis, priority = priority,
        )
    }
}
