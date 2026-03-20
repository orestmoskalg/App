package com.example.myapplication2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val type: String,
    val searchQuery: String = "",
    val title: String,
    val subtitle: String,
    val body: String,
    val expertOpinion: String? = null,
    val analytics: String? = null,
    val actionChecklistJson: String = "[]",
    val riskFlagsJson: String = "[]",
    val impactAreasJson: String = "[]",
    val confidenceLabel: String = "",
    val urgencyLabel: String = "",
    val linksJson: String = "[]",
    val resourcesJson: String = "[]",
    val socialInsightsJson: String = "[]",
    val detailedSectionsJson: String = "[]",
    val niche: String = "",
    val jurisdictionKey: String = "",
    val dateMillis: Long,
    val priority: String = "MEDIUM",
    val isPinned: Boolean = false,
    val orderIndex: Int = 0,
)

@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val payload: String,
    val timestampMillis: Long,
)

@Entity(tableName = "generation_log")
data class GenerationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekKey: String,
    val generationType: String,
    val timestampMillis: Long,
)
