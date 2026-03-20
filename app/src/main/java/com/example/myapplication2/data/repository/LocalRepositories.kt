package com.example.myapplication2.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.data.local.dao.CacheDao
import com.example.myapplication2.data.local.dao.CardDao
import com.example.myapplication2.data.local.dao.GenerationLogDao
import com.example.myapplication2.data.local.entity.CacheEntity
import com.example.myapplication2.data.local.entity.GenerationLogEntity
import com.example.myapplication2.data.mapper.CardMapper.toDomain
import com.example.myapplication2.data.mapper.CardMapper.toEntity
import com.example.myapplication2.domain.model.*
import com.example.myapplication2.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val Context.dataStore by preferencesDataStore(name = "app_settings")

/** Fills [UserProfile.sector] for JSON saved before sector existed (infer from first niche). */
private fun UserProfile.withLegacyMigration(): UserProfile {
    if (sector.isNotBlank()) return this
    val sk = niches.firstOrNull()?.let { NicheCatalog.findByKeyOrName(it)?.sectorKey }
        ?: SectorCatalog.DEFAULT_KEY
    return copy(sector = sk)
}

class CardRepositoryImpl(private val dao: CardDao) : CardRepository {
    override fun observePinnedCards(): Flow<List<DashboardCard>> =
        dao.observePinnedCards().map { it.map { e -> e.toDomain() } }

    override fun observeSearchHistory(): Flow<List<DashboardCard>> =
        dao.observeSearchHistory().map { it.map { e -> e.toDomain() } }

    override fun observeCardsByType(type: CardType): Flow<List<DashboardCard>> =
        dao.observeCardsByType(type.name).map { it.map { e -> e.toDomain() } }

    override fun observeCard(cardId: String): Flow<DashboardCard?> =
        dao.observeCard(cardId).map { it?.toDomain() }

    override suspend fun getCard(cardId: String): DashboardCard? =
        dao.getCard(cardId)?.toDomain()

    override suspend fun saveCards(cards: List<DashboardCard>) =
        dao.insertCards(cards.map { it.toEntity() })

    override suspend fun saveCard(card: DashboardCard) =
        dao.insertCard(card.toEntity())

    override suspend fun setPinned(cardId: String, pinned: Boolean) =
        dao.setPinned(cardId, pinned)

    override suspend fun reorderPinnedCards(orderedCardIds: List<String>) =
        orderedCardIds.forEachIndexed { index, id -> dao.setOrderIndex(id, index) }

    override suspend fun updateCard(card: DashboardCard) =
        dao.updateCard(card.toEntity())

    override suspend fun deleteCard(cardId: String) = dao.deleteCard(cardId)

    override suspend fun clearByType(type: CardType) = dao.deleteByType(type.name)

    override suspend fun clearUnpinnedByType(type: CardType) = dao.deleteUnpinnedByType(type.name)
    override suspend fun clearSearchHistory() = dao.deleteByType(CardType.SEARCH_HISTORY.name)
    override suspend fun clearAll() = dao.clearAll()

    override suspend fun getUserStats(): UserStats = UserStats(
        searchCount  = dao.countSearchHistory(),
        calendarCount = dao.countCalendarEvents(),
        pinnedCount  = dao.countPinned(),
        totalCards   = dao.countAll(),
    )
}

class CacheRepositoryImpl(private val dao: CacheDao) : CacheRepository {
    override suspend fun getCachedPayload(key: String): String? = dao.get(key)?.payload

    override suspend fun getCachedPayloadIfFresh(key: String, maxAgeMillis: Long): String? {
        val entity = dao.get(key) ?: return null
        val age = System.currentTimeMillis() - entity.timestampMillis
        return if (age <= maxAgeMillis) entity.payload else null
    }

    override suspend fun putCachedPayload(key: String, payload: String, timestampMillis: Long) =
        dao.put(CacheEntity(key, payload, timestampMillis))

    override suspend fun clearAll() = dao.clearAll()
}

class GenerationRepositoryImpl(private val dao: GenerationLogDao) : GenerationRepository {
    override suspend fun getSnapshot(): GenerationSnapshot {
        val weekKey = WeekKeyHelper.currentWeekKey()
        val logs = dao.getByWeek(weekKey)
        val usage = logs.groupBy { it.generationType }
            .mapKeys { GenerationType.valueOf(it.key) }
            .mapValues { it.value.size }
        return GenerationSnapshot(weekKey, usage)
    }

    override suspend fun canGenerate(type: GenerationType, maxPerWeek: Int): Boolean {
        val count = dao.countByWeekAndType(WeekKeyHelper.currentWeekKey(), type.name)
        return count < maxPerWeek
    }

    override suspend fun recordGeneration(type: GenerationType) {
        dao.insert(GenerationLogEntity(
            weekKey = WeekKeyHelper.currentWeekKey(),
            generationType = type.name,
            timestampMillis = System.currentTimeMillis(),
        ))
    }

    override suspend fun getRemainingGenerations(type: GenerationType, maxPerWeek: Int): Int {
        val count = dao.countByWeekAndType(WeekKeyHelper.currentWeekKey(), type.name)
        return maxOf(0, maxPerWeek - count)
    }
}

