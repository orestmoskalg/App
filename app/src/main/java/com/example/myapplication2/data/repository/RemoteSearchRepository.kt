package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NichePromptEnrichmentCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.core.common.SectorRegulatoryContext
import com.example.myapplication2.core.model.*
import com.example.myapplication2.data.remote.GrokApi
import com.example.myapplication2.data.remote.GrokError
import com.example.myapplication2.data.remote.WebCitation
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.ResearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// ══════════════════════════════════════════════════════════════════════════════
//  CACHE
// ══════════════════════════════════════════════════════════════════════════════

private data class CacheEntry(val result: ResearchResult, val createdAt: Long)
private val CACHE_TTL_MS = 10 * 60 * 1_000L  // 10 min — enough for a session

private object ResearchCache {
    private val store = ConcurrentHashMap<String, CacheEntry>()

    fun get(query: String, niches: String, country: String, sector: String): ResearchResult? {
        val entry = store[key(query, niches, country, sector)] ?: return null
        return if (System.currentTimeMillis() - entry.createdAt < CACHE_TTL_MS) entry.result else null
    }

    fun put(query: String, niches: String, country: String, sector: String, result: ResearchResult) {
        store[key(query, niches, country, sector)] = CacheEntry(result, System.currentTimeMillis())
        if (store.size > 50) {
            val cutoff = System.currentTimeMillis() - CACHE_TTL_MS
            store.entries.removeAll { it.value.createdAt < cutoff }
        }
    }

    private fun key(query: String, niches: String, country: String, sector: String) =
        "${query.lowercase().trim()}|$niches|${country.trim()}|${sector.trim()}"
}

// ══════════════════════════════════════════════════════════════════════════════
//  SYSTEM PROMPTS
// ══════════════════════════════════════════════════════════════════════════════

private fun dateContext(): String {
    val fmt = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
    return "Current date: ${fmt.format(Date())}."
}

private fun researchSystem(profile: UserProfile): String {
    val ctx = CountryRegulatoryContext.forCountry(profile.country)
    val sectorKey = SectorRegulatoryContext.sectorKeyOrDefault(profile)
    val sectorLabel = SectorCatalog.labelOrKey(sectorKey)
    val domain = SectorRegulatoryContext.combinedExpertAndJurisdictionPrompt(sectorKey, profile.country)
    val docHint = if (sectorKey == SectorKeys.MEDICAL_DEVICES) ctx.documentExamples else "sector-appropriate regulations, guidance, and standards for $sectorLabel"
    return """$domain
${dateContext()}
RULES:
- Reply ONLY with valid JSON — no markdown fences, no prose, no trailing text
- All answers MUST be for ${ctx.jurisdictionName} only, in sector: $sectorLabel. Do not mix unrelated jurisdictions unless comparing explicitly.
- Tailor answers to the user's niche tags within this sector (not only medical devices unless that is the selected sector).
- All text fields in English (article codes, URLs, ISO numbers in English)
- Cite exact regulatory references ($docHint)
- Never fabricate references — mark uncertain info explicitly
- Keep each field concise so the full JSON fits in one response without truncation"""
}

private fun socialSystem(profile: UserProfile): String {
    val ctx = CountryRegulatoryContext.forCountry(profile.country)
    val sectorLabel = SectorCatalog.labelOrKey(SectorRegulatoryContext.sectorKeyOrDefault(profile))
    return """You are a regulatory affairs analyst for $sectorLabel who monitors industry and professional communities in ${ctx.jurisdictionName}.
${dateContext()}
Reflect what practitioners in this sector discuss (associations, LinkedIn, specialist forums, Reddit, and jurisdiction-relevant sources).
Reply in English. Focus on real, current practitioner concerns — not generic advice.
Reply ONLY with valid JSON — no markdown, no prose outside JSON."""
}

// ══════════════════════════════════════════════════════════════════════════════
//  REPOSITORY
// ══════════════════════════════════════════════════════════════════════════════

