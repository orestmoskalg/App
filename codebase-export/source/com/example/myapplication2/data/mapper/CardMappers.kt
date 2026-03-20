package com.example.myapplication2.data.mapper

import com.example.myapplication2.core.common.JsonProvider
import com.example.myapplication2.core.model.CardLink
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.core.model.SearchSection
import com.example.myapplication2.core.model.SocialPost
import com.example.myapplication2.data.local.entity.StoredCardEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

fun StoredCardEntity.toDomain(jsonProvider: JsonProvider): DashboardCard {
    val json = jsonProvider.json
    return DashboardCard(
        id = id,
        type = CardType.valueOf(type),
        searchQuery = searchQuery,
        title = title,
        subtitle = subtitle,
        body = body,
        expertOpinion = expertOpinion,
        analytics = analytics,
        actionChecklist = json.decodeFromString(ListSerializer(String.serializer()), actionChecklistJson),
        riskFlags = json.decodeFromString(ListSerializer(String.serializer()), riskFlagsJson),
        impactAreas = json.decodeFromString(ListSerializer(String.serializer()), impactAreasJson),
        confidenceLabel = confidenceLabel,
        urgencyLabel = urgencyLabel,
        links = json.decodeFromString(ListSerializer(CardLink.serializer()), linksJson),
        resources = json.decodeFromString(ListSerializer(String.serializer()), resourcesJson),
        socialInsights = json.decodeFromString(ListSerializer(SocialPost.serializer()), socialInsightsJson),
        detailedSections = json.decodeFromString(ListSerializer(SearchSection.serializer()), detailedSectionsJson),
        niche = niche,
        dateMillis = dateMillis,
        priority = Priority.valueOf(priority),
        isPinned = isPinned,
        orderIndex = orderIndex,
    )
}

fun DashboardCard.toEntity(jsonProvider: JsonProvider): StoredCardEntity {
    val json = jsonProvider.json
    return StoredCardEntity(
        id = id,
        type = type.name,
        searchQuery = searchQuery,
        title = title,
        subtitle = subtitle,
        body = body,
        expertOpinion = expertOpinion,
        analytics = analytics,
        actionChecklistJson = json.encodeToString(ListSerializer(String.serializer()), actionChecklist),
        riskFlagsJson = json.encodeToString(ListSerializer(String.serializer()), riskFlags),
        impactAreasJson = json.encodeToString(ListSerializer(String.serializer()), impactAreas),
        confidenceLabel = confidenceLabel,
        urgencyLabel = urgencyLabel,
        linksJson = json.encodeToString(ListSerializer(CardLink.serializer()), links),
        resourcesJson = json.encodeToString(ListSerializer(String.serializer()), resources),
        socialInsightsJson = json.encodeToString(ListSerializer(SocialPost.serializer()), socialInsights),
        detailedSectionsJson = json.encodeToString(ListSerializer(SearchSection.serializer()), detailedSections),
        niche = niche,
        dateMillis = dateMillis,
        priority = priority.name,
        isPinned = isPinned,
        orderIndex = orderIndex,
        isSearchHistory = type == CardType.SEARCH_HISTORY,
    )
}