class AppSettingsRepositoryImpl(private val context: Context) : AppSettingsRepository {
    private val nichesKey          = stringPreferencesKey("selected_niches")
    private val lastCalendarKey    = longPreferencesKey("last_calendar_refresh")
    private val knowledgeSeededKey   = booleanPreferencesKey("knowledge_base_seeded")
    private val knowledgeSeedVersionKey = intPreferencesKey("knowledge_seed_version")
    private val onboardingKey      = booleanPreferencesKey("onboarding_completed")
    private val checklistKey       = stringPreferencesKey("checklist_states")
    private val notifEnabledKey    = booleanPreferencesKey("notif_enabled")
    private val deadlineAlertsKey  = booleanPreferencesKey("notif_deadlines")
    private val weeklyDigestKey    = booleanPreferencesKey("notif_weekly")
    private val urgentOnlyKey      = booleanPreferencesKey("notif_urgent_only")
    private val apiKeyKey          = stringPreferencesKey("api_key")

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun <T> readPref(key: Preferences.Key<T>, default: T): T =
        context.dataStore.data.map { it[key] ?: default }.first()

    private suspend fun <T> writePref(key: Preferences.Key<T>, value: T) =
        context.dataStore.edit { it[key] = value }

    // ── Niches ───────────────────────────────────────────────────────────────

    override fun observeSelectedNiches(): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[nichesKey] ?: return@map emptyList()
            runCatching {
                AppJson.decodeFromString(ListSerializer(String.serializer()), raw)
            }.getOrDefault(emptyList())
        }

    override suspend fun saveSelectedNiches(niches: List<String>) {
        writePref(nichesKey, AppJson.encodeToString(ListSerializer(String.serializer()), niches))
    }

    override suspend fun getSelectedNiches(): List<String> {
        val raw = readPref(nichesKey, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    // ── Calendar ─────────────────────────────────────────────────────────────

    override suspend fun getLastCalendarRefreshMillis(): Long = readPref(lastCalendarKey, 0L)
    override suspend fun setLastCalendarRefreshMillis(value: Long) {
        writePref(lastCalendarKey, value)
    }

    override suspend fun isKnowledgeBaseSeeded(): Boolean {
        val explicit = readPref(knowledgeSeededKey, false)
        if (explicit) return true
        // Migration: older builds used last_calendar_refresh to gate knowledge seeding
        if (readPref(lastCalendarKey, 0L) > 0L) {
            writePref(knowledgeSeededKey, true)
            return true
        }
        return false
    }

    override suspend fun setKnowledgeBaseSeeded(value: Boolean) {
        writePref(knowledgeSeededKey, value)
    }

    override suspend fun getKnowledgeBaseSeedVersion(): Int =
        readPref(knowledgeSeedVersionKey, 0)

    override suspend fun setKnowledgeBaseSeedVersion(version: Int) {
        writePref(knowledgeSeedVersionKey, version)
    }

    // ── Onboarding ───────────────────────────────────────────────────────────

    override suspend fun isOnboardingCompleted(): Boolean = readPref(onboardingKey, false)
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        writePref(onboardingKey, completed)
    }

    // ── Checklist persistence ─────────────────────────────────────────────────

    override suspend fun getChecklistStates(): Map<String, Boolean> {
        val raw = readPref(checklistKey, "")
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            AppJson.decodeFromString(
                MapSerializer(String.serializer(), Boolean.serializer()), raw
            )
        }.getOrDefault(emptyMap())
    }

    override suspend fun saveChecklistStates(states: Map<String, Boolean>) {
        writePref(checklistKey, AppJson.encodeToString(
            MapSerializer(String.serializer(), Boolean.serializer()), states
        ))
    }

    // ── Notification settings ─────────────────────────────────────────────────

    override suspend fun getNotificationsEnabled(): Boolean  = readPref(notifEnabledKey, true)
    override suspend fun setNotificationsEnabled(v: Boolean) { writePref(notifEnabledKey, v) }
    override suspend fun getDeadlineAlertsEnabled(): Boolean  = readPref(deadlineAlertsKey, true)
    override suspend fun setDeadlineAlertsEnabled(v: Boolean) { writePref(deadlineAlertsKey, v) }
    override suspend fun getWeeklyDigestEnabled(): Boolean  = readPref(weeklyDigestKey, false)
    override suspend fun setWeeklyDigestEnabled(v: Boolean) { writePref(weeklyDigestKey, v) }
    override suspend fun getUrgentOnlyEnabled(): Boolean  = readPref(urgentOnlyKey, false)
    override suspend fun setUrgentOnlyEnabled(v: Boolean) { writePref(urgentOnlyKey, v) }

    // ── API Key ───────────────────────────────────────────────────────────────

    override suspend fun getApiKeyPreview(): String = readPref(apiKeyKey, "")
    override suspend fun getApiKey(): String         = readPref(apiKeyKey, "")
    override suspend fun saveApiKey(key: String) { writePref(apiKeyKey, key) }

    override suspend fun resetSessionForOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[onboardingKey] = false
            prefs[knowledgeSeededKey] = false
            prefs[knowledgeSeedVersionKey] = 0
            prefs[lastCalendarKey] = 0L
            prefs[nichesKey] = AppJson.encodeToString(ListSerializer(String.serializer()), emptyList())
            prefs[checklistKey] = ""
        }
    }
}

class UserProfileRepositoryImpl(private val context: Context) : UserProfileRepository {
    private val profileKey = stringPreferencesKey("user_profile")

    override fun observeUserProfile(): Flow<UserProfile?> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[profileKey] ?: return@map null
            runCatching { AppJson.decodeFromString(UserProfile.serializer(), raw) }.getOrNull()
                ?.withLegacyMigration()
        }

    override suspend fun getUserProfile(): UserProfile? {
        val raw = context.dataStore.data.map { it[profileKey] }.first() ?: return null
        return runCatching { AppJson.decodeFromString(UserProfile.serializer(), raw) }.getOrNull()
            ?.withLegacyMigration()
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[profileKey] = AppJson.encodeToString(UserProfile.serializer(), profile)
        }
    }

    override suspend fun clearUserProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(profileKey)
        }
    }

}


