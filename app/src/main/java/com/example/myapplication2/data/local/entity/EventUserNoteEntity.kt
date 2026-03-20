package com.example.myapplication2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_user_notes")
data class EventUserNoteEntity(
    @PrimaryKey val id: String,
    val eventCardId: String,
    val eventTitle: String,
    val text: String,
    /** JSON array of strings */
    val tagsJson: String = "[]",
    val pinned: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val status: String = "not_started",
)
