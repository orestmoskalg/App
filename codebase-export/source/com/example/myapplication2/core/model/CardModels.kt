package com.example.myapplication2.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
}

@Serializable
enum class CardType {
    SEARCH_HISTORY,
    REGULATORY_EVENT,
    INSIGHT,
    STRATEGY,
    LEARNING_MODULE,
    ACTION_ITEM,
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
    QUERY_BRIEFING,
    RELATED_EVENTS,
    EXPERT_ANALYTICS,
    STRATEGIC_FOCUS,
    SOCIAL_DISCUSSION,
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
    val id: String = UUID.randomUUID().toString(),
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
    val dateMillis: Long,
    val priority: Priority = Priority.MEDIUM,
    val isPinned: Boolean = false,
    val orderIndex: Int = 0,
)
