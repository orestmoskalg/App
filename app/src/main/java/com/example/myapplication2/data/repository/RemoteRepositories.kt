package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.RegulatoryPrepLinks
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.common.SectorRegulatoryContext
import com.example.myapplication2.core.model.*
import com.example.myapplication2.data.remote.GrokApi
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.*
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

// ══════════════════════════════════════════════════════════════════════════════
//  SHARED
// ══════════════════════════════════════════════════════════════════════════════

private fun dateCtx(): String =
    "Today: ${SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date())}."

/** System prompt: regulatory sector × country × user niches (not medical-only). */
private fun regulatorySystemPrompt(userProfile: UserProfile): String {
    val ctx = CountryRegulatoryContext.forCountry(userProfile.country)
    val countryLine = userProfile.country.ifBlank { "—" }
    val sectorKey = SectorRegulatoryContext.sectorKeyOrDefault(userProfile)
    val sectorLabel = SectorCatalog.labelOrKey(sectorKey)
    val domainBlock = SectorRegulatoryContext.combinedExpertAndJurisdictionPrompt(sectorKey, userProfile.country)
    return """
$domainBlock
${dateCtx()}
User country selection: "$countryLine" → ${ctx.jurisdictionName}
Regulatory sector (sphere): $sectorLabel
User niches (focus tags): ${userProfile.niches.joinToString(", ").ifBlank { "—" }}

RULES:
- Reply ONLY with valid JSON. No markdown fences. No prose. Never truncate.
- All text fields in English. Article codes, URLs, ISO numbers in English.
- Stay within ${ctx.jurisdictionName} and sector "$sectorLabel". Do not apply medical device rules unless the sector is Medical devices & IVD.
- Cite real regulations and official guidance for this sector in ${ctx.jurisdictionName}.
- Never fabricate regulatory references.
""".trimIndent()
}

internal fun String.cleanJsonArray(): String {
    val s = indexOfFirst { it == '[' || it == '{' }
    val e = indexOfLast  { it == ']' || it == '}' }
    return if (s >= 0 && e >= 0) substring(s, e + 1) else this
}

internal fun String.cleanMarkdown(): String = this
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("\\*(.+?)\\*"),       "$1")
    .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
    .replace(Regex("`(.+?)`"),           "$1")
    .trim()

private fun parseAbsoluteDate(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    return runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s)?.time }.getOrNull()
}

private fun JsonElement?.str(): String = (this as? JsonPrimitive)?.contentOrNull ?: ""
private fun String.toPriority() = runCatching { Priority.valueOf(this) }.getOrDefault(Priority.MEDIUM)