class RemoteSearchRepository(private val api: GrokApi) : ResearchRepository {

    // ── Public API ─────────────────────────────────────────────────────────────

    override suspend fun search(query: String, userProfile: UserProfile): DashboardCard =
        runCatching { buildCard(research(query, userProfile)) }
            .getOrElse { buildFallbackCard(query, it) }

    override suspend fun research(query: String, userProfile: UserProfile): ResearchResult {
        val nichesKey = userProfile.niches.joinToString(",")
        val countryKey = userProfile.country.trim()
        val sectorKey = SectorRegulatoryContext.sectorKeyOrDefault(userProfile)
        ResearchCache.get(query, nichesKey, countryKey, sectorKey)?.let { return it }

        val result = coroutineScope {
            val mainJob   = async { fetchMain(query, userProfile) }
            val socialJob = async { runCatching { fetchSocial(query, userProfile) }.getOrNull() }
            mainJob.await().copy(
                socialSummary  = socialJob.await(),
                timestampMillis = System.currentTimeMillis(),
            )
        }

        ResearchCache.put(query, nichesKey, countryKey, sectorKey, result)
        return result
    }

    // ── Main research call ─────────────────────────────────────────────────────

    private suspend fun fetchMain(query: String, profile: UserProfile): ResearchResult {
        val system  = researchSystem(profile)
        val prompt  = buildMainPrompt(query, profile)
        val raw     = api.chat(system, prompt, maxTokens = 4000)
        return parseMain(query, raw)
    }

    private fun buildMainPrompt(query: String, p: UserProfile): String {
        val ctx = CountryRegulatoryContext.forCountry(p.country)
        val nichesList = p.niches.joinToString(", ")
        val sectorKey = SectorRegulatoryContext.sectorKeyOrDefault(p)
        val sectorLabel = SectorCatalog.labelOrKey(sectorKey)
        val docLine = if (sectorKey == SectorKeys.MEDICAL_DEVICES) ctx.documentExamples else "use instruments appropriate to $sectorLabel (not MDR/IVDR unless sector is medical devices)"
        val nicheDeep = NichePromptEnrichmentCatalog.promptBlockForProfile(p)
        return """
MANDATORY SCOPE:
- Country / jurisdiction: ${ctx.jurisdictionName} ONLY. Regulatory references must fit this jurisdiction and sector: $sectorLabel ($docLine).
- User niches: $nichesList. Tailor the answer to these focus tags within the sector. Key findings, action plan and affectedNiches must relate to these niches.
$nicheDeep

QUERY: "$query"
USER: role="${p.role}", sector="$sectorLabel", niches="$nichesList", country="${p.country}"

Return EXACTLY this JSON (English text, English codes/URLs):
{
  "executiveSummary": "3-4 sentences. What it is, why it matters now in 2026, key conclusion with exact article reference.",
  "regulatoryContext": "One line per document: 'MDR Art.61(1) — title; MEDDEV 2.7/1 Rev4 — title; MDCG 2020-13 — title'",
  "keyFindings": [
    "Finding 1 + exact norm (MDR Art.XX or MDCG XX-XX)",
    "Finding 2 + norm",
    "Finding 3 + norm",
    "Finding 4 + norm",
    "Finding 5 + norm"
  ],
  "applicableDocuments": [
    {"code":"MDR 2017/745 Art.XX","title":"Provision title","type":"REGULATION","url":"https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745","isBinding":true},
    {"code":"MDCG XXXX-XX","title":"Guidance title","type":"GUIDANCE","url":"https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en","isBinding":false},
    {"code":"ISO XXXXX:XXXX","title":"Standard title","type":"STANDARD","url":"","isBinding":false}
  ],
  "riskMatrix": [
    {"scenario":"Concrete risk 1","consequence":"Fine/ban/recall — specific","likelihood":"HIGH","severity":"HIGH"},
    {"scenario":"Risk 2","consequence":"Consequence","likelihood":"MEDIUM","severity":"HIGH"},
    {"scenario":"Risk 3","consequence":"Consequence","likelihood":"LOW","severity":"MEDIUM"}
  ],
  "actionPlan": [
    {"step":1,"title":"Step title","description":"What to do, where to find, who is responsible","timeframe":"1-3 days","owner":"RA Manager","isBlocking":true},
    {"step":2,"title":"Step 2","description":"Description","timeframe":"1 week","owner":"QA Team","isBlocking":true},
    {"step":3,"title":"Step 3","description":"Description","timeframe":"2-4 weeks","owner":"RA Manager","isBlocking":false},
    {"step":4,"title":"Step 4","description":"Description","timeframe":"1-3 months","owner":"NB Liaison","isBlocking":false},
    {"step":5,"title":"Ongoing monitoring","description":"Continuous compliance","timeframe":"Ongoing","owner":"QA/RA Team","isBlocking":false}
  ],
  "expertInsight": "Practical advice: typical mistake 70%+ companies make + what to do instead. 3 sentences.",
  "marketContext": "Market / policy context 2026 for this sector in the jurisdiction. 2-3 sentences.",
  "relatedQueries": ["Related question 1","Question 2","Question 3","Question 4"],
  "affectedNiches": ["Niche 1 from user's niches","Niche 2"],
  "complianceDeadline": "DD.MM.YYYY if there is a specific deadline, otherwise empty string",
  "confidenceScore": 88,
  "sources": ["MDR 2017/745","MDCG XXXX-XX"]
}"""
    }

