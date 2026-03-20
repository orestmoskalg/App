package com.example.myapplication2.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.myapplication2.core.common.JsonProvider
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.data.local.dao.CacheDao
import com.example.myapplication2.data.local.dao.CardDao
import com.example.myapplication2.data.local.entity.CachedPayloadEntity
import com.example.myapplication2.data.mapper.toDomain
import com.example.myapplication2.data.mapper.toEntity
import com.example.myapplication2.domain.model.GenerationSnapshot
import com.example.myapplication2.domain.model.GenerationType
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.AppSettingsRepository
import com.example.myapplication2.domain.repository.CacheRepository
import com.example.myapplication2.domain.repository.CardRepository
import com.example.myapplication2.domain.repository.GenerationRepository
import com.example.myapplication2.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException

private object PreferenceKeys {
    val userProfile = stringPreferencesKey("user_profile")
    val selectedNiches = stringPreferencesKey("selected_niches")
    val generationWeekKey = stringPreferencesKey("generation_week_key")
    val generationUsage = stringPreferencesKey("generation_usage")
    val lastCalendarRefresh = longPreferencesKey("last_calendar_refresh")
}

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val jsonProvider: JsonProvider,
) : CardRepository {

    override fun observePinnedCards(): Flow<List<DashboardCard>> =
        cardDao.observePinnedCards().map { list -> list.map { it.toDomain(jsonProvider) } }

    override fun observeSearchHistory(): Flow<List<DashboardCard>> =
        cardDao.observeSearchHistory().map { list -> list.map { it.toDomain(jsonProvider) } }

    override fun observeCard(cardId: String): Flow<DashboardCard?> =
        cardDao.observeById(cardId).map { entity -> entity?.toDomain(jsonProvider) }

    override suspend fun getCard(cardId: String): DashboardCard? =
        cardDao.getById(cardId)?.toDomain(jsonProvider)

    override suspend fun saveCards(cards: List<DashboardCard>) {
        cardDao.upsertAll(cards.map { it.toEntity(jsonProvider) })
    }

    override suspend fun saveCard(card: DashboardCard) {
        cardDao.upsert(card.toEntity(jsonProvider))
    }

    override suspend fun setPinned(cardId: String, pinned: Boolean) {
        val current = cardDao.getById(cardId) ?: return
        val orderIndex = if (pinned) cardDao.getPinnedCardsSnapshot().size else current.orderIndex
        cardDao.upsert(current.copy(isPinned = pinned, orderIndex = orderIndex))
    }

    override suspend fun reorderPinnedCards(orderedCardIds: List<String>) {
        cardDao.updatePinnedOrder(orderedCardIds)
    }

    override suspend fun updateCard(card: DashboardCard) {
        cardDao.update(card.toEntity(jsonProvider))
    }
}

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val jsonProvider: JsonProvider,
) : UserProfileRepository {

    override fun observeUserProfile(): Flow<UserProfile?> =
        dataStore.safeData().map { preferences ->
            preferences[PreferenceKeys.userProfile]?.let {
                jsonProvider.json.decodeFromString<UserProfile>(it)
            }
        }

    override suspend fun getUserProfile(): UserProfile? = observeUserProfile().first()

    override suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.userProfile] = jsonProvider.json.encodeToString(
                UserProfile.serializer(),
                profile,
            )
        }
    }
}

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val jsonProvider: JsonProvider,
) : AppSettingsRepository {

    override fun observeSelectedNiches(): Flow<List<String>> =
        dataStore.safeData().map { preferences ->
            preferences[PreferenceKeys.selectedNiches]?.let {
                jsonProvider.json.decodeFromString<List<String>>(it)
            } ?: emptyList()
        }

    override suspend fun saveSelectedNiches(niches: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.selectedNiches] = jsonProvider.json.encodeToString(
                ListSerializer(String.serializer()),
                niches,
            )
        }
    }

    override suspend fun getSelectedNiches(): List<String> = observeSelectedNiches().first()

    override suspend fun getLastCalendarRefreshMillis(): Long =
        dataStore.safeData().first()[PreferenceKeys.lastCalendarRefresh] ?: 0L

    override suspend fun setLastCalendarRefreshMillis(value: Long) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.lastCalendarRefresh] = value
        }
    }
}

@Singleton
class GenerationRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val jsonProvider: JsonProvider,
) : GenerationRepository {

    override suspend fun getSnapshot(): GenerationSnapshot {
        return GenerationSnapshot(weekKey = "unlimited", usageByType = emptyMap())
    }

    override suspend fun canGenerate(maxPerWeek: Int): Boolean {
        return true
    }

    override suspend fun recordGeneration(type: GenerationType) {
        // Weekly limits are disabled for this product version.
    }
}

@Singleton
class CacheRepositoryImpl @Inject constructor(
    private val cacheDao: CacheDao,
) : CacheRepository {

    override suspend fun getCachedPayload(key: String): String? = cacheDao.get(key)?.payload

    override suspend fun getCachedPayloadIfFresh(key: String, maxAgeMillis: Long): String? {
        val entity = cacheDao.get(key) ?: return null
        if (System.currentTimeMillis() - entity.timestampMillis > maxAgeMillis) return null
        return entity.payload
    }

    override suspend fun putCachedPayload(key: String, payload: String, timestampMillis: Long) {
        cacheDao.upsert(
            CachedPayloadEntity(
                key = key,
                payload = payload,
                timestampMillis = timestampMillis,
            ),
        )
    }

    override suspend fun clearAll() {
        cacheDao.clearAll()
    }
}

private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
