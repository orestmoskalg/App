package com.example.myapplication2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stored_cards")
data class StoredCardEntity(
    @PrimaryKey val id: String,
    val type: String,
    val searchQuery: String,
    val title: String,
    val subtitle: String,
    val body: String,
    val expertOpinion: String?,
    val analytics: String?,
    val actionChecklistJson: String,
    val riskFlagsJson: String,
    val impactAreasJson: String,
    val confidenceLabel: String,
    val urgencyLabel: String,
    val linksJson: String,
    val resourcesJson: String,
    val socialInsightsJson: String,
    val detailedSectionsJson: String,
    val niche: String,
    val dateMillis: Long,
    val priority: String,
    val isPinned: Boolean,
    val orderIndex: Int,
    val isSearchHistory: Boolean,
)
