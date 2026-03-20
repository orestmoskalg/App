package com.example.myapplication2.search

import com.example.myapplication2.FakeCardRepository
import com.example.myapplication2.FakeGenerationRepository
import com.example.myapplication2.FakeSearchRepository
import com.example.myapplication2.core.model.CardLink
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.usecase.ExpertSearchUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Тестування пошукових запитів та відповідей (ExpertSearchUseCase).
 */
class SearchQueryAndResponseTest {

    private val profile = UserProfile(
        role = "RA Manager",
        sector = SectorKeys.MEDICAL_DEVICES,
        niches = listOf("Software as Medical Device SaMD AI ML"),
        deviceTypes = listOf("Class IIb"),
        country = "Ukraine",
    )

    @Test
    fun search_passes_query_to_repository() = runTest {
        val recordedQueries = mutableListOf<String>()
        val searchRepo = FakeSearchRepository { query, _ ->
            recordedQueries.add(query)
            sampleResponse(query, "Відповідь на: $query")
        }
        val useCase = ExpertSearchUseCase(
            searchRepo = searchRepo,
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        useCase("What's new in MDR 2025?", profile).getOrThrow()

        assertThat(recordedQueries).containsExactly("What's new in MDR 2025?")
    }

    @Test
    fun search_returns_card_with_search_history_type() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { query, _ -> sampleResponse(query, "MDR 2025") },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("MDR 2025 changes", profile).getOrThrow()

        assertThat(result.type).isEqualTo(CardType.SEARCH_HISTORY)
    }

    @Test
    fun search_saves_card_to_repository() = runTest {
        val cardRepo = FakeCardRepository()
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { query, _ -> sampleResponse(query, "MDR") },
            cardRepo = cardRepo,
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("Що змінилось у MDR?", profile).getOrThrow()

        assertThat(cardRepo.savedCards).hasSize(1)
        assertThat(cardRepo.savedCards.single().id).isEqualTo(result.id)
        assertThat(cardRepo.savedCards.single().body).isEqualTo(result.body)
    }

    @Test
    fun search_response_has_required_structure() = runTest {
        val searchRepo = FakeSearchRepository { query, _ ->
            sampleResponse(
                query = query,
                title = "MDR 2025",
                body = "Пряма відповідь: основні зміни стосуються Article 10.",
                impactAreas = listOf("Зміна 1", "Зміна 2", "Зміна 3"),
                actionChecklist = listOf("[Критичне до 30 днів] Оновити документацію"),
                riskFlags = listOf("Ризик затримки CE"),
            )
        }
        val useCase = ExpertSearchUseCase(
            searchRepo = searchRepo,
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("MDR 2025", profile).getOrThrow()

        assertThat(result.title).isEqualTo("MDR 2025")
        assertThat(result.body).startsWith("Пряма відповідь:")
        assertThat(result.impactAreas).hasSize(3)
        assertThat(result.actionChecklist).isNotEmpty()
        assertThat(result.actionChecklist.first()).contains("[Критичне до 30 днів]")
        assertThat(result.riskFlags).isNotEmpty()
    }

    @Test
    fun search_regulatory_query_returns_structured_response() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { query, _ ->
                sampleResponse(query, "CER/PER impact", "Так, впливає на CER та PER.")
            },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("Чи впливає це на CER/PER, PMCF/PMPF або PMS plan?", profile).getOrThrow()

        assertThat(result.title).isEqualTo("CER/PER impact")
        assertThat(result.body).contains("CER")
        assertThat(result.searchQuery).isEqualTo("Чи впливає це на CER/PER, PMCF/PMPF або PMS plan?")
    }

    @Test
    fun search_deadline_query_returns_timeline_focused_response() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { query, _ ->
                sampleResponse(
                    query = query,
                    title = "Дедлайни legacy devices",
                    body = "Перехідний період до 26 травня 2028.",
                    actionChecklist = listOf(
                        "[Критичне до 30 днів] Перевірити терміни",
                        "[Високе до 90 днів] Оновити план",
                    ),
                )
            },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("What are the deadlines for legacy devices until 2028?", profile).getOrThrow()