    // ── Social call ────────────────────────────────────────────────────────────

    private suspend fun fetchSocial(query: String, profile: UserProfile): SocialSummary {
        val ctx = CountryRegulatoryContext.forCountry(profile.country)
        val sectorLabel = SectorCatalog.labelOrKey(SectorRegulatoryContext.sectorKeyOrDefault(profile))
        val prompt = """
TOPIC: "$query" in ${ctx.jurisdictionName} — sector: $sectorLabel.

Simulate what regulatory and compliance practitioners in this sector (${ctx.jurisdictionName}) are currently discussing.
Base it on real community patterns from professional networks, sector associations, LinkedIn, specialist forums, Reddit, and jurisdiction-relevant sources.

Return EXACTLY this JSON (all text in English):
{
  "overallSentiment": "2-3 sentences — overall community sentiment on this topic in 2026",
  "topConcerns": [
    "Concrete concern 1 from practitioners",
    "Concern 2",
    "Concern 3"
  ],
  "topInsights": [
    "Practical insight 1 shared by experienced RA specialists",
    "Insight 2",
    "Insight 3"
  ],
  "discussions": [
    {
      "platform": "LINKEDIN",
      "title": "Discussion or post title",
      "snippet": "Short description 50-80 words of what is discussed",
      "author": "RA Director or regulatory body",
      "url": "https://www.linkedin.com/feed/",
      "engagement": "142 likes",
      "postedDate": "2026-02",
      "sentiment": "CONCERN",
      "keyTakeaway": "Key takeaway from discussion — 1 sentence"
    },
    {
      "platform": "REDDIT",
      "title": "Reddit post title",
      "snippet": "Discussion description",
      "author": "r/regulatoryaffairs",
      "url": "https://www.reddit.com/r/regulatoryaffairs/",
      "engagement": "87 upvotes",
      "postedDate": "2026-01",
      "sentiment": "DEBATE",
      "keyTakeaway": "Takeaway"
    },
    {
      "platform": "RAPS",
      "title": "RAPS article title",
      "snippet": "Description",
      "author": "RAPS",
      "url": "https://www.raps.org/regulatory-focus",
      "engagement": "",
      "postedDate": "2025-12",
      "sentiment": "NEUTRAL",
      "keyTakeaway": "Takeaway"
    }
  ]
}"""

        val result = api.chatWithWebSearch(
            systemPrompt = socialSystem(profile),
            userPrompt   = prompt,
        )
        return parseSocial(result.content, result.citations)
    }

