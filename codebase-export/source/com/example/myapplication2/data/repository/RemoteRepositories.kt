package com.example.myapplication2.data.repository

import android.util.Patterns
import com.example.myapplication2.core.model.CardLink
import com.example.myapplication2.core.common.JsonProvider
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.SearchSection
import com.example.myapplication2.core.model.SearchSectionType
import com.example.myapplication2.core.model.SocialPost
import com.example.myapplication2.data.remote.GrokApi
import com.example.myapplication2.data.remote.GrokChatRequest
import com.example.myapplication2.data.remote.GrokMessage
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.CalendarRepository
import com.example.myapplication2.domain.repository.CacheRepository
import com.example.myapplication2.domain.repository.InsightsRepository
import com.example.myapplication2.domain.repository.LearningRepository
import com.example.myapplication2.domain.repository.SearchRepository
import com.example.myapplication2.domain.repository.StrategyRepository
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class LinkValidator @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun filterValidLinks(card: DashboardCard): DashboardCard {
        return withContext(Dispatchers.IO) {
            val verifiedCardLinks = verifyLinks(card.links, maxItems = 8)
            val verifiedSections = card.detailedSections.map { section ->
                section.copy(links = verifyLinks(section.links, maxItems = 2))
            }
            val verifiedSocial = card.socialInsights.take(4).filter { isUrlReachable(it.url) }

            card.copy(
                links = verifiedCardLinks,
                detailedSections = verifiedSections,
                socialInsights = verifiedSocial,
            )
        }
    }

    private fun verifyLinks(links: List<CardLink>, maxItems: Int): List<CardLink> {
        return links
            .asSequence()
            .mapNotNull(::canonicalizeLink)
            .filter { Patterns.WEB_URL.matcher(it.url).matches() }
            .distinctBy { it.url }
            .sortedWith(compareByDescending<CardLink> { isOfficialSourceUrl(it.url) }.thenBy { it.title })
            .take(maxItems)
            .mapNotNull { link ->
                if (isUrlReachable(link.url)) {
                    link.copy(isVerified = true)
                } else {
                    null
                }
            }
            .toList()
    }

    private fun isUrlReachable(url: String): Boolean {
        val headRequest = Request.Builder().url(url).head().build()
        val getRequest = Request.Builder().url(url).get().build()

        val headSuccess = runCatching {
            okHttpClient.newCall(headRequest).execute().use { response ->
                response.isSuccessful || response.code in 300..399
            }
        }.getOrDefault(false)

        if (headSuccess) return true

        return runCatching {
            okHttpClient.newCall(getRequest).execute().use { response ->
                response.isSuccessful || response.code in 300..399
            }
        }.getOrDefault(false)
    }
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val grokApi: GrokApi,
    private val jsonProvider: JsonProvider,
    private val seedContentFactory: SeedContentFactory,
    private val linkValidator: LinkValidator,
    private val cacheRepository: CacheRepository,
) : SearchRepository {

    override suspend fun search(query: String, userProfile: UserProfile): DashboardCard {
        val trimmedQuery = query.trim()
        if (shouldReturnBasicSearchDisclaimer(trimmedQuery)) {
            return seedContentFactory.outOfScopeSearchCard(trimmedQuery, userProfile)
                .copy(searchQuery = trimmedQuery)
        }

        val cacheKey = searchCacheKey(trimmedQuery, userProfile)
        val cachedCard = cacheRepository.getCachedPayload(cacheKey)?.let { payload ->
            decodeDashboardCardPayload(payload, jsonProvider)
        }?.copy(searchQuery = trimmedQuery)
        val freshCachedCard = cachedCard?.takeIf(::isFreshSearchCache)
        if (freshCachedCard != null) {
            return freshCachedCard.applySearchGrounding(
                query = trimmedQuery,
                origin = SearchCardOrigin.CACHE,
            )
        }

        val plan = requestQueryPlan(trimmedQuery, userProfile) ?: fallbackQueryPlan(trimmedQuery, userProfile)
        val evidence = requestEvidenceReport(trimmedQuery, userProfile, plan)
            ?: fallbackEvidenceReport(trimmedQuery, plan)
        val draft = requestPrimarySearchDraft(trimmedQuery, userProfile, plan, evidence)
        val finalCard = when {
            draft != null -> {
                val reviewed = requestSearchReview(trimmedQuery, userProfile, plan, evidence, draft)
                val judgedAnswer = requestJudgeSynthesis(trimmedQuery, userProfile, plan, evidence, draft, reviewed)
                    ?: synthesizeFallbackFinalAnswer(plan, evidence, draft, reviewed)
                val reviewedCard = createDashboardCardFromSearch(
                    query = trimmedQuery,
                    userProfile = userProfile,
                    plan = plan,
                    evidence = evidence,
                    finalAnswer = judgedAnswer,
                )
                val withCommunity = if (plan.needsCommunityView) {
                    val communityReport = requestSocialDiscussionReport(trimmedQuery, userProfile, reviewedCard)
                    communityReport?.let { mergeCommunityDiscussion(reviewedCard, it) } ?: reviewedCard
                } else {
                    reviewedCard
                }
                val grounded = withCommunity.applySearchGrounding(
                    query = trimmedQuery,
                    origin = SearchCardOrigin.PRIMARY,
                )
                val validated = linkValidator.filterValidLinks(grounded)
                validated.copy(dateMillis = System.currentTimeMillis())
            }
            cachedCard != null -> cachedCard.copy(
                searchQuery = trimmedQuery,
                confidenceLabel = "Може бути застаріло",
                urgencyLabel = cachedCard.urgencyLabel.ifBlank { "Перевірити оновлення" },
            ).applySearchGrounding(
                query = trimmedQuery,
                origin = SearchCardOrigin.STALE_CACHE,
            )
            else -> seedContentFactory.searchCard(trimmedQuery, userProfile)
                .copy(searchQuery = trimmedQuery)
                .applySearchGrounding(
                    query = trimmedQuery,
                    origin = SearchCardOrigin.FALLBACK,
                )
        }

        if (draft != null) {
            cacheRepository.putCachedPayload(
                key = cacheKey,
                payload = jsonProvider.json.encodeToString(DashboardCard.serializer(), finalCard),
                timestampMillis = System.currentTimeMillis(),
            )
        }
        return finalCard
    }

    private suspend fun requestQueryPlan(
        query: String,
        userProfile: UserProfile,
    ): SearchQueryPlan? {
        val jsonResponse = requestCompletion(
            systemPrompt = buildQueryPlanSystemPrompt(userProfile),
            userPrompt = buildQueryPlanUserPrompt(query, userProfile),
            temperature = 0.02,
            maxTokens = 900,
        )
        if (jsonResponse.isBlank()) return null
        val payload = sanitizeJson(jsonResponse)
        return runCatching {
            jsonProvider.json.decodeFromString(SearchQueryPlan.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun requestPrimarySearchDraft(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
    ): SearchAnswerDraft? {
        val executionProfile = buildSearchExecutionProfile(query)
        val jsonResponse = requestCompletion(
            systemPrompt = buildSearchSystemPrompt(userProfile),
            userPrompt = buildSearchUserPrompt(query, userProfile, plan, evidence),
            temperature = executionProfile.primaryTemperature,
            maxTokens = executionProfile.primaryMaxTokens,
        )
        if (jsonResponse.isBlank()) return null
        val payload = sanitizeJson(jsonResponse)
        return runCatching {
            jsonProvider.json.decodeFromString(SearchAnswerDraft.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun requestSearchReview(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
        draft: SearchAnswerDraft,
    ): SearchReviewReport? {
        val executionProfile = buildSearchExecutionProfile(query)
        val jsonResponse = requestCompletion(
            systemPrompt = buildReviewSystemPrompt(userProfile),
            userPrompt = buildReviewUserPrompt(query, userProfile, plan, evidence, draft),
            temperature = executionProfile.verificationTemperature,
            maxTokens = executionProfile.verificationMaxTokens,
        )
        if (jsonResponse.isBlank()) return null
        val payload = sanitizeJson(jsonResponse)
        return runCatching {
            jsonProvider.json.decodeFromString(SearchReviewReport.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun requestJudgeSynthesis(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
        draft: SearchAnswerDraft,
        review: SearchReviewReport?,
    ): SearchFinalAnswerDto? {
        val executionProfile = buildSearchExecutionProfile(query)
        val jsonResponse = requestCompletion(
            systemPrompt = buildJudgeSystemPrompt(userProfile),
            userPrompt = buildJudgeUserPrompt(query, userProfile, plan, evidence, draft, review),
            temperature = executionProfile.verificationTemperature,
            maxTokens = executionProfile.verificationMaxTokens,
        )
        if (jsonResponse.isBlank()) return null
        val payload = sanitizeJson(jsonResponse)
        return runCatching {
            jsonProvider.json.decodeFromString(SearchFinalAnswerDto.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun requestEvidenceReport(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
    ): SearchEvidenceReport? {
        val executionProfile = buildSearchExecutionProfile(query)
        val jsonResponse = requestCompletion(
            systemPrompt = buildEvidenceSystemPrompt(userProfile),
            userPrompt = buildEvidenceUserPrompt(query, userProfile, plan),
            temperature = 0.02,
            maxTokens = executionProfile.verificationMaxTokens,
        )
        if (jsonResponse.isBlank()) return null
        val payload = sanitizeJson(jsonResponse)
        return runCatching {
            jsonProvider.json.decodeFromString(SearchEvidenceReport.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun requestSocialDiscussionReport(
        query: String,
        userProfile: UserProfile,
        card: DashboardCard,
    ): SearchSocialDiscussionReport? {
        val executionProfile = buildSearchExecutionProfile(query)
        val jsonResponse = requestCompletion(
            systemPrompt = buildSocialSystemPrompt(userProfile),
            userPrompt = buildSocialUserPrompt(query, userProfile, card),
            temperature = executionProfile.socialTemperature,
            maxTokens = executionProfile.socialMaxTokens,
        )
        if (jsonResponse.isBlank()) return null
        val payload = sanitizeJson(jsonResponse)
        return runCatching {
            jsonProvider.json.decodeFromString(SearchSocialDiscussionReport.serializer(), payload)
        }.getOrNull()
    }

    private suspend fun requestCompletion(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        maxTokens: Int,
    ): String {
        return runCatching {
            grokApi.createCompletion(
                GrokChatRequest(
                    model = "grok-4-latest",
                    temperature = temperature,
                    maxTokens = maxTokens,
                    messages = listOf(
                        GrokMessage(role = "system", content = systemPrompt),
                        GrokMessage(role = "user", content = userPrompt),
                    ),
                ),
            ).choices.firstOrNull()?.message?.content.orEmpty()
        }.getOrNull().orEmpty()
    }

    private fun buildQueryPlanSystemPrompt(userProfile: UserProfile): String = """
        Ти аналізуєш regulatory search query для медичних виробів і готуєш короткий пошуковий план.
        Користувач: роль ${userProfile.role}, ніші ${userProfile.niches.joinToString()}, типи девайсів ${userProfile.deviceTypes.joinToString()}, країна ${userProfile.country}.

        Поверни тільки JSON SearchQueryPlan:
        - intent: definition / deadline / document / action / impact / comparison / risk / community / general
        - answerStyle: direct / checklist / comparison / risk / timeline
        - requiresOfficialSources: true/false
        - needsCommunityView: true/false (true тільки якщо користувач явно просить discussions/community/practitioner view)
        - titleHint
        - subtitleHint
        - focusPoints: 3-6 коротких фокусів
        - officialSourceTargets: 2-5 short labels (MDR, IVDR, MDCG, EUDAMED, NANDO, Article 10, Annex VIII ...)

        Не пиши довгих пояснень. Не генеруй відповідь на запит. Тільки план.
    """.trimIndent()

    private fun buildQueryPlanUserPrompt(query: String, userProfile: UserProfile): String = """
        Query: ${query.trim()}
        Role: ${userProfile.role}
        Niche: ${userProfile.niches.firstOrNull().orEmpty()}
        Device types: ${userProfile.deviceTypes.joinToString()}
        Return only JSON.
    """.trimIndent()

    private fun buildSearchSystemPrompt(userProfile: UserProfile): String = """
        Ти — Agent 3: Answer Synthesis Agent для regulatory/compliance тем у medical devices.
        Ти формуєш корисну відповідь для користувача на основі intake і evidence, але не є фінальним judge.

        Критичні правила:
        1. Перше речення має прямо відповідати на запит користувача.
        2. Якщо немає впевненого exact source для дати, статті, annex або MDCG — скажи це прямо і знизь confidenceLabel.
        3. Не вигадуй social discussions, exact deadlines чи article references.
        4. Кожен key finding, action, risk і expert note мають прямо стосуватися саме цього query, а не regulatory теми загалом.
        5. Краще менше пунктів, але точних. Без води, без повторень.
        6. Якщо інформації бракує, чесно зазнач evidence gaps замість домислювання.
        7. Official links мають підтримувати відповідь, а не бути випадковими загальними лінками.

        Профіль: роль ${userProfile.role}, ніші ${userProfile.niches.joinToString()}, типи девайсів ${userProfile.deviceTypes.joinToString()}, країна ${userProfile.country}.

        Поверни тільки JSON SearchAnswerDraft:
        - title
        - subtitle
        - directAnswer
        - whyItMatters
        - keyFindings (3-6)
        - actions (3-6)
        - risks (2-5)
        - officialLinks (8-12 CardLink, пріоритет official sources і high-value research links)
        - followUpQuestions (3)
        - expertNote
        - evidenceGaps (0-4)
        - confidenceLabel: Висока впевненість / Середня / Потребує перевірки
        - urgencyLabel: Критично / До 90 днів / Моніторинг / Перевірити оновлення
    """.trimIndent()

    private fun buildSearchUserPrompt(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
    ): String = """
        Запит: ${query.trim()}
        Intent: ${plan.intent}
        Answer style: ${plan.answerStyle}
        Requires official sources: ${plan.requiresOfficialSources}
        Needs community view: ${plan.needsCommunityView}
        Focus points: ${plan.focusPoints.joinToString()}
        Official source targets: ${plan.officialSourceTargets.joinToString()}
        Profile role: ${userProfile.role}
        Profile niche: ${userProfile.niches.firstOrNull().orEmpty()}
        Profile device types: ${userProfile.deviceTypes.joinToString()}
        Evidence summary: ${evidence.summary}
        Supported claims: ${evidence.supportedClaims.joinToString()}
        Evidence gaps: ${evidence.evidenceGaps.joinToString()}
        Official links: ${jsonProvider.json.encodeToString(ListSerializer(CardLink.serializer()), evidence.officialLinks)}

        Відповідь має бути практичною для юзера:
        - спочатку пряма відповідь;
        - потім тільки найкорисніші факти і наслідки;
        - тільки офіційні або максимально авторитетні лінки;
        - якщо exact confirmation немає, чесно зазнач невизначеність.
        - не включай пункти, які можна вставити майже в будь-який regulatory query.
        - спирайся лише на supported claims та official links з evidence block вище.

        Return only JSON.
    """.trimIndent()

    private fun buildEvidenceSystemPrompt(userProfile: UserProfile): String = """
        Ти збираєш evidence bundle для regulatory research по medical devices.
        Роль користувача: ${userProfile.role}, ніша: ${userProfile.niches.joinToString()}, типи девайсів: ${userProfile.deviceTypes.joinToString()}, країна: ${userProfile.country}.

        Завдання:
        - визначити, які твердження можна підтримати для цього query;
        - повернути тільки official або highly authoritative links;
        - явно вказати, чого не вистачає для точної відповіді.

        Поверни тільки JSON SearchEvidenceReport:
        - summary
        - supportedClaims (3-6 коротких тверджень)
        - officialLinks (8-12 CardLink)
        - evidenceGaps (0-4)
        - sourceNotes (2-5 коротких note про силу/тип джерел)
    """.trimIndent()

    private fun buildEvidenceUserPrompt(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
    ): String = """
        Query: $query
        Intent: ${plan.intent}
        Focus points: ${plan.focusPoints.joinToString()}
        Official source targets: ${plan.officialSourceTargets.joinToString()}
        User role: ${userProfile.role}
        User niche: ${userProfile.niches.firstOrNull().orEmpty()}
        Device types: ${userProfile.deviceTypes.joinToString()}

        Потрібно повернути evidence-first блок для подальшої генерації відповіді.
        Return only JSON.
    """.trimIndent()

    private fun buildReviewSystemPrompt(userProfile: UserProfile): String = """
        Ти — Agent 4: Red-Team Skeptic для regulatory search answer.
        Твоя задача: атакувати слабкі місця відповіді, знаходити непідтверджені claim-и, general filler, надто широкі формулювання і ризикові exact references.
        Роль користувача: ${userProfile.role}. Ніша: ${userProfile.niches.joinToString()}.

        Правила:
        - Якщо перше речення не відповідає прямо на query — виправ.
        - Якщо є exact dates/articles/annexes без strong support — понизь confidence.
        - Не додавай нових неперевірених claims.
        - Видали findings/actions/risks, які не є прямо релевантними до query.
        - Залиши тільки links, які реально підсилюють research answer.
        - Objections і mustFixes — чітко, одним реченням кожен.

        Поверни тільки JSON SearchReviewReport:
        - correctedTitle
        - correctedSubtitle
        - correctedDirectAnswer
        - correctedWhyItMatters
        - refinedFindings
        - refinedActions
        - refinedRisks
        - refinedLinks
        - refinedExpertNote
        - evidenceGaps
        - objections
        - mustFixes
        - confidenceLabel
        - urgencyLabel
    """.trimIndent()

    private fun buildReviewUserPrompt(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
        draft: SearchAnswerDraft,
    ): String = """
        Query: $query
        Plan: ${jsonProvider.json.encodeToString(SearchQueryPlan.serializer(), plan)}
        Evidence: ${jsonProvider.json.encodeToString(SearchEvidenceReport.serializer(), evidence)}
        User role: ${userProfile.role}
        User niche: ${userProfile.niches.firstOrNull().orEmpty()}
        Draft answer:
        ${jsonProvider.json.encodeToString(SearchAnswerDraft.serializer(), draft)}

        Return only JSON.
    """.trimIndent()

    private fun buildJudgeSystemPrompt(userProfile: UserProfile): String = """
        Ти — Agent 5: Judge / Synthesizer для regulatory search helper.
        Ти отримуєш intake, evidence, draft answer і red-team critique. Твоя задача — видати найкращу фінальну відповідь для користувача.
        Роль користувача: ${userProfile.role}. Ніша: ${userProfile.niches.joinToString()}.

        Правила:
        - Приймай тільки ті твердження, які сумісні з evidence і витримують red-team critique.
        - Якщо є невирішені exact dates/articles/annexes — познач це в evidenceGaps і не звучай категорично.
        - Відповідь має бути короткою, чіткою, корисною і строго прив'язаною до query.
        - Дії та ризики мають бути практичними, але не generic.
        - Без води, без повторень. Тільки факти та конкретні дії.

        Поверни тільки JSON SearchFinalAnswerDto:
        - title
        - subtitle
        - directAnswer
        - whyItMatters
        - keyFindings
        - actions
        - risks
        - officialLinks
        - followUpQuestions
        - expertNote
        - evidenceGaps
        - confidenceLabel
        - urgencyLabel
        - auditNotes
    """.trimIndent()

    private fun buildJudgeUserPrompt(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
        draft: SearchAnswerDraft,
        review: SearchReviewReport?,
    ): String = """
        Query: $query
        User role: ${userProfile.role}
        User niche: ${userProfile.niches.firstOrNull().orEmpty()}
        Intake plan: ${jsonProvider.json.encodeToString(SearchQueryPlan.serializer(), plan)}
        Evidence: ${jsonProvider.json.encodeToString(SearchEvidenceReport.serializer(), evidence)}
        Draft answer: ${jsonProvider.json.encodeToString(SearchAnswerDraft.serializer(), draft)}
        Red-team critique: ${jsonProvider.json.encodeToString(SearchReviewReport.serializer(), review ?: SearchReviewReport())}

        Return only JSON.
    """.trimIndent()

    private fun buildSocialSystemPrompt(userProfile: UserProfile): String = """
        Ти збираєш тільки community/practitioner context для regulatory теми. Роль: ${userProfile.role}, ніша: ${userProfile.niches.joinToString()}.

        Повертай social block тільки якщо є справді корисний practitioner angle. Якщо нема впевненого community context — повертай порожні lists та blank summary.
        Не вигадуй discussion як факт.

        Поверни JSON SearchSocialDiscussionReport: discussionSummary, keyThemes, posts (platform, author, text, dateMillis, url), links.
    """.trimIndent()

    private fun buildSocialUserPrompt(
        query: String,
        userProfile: UserProfile,
        card: DashboardCard,
    ): String = """
        Original query: $query
        User context:
        - role: ${userProfile.role}
        - country: ${userProfile.country}
        - niches: ${userProfile.niches.joinToString()}
        - device types: ${userProfile.deviceTypes.joinToString()}

        Existing dossier context:
        ${jsonProvider.json.encodeToString(DashboardCard.serializer(), card)}

        Find only the most relevant practitioner discussions related to this topic, and only if they add value beyond official sources.
        Return only JSON.
    """.trimIndent()

    private fun createDashboardCardFromSearch(
        query: String,
        userProfile: UserProfile,
        plan: SearchQueryPlan,
        evidence: SearchEvidenceReport,
        finalAnswer: SearchFinalAnswerDto,
    ): DashboardCard {
        val roleLabel = userProfile.role.ifBlank { "RA / QA" }
        val mergedLinks = mergeLinks(evidence.officialLinks + finalAnswer.officialLinks, emptyList(), maxItems = 24)
        val findings = (evidence.supportedClaims + finalAnswer.keyFindings)
            .researchNormalized(maxItems = 6)
        val actions = finalAnswer.actions
            .researchNormalized(maxItems = 6)
        val risks = finalAnswer.risks
            .researchNormalized(maxItems = 5)
        val gaps = (evidence.evidenceGaps + finalAnswer.evidenceGaps)
            .researchNormalized(maxItems = 4)
        val directAnswer = finalAnswer.directAnswer
            .ifBlank { "Потрібна додаткова перевірка: не вдалося сформувати достатньо точну відповідь на цей запит." }
        val whyItMatters = finalAnswer.whyItMatters
        val expertNote = finalAnswer.expertNote
        val subtitle = finalAnswer.subtitle.ifBlank { plan.subtitleHint.ifBlank { "Відповідь адаптована під роль $roleLabel." } }

        return DashboardCard(
            type = CardType.SEARCH_HISTORY,
            searchQuery = query,
            title = finalAnswer.title.ifBlank {
                plan.titleHint.ifBlank { query }
            },
            subtitle = subtitle,
            body = directAnswer,
            expertOpinion = expertNote.nullIfBlank(),
            analytics = buildAnalyticsSummary(roleLabel, whyItMatters, gaps, evidence.sourceNotes + finalAnswer.auditNotes).nullIfBlank(),
            actionChecklist = actions.ifEmpty {
                listOf("[Середнє] Відкрити офіційні джерела та звірити деталі по цьому запиту.")
            },
            riskFlags = risks.ifEmpty {
                listOf("Без перевірки official sources відповідь не варто використовувати як остаточний compliance висновок.")
            },
            impactAreas = findings.ifEmpty {
                listOf("Відповідь сформовано стисло; відкрийте джерела нижче для deeper review.")
            },
            confidenceLabel = finalAnswer.confidenceLabel.ifBlank { "Середня" },
            urgencyLabel = finalAnswer.urgencyLabel.ifBlank { "Моніторинг" },
            links = mergedLinks,
            resources = finalAnswer.followUpQuestions.normalizedUnique(maxItems = 3),
            detailedSections = buildResearchSections(
                directAnswer = directAnswer,
                whyItMatters = whyItMatters,
                expertNote = expertNote,
                focusPoints = plan.focusPoints,
                findings = findings,
                actions = actions,
                risks = risks,
                gaps = gaps,
                evidenceSummary = evidence.summary,
                sourceNotes = evidence.sourceNotes,
                links = mergedLinks,
            ),
            niche = userProfile.niches.firstOrNull().orEmpty(),
            dateMillis = System.currentTimeMillis(),
        )
    }

    private fun mergeCommunityDiscussion(
        card: DashboardCard,
        report: SearchSocialDiscussionReport,
    ): DashboardCard {
        if (report.discussionSummary.isBlank() && report.posts.isEmpty() && report.links.isEmpty()) return card
        val socialSection = SearchSection(
            type = SearchSectionType.SOCIAL_DISCUSSION,
            title = "Практичні обговорення",
            content = report.discussionSummary,
            resources = report.keyThemes.normalizedUnique(maxItems = 5),
            links = mergeLinks(emptyList(), report.links, maxItems = 4),
        )
        return card.copy(
            socialInsights = report.posts
                .filter { it.url.isNotBlank() && Patterns.WEB_URL.matcher(it.url).matches() }
                .distinctBy { "${it.platform}|${it.author}|${it.url}|${it.text}" }
                .take(6),
            links = mergeLinks(card.links, report.links, maxItems = 24),
            detailedSections = card.detailedSections.filterNot { it.type == SearchSectionType.SOCIAL_DISCUSSION } + socialSection,
        )
    }
}

@Serializable
private data class CalendarEventRaw(
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val date: String = "",
    val dateMillis: Long = 0L,
    val priority: String = "medium",
    val niche: String = "",
    val description: String = "",
    val source: String = "",
    val officialLinks: List<CardLink> = emptyList(),
    val analysis: String = "",
    val impactSummary: String = "",
    val actions: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val importanceScore: Int = 50,
)

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val grokApi: GrokApi,
    private val jsonProvider: JsonProvider,
    private val seedContentFactory: SeedContentFactory,
    private val linkValidator: LinkValidator,
    private val cacheRepository: CacheRepository,
) : CalendarRepository {

    override suspend fun generateCalendar(niches: List<String>, userProfile: UserProfile): List<DashboardCard> {
        val cacheKey = "calendar_v6_${nichesCacheKey(niches)}"
        val fallback = seedContentFactory.calendarCards(niches)
        val fiveDaysMillis = 5 * 24 * 60 * 60 * 1000L

        val freshCached = cacheRepository.getCachedPayloadIfFresh(cacheKey, fiveDaysMillis)
        if (freshCached != null) {
            val parsed = runCatching {
                jsonProvider.json.decodeFromString(ListSerializer(DashboardCard.serializer()), freshCached)
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }

        val response = runCatching {
            grokApi.createCompletion(
                GrokChatRequest(
                    model = "grok-4-latest",
                    temperature = 0.0,
                    maxTokens = 20480,
                    messages = listOf(
                        GrokMessage(role = "system", content = buildCalendarSystemPrompt(userProfile, niches)),
                        GrokMessage(role = "user", content = buildCalendarUserPrompt(niches, userProfile)),
                    ),
                ),
            ).choices.firstOrNull()?.message?.content.orEmpty()
        }.getOrNull().orEmpty()

        val rawEvents = if (response.isNotBlank()) {
            val payload = sanitizeJson(response)
            parseCalendarEvents(payload)?.take(150)
        } else null

        val cards = if (!rawEvents.isNullOrEmpty()) {
            rawEvents.mapIndexed { index, raw ->
                calendarEventToCard(raw, userProfile, niches.getOrNull(index % niches.size).orEmpty())
            }
        } else {
            cacheRepository.getCachedPayload(cacheKey)?.let { payload ->
                runCatching {
                    jsonProvider.json.decodeFromString(ListSerializer(DashboardCard.serializer()), payload)
                }.getOrNull()
            } ?: fallback
        }

        val now = System.currentTimeMillis()
        val oneYearMs = 365L * 24 * 60 * 60 * 1000
        val threeYearsMs = 3 * 365L * 24 * 60 * 60 * 1000
        val minDateMillis = now - oneYearMs
        val maxDateMillis = now + threeYearsMs

        val validatedCards = buildList {
            cards
                .filter { it.dateMillis in minDateMillis..maxDateMillis }
                .forEach { card ->
                    add(linkValidator.filterValidLinks(card.copy(type = CardType.REGULATORY_EVENT)))
                }
        }
        cacheRepository.putCachedPayload(
            key = cacheKey,
            payload = jsonProvider.json.encodeToString(ListSerializer(DashboardCard.serializer()), validatedCards),
            timestampMillis = System.currentTimeMillis(),
        )
        return validatedCards
    }

    private fun parseCalendarEvents(payload: String): List<CalendarEventRaw>? {
        val parse: (String) -> List<CalendarEventRaw>? = { s ->
            runCatching {
                jsonProvider.json.decodeFromString(ListSerializer(CalendarEventRaw.serializer()), s)
            }.getOrNull()?.filter { it.title.isNotBlank() }
        }
        return parse(payload) ?: payload.indexOf('[').takeIf { it >= 0 }?.let { parse(payload.substring(it)) }
    }

    private fun buildCalendarSystemPrompt(userProfile: UserProfile, niches: List<String>): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.YEAR, -1)
        val startDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(cal.time)
        cal.add(java.util.Calendar.YEAR, 4)
        val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(cal.time)
        return """
        EU MDR/IVDR expert. Niches: ${niches.joinToString(", ")}. Sources: EUR-Lex, MDCG, EC, EUDAMED.
        Date range: $startDate–$endDate. Role: ${userProfile.role}. Country: ${userProfile.country}.

        Return ONLY valid JSON array, no text:
        [{"title":"","date":"YYYY-MM-DD","body":"","priority":"high|medium|low","source":"","niche":"","officialLinks":[{"title":"","url":"","sourceLabel":""}],"importanceScore":1-100}]
        Min 50 events, max 150. Short values: title≤80 chars, body≤120. Key deadlines: EUDAMED 2026-05-28, legacy 2026-11-28.
        """.trimIndent()
    }

    private fun buildCalendarUserPrompt(niches: List<String>, userProfile: UserProfile): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.YEAR, -1)
        val start = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(cal.time)
        cal.add(java.util.Calendar.YEAR, 4)
        val end = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(cal.time)
        return "Niches: ${niches.joinToString(",")}. Role: ${userProfile.role}. Devices: ${userProfile.deviceTypes.take(5).joinToString(",")}. Events $start–$end, JSON only, 50–150 items."
    }

    private fun calendarEventToCard(raw: CalendarEventRaw, userProfile: UserProfile, niche: String): DashboardCard {
        val priority = when (raw.priority.uppercase()) {
            "CRITICAL", "HIGH" -> if (raw.priority.equals("critical", ignoreCase = true)) Priority.CRITICAL else Priority.HIGH
            else -> Priority.MEDIUM
        }
        val dateMillis = when {
            raw.dateMillis > 0 -> raw.dateMillis
            raw.date.isNotBlank() -> parseDateToMillis(raw.date) ?: (System.currentTimeMillis() + 86_400_000L)
            else -> System.currentTimeMillis() + 86_400_000L
        }
        val body = raw.body.ifBlank { raw.description }
        val mergedLinks = mergeLinks(raw.officialLinks, emptyList(), maxItems = 12)
        return DashboardCard(
            type = CardType.REGULATORY_EVENT,
            title = raw.title.ifBlank { "Regulatory event" },
            subtitle = raw.subtitle.ifBlank { "Подія" },
            body = body,
            expertOpinion = raw.analysis.takeIf { it.isNotBlank() },
            analytics = buildString {
                if (raw.impactSummary.isNotBlank()) append(raw.impactSummary)
                if (raw.importanceScore in 1..100) append(" Важливість: ${raw.importanceScore}/100.")
            }.trim().takeIf { it.isNotBlank() },
            actionChecklist = raw.actions.take(5),
            riskFlags = raw.risks.take(4),
            impactAreas = listOfNotNull(
                raw.impactSummary.takeIf { it.isNotBlank() },
                "Importance: ${raw.importanceScore}/100",
            ).filter { it.isNotBlank() }.take(4),
            confidenceLabel = when {
                raw.importanceScore >= 80 -> "Критично"
                raw.importanceScore >= 50 -> "Важливо"
                else -> "Моніторинг"
            },
            urgencyLabel = when (priority) {
                Priority.CRITICAL -> "Терміново"
                Priority.HIGH -> "До 90 днів"
                else -> "Перевірити"
            },
            links = mergedLinks,
            resources = emptyList(),
            niche = raw.niche.ifBlank { niche },
            dateMillis = dateMillis,
            priority = priority,
        )
    }
}

@Singleton
class InsightsRepositoryImpl @Inject constructor(
    private val seedContentFactory: SeedContentFactory,
) : InsightsRepository {
    override suspend fun loadInsights(userProfile: UserProfile): List<DashboardCard> = seedContentFactory.insightCards(userProfile)
}

@Singleton
class StrategyRepositoryImpl @Inject constructor(
    private val seedContentFactory: SeedContentFactory,
) : StrategyRepository {
    override suspend fun loadStrategies(userProfile: UserProfile): List<DashboardCard> = seedContentFactory.strategyCards(userProfile)
}

@Singleton
class LearningRepositoryImpl @Inject constructor(
    private val seedContentFactory: SeedContentFactory,
) : LearningRepository {
    override suspend fun loadLearningModules(userProfile: UserProfile): List<DashboardCard> = seedContentFactory.learningCards(userProfile)
}

private fun parseDateToMillis(dateStr: String): Long? {
    return runCatching {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.parse(dateStr.trim())?.time
    }.getOrNull()
}

internal fun sanitizeJson(raw: String): String {
    val cleaned = raw
        .replace("```json", "")
        .replace("```", "")
        .trim()

    val objectStart = cleaned.indexOf('{').takeIf { it >= 0 } ?: Int.MAX_VALUE
    val arrayStart = cleaned.indexOf('[').takeIf { it >= 0 } ?: Int.MAX_VALUE
    val start = minOf(objectStart, arrayStart)
    if (start == Int.MAX_VALUE) return cleaned

    val opening = cleaned[start]
    val closing = if (opening == '[') ']' else '}'
    val end = cleaned.lastIndexOf(closing)
    if (end <= start) return cleaned.substring(start).trim()
    return cleaned.substring(start, end + 1).trim()
}

@Serializable
private data class SearchQueryPlan(
    val intent: String = "general",
    val answerStyle: String = "direct",
    val requiresOfficialSources: Boolean = true,
    val needsCommunityView: Boolean = false,
    val titleHint: String = "",
    val subtitleHint: String = "",
    val focusPoints: List<String> = emptyList(),
    val officialSourceTargets: List<String> = emptyList(),
)

@Serializable
private data class SearchAnswerDraft(
    val title: String = "",
    val subtitle: String = "",
    val directAnswer: String = "",
    val whyItMatters: String = "",
    val keyFindings: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val officialLinks: List<CardLink> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val expertNote: String = "",
    val evidenceGaps: List<String> = emptyList(),
    val confidenceLabel: String = "",
    val urgencyLabel: String = "",
)

@Serializable
private data class SearchEvidenceReport(
    val summary: String = "",
    val supportedClaims: List<String> = emptyList(),
    val officialLinks: List<CardLink> = emptyList(),
    val evidenceGaps: List<String> = emptyList(),
    val sourceNotes: List<String> = emptyList(),
)

@Serializable
private data class SearchReviewReport(
    val correctedTitle: String = "",
    val correctedSubtitle: String = "",
    val correctedDirectAnswer: String = "",
    val correctedWhyItMatters: String = "",
    val refinedFindings: List<String> = emptyList(),
    val refinedActions: List<String> = emptyList(),
    val refinedRisks: List<String> = emptyList(),
    val refinedLinks: List<CardLink> = emptyList(),
    val refinedExpertNote: String = "",
    val evidenceGaps: List<String> = emptyList(),
    val objections: List<String> = emptyList(),
    val mustFixes: List<String> = emptyList(),
    val confidenceLabel: String = "",
    val urgencyLabel: String = "",
)

@Serializable
private data class SearchFinalAnswerDto(
    val title: String = "",
    val subtitle: String = "",
    val directAnswer: String = "",
    val whyItMatters: String = "",
    val keyFindings: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val officialLinks: List<CardLink> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val expertNote: String = "",
    val evidenceGaps: List<String> = emptyList(),
    val confidenceLabel: String = "",
    val urgencyLabel: String = "",
    val auditNotes: List<String> = emptyList(),
)

@Serializable
private data class SearchSocialDiscussionReport(
    val discussionSummary: String = "",
    val keyThemes: List<String> = emptyList(),
    val posts: List<SocialPost> = emptyList(),
    val links: List<CardLink> = emptyList(),
)

fun nichesCacheKey(niches: List<String>): String {
    return digestKey(niches.sorted().joinToString("_"))
}

private fun searchCacheKey(query: String, userProfile: UserProfile): String {
    return "search_v10_" + digestKey(
        listOf(
            query.trim().lowercase(),
            userProfile.role,
            userProfile.country,
            userProfile.niches.joinToString("|"),
            userProfile.deviceTypes.joinToString("|"),
        ).joinToString("::"),
    )
}

private fun isFreshSearchCache(card: DashboardCard): Boolean {
    val ageMillis = System.currentTimeMillis() - card.dateMillis
    return ageMillis in 0..(12 * 60 * 60 * 1000L)
}

private fun decodeDashboardCardPayload(payload: String, jsonProvider: JsonProvider): DashboardCard? {
    val sanitized = sanitizeJson(payload)
    return runCatching {
        jsonProvider.json.decodeFromString(DashboardCard.serializer(), sanitized)
    }.getOrNull()
}

private fun List<String>.normalizedUnique(maxItems: Int): List<String> {
    return this
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(maxItems)
}

private fun List<String>.researchNormalized(maxItems: Int): List<String> {
    return this
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { item ->
            val normalized = item.lowercase()
            normalized.length < 12 ||
                normalized == "n/a" ||
                normalized == "unknown" ||
                normalized.contains("regulatory topic is important") ||
                normalized.contains("важлива регуляторна тема") ||
                normalized.contains("потрібно врахувати вимоги") ||
                normalized.contains("review the applicable requirements") ||
                normalized.contains("зверніться до офіційних джерел") ||
                normalized.contains("consult official sources")
        }
        .distinct()
        .take(maxItems)
}

private fun mergeLinks(
    base: List<CardLink>,
    extra: List<CardLink>,
    maxItems: Int,
): List<CardLink> {
    return (base + extra)
        .mapNotNull(::canonicalizeLink)
        .filter { it.title.isNotBlank() && it.url.isNotBlank() }
        .distinctBy { canonicalizeUrl(it.url) }
        .sortedWith(compareByDescending<CardLink> { isOfficialSourceUrl(it.url) }.thenBy { it.title })
        .take(maxItems)
}

private fun String.withVerificationNotes(notes: String): String {
    if (notes.isBlank()) return this
    if (this.isBlank()) return notes
    return "$this\n\nПеревірка якості відповіді:\n$notes"
}

private fun String.nullIfBlank(): String? = takeIf { it.isNotBlank() }

private data class SearchExecutionProfile(
    val operatingMode: String,
    val responseDepth: String,
    val primaryMaxTokens: Int,
    val primaryTemperature: Double,
    val verificationMaxTokens: Int,
    val verificationTemperature: Double,
    val socialMaxTokens: Int,
    val socialTemperature: Double,
)

private enum class SearchCardOrigin {
    PRIMARY,
    CACHE,
    STALE_CACHE,
    FALLBACK,
}

private fun buildSearchExecutionProfile(query: String): SearchExecutionProfile {
    return when (inferSearchOperatingMode(query)) {
        "deadline" -> SearchExecutionProfile(
            operatingMode = "deadline",
            responseDepth = "targeted_timeline",
            primaryMaxTokens = 4500,
            primaryTemperature = 0.04,
            verificationMaxTokens = 2100,
            verificationTemperature = 0.02,
            socialMaxTokens = 1800,
            socialTemperature = 0.03,
        )
        "action" -> SearchExecutionProfile(
            operatingMode = "action",
            responseDepth = "deep_action_dossier",
            primaryMaxTokens = 6600,
            primaryTemperature = 0.05,
            verificationMaxTokens = 2900,
            verificationTemperature = 0.02,
            socialMaxTokens = 2200,
            socialTemperature = 0.04,
        )
        "document" -> SearchExecutionProfile(
            operatingMode = "document",
            responseDepth = "source_anchored_dossier",
            primaryMaxTokens = 6100,
            primaryTemperature = 0.04,
            verificationMaxTokens = 2700,
            verificationTemperature = 0.02,
            socialMaxTokens = 1900,
            socialTemperature = 0.03,
        )
        "risk" -> SearchExecutionProfile(
            operatingMode = "risk",
            responseDepth = "risk_focused_dossier",
            primaryMaxTokens = 5800,
            primaryTemperature = 0.05,
            verificationMaxTokens = 2700,
            verificationTemperature = 0.02,
            socialMaxTokens = 2400,
            socialTemperature = 0.04,
        )
        "compliance" -> SearchExecutionProfile(
            operatingMode = "compliance",
            responseDepth = "control_gap_dossier",
            primaryMaxTokens = 5900,
            primaryTemperature = 0.05,
            verificationMaxTokens = 2700,
            verificationTemperature = 0.02,
            socialMaxTokens = 2100,
            socialTemperature = 0.04,
        )
        "business" -> SearchExecutionProfile(
            operatingMode = "business",
            responseDepth = "market_execution_dossier",
            primaryMaxTokens = 5400,
            primaryTemperature = 0.05,
            verificationMaxTokens = 2400,
            verificationTemperature = 0.02,
            socialMaxTokens = 2100,
            socialTemperature = 0.04,
        )
        else -> SearchExecutionProfile(
            operatingMode = "general",
            responseDepth = "standard_regulatory_dossier",
            primaryMaxTokens = 5000,
            primaryTemperature = 0.05,
            verificationMaxTokens = 2400,
            verificationTemperature = 0.02,
            socialMaxTokens = 1900,
            socialTemperature = 0.04,
        )
    }
}

private fun inferSearchOperatingMode(query: String): String {
    val value = query.lowercase()
    return when {
        value.contains("deadline") || value.contains("when ") || value.contains("when does") ||
            value.contains("by ") || value.contains("until") || value.contains("строк") ||
            value.contains("термін") || value.contains("дедлайн") || value.contains("до ") -> "deadline"

        value.contains("step-by-step") || value.contains("step by step") || value.contains("how to") ||
            value.contains("what should i do") || value.contains("що робити") ||
            value.contains("покрок") || value.contains("prepare") || value.contains("підгот") ||
            value.contains("implement") || value.contains("оновити") || value.contains("update") ||
            value.contains("checklist") -> "action"

        value.contains("risk") || value.contains("ризик") || value.contains("consequence") ||
            value.contains("наслід") || value.contains("penalt") || value.contains("штраф") ||
            value.contains("recall") || value.contains("non-compliance") || value.contains("невідповід") ->
            "risk"

        value.contains("technical documentation") || value.contains("tech doc") ||
            value.contains("document") || value.contains("documentation") ||
            value.contains("документ") || value.contains("article") || value.contains("annex") ||
            value.contains("статт") || value.contains("додат") || value.contains("mdcg") ||
            value.contains("guidance") || value.contains("ifu") || value.contains("cer") ||
            value.contains("per") || value.contains("template") || value.contains("sop") -> "document"

        value.contains("compliance") || value.contains("qms") || value.contains("audit") ||
            value.contains("gap") || value.contains("conformity") || value.contains("requirement") ||
            value.contains("обов") || value.contains("вимог") || value.contains("відповідн") ||
            value.contains("capa") || value.contains("control") -> "compliance"

        value.contains("market") || value.contains("launch") || value.contains("import") ||
            value.contains("distribution") || value.contains("supplier") || value.contains("operator") ||
            value.contains("registration") || value.contains("commercial") || value.contains("sell") ||
            value.contains("placing on the market") || value.contains("go-to-market") ||
            value.contains("вивести на ринок") || value.contains("ринок") || value.contains("постач") ->
            "business"

        else -> "general"
    }
}

private fun inferSearchIntent(query: String): String {
    val value = query.lowercase()
    return when {
        value.contains("difference") || value.contains("різниц") || value.contains("vs") || value.contains("compare") || value.contains("порів") -> "comparison"
        value.contains("step-by-step") || value.contains("step by step") || value.contains("how to") || value.contains("що робити") || value.contains("what should") || value.contains("підгот") || value.contains("prepare") || value.contains("conduct") || value.contains("оновити") || value.contains("update") -> "action_checklist"
        value.contains("deadline") || value.contains("when does") || value.contains("what must be done by") || value.contains("коли") || value.contains("строк") || value.contains("term") || value.contains("deadline") || value.contains("до ") -> "deadline_timing"
        value.contains("risk") || value.contains("ризик") || value.contains("наслід") || value.contains("non-compliance") || value.contains("penalt") || value.contains("recall") -> "risk_analysis"
        value.contains("market") || value.contains("launch") || value.contains("import") || value.contains("distribution") || value.contains("supplier") || value.contains("registration") || value.contains("operator") -> "business_impact"
        value.contains("compliance") || value.contains("qms") || value.contains("audit") || value.contains("gap") || value.contains("conformity") || value.contains("вимог") || value.contains("обов") -> "compliance_review"
        value.contains("document") || value.contains("documentation") || value.contains("документ") || value.contains("ifu") || value.contains("template") || value.contains("sop") -> "document_update"
        value.contains("article") || value.contains("annex") || value.contains("статт") || value.contains("додат") || value.contains("mdcg") || value.contains("guidance") || value.contains("udi") -> "specific_guidance"
        value.contains("my ") || value.contains("для мо") || value.contains("for my") || value.contains("as ra") || value.contains("my class") || value.contains("мої") || value.contains("my device") -> "personalized_impact"
        value.contains("impact") || value.contains("вплив") || value.contains("що означає") || value.contains("affect my") || value.contains("affect") -> "impact_analysis"
        value.contains("source") || value.contains("джерел") || value.contains("посилан") -> "source_lookup"
        value.contains("what's new") || value.contains("what is new") || value.contains("what changed") || value.contains("main changes") || value.contains("нов") || value.contains("зміни") || value.contains("explain") -> "basic_general"
        else -> "general_regulatory_query"
    }
}

private fun extractSearchFocusSignals(query: String): List<String> {
    val knownSignals = listOf(
        "mdr", "ivdr", "mdcg", "eudamed", "cer", "per", "pmcf", "pmpf", "pms",
        "vigilance", "udi", "gspr", "technical documentation", "notified body",
        "clinical evaluation", "performance evaluation", "iso 13485", "iso 14971",
        "iec 62304", "iec 62366", "software", "saas", "ai", "labeling", "classification",
        "legacy devices", "class iia", "class iib", "class iii", "orthopedic implants",
        "custom-made devices", "qms", "article 10", "article 61", "annex viii", "post-market surveillance",
    )
    val normalized = query.lowercase()
    return knownSignals.filter { normalized.contains(it) }.take(8)
}

private fun fallbackQueryPlan(query: String, userProfile: UserProfile): SearchQueryPlan {
    val normalized = query.trim()
    return SearchQueryPlan(
        intent = inferSearchIntent(normalized),
        answerStyle = when (inferSearchOperatingMode(normalized)) {
            "deadline" -> "timeline"
            "action" -> "checklist"
            "risk" -> "risk"
            else -> "direct"
        },
        requiresOfficialSources = queryNeedsStrictFactCheck(normalized),
        needsCommunityView = queryAsksForCommunityView(normalized),
        titleHint = normalized.take(72),
        subtitleHint = "Відповідь адаптована під роль ${userProfile.role.ifBlank { "RA / QA" }}.",
        focusPoints = extractSearchFocusSignals(normalized).ifEmpty { listOf("regulatory answer", "official sources") },
        officialSourceTargets = inferOfficialSourceTargets(normalized),
    )
}

private fun fallbackEvidenceReport(
    query: String,
    plan: SearchQueryPlan,
): SearchEvidenceReport {
    val officialLinks = buildOfficialGroundingLinks(query)
    return SearchEvidenceReport(
        summary = "Зібрано базову evidence-рамку по запиту без окремого evidence-pass.",
        supportedClaims = plan.focusPoints.researchNormalized(maxItems = 4),
        officialLinks = officialLinks,
        evidenceGaps = if (queryNeedsStrictFactCheck(query)) {
            listOf("Потрібно додатково перевірити точні дати, статті або annex references.")
        } else {
            emptyList()
        },
        sourceNotes = buildList {
            if (officialLinks.any { isOfficialSourceUrl(it.url) }) {
                add("Використано official sources як базову опору для відповіді.")
            }
            if (plan.requiresOfficialSources) {
                add("Для цього типу запиту official links мають пріоритет над general commentary.")
            }
        },
    )
}

private fun synthesizeFallbackFinalAnswer(
    plan: SearchQueryPlan,
    evidence: SearchEvidenceReport,
    draft: SearchAnswerDraft,
    review: SearchReviewReport?,
): SearchFinalAnswerDto {
    return SearchFinalAnswerDto(
        title = (review?.correctedTitle ?: "").ifBlank { draft.title },
        subtitle = (review?.correctedSubtitle ?: "").ifBlank { draft.subtitle.ifBlank { plan.subtitleHint } },
        directAnswer = (review?.correctedDirectAnswer ?: "").ifBlank { draft.directAnswer },
        whyItMatters = (review?.correctedWhyItMatters ?: "").ifBlank { draft.whyItMatters },
        keyFindings = (evidence.supportedClaims + review?.refinedFindings.orEmpty() + draft.keyFindings).researchNormalized(maxItems = 6),
        actions = (review?.refinedActions.orEmpty() + draft.actions).researchNormalized(maxItems = 6),
        risks = (review?.refinedRisks.orEmpty() + draft.risks).researchNormalized(maxItems = 5),
        officialLinks = mergeLinks(evidence.officialLinks + draft.officialLinks, review?.refinedLinks.orEmpty(), maxItems = 24),
        followUpQuestions = draft.followUpQuestions.normalizedUnique(maxItems = 3),
        expertNote = (review?.refinedExpertNote ?: "").ifBlank { draft.expertNote },
        evidenceGaps = (evidence.evidenceGaps + review?.evidenceGaps.orEmpty() + draft.evidenceGaps).researchNormalized(maxItems = 4),
        confidenceLabel = (review?.confidenceLabel ?: "").ifBlank { draft.confidenceLabel },
        urgencyLabel = (review?.urgencyLabel ?: "").ifBlank { draft.urgencyLabel },
        auditNotes = (review?.objections.orEmpty() + review?.mustFixes.orEmpty()).researchNormalized(maxItems = 4),
    )
}

private fun inferOfficialSourceTargets(query: String): List<String> {
    val normalized = query.lowercase()
    return buildList {
        if (normalized.contains("mdr")) add("MDR")
        if (normalized.contains("ivdr") || normalized.contains("ivd")) add("IVDR")
        if (normalized.contains("mdcg")) add("MDCG")
        if (normalized.contains("eudamed")) add("EUDAMED")
        if (normalized.contains("notified body") || normalized.contains("nando")) add("NANDO")
        if (normalized.contains("article")) add("Article")
        if (normalized.contains("annex")) add("Annex")
        if (normalized.contains("udi")) add("UDI")
    }.ifEmpty { listOf("EUR-Lex", "European Commission") }.distinct().take(5)
}

private fun queryAsksForCommunityView(query: String): Boolean {
    val normalized = query.lowercase()
    return normalized.contains("community") ||
        normalized.contains("discussion") ||
        normalized.contains("linkedin") ||
        normalized.contains("x.com") ||
        normalized.contains("twitter") ||
        normalized.contains("практик") ||
        normalized.contains("обговор") ||
        normalized.contains("коментар")
}

private fun buildAnalyticsSummary(
    roleLabel: String,
    whyItMatters: String,
    gaps: List<String>,
    sourceNotes: List<String>,
): String {
    val parts = buildList {
        if (whyItMatters.isNotBlank()) add(whyItMatters.trim())
        if (gaps.isNotEmpty()) add("Перевірити: " + gaps.joinToString("; "))
        if (sourceNotes.isNotEmpty()) add(sourceNotes.joinToString("; "))
    }
    return parts.joinToString(". ")
}

private fun buildResearchSections(
    directAnswer: String,
    whyItMatters: String,
    expertNote: String,
    focusPoints: List<String>,
    findings: List<String>,
    actions: List<String>,
    risks: List<String>,
    gaps: List<String>,
    evidenceSummary: String,
    sourceNotes: List<String>,
    links: List<CardLink>,
): List<SearchSection> {
    val primaryLinks = links.take(5)
    val evidenceContent = buildList {
        if (evidenceSummary.isNotBlank()) add(evidenceSummary)
        addAll(findings.take(4))
        addAll(focusPoints.take(3))
    }.researchNormalized(maxItems = 6).joinToString(separator = "\n• ", prefix = "• ")
    val decisionContent = buildList {
        addAll(actions.take(3))
        addAll(risks.take(2))
        addAll(gaps.take(2))
    }.researchNormalized(maxItems = 6).joinToString(separator = "\n• ", prefix = "• ")
    return buildList {
        add(
            SearchSection(
                type = SearchSectionType.QUERY_BRIEFING,
                title = "Відповідь і рамка запиту",
                content = directAnswer,
                resources = focusPoints.researchNormalized(maxItems = 4),
                links = primaryLinks.take(3),
            ),
        )
        if (evidenceContent.isNotBlank()) {
            add(
                SearchSection(
                    type = SearchSectionType.RELATED_EVENTS,
                    title = "Докази та нормативна опора",
                    content = evidenceContent,
                    resources = (findings + sourceNotes).researchNormalized(maxItems = 5),
                    links = primaryLinks.take(4),
                ),
            )
        }
        if (whyItMatters.isNotBlank() || expertNote.isNotBlank()) {
            add(
                SearchSection(
                    type = SearchSectionType.EXPERT_ANALYTICS,
                    title = "Експертна інтерпретація",
                    content = buildString {
                        if (whyItMatters.isNotBlank()) append(whyItMatters.trim())
                        if (expertNote.isNotBlank()) {
                            if (isNotBlank()) append("\n\n")
                            append(expertNote.trim())
                        }
                    },
                    resources = focusPoints.researchNormalized(maxItems = 3),
                    links = primaryLinks.take(3),
                ),
            )
        }
        if (decisionContent.isNotBlank()) {
            add(
                SearchSection(
                    type = SearchSectionType.STRATEGIC_FOCUS,
                    title = "Рішення, ризики і що перевірити далі",
                    content = decisionContent,
                    resources = (actions + gaps).researchNormalized(maxItems = 5),
                    links = primaryLinks.take(4),
                ),
            )
        }
    }
}

private fun DashboardCard.applySearchGrounding(
    query: String,
    origin: SearchCardOrigin,
): DashboardCard {
    val officialLinks = buildOfficialGroundingLinks(query)
    val mergedLinks = mergeLinks(officialLinks, links, maxItems = 24)
    val needsStrictFactCheck = queryNeedsStrictFactCheck(query) || cardContainsDateSignals(this)
    val verificationNote = buildGroundingVerificationNote(
        query = query,
        origin = origin,
        officialLinks = mergedLinks,
        needsStrictFactCheck = needsStrictFactCheck,
    )
    val updatedSections = detailedSections.withGroundingContext(
        officialLinks = officialLinks,
        needsStrictFactCheck = needsStrictFactCheck,
    )
    val updatedImpactAreas = buildList {
        if (origin == SearchCardOrigin.FALLBACK) {
            add("Відповідь слугує стартовим орієнтиром: вона допомагає швидко перейти до правильних джерел і перевірок по темі.")
        }
        if (origin == SearchCardOrigin.STALE_CACHE) {
            add("Показано збережений результат пошуку: він може бути корисним, але потребує перевірки на актуальність.")
        }
        if (mergedLinks.any { isOfficialSourceUrl(it.url) }) {
            add("До відповіді додано офіційні джерела для ручної перевірки: EUR-Lex, European Commission, EUDAMED або NANDO.")
        }
        if (needsStrictFactCheck) {
            add("Запит або відповідь містять дати, дедлайни, статті чи annexes, тому точні значення треба звірити по офіційних документах.")
        }
        addAll(impactAreas)
    }.normalizedUnique(maxItems = 12)
    val updatedActions = buildList {
        if (needsStrictFactCheck) {
            add("[Критичне до 30 днів] Звірити всі exact dates, статті, annexes і guidance references по офіційних джерелах перед використанням відповіді.")
        }
        if (origin == SearchCardOrigin.FALLBACK) {
            add("[Критичне до 30 днів] Перейти за офіційними посиланнями нижче і звірити ключові факти по темі.")
        }
        if (origin == SearchCardOrigin.STALE_CACHE) {
            add("[Високе до 90 днів] Повторити пошук або вручну перевірити, чи не з'явилися нові guidance, deadlines або revisions.")
        }
        addAll(actionChecklist)
    }.normalizedUnique(maxItems = 16)
    val updatedRisks = buildList {
        if (needsStrictFactCheck) {
            add("Точні дати, transition windows або document references можуть бути неточними без звірки по офіційних джерелах.")
        }
        if (origin == SearchCardOrigin.FALLBACK) {
            add("Висновок варто використовувати як швидкий орієнтир і доповнювати перевіркою по офіційних джерелах.")
        }
        if (origin == SearchCardOrigin.STALE_CACHE) {
            add("Збережений результат може бути застарілим, якщо після кешування з'явилися нові revisions, corrigenda або guidance.")
        }
        addAll(riskFlags)
    }.normalizedUnique(maxItems = 12)
    return copy(
        subtitle = groundedSubtitle(origin, subtitle, needsStrictFactCheck),
        analytics = analytics.orEmpty().withVerificationNotes(verificationNote).nullIfBlank(),
        impactAreas = updatedImpactAreas,
        actionChecklist = updatedActions,
        riskFlags = updatedRisks,
        confidenceLabel = groundedConfidenceLabel(
            current = confidenceLabel,
            origin = origin,
            needsStrictFactCheck = needsStrictFactCheck,
            officialLinkCount = mergedLinks.count { isOfficialSourceUrl(it.url) },
        ),
        urgencyLabel = groundedUrgencyLabel(urgencyLabel, origin, needsStrictFactCheck),
        links = mergedLinks,
        detailedSections = updatedSections,
    )
}

private fun shouldReturnBasicSearchDisclaimer(query: String): Boolean {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return true
    return normalized in obviousCasualQueries()
}

private fun inferSearchScopeMode(query: String): String {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return "hard_disclaimer"
    if (obviousCasualQueries().contains(normalized)) return "hard_disclaimer"
    if (regulatorySignals().any { normalized.contains(it) }) return "regulatory_direct"
    if (mixedRegulatorySignals().any { normalized.contains(it) }) return "soft_regulatory_interpretation"
    return "soft_regulatory_interpretation"
}

private fun obviousCasualQueries(): List<String> = listOf(
    "hi", "hello", "hey", "thanks", "thank you", "дякую", "привіт", "ок", "okay",
    "how are you", "what time is it", "котра година",
)

private fun regulatorySignals(): List<String> = listOf(
    "regulation", "регуля", "compliance", "mdr", "ivdr", "mdcg", "eudamed", "udi",
    "technical documentation", "qms", "quality system", "clinical evaluation",
    "performance evaluation", "pms", "pmcf", "pmpf", "vigilance", "cer", "per",
    "article", "annex", "guidance", "notified body", "legacy device", "class ii",
    "class iii", "software", "saas", "medical device", "ivd", "iso 13485", "iso 14971",
    "iec 62304", "iec 62366", "custom-made", "labeling", "ifu", "ce", "recertif",
    "audit", "risk management", "post-market", "clinical", "claim", "evidence",
    "registration", "importer", "distributor", "authorized representative", "supplier",
    "deadline", "quality", "documentation",
)

private fun mixedRegulatorySignals(): List<String> = listOf(
    "market", "launch", "supplier", "quality", "audit", "risk", "documentation",
    "deadline", "import", "distribution", "registration", "product", "device",
    "clinical", "software", "safety", "claim", "evidence", "approval", "submission",
    "compliant", "requirement", "standard", "technical file", "change control",
)

private fun digestKey(raw: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

internal fun canonicalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return ""
    val withScheme = when {
        trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.removePrefix("http://")
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> "https://$trimmed"
    }
    return runCatching {
        val uri = URI(withScheme)
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return trimmed
        val path = uri.path?.trimEnd('/').orEmpty()
        val query = uri.query
            ?.split("&")
            ?.filterNot { parameter ->
                val key = parameter.substringBefore("=").lowercase()
                key.startsWith("utm_") || key == "fbclid" || key == "gclid"
            }
            ?.joinToString("&")
            ?.takeIf { it.isNotBlank() }
        URI(
            "https",
            uri.userInfo,
            host,
            if (uri.port == -1 || uri.port == 443) -1 else uri.port,
            path.ifBlank { "/" },
            query,
            null,
        ).toString().removeSuffix("/").ifBlank { trimmed }
    }.getOrDefault(trimmed)
}

internal fun isOfficialSourceUrl(url: String): Boolean {
    val normalized = canonicalizeUrl(url)
    return normalized.contains("eur-lex.europa.eu") ||
        normalized.contains("health.ec.europa.eu") ||
        normalized.contains("ec.europa.eu/docsroom") ||
        normalized.contains("ec.europa.eu/tools/eudamed") ||
        normalized.contains("webgate.ec.europa.eu") ||
        normalized.contains("data.europa.eu")
}

internal fun buildOfficialGroundingLinks(query: String): List<CardLink> {
    val normalized = query.lowercase()
    val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
    val links = buildList {
        add(
            CardLink(
                title = "EUR-Lex search",
                url = "https://eur-lex.europa.eu/search.html?scope=EURLEX&text=$encodedQuery",
                sourceLabel = "Official legal search",
            ),
        )
        add(
            CardLink(
                title = "European Commission medical devices",
                url = "https://health.ec.europa.eu/medical-devices-sector_en",
                sourceLabel = "Official guidance hub",
            ),
        )
        add(
            CardLink(
                title = "European Commission guidance hub",
                url = "https://health.ec.europa.eu/medical-devices-clinical-investigations-and-performance-studies/guidance_en",
                sourceLabel = "Official guidance hub",
            ),
        )
        add(
            CardLink(
                title = "DocsRoom medical devices search",
                url = "https://ec.europa.eu/docsroom/documents?keywords=medical%20device&locale=en",
                sourceLabel = "Official document repository",
            ),
        )
        if (normalized.contains("mdr") || normalized.contains("article") || normalized.contains("annex")) {
            add(
                CardLink(
                    title = "MDR Regulation (EU) 2017/745",
                    url = "https://eur-lex.europa.eu/eli/reg/2017/745/oj",
                    sourceLabel = "Official regulation text",
                ),
            )
        }
        if (normalized.contains("ivdr") || normalized.contains("ivd")) {
            add(
                CardLink(
                    title = "IVDR Regulation (EU) 2017/746",
                    url = "https://eur-lex.europa.eu/eli/reg/2017/746/oj",
                    sourceLabel = "Official regulation text",
                ),
            )
        }
        if (normalized.contains("mdcg") || normalized.contains("guidance")) {
            add(
                CardLink(
                    title = "MDCG endorsed documents",
                    url = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en",
                    sourceLabel = "Official MDCG hub",
                ),
            )
        }
        if (normalized.contains("eudamed")) {
            add(
                CardLink(
                    title = "EUDAMED overview",
                    url = "https://health.ec.europa.eu/medical-devices-eudamed/overview_en",
                    sourceLabel = "Official EUDAMED overview",
                ),
            )
            add(
                CardLink(
                    title = "EUDAMED latest updates",
                    url = "https://health.ec.europa.eu/medical-devices-eudamed/latest-updates_en",
                    sourceLabel = "Official EUDAMED updates",
                ),
            )
            add(
                CardLink(
                    title = "EUDAMED portal",
                    url = "https://ec.europa.eu/tools/eudamed/#/screen/home",
                    sourceLabel = "Official EUDAMED portal",
                ),
            )
        }
        if (normalized.contains("udi")) {
            add(
                CardLink(
                    title = "UDI / Device registration",
                    url = "https://health.ec.europa.eu/medical-devices-eudamed/udidevice-registration_en",
                    sourceLabel = "Official UDI guidance",
                ),
            )
        }
        if (normalized.contains("notified body") || normalized.contains("nando")) {
            add(
                CardLink(
                    title = "NANDO notified bodies",
                    url = "https://webgate.ec.europa.eu/single-market-compliance-space/notified-bodies",
                    sourceLabel = "Official notified body database",
                ),
            )
        }
    }
    return links
        .mapNotNull(::canonicalizeLink)
        .distinctBy { it.url }
        .sortedWith(compareByDescending<CardLink> { isOfficialSourceUrl(it.url) }.thenBy { it.title })
        .take(12)
}

private fun canonicalizeLink(link: CardLink): CardLink? {
    val canonicalUrl = canonicalizeUrl(link.url)
    if (canonicalUrl.isBlank()) return null
    return link.copy(url = canonicalUrl)
}

private fun groundedSubtitle(
    origin: SearchCardOrigin,
    current: String,
    needsStrictFactCheck: Boolean,
): String {
    return when {
        origin == SearchCardOrigin.FALLBACK -> current.ifBlank { "Швидкий орієнтир по темі. Ключові джерела нижче." }
        origin == SearchCardOrigin.STALE_CACHE -> current.ifBlank { "Збережений результат. Варто звірити актуальність." }
        needsStrictFactCheck && current.isNotBlank() -> "$current Для точних дат і посилань на документи відкрийте джерела нижче."
        needsStrictFactCheck -> "Для точних дат і статей відкрийте джерела нижче."
        else -> current
    }
}

private fun groundedConfidenceLabel(
    current: String,
    origin: SearchCardOrigin,
    needsStrictFactCheck: Boolean,
    officialLinkCount: Int,
): String {
    if (origin == SearchCardOrigin.FALLBACK) return "Орієнтовна відповідь"
    if (origin == SearchCardOrigin.STALE_CACHE) return "Може бути застаріло"
    if (needsStrictFactCheck && officialLinkCount < 2) return "Потрібна звірка"
    if (needsStrictFactCheck && current == "Висока впевненість") return "Середня"
    if (current.isBlank() && officialLinkCount > 0) return "Середня"
    return current
}

private fun groundedUrgencyLabel(
    current: String,
    origin: SearchCardOrigin,
    needsStrictFactCheck: Boolean,
): String {
    return when {
        origin == SearchCardOrigin.FALLBACK -> "Звірити джерела"
        origin == SearchCardOrigin.STALE_CACHE -> current.ifBlank { "Перевірити оновлення" }
        needsStrictFactCheck && current.isBlank() -> "Критично"
        else -> current
    }
}

private fun List<SearchSection>.withGroundingContext(
    officialLinks: List<CardLink>,
    needsStrictFactCheck: Boolean,
): List<SearchSection> {
    return map { section ->
        if (section.type != SearchSectionType.QUERY_BRIEFING) {
            section
        } else {
            val additionalResources = buildList {
                if (needsStrictFactCheck) {
                    add("Звірити всі дати, статті та annexes по official sources.")
                }
                addAll(section.resources)
            }.normalizedUnique(maxItems = 5)
            val extraNote = if (needsStrictFactCheck) {
                "\n\nДля exact dates, deadlines, articles і annex references використовуйте official links нижче."
            } else {
                ""
            }
            section.copy(
                content = section.content + extraNote,
                resources = additionalResources,
                links = mergeLinks(officialLinks, section.links, maxItems = 6),
            )
        }
    }
}

private fun buildGroundingVerificationNote(
    query: String,
    origin: SearchCardOrigin,
    officialLinks: List<CardLink>,
    needsStrictFactCheck: Boolean,
): String {
    val officialCount = officialLinks.count { isOfficialSourceUrl(it.url) }
    return buildList {
        if (origin == SearchCardOrigin.FALLBACK) {
            add("Відповідь сформовано як швидкий орієнтир по темі: для остаточного рішення відкрийте ключові джерела нижче.")
        }
        if (origin == SearchCardOrigin.STALE_CACHE) {
            add("Показано кешований результат, який може бути корисним для швидкого старту, але його слід перевірити на актуальність.")
        }
        if (officialCount > 0) {
            add("До відповіді додано $officialCount офіційних джерел для ручної звірки.")
        }
        if (needsStrictFactCheck) {
            add("Запит \"$query\" містить date/document-level сигнали, тому exact dates, articles, annexes і transition windows потрібно перевірити по офіційних документах.")
        }
    }.joinToString(" ")
}

private fun queryNeedsStrictFactCheck(query: String): Boolean {
    val normalized = query.lowercase()
    return normalized.contains("deadline") ||
        normalized.contains("when") ||
        normalized.contains("date") ||
        normalized.contains("строк") ||
        normalized.contains("термін") ||
        normalized.contains("дедлайн") ||
        normalized.contains("article") ||
        normalized.contains("annex") ||
        normalized.contains("статт") ||
        normalized.contains("додат") ||
        normalized.contains("mdcg") ||
        containsDatePattern(normalized)
}

private fun cardContainsDateSignals(card: DashboardCard): Boolean {
    val aggregated = buildString {
        append(card.body)
        append('\n')
        append(card.analytics.orEmpty())
        append('\n')
        append(card.expertOpinion.orEmpty())
        append('\n')
        append(card.impactAreas.joinToString("\n"))
        append('\n')
        append(card.actionChecklist.joinToString("\n"))
        append('\n')
        append(card.riskFlags.joinToString("\n"))
        append('\n')
        append(card.detailedSections.joinToString("\n") { it.content })
    }
    return containsDatePattern(aggregated.lowercase())
}

private fun containsDatePattern(value: String): Boolean {
    return Regex("""\b\d{1,2}\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec|січ|лют|бер|кві|трав|чер|лип|сер|вер|жов|лис|груд)[a-zа-яіїє]*\s+\d{4}\b""").containsMatchIn(value) ||
        Regex("""\b\d{4}\b""").findAll(value).count() >= 2 ||
        Regex("""\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b""").containsMatchIn(value)
}