        assertThat(result.body).contains("2028")
        assertThat(result.actionChecklist.any { it.contains("30 днів") }).isTrue()
        assertThat(result.actionChecklist.any { it.contains("90 днів") }).isTrue()
    }

    @Test
    fun search_action_query_returns_checklist_response() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { query, _ ->
                sampleResponse(
                    query = query,
                    title = "Technical documentation steps",
                    body = "Крок 1: Зібрати докази. Крок 2: Оформити звіт.",
                    actionChecklist = listOf(
                        "[Критичне до 30 днів] Зібрати докази",
                        "[Середнє] Оформити звіт",
                    ),
                )
            },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("How to prepare technical documentation under MDR step by step?", profile).getOrThrow()

        assertThat(result.actionChecklist).hasSize(2)
        assertThat(result.body).contains("Крок")
    }

    @Test
    fun search_rejects_blank_query() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { _, _ -> sampleResponse("", "") },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("", profile)

        assertThat(result.exceptionOrNull()).isNotNull()
        assertThat(result.exceptionOrNull()?.message).contains("Enter a search query")
    }

    @Test
    fun search_rejects_whitespace_only_query() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { _, _ -> sampleResponse("", "") },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("   \t\n  ", profile)

        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun search_rejects_when_generation_limit_reached() = runTest {
        val useCase = ExpertSearchUseCase(
            searchRepo = FakeSearchRepository { query, _ -> sampleResponse(query, "ok") },
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = false),
        )

        val result = useCase("MDR 2025", profile)

        assertThat(result.exceptionOrNull()).isNotNull()
        assertThat(result.exceptionOrNull()?.message).contains("limit")
    }

    @Test
    fun search_passes_user_profile_to_repository() = runTest {
        val recordedProfiles = mutableListOf<UserProfile>()
        val searchRepo = FakeSearchRepository { _, p ->
            recordedProfiles.add(p)
            sampleResponse("query", "title")
        }
        val useCase = ExpertSearchUseCase(
            searchRepo = searchRepo,
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        useCase("MDR", profile).getOrThrow()

        assertThat(recordedProfiles).hasSize(1)
        assertThat(recordedProfiles.single().role).isEqualTo("RA Manager")
        assertThat(recordedProfiles.single().niches).contains("Software as Medical Device SaMD AI ML")
    }

    @Test
    fun search_response_includes_searchQuery_when_provided_by_repository() = runTest {
        val searchRepo = FakeSearchRepository { query, _ ->
            sampleResponse(query, "Topic title").copy(searchQuery = query)
        }
        val useCase = ExpertSearchUseCase(
            searchRepo = searchRepo,
            cardRepo = FakeCardRepository(),
            generationRepo = FakeGenerationRepository(canGenerate = true),
        )

        val result = useCase("My custom query", profile).getOrThrow()

        assertThat(result.searchQuery).isEqualTo("My custom query")
    }

    private fun sampleResponse(
        query: String,
        title: String,
        body: String = "Sample answer body.",
        impactAreas: List<String> = listOf("Impact 1", "Impact 2"),
        actionChecklist: List<String> = listOf("[Середнє] Sample action"),
        riskFlags: List<String> = listOf("Sample risk"),
    ): DashboardCard = DashboardCard.create(
        type = CardType.SEARCH_HISTORY,
        searchQuery = query,
        title = title,
        subtitle = "Релевантно для профілю",
        body = body,
        impactAreas = impactAreas,
        actionChecklist = actionChecklist,
        riskFlags = riskFlags,
        links = listOf(CardLink("EUR-Lex", "https://eur-lex.europa.eu/", "", false)),
        resources = listOf("Follow-up 1", "Follow-up 2", "Follow-up 3"),
        dateMillis = System.currentTimeMillis(),
        priority = Priority.HIGH,
    )
}