    // ── Parsers ────────────────────────────────────────────────────────────────

    private fun parseMain(query: String, raw: String): ResearchResult {
        val root = raw.extractJson().let {
            runCatching { AppJson.parseToJsonElement(it).jsonObject }
                .getOrElse { AppJson.parseToJsonElement(raw).jsonObject }
        }

        fun strings(k: String) = root[k]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val documents = root["applicableDocuments"]?.jsonArray?.mapNotNull { el ->
            runCatching {
                el.jsonObject.let { o ->
                    RegulatoryDocument(
                        code      = o["code"].str(),
                        title     = o["title"].str(),
                        type      = o["type"].enumOr(DocType.GUIDANCE),
                        url       = o["url"].str(),
                        isBinding = o["isBinding"]?.jsonPrimitive?.booleanOrNull ?: false,
                    )
                }
            }.getOrNull()
        } ?: emptyList()

        val risks = root["riskMatrix"]?.jsonArray?.mapNotNull { el ->
            runCatching {
                el.jsonObject.let { o ->
                    RiskItem(
                        scenario    = o["scenario"].str(),
                        consequence = o["consequence"].str(),
                        likelihood  = o["likelihood"].enumOr(RiskLevel.MEDIUM),
                        severity    = o["severity"].enumOr(RiskLevel.MEDIUM),
                    )
                }
            }.getOrNull()
        } ?: emptyList()

        val plan = root["actionPlan"]?.jsonArray?.mapNotNull { el ->
            runCatching {
                el.jsonObject.let { o ->
                    ActionStep(
                        step        = o["step"]?.jsonPrimitive?.intOrNull ?: 0,
                        title       = o["title"].str(),
                        description = o["description"].str(),
                        timeframe   = o["timeframe"].str(),
                        owner       = o["owner"].str(),
                        isBlocking  = o["isBlocking"]?.jsonPrimitive?.booleanOrNull ?: false,
                    )
                }
            }.getOrNull()
        } ?: emptyList()

        return ResearchResult(
            query              = query,
            executiveSummary   = root["executiveSummary"].str(),
            regulatoryContext  = root["regulatoryContext"].str(),
            keyFindings        = strings("keyFindings"),
            applicableDocuments = documents,
            riskMatrix         = risks,
            actionPlan         = plan,
            expertInsight      = root["expertInsight"].str(),
            marketContext      = root["marketContext"].str(),
            socialSummary      = null,
            relatedQueries     = strings("relatedQueries"),
            affectedNiches     = strings("affectedNiches"),
            complianceDeadline = root["complianceDeadline"].str(),
            confidenceScore    = root["confidenceScore"]?.jsonPrimitive?.intOrNull ?: 85,
            sources            = strings("sources"),
            timestampMillis    = System.currentTimeMillis(),
        )
    }

