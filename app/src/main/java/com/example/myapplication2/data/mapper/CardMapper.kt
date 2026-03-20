package com.example.myapplication2.data.mapper

import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.model.*
import com.example.myapplication2.data.local.entity.CardEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

object CardMapper {

    fun CardEntity.toDomain(): DashboardCard = DashboardCard(
        id = id,
        type = CardType.valueOf(type),
        searchQuery = searchQuery,
        title = title,
        subtitle = subtitle,
        body = body,
        expertOpinion = expertOpinion,
        analytics = analytics,
        actionChecklist = decodeStringList(actionChecklistJson),
        riskFlags = decodeStringList(riskFlagsJson),
        impactAreas = decodeStringList(impactAreasJson),
        confidenceLabel = confidenceLabel,
        urgencyLabel = urgencyLabel,
        links = decodeList(linksJson, ListSerializer(CardLink.serializer())),
        resources = decodeStringList(resourcesJson),
        socialInsights = decodeList(socialInsightsJson, ListSerializer(SocialPost.serializer())),
        detailedSections = decodeList(detailedSectionsJson, ListSerializer(SearchSection.serializer())),
        niche = niche,
        jurisdictionKey = jurisdictionKey,
        dateMillis = dateMillis,
        priority = Priority.valueOf(priority),
        isPinned = isPinned,
        orderIndex = orderIndex,
    )

    fun DashboardCard.toEntity(): CardEntity = CardEntity(
        id = id,
        type = type.name,
        searchQuery = searchQuery,
        title = title,
        subtitle = subtitle,
        body = body,
        expertOpinion = expertOpinion,
        analytics = analytics,
        actionChecklistJson = encodeStringList(actionChecklist),
        riskFlagsJson = encodeStringList(riskFlags),
        impactAreasJson = encodeStringList(impactAreas),
        confidenceLabel = confidenceLabel,
        urgencyLabel = urgencyLabel,
        linksJson = encodeList(links, ListSerializer(CardLink.serializer())),
        resourcesJson = encodeStringList(resources),
        socialInsightsJson = encodeList(socialInsights, ListSerializer(SocialPost.serializer())),
        detailedSectionsJson = encodeList(detailedSections, ListSerializer(SearchSection.serializer())),
        niche = niche,
        jurisdictionKey = jurisdictionKey,
        dateMillis = dateMillis,
        priority = priority.name,
        isPinned = isPinned,
        orderIndex = orderIndex,
    )

    private fun decodeStringList(json: String): List<String> = runCatching {
        AppJson.decodeFromString(ListSerializer(String.serializer()), json)
    }.getOrDefault(emptyList())

    private fun encodeStringList(list: List<String>): String =
        AppJson.encodeToString(ListSerializer(String.serializer()), list)

    private fun <T> decodeList(json: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T =
        runCatching { AppJson.decodeFromString(deserializer, json) }.getOrElse {
            AppJson.decodeFromString(deserializer, "[]")
        }

    private fun <T> encodeList(list: T, serializer: kotlinx.serialization.SerializationStrategy<T>): String =
        AppJson.encodeToString(serializer, list)
}
