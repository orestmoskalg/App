package com.example.myapplication2.data.repository

import com.example.myapplication2.data.local.dao.EventUserNoteDao
import com.example.myapplication2.data.local.entity.EventUserNoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EventNotesRepository(private val dao: EventUserNoteDao) {

    fun observeAll(): Flow<List<EventUserNoteEntity>> = dao.observeAll()

    fun observeForCard(cardId: String): Flow<List<EventUserNoteEntity>> = dao.observeForCard(cardId)

    suspend fun addNote(eventCardId: String, eventTitle: String, text: String) {
        dao.insert(
            EventUserNoteEntity(
                id = UUID.randomUUID().toString(),
                eventCardId = eventCardId,
                eventTitle = eventTitle,
                text = text,
            ),
        )
    }

    suspend fun deleteNote(id: String) = dao.deleteById(id)
}