    private fun parseSocial(raw: String, citations: List<WebCitation>): SocialSummary {
        // Try JSON first — our new prompt returns structured JSON
        val jsonResult = runCatching {
            val root = AppJson.parseToJsonElement(raw.extractJson()).jsonObject
            val concerns = root["topConcerns"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.filter { it.length > 10 }
                ?: emptyList()
            val insights = root["topInsights"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.filter { it.length > 10 }
                ?: emptyList()
            val discussions = root["discussions"]?.jsonArray?.mapNotNull { el ->
                runCatching {
                    el.jsonObject.let { o ->
                        SocialDiscussion(
                            platform    = o["platform"].enumOr(SocialPlatform.OTHER),
                            title       = o["title"].str().take(120),
                            snippet     = o["snippet"].str().take(300),
                            author      = o["author"].str(),
                            url         = o["url"].str(),
                            engagement  = o["engagement"].str(),
                            postedDate  = o["postedDate"].str().take(10),
                            sentiment   = o["sentiment"].enumOr(DiscussionSentiment.NEUTRAL),
                            keyTakeaway = o["keyTakeaway"].str().take(150),
                        )
                    }
                }.getOrNull()
            } ?: emptyList()

            // Supplement with real citations from web search if available
            val realDiscussions = citations
                .filter { it.url.isNotBlank() && it.title.isNotBlank() }
                .mapNotNull { mapCitation(it) }
                .distinctBy { it.url }
                .take(3)

            SocialSummary(
                overallSentiment = root["overallSentiment"].str()
                    .ifBlank { "RA/QA community is actively discussing this topic." },
                topConcerns  = concerns.take(3),
                topInsights  = insights.take(3),
                discussions  = (realDiscussions + discussions).distinctBy { it.url }.take(6),
            )
        }.getOrNull()

        if (jsonResult != null) return jsonResult

        // Fallback: parse plain text response
        val lines = raw.trim().split("\n").filter { it.isNotBlank() }
        val paragraphs = lines.filterNot { it.trimStart().let { l -> l.startsWith("-") || l.startsWith("•") } }
        val bullets    = lines.filter { it.trimStart().let { l -> l.startsWith("-") || l.startsWith("•") } }
            .map { it.trimStart('-', '•', ' ').trim() }

        return SocialSummary(
            overallSentiment = paragraphs.take(3).joinToString(" ").take(500)
                .ifBlank { "Community is actively discussing this topic." },
            topConcerns  = bullets.filter { it.length > 15 }.take(3),
            topInsights  = bullets.drop(3).filter { it.length > 15 }.take(3),
            discussions  = citations.filter { it.url.isNotBlank() }.mapNotNull { mapCitation(it) }.take(6),
        )
    }

    // ── Citation mapper ────────────────────────────────────────────────────────

    private fun mapCitation(c: WebCitation): SocialDiscussion? {
        val low = c.url.lowercase()
        val platform = when {
            "linkedin.com"    in low -> SocialPlatform.LINKEDIN
            "reddit.com"      in low -> SocialPlatform.REDDIT
            "twitter.com"     in low || "x.com" in low -> SocialPlatform.TWITTER_X
            "raps.org"        in low -> SocialPlatform.RAPS
            "johner"          in low || "mtf.eu" in low -> SocialPlatform.MEDTECH_FORUM
            "elsevier.com"    in low -> SocialPlatform.ELSEVIERCONNECT
            "mddionline.com"  in low || "medtechdive.com" in low || "mdr.news" in low -> SocialPlatform.MDR_NEWS
            "regulatoryfocus" in low -> SocialPlatform.REGULATORY_FOCUS
            else -> SocialPlatform.OTHER
        }
        val regulatoryKeywords = listOf("regulatory","medical","device","mdr","ivdr","iso",
            "compliance","medtech","eudamed","pharma","health","notified","clinical")
        if (platform == SocialPlatform.OTHER && regulatoryKeywords.none { it in low }) return null

        val snippet = c.snippet.take(300)
        return SocialDiscussion(
            platform    = platform,
            title       = c.title.take(120),
            snippet     = snippet,
            author      = platformAuthor(c.url, low),
            url         = c.url,
            engagement  = "",
            postedDate  = c.publishedDate.take(10),
            sentiment   = inferSentiment(c.title + " " + snippet),
            keyTakeaway = snippet.take(130).trimEnd { !it.isLetter() }
                .let { if (snippet.length > 130) "$it…" else it },
        )
    }

    private fun platformAuthor(url: String, low: String) = when {
        "linkedin.com"     in low -> "LinkedIn"
        "reddit.com/r/"    in low -> "r/" + url.substringAfter("reddit.com/r/").substringBefore("/").take(25)
        "raps.org"         in low -> "RAPS"
        "johner"           in low -> "Johner Institute"
        "medtechdive.com"  in low -> "MedTech Dive"
        "mddionline.com"   in low -> "MD+DI"
        "emergobyul.com"   in low -> "Emergo by UL"
        else -> runCatching {
            java.net.URI(url).host?.removePrefix("www.")?.split(".")?.firstOrNull()
                ?.replaceFirstChar { it.uppercase() } ?: ""
        }.getOrDefault("")
    }

    private fun inferSentiment(text: String): DiscussionSentiment {
        val t = text.lowercase()
        return when {
            listOf("urgent","deadline","critical","crisis","emergency").any { it in t }
                -> DiscussionSentiment.URGENT
            listOf("concern","worried","struggle","difficult","problem","issue",
                   "challenging","confusion","unclear","concern","problem").any { it in t }
                -> DiscussionSentiment.CONCERN
            listOf("debate","controversy","disagree","different views","discussion").any { it in t }
                -> DiscussionSentiment.DEBATE
            listOf("positive","excellent","helpful","great","clarity","clear",
                   "approved","compliant","success").any { it in t }
                -> DiscussionSentiment.POSITIVE
            else -> DiscussionSentiment.NEUTRAL
        }
    }

    // ── Card builder ───────────────────────────────────────────────────────────

    private fun buildCard(r: ResearchResult): DashboardCard {
        val priority = when {
            r.riskMatrix.any { it.severity == RiskLevel.HIGH && it.likelihood == RiskLevel.HIGH } -> Priority.CRITICAL
            r.riskMatrix.any { it.severity == RiskLevel.HIGH } -> Priority.HIGH
            else -> Priority.MEDIUM
        }
        return DashboardCard.create(
            type          = CardType.SEARCH_HISTORY,
            searchQuery   = r.query,
            title         = r.keyFindings.firstOrNull()?.take(75) ?: r.query,
            subtitle      = "${r.applicableDocuments.firstOrNull()?.code ?: "MDR/IVDR"} • ${r.affectedNiches.take(2).joinToString(", ")}",
            body          = runCatching { AppJson.encodeToString(ResearchResult.serializer(), r) }.getOrElse { r.executiveSummary },
            expertOpinion = r.expertInsight,
            analytics     = r.marketContext,
            actionChecklist = r.actionPlan.map { "Step ${it.step}: ${it.title} (${it.timeframe})" },
            riskFlags     = r.riskMatrix.filter { it.severity == RiskLevel.HIGH }.map { it.consequence },
            impactAreas   = r.affectedNiches,
            confidenceLabel = "Accuracy: ${r.confidenceScore}%",
            urgencyLabel  = r.complianceDeadline.ifBlank { r.actionPlan.firstOrNull()?.timeframe ?: "" },
            links         = r.applicableDocuments.filter { it.url.isNotBlank() }.take(5).map {
                CardLink(
                    title       = "${it.code} — ${it.title}",
                    url         = it.url,
                    sourceLabel = if (it.isBinding) "Required" else "Guidance",
                    isVerified  = it.isBinding,
                )
            },
            resources     = r.sources,
            dateMillis    = r.timestampMillis,
            priority      = priority,
        )
    }

    // ── Fallback card ──────────────────────────────────────────────────────────

    private fun buildFallbackCard(query: String, e: Throwable): DashboardCard {
        val reason = when (e) {
            is GrokError.Unauthorized  -> "Invalid or missing OpenAI API key (secrets.properties)."
            is GrokError.RateLimited   -> "OpenAI API limit exceeded. Wait a few minutes."
            is GrokError.NetworkError  -> "No internet connection."
            is GrokError.EmptyResponse -> "Empty server response. Try again."
            else                       -> e.message?.take(80) ?: "Unknown error"
        }
        return DashboardCard.create(
            type          = CardType.SEARCH_HISTORY,
            searchQuery   = query,
            title         = "Offline: $query",
            subtitle      = "Offline mode • MDR/IVDR reference",
            body          = offlineBody(query),
            expertOpinion = reason,
            actionChecklist = listOf(
                "Check OPENAI_API_KEY in secrets.properties and rebuild",
                "Check internet connection",
                "Visit eur-lex.europa.eu directly",
                "View MDCG guidance on ec.europa.eu",
            ),
            riskFlags     = listOf("AI analysis unavailable — check API key configuration"),
            links         = OFFLINE_LINKS,
            dateMillis    = System.currentTimeMillis(),
            priority      = Priority.LOW,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  JSON HELPERS
// ══════════════════════════════════════════════════════════════════════════════

private fun String.extractJson(): String {
    val s = indexOfFirst { it == '{' || it == '[' }
    val e = indexOfLast  { it == '}' || it == ']' }
    return if (s >= 0 && e > s) substring(s, e + 1) else this
}

private fun JsonElement?.str(): String = (this as? JsonPrimitive)?.contentOrNull ?: ""

private inline fun <reified T : Enum<T>> JsonElement?.enumOr(default: T): T =
    runCatching { enumValueOf<T>((this as? JsonPrimitive)?.content ?: "") }.getOrDefault(default)

// ══════════════════════════════════════════════════════════════════════════════
//  OFFLINE CONTENT
// ══════════════════════════════════════════════════════════════════════════════

private val OFFLINE_LINKS = listOf(
    CardLink("EUR-Lex MDR 2017/745",     "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745",  "EU",   true),
    CardLink("EUR-Lex IVDR 2017/746",    "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0746",  "EU",   true),
    CardLink("MDCG Guidance Documents",  "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en", "EC", true),
    CardLink("EUDAMED",                  "https://ec.europa.eu/tools/eudamed",                                    "EC",   true),
    CardLink("NANDO — Notified Bodies",  "https://ec.europa.eu/growth/tools-databases/nando/",                   "EC",   true),
)

private fun offlineBody(q: String): String {
    val t = q.lowercase()
    return when {
        "ivdr" in t || "in vitro" in t -> """Offline mode. IVDR 2017/746 — key dates 2026:
• Class D: deadline 31 March 2026
• Class B: deadline 26 May 2026
• Class C: phase 1 June 2026
Resource: IVDR 2017/746, MDCG 2021-2 (QMS for IVDR)"""
        "samd" in t || "software" in t || " ai " in t -> """Offline mode. SaMD — basics:
• Classification: MDR Annex VIII Rule 11
• Standards: IEC 62304, IEC 62366-1
• Cybersecurity: MDCG 2019-16 Rev.4 (2024)
• AI Act: EU 2024/1689 + MDR = dual regulation"""
        "udi" in t || "eudamed" in t -> """Offline mode. UDI/EUDAMED:
• UDI = UDI-DI + UDI-PI
• Class I deadline: 22 March 2026
• EUDAMED Actor Registration: mandatory from 1 July 2026
• Resource: MDCG 2021-23"""
        "cer" in t || "clinical" in t -> """Offline mode. Clinical evaluation:
• MDR Article 61 — main norm
• MEDDEV 2.7/1 Rev.4 — methodology
• PMCF mandatory Class IIa+ (Annex XIV Part B)
• Equivalence — strict conditions Class III"""
        "pms" in t || "psur" in t -> """Offline mode. Post-Market Surveillance:
• MDR Articles 83-86
• PSUR for Class IIb/III — annually
• Serious Incident: report 15 days (2 days if threat)
• MDCG 2022-21 — PMS guidelines"""
        "classif" in t -> """Offline mode. MDR classification:
• MDR Annex VIII: Rules 1-22
• Rule 11 (SaMD): minimum IIa for active devices
• MDCG 2021-24: Manual on Classification"""
        else -> """Offline mode. Main resources:
• EUR-Lex: MDR 2017/745 and IVDR 2017/746 text
• MDCG: health.ec.europa.eu
• EUDAMED: ec.europa.eu/tools/eudamed
• NB Registry (NANDO): ec.europa.eu/growth/tools-databases/nando
Connect to the internet for full AI analysis."""
    }
}
