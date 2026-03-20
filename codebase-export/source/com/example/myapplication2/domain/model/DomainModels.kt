package com.example.myapplication2.domain.model

import com.example.myapplication2.core.model.Priority
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserProfile(
    val role: String = "",
    val niches: List<String> = emptyList(),
    val deviceTypes: List<String> = emptyList(),
    val country: String = "",
)

@Serializable
enum class ActionStatus {
    TODO,
    IN_PROGRESS,
    DONE,
}

@Serializable
data class ActionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val deadlineMillis: Long? = null,
    val daysLeft: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: ActionStatus = ActionStatus.TODO,
    val relatedRegulation: String,
    val impactScore: Int,
)

@Serializable
enum class EventType {
    DEADLINE,
    GUIDANCE,
    WEBINAR,
    CONSULTATION,
    CONFERENCE,
    UPDATE,
}

@Serializable
data class RegulatoryEvent(
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long,
    val title: String,
    val type: EventType,
    val priority: Priority = Priority.MEDIUM,
    val affectedNiches: List<String> = emptyList(),
)

@Serializable
enum class GenerationType {
    INSIGHTS,
    SEARCH,
    CALENDAR,
    STRATEGY,
    LEARNING,
}

@Serializable
data class GenerationSnapshot(
    val weekKey: String,
    val usageByType: Map<GenerationType, Int>,
) {
    val totalUsage: Int
        get() = usageByType.values.sum()
}
