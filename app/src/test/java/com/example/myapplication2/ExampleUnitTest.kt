package com.example.myapplication2

import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.usecase.ExpertSearchUseCase
import com.example.myapplication2.domain.usecase.GenerateRegulatoryCalendarUseCase
import com.example.myapplication2.domain.usecase.PinCardUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExampleUnitTest {

    private val profile = UserProfile(
        role = "RA Manager",
        sector = SectorKeys.MEDICAL_DEVICES,
        niches = listOf("Software as Medical Device SaMD AI ML"),
        deviceTypes = listOf("Class IIb"),
        country = "Ukraine",
    )

    @Test
    fun expertSearch_accepts_non_empty_query() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository(),
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("best pizza in kyiv", profile).getOrThrow()

        assertThat(result.title).isEqualTo("best pizza in kyiv")
    }

    @Test
    fun expertSearch_rejects_blank_query() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository(),
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("   ", profile)

        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun calendarUseCase_persists_events_and_updates_settings() = runTest {
        val event = DashboardCard.create(
            type = CardType.REGULATORY_EVENT,
            title = "Deadline",
            subtitle = "sub",
            body = "body",
            dateMillis = 2000L,
            priority = Priority.HIGH,
        )
        val calendarRepository = FakeCalendarRepository { _, _ -> listOf(event) }
        val cardRepository = FakeCardRepository()
        val settings = FakeAppSettingsRepository()
        val useCase = GenerateRegulatoryCalendarUseCase(
            calendarRepository,
            cardRepository,
            settings,
            FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase(listOf("Software as Medical Device SaMD AI ML"), profile).getOrThrow()

        assertThat(result.single().title).isEqualTo("Deadline")
        assertThat(calendarRepository.callCount).isEqualTo(1)
        assertThat(settings.lastCalendarRefreshMillis).isGreaterThan(0L)
    }

    @Test
    fun pinCardUseCase_updates_pinned_state() = runTest {
        val repository = FakeCardRepository()
        val useCase = PinCardUseCase(repository)
        val card = sampleCard("pin me")

        useCase(card.id, true)

        assertThat(repository.savedCards).isEmpty()
        assertThat(repository.lastPinnedUpdate).isEqualTo(card.id to true)
    }

    private fun sampleCard(title: String) = DashboardCard.create(
        type = CardType.SEARCH_HISTORY,
        title = title,
        subtitle = "subtitle",
        body = "body",
        dateMillis = 1000L,
        priority = Priority.HIGH,
    )
}
