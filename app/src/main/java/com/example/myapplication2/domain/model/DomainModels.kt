package com.example.myapplication2.domain.model

import com.example.myapplication2.core.model.Priority
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.UUID

@Serializable
data class UserProfile(
    val role: String = "",
    /** Regulatory sector / sphere (see [com.example.myapplication2.core.common.SectorCatalog]). */
    val sector: String = "",
    val niches: List<String> = emptyList(),
    val deviceTypes: List<String> = emptyList(),
    val country: String = "",
) {
    val isComplete: Boolean get() =
        role.isNotBlank() &&
            sector.isNotBlank() &&
            niches.isNotEmpty() &&
            country.isNotBlank()
}

@Serializable
enum class ActionStatus { TODO, IN_PROGRESS, DONE }

@Serializable
data class ActionItem(
    val id: String,
    val title: String,
    val description: String,
    val deadlineMillis: Long? = null,
    val daysLeft: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: ActionStatus = ActionStatus.TODO,
    val relatedRegulation: String,
    val impactScore: Int,
) {
    companion object {
        fun create(
            title: String, description: String, relatedRegulation: String,
            impactScore: Int, deadlineMillis: Long? = null, daysLeft: Int? = null,
            priority: Priority = Priority.MEDIUM, status: ActionStatus = ActionStatus.TODO,
        ) = ActionItem(UUID.randomUUID().toString(), title, description,
            deadlineMillis, daysLeft, priority, status, relatedRegulation, impactScore)
    }
}

@Serializable
enum class EventType { DEADLINE, GUIDANCE, WEBINAR, CONSULTATION, CONFERENCE, UPDATE }

@Serializable
data class RegulatoryEvent(
    val id: String,
    val dateMillis: Long,
    val title: String,
    val type: EventType,
    val priority: Priority = Priority.MEDIUM,
    val affectedNiches: List<String> = emptyList(),
) {
    companion object {
        fun create(
            dateMillis: Long, title: String, type: EventType,
            priority: Priority = Priority.MEDIUM, affectedNiches: List<String> = emptyList(),
        ) = RegulatoryEvent(UUID.randomUUID().toString(), dateMillis, title, type, priority, affectedNiches)
    }
}

@Serializable
enum class GenerationType { INSIGHTS, SEARCH, CALENDAR, STRATEGY, LEARNING }

@Serializable
data class GenerationSnapshot(
    val weekKey: String,
    val usageByType: Map<GenerationType, Int> = emptyMap(),
) {
    val totalUsage: Int get() = usageByType.values.sum()
    fun usageFor(type: GenerationType): Int = usageByType[type] ?: 0
}

object WeekKeyHelper {
    fun currentWeekKey(): String {
        val cal = Calendar.getInstance().apply {
            minimalDaysInFirstWeek = 4
            firstDayOfWeek = Calendar.MONDAY
        }
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        return "${year}-W${week.toString().padStart(2, '0')}"
    }

    fun isCurrentWeek(weekKey: String): Boolean = weekKey == currentWeekKey()
}