private fun JsonElement?.cardLinksFromJson(): List<CardLink> {
    val arr = this?.jsonArray ?: return emptyList()
    return arr.mapNotNull { el ->
        runCatching {
            val o = el.jsonObject
            val url = o["url"]?.str()?.trim() ?: return@mapNotNull null
            if (!url.startsWith("http", ignoreCase = true)) return@mapNotNull null
            CardLink(
                title = o["title"]?.str()?.takeIf { it.isNotBlank() } ?: "Resource",
                url = url,
                sourceLabel = o["sourceLabel"]?.str() ?: "",
                isVerified = o["isVerified"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }.getOrNull()
    }
}

private fun nicheLabel(key: String?): String =
    if (key == null) "General"
    else NicheCatalog.findByKeyOrName(key)?.nameEn ?: key

/** Text for prompts when user picks a niche vs “General” (all profile niches). */
private fun knowledgeNicheDescription(userProfile: UserProfile, nicheOverride: String?): String =
    when {
        nicheOverride != null -> nicheLabel(nicheOverride)
        userProfile.niches.isEmpty() -> "General"
        userProfile.niches.size == 1 -> nicheLabel(userProfile.niches.first())
        else -> userProfile.niches.joinToString(", ") { nicheLabel(it) }
    }

/** Allowed English niche strings for JSON “niche” field (matches [NicheCatalog] names). */
private fun allowedNicheNamesLine(userProfile: UserProfile, nicheOverride: String?): String =
    when {
        nicheOverride != null -> nicheLabel(nicheOverride)
        userProfile.niches.isEmpty() -> "General"
        else -> userProfile.niches.joinToString(", ") { nicheLabel(it) }
    }

// ══════════════════════════════════════════════════════════════════════════════
//  INSIGHTS  — ≥20 per selected niche + user jurisdiction
// ══════════════════════════════════════════════════════════════════════════════

class RemoteInsightsRepository(private val api: GrokApi) : InsightsRepository {

    override suspend fun loadInsights(
        userProfile: UserProfile,
        nicheOverride: String?,
    ): List<DashboardCard> {
        val ctx = CountryRegulatoryContext.forCountry(userProfile.country)
        return try {
            val nicheDesc = knowledgeNicheDescription(userProfile, nicheOverride)
            val allowedNiches = allowedNicheNamesLine(userProfile, nicheOverride)
            val nicheFallbackForParse = when {
                nicheOverride != null -> nicheLabel(nicheOverride)
                userProfile.niches.isEmpty() -> "General"
                else -> nicheLabel(userProfile.niches.first())
            }
            val role        = userProfile.role

            val prompt = """
${dateCtx()}
User's country: "${userProfile.country}" (${ctx.jurisdictionName})
User role: "$role", niche scope: "$nicheDesc"
${if (nicheOverride == null && userProfile.niches.size > 1) "Distribute the 20 insights across ALL listed niches; do not focus on only one niche.\n" else ""}
Each JSON object MUST set "niche" to exactly one of these strings (copy verbatim): $allowedNiches

Generate exactly 20 high-value regulatory insights for ${ctx.jurisdictionName} for this scope.
Each insight MUST:
1. Be specific to its "niche" string — not generic device advice
2. Apply only to ${ctx.jurisdictionName}. Cite regulators: ${SectorRegulatoryContext.regulatoryAuthorityCitationHint(SectorRegulatoryContext.sectorKeyOrDefault(userProfile), ctx)}
3. Reference a REAL current situation in 2025-2026
4. Include at least one specific statistic with source
5. Cite exact guidance code or regulation article where applicable
6. Give a practical action "$role" can take THIS WEEK

Return JSON array of exactly 20 objects:
[{
  "title": "Concrete title — max 75 chars — name the regulation or event",
  "subtitle": "Authority / document • 2025 or 2026",
  "body": "Description 220 words WITHOUT * or #. (1) What is happening, (2) Which devices in this niche are affected in ${ctx.jurisdictionName}, (3) Statistics, (4) Consequences of ignoring.",
  "expertOpinion": "Reviewer advice: typical mistake + what to do. 80 words.",
  "analytics": "Concrete figure with source",
  "actionChecklist": ["Action 1 with exact document","Action 2","Action 3","Action 4"],
  "impactAreas": ["area 1","area 2"],
  "niche": "one of: $allowedNiches",
  "priority": "HIGH or MEDIUM"
}]"""

            val raw = api.chat(regulatorySystemPrompt(userProfile), prompt, maxTokens = 14_000)
            val parsed = AppJson.parseToJsonElement(raw.cleanJsonArray()).jsonArray
                .mapNotNull { parseInsight(it, nicheFallbackForParse, ctx.jurisdictionKey) }
                .filter { it.title.isNotBlank() && it.body.isNotBlank() }
            SeedContentFactory.ensureMinimumKnowledgeItems(
                parsed,
                userProfile.niches,
                ctx.jurisdictionKey,
                SeedContentFactory::syntheticInsightPadding,
            )
        } catch (_: Exception) {
            SeedContentFactory.insightCards(userProfile.niches, ctx.jurisdictionKey)
        }
    }

    private fun parseInsight(el: JsonElement, nicheFallback: String, jurisdictionKey: String): DashboardCard? =
        runCatching {
            val o = el.jsonObject
            val niche = o["niche"]?.str()?.takeIf { it.isNotBlank() } ?: nicheFallback
            DashboardCard.create(
                type            = CardType.INSIGHT,
                title           = o["title"]?.str() ?: return null,
                subtitle        = o["subtitle"]?.str() ?: "",
                body            = o["body"]?.str()?.cleanMarkdown() ?: return null,
                expertOpinion   = o["expertOpinion"]?.str()?.cleanMarkdown(),
                analytics       = o["analytics"]?.str(),
                actionChecklist = o["actionChecklist"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                impactAreas     = o["impactAreas"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                niche           = niche,
                dateMillis      = System.currentTimeMillis(),
                priority        = o["priority"]?.str()?.toPriority() ?: Priority.MEDIUM,
                jurisdictionKey = jurisdictionKey,
            )
        }.getOrNull()
}

// ══════════════════════════════════════════════════════════════════════════════
//  STRATEGY  — ≥20 strategies (mixed horizons) + jurisdiction
// ══════════════════════════════════════════════════════════════════════════════

class RemoteStrategyRepository(private val api: GrokApi) : StrategyRepository {

    override suspend fun loadStrategies(userProfile: UserProfile, nicheOverride: String?): List<DashboardCard> {
        val ctx = CountryRegulatoryContext.forCountry(userProfile.country)
        val nichesList = if (nicheOverride != null) listOf(nicheOverride) else userProfile.niches
        val nicheDesc = knowledgeNicheDescription(userProfile, nicheOverride)
        val allowedNiches = allowedNicheNamesLine(userProfile, nicheOverride)
        val nicheFallbackForParse = when {
            nicheOverride != null -> nicheLabel(nicheOverride)
            userProfile.niches.isEmpty() -> "General"
            else -> nicheLabel(userProfile.niches.first())
        }
        return try {
            val prompt = """
${dateCtx()}
User's country: "${userProfile.country}" (${ctx.jurisdictionName})
User role: "${userProfile.role}", niche scope: "$nicheDesc"
(Profile niche keys for context: ${nichesList.take(4).joinToString(", ")})
${if (nicheOverride == null && userProfile.niches.size > 1) "Distribute the 20 strategies across ALL listed niches.\n" else ""}
Each JSON object MUST set "niche" to exactly one of: $allowedNiches

Generate exactly 20 concrete compliance strategies for ${ctx.jurisdictionName} for this scope.
Include a mix of horizons: ~7 short-term (0–3 months), ~7 mid-term (3–12 months), ~6 long-term (1–3 years).
Each strategy must cite rules or guidance applicable in ${ctx.jurisdictionName}.

Return JSON array of exactly 20 objects:
[{
  "title": "Title: who, what and by when",
  "subtitle": "Short/Mid/Long-term • horizon • ${ctx.jurisdictionName}",
  "body": "180–220 words without */#. (1) WHY now, (2) STEP-BY-STEP plan with dates, (3) Budget, (4) Typical mistakes.",
  "expertOpinion": "Concrete case + solution. 80 words.",
  "analytics": "Supporting statistics or ROI",
  "actionChecklist": ["Step 1: action + deadline","Step 2","Step 3","Step 4","Step 5: verification"],
  "impactAreas": ["area 1","area 2","area 3"],
  "niche": "one of: $allowedNiches",
  "priority": "HIGH or MEDIUM"
}]"""

            val raw = api.chat(regulatorySystemPrompt(userProfile), prompt, maxTokens = 16_000)
            val parsed = AppJson.parseToJsonElement(raw.cleanJsonArray()).jsonArray
                .mapNotNull { parseStrategy(it, nicheFallbackForParse, ctx.jurisdictionKey) }
                .filter { it.title.isNotBlank() }
            SeedContentFactory.ensureMinimumKnowledgeItems(
                parsed,
                nichesList,
                ctx.jurisdictionKey,
                SeedContentFactory::syntheticStrategyPadding,
            )
        } catch (_: Exception) {
            SeedContentFactory.strategyCards(nichesList, ctx.jurisdictionKey)
        }
    }

    private fun parseStrategy(el: JsonElement, nicheFallback: String, jurisdictionKey: String): DashboardCard? =
        runCatching {
            val o = el.jsonObject
            val niche = o["niche"]?.str()?.takeIf { it.isNotBlank() } ?: nicheFallback
            DashboardCard.create(
                type            = CardType.STRATEGY,
                title           = o["title"]?.str() ?: return null,
                subtitle        = o["subtitle"]?.str() ?: "",
                body            = o["body"]?.str()?.cleanMarkdown() ?: return null,
                expertOpinion   = o["expertOpinion"]?.str()?.cleanMarkdown(),
                analytics       = o["analytics"]?.str(),
                actionChecklist = o["actionChecklist"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                impactAreas     = o["impactAreas"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                niche           = niche,
                dateMillis      = System.currentTimeMillis(),
                priority        = o["priority"]?.str()?.toPriority() ?: Priority.MEDIUM,
                jurisdictionKey = jurisdictionKey,
            )
        }.getOrNull()
}

// ══════════════════════════════════════════════════════════════════════════════
//  LEARNING  — ≥20 modules per niche + jurisdiction
// ══════════════════════════════════════════════════════════════════════════════

class RemoteLearningRepository(private val api: GrokApi) : LearningRepository {

    override suspend fun loadLearningModules(
        userProfile: UserProfile,
        nicheOverride: String?,
    ): List<DashboardCard> {
        val ctx = CountryRegulatoryContext.forCountry(userProfile.country)
        return try {
            val nicheDesc = knowledgeNicheDescription(userProfile, nicheOverride)
            val allowedNiches = allowedNicheNamesLine(userProfile, nicheOverride)
            val nicheFallbackForParse = when {
                nicheOverride != null -> nicheLabel(nicheOverride)
                userProfile.niches.isEmpty() -> "General"
                else -> nicheLabel(userProfile.niches.first())
            }
            val level       = userProfile.role.inferLevel()

            val prompt = """
${dateCtx()}
User's country: "${userProfile.country}" (${ctx.jurisdictionName})
User role: "${userProfile.role}" (level: $level), niche scope: "$nicheDesc"
${if (nicheOverride == null && userProfile.niches.size > 1) "Distribute the 20 modules across ALL listed niches; order as a curriculum.\n" else ""}
Each JSON object MUST set "niche" to exactly one of: $allowedNiches

Generate exactly 20 learning modules for ${ctx.jurisdictionName} for this scope.
Modules must form a LOGICAL CURRICULUM: from basics to expert level.
Each module must be SPECIFIC to its "niche" string — not generic regulation text.
Cover: applicable rules in ${ctx.jurisdictionName}, documentation and evidence typical for this sector, market surveillance where relevant, ${SectorRegulatoryContext.jurisdictionReviewProcessPhrase(ctx, userProfile.sector)},
standards for that niche, common audit findings, practical cases.

Return JSON array of exactly 20 objects:
[{
  "title": "Module N: Concrete topic",
  "subtitle": "Level $level • XX min • focus for ${ctx.jurisdictionName}",
  "body": "220 words PLAIN TEXT without */#. (1) Learning goals, (2) 3-4 concepts with references for ${ctx.jurisdictionName}, (3) Practical case, (4) Typical mistake.",
  "resources": [
    "Primary regulation — https://...",
    "Official guidance — https://...",
    "ISO or consensus standard — https://..."
  ],
  "actionChecklist": [
    "Read the relevant section",
    "Apply to a real device in this niche",
    "Verify understanding with a practical task",
    "Discuss with RA colleague or NB"
  ],
  "durationMinutes": 45,
  "niche": "one of: $allowedNiches",
  "priority": "HIGH or MEDIUM"
}]"""

            val raw = api.chat(regulatorySystemPrompt(userProfile), prompt, maxTokens = 14_000)
            val parsed = AppJson.parseToJsonElement(raw.cleanJsonArray()).jsonArray
                .mapNotNull { parseLearning(it, nicheFallbackForParse, ctx.jurisdictionKey) }
                .filter { it.title.isNotBlank() }
            SeedContentFactory.ensureMinimumKnowledgeItems(
                parsed,
                userProfile.niches,
                ctx.jurisdictionKey,
                SeedContentFactory::syntheticLearningPadding,
            )
        } catch (_: Exception) {
            SeedContentFactory.learningCards(userProfile.niches, ctx.jurisdictionKey)
        }
    }

    private fun parseLearning(el: JsonElement, nicheFallback: String, jurisdictionKey: String): DashboardCard? =
        runCatching {
            val o        = el.jsonObject
            val duration = o["durationMinutes"]?.jsonPrimitive?.intOrNull ?: 45
            val niche    = o["niche"]?.str()?.takeIf { it.isNotBlank() } ?: nicheFallback
            DashboardCard.create(
                type            = CardType.LEARNING_MODULE,
                title           = o["title"]?.str() ?: return null,
                subtitle        = o["subtitle"]?.str() ?: "$duration min",
                body            = o["body"]?.str()?.cleanMarkdown() ?: return null,
                resources       = o["resources"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                actionChecklist = o["actionChecklist"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                niche           = niche,
                dateMillis      = System.currentTimeMillis(),
                priority        = o["priority"]?.str()?.toPriority() ?: Priority.MEDIUM,
                jurisdictionKey = jurisdictionKey,
            )
        }.getOrNull()

    private fun String.inferLevel(): String = when {
        contains("Senior", ignoreCase = true) || contains("Director", ignoreCase = true)
                || contains("Head", ignoreCase = true) -> "Advanced"
        contains("Junior", ignoreCase = true) || contains("Engineer", ignoreCase = true) -> "Beginner"
        else -> "Intermediate"
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  CALENDAR  — real web search via chatWithWebSearch
// ══════════════════════════════════════════════════════════════════════════════

class RemoteCalendarRepository(private val api: GrokApi) : CalendarRepository {

    override suspend fun generateCalendar(
        niches: List<String>,
        userProfile: UserProfile,
    ): List<DashboardCard> {
        val ctx = CountryRegulatoryContext.forCountry(userProfile.country)
        return try {
            val nichesList = niches.take(4)
            val nichesStr = nichesList.joinToString(", ")
            val singleNiche = nichesList.size == 1
            val cal       = Calendar.getInstance()
            val fmt       = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
            val from      = fmt.format(cal.time)
            cal.add(Calendar.MONTH, 14)
            val to        = fmt.format(cal.time)
            val searchSources = ctx.searchKeywords.joinToString(", ").take(200)
            val sectorKey = SectorRegulatoryContext.sectorKeyOrDefault(userProfile)
            val sectorLabel = SectorCatalog.labelOrKey(sectorKey)

            // Use chatWithWebSearch to get REAL current regulatory news for the user's jurisdiction
            val systemPrompt = """You are a regulatory calendar expert for $sectorLabel in ${ctx.jurisdictionName}.
${dateCtx()}
Search for REAL current regulatory events, deadlines, and guidance relevant to $sectorLabel in ${ctx.jurisdictionName}.
Use sources such as: $searchSources
Return ONLY valid JSON array. No markdown. No prose."""

            val nicheInstruction = if (singleNiche) {
                "\nReturn ONLY events directly relevant to this single niche: \"$nichesStr\". Exclude general events that do not apply to it.\n"
            } else ""

            val userPrompt = """
Find and return ALL relevant real ${ctx.jurisdictionName} regulatory events for sector "$sectorLabel" and niches: "$nichesStr".
$nicheInstruction
Calendar window: $from → $to

IMPORTANT: You MUST return at least 15 events, and preferably 20 or more. Include every relevant deadline, conference, guidance update, and regulatory milestone you can identify. Do not stop at 4–5 events.

Search for (focus on ${ctx.jurisdictionName} and $sectorLabel):
${ctx.searchKeywords.take(10).joinToString("\n") { "- $it" }}
- Important regulatory conferences and sector associations for "$sectorLabel"
- New standards or guidance relevant to "$nichesStr"
- Market surveillance and authority updates
- Periodic reporting, renewal, registration, and certificate deadlines typical for this sector
- Transition deadlines and compliance milestones

Return a JSON array. Each object = one event. Use SHORT body (2–3 sentences, ~80 words) so you can include many events.
Each event MUST include "links": an array of 2 to 5 objects with real https URLs (official regulation, authority, guidance, standard, register) that help the regulated company prepare — not generic homepages only.
[{
  "title": "Real event title — max 80 chars",
  "subtitle": "Official source • event type",
  "body": "2-3 sentences: what is happening, which products/subjects in this sector are affected, what to prepare. Keep under 100 words.",
  "absoluteDate": "YYYY-MM-DD",
  "actionChecklist": ["Action 1","Action 2","Action 3"],
  "riskFlags": ["Specific legal consequence"],
  "urgencyLabel": "22 March 2026",
  "confidenceLabel": "Source name",
  "priority": "CRITICAL or HIGH or MEDIUM",
  "niche": "${if (singleNiche) "must be exactly: $nichesStr" else "most relevant niche from: $nichesStr or General"}",
  "links": [
    { "title": "EUR-Lex MDR", "url": "https://eur-lex.europa.eu/...", "sourceLabel": "EU" },
    { "title": "MDCG document", "url": "https://...", "sourceLabel": "MDCG" }
  ]
}]"""

            val result = api.chatWithWebSearch(
                systemPrompt = systemPrompt,
                userPrompt   = userPrompt,
                maxTokens    = 8192,
            )

            val parsed = AppJson.parseToJsonElement(result.content.cleanJsonArray()).jsonArray
                .mapNotNull { parseEvent(it, niches, ctx) }
                .map { RegulatoryPrepLinks.mergeIntoCard(it, ctx) }
                .filter { it.title.isNotBlank() }
                .sortedBy { it.dateMillis }

            parsed.ifEmpty { SeedContentFactory.calendarCards(niches, ctx.jurisdictionKey) }
        } catch (_: Exception) {
            SeedContentFactory.calendarCards(niches, ctx.jurisdictionKey)
        }
    }

    private fun parseEvent(
        el: JsonElement,
        niches: List<String>,
        ctx: CountryRegulatoryContext.Context,
    ): DashboardCard? =
        runCatching {
            val o = el.jsonObject
            val dateMs = parseAbsoluteDate(o["absoluteDate"]?.str())
                ?: run {
                    val days = o["daysFromNow"]?.jsonPrimitive?.intOrNull ?: 60
                    System.currentTimeMillis() + days * 86_400_000L
                }
            DashboardCard.create(
                type            = CardType.REGULATORY_EVENT,
                title           = o["title"]?.str() ?: return null,
                subtitle        = o["subtitle"]?.str() ?: "",
                body            = o["body"]?.str()?.cleanMarkdown() ?: "",
                actionChecklist = o["actionChecklist"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                riskFlags       = o["riskFlags"]?.jsonArray?.mapNotNull { it.str() } ?: emptyList(),
                urgencyLabel    = o["urgencyLabel"]?.str() ?: "",
                confidenceLabel = o["confidenceLabel"]?.str() ?: "",
                niche           = o["niche"]?.str() ?: niches.firstOrNull() ?: "General",
                jurisdictionKey = ctx.jurisdictionKey,
                dateMillis      = dateMs,
                priority        = o["priority"]?.str()?.toPriority() ?: Priority.MEDIUM,
                links           = o["links"]?.cardLinksFromJson() ?: emptyList(),
            )
        }.getOrNull()
}
