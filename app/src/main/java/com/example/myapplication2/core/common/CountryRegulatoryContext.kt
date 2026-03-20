package com.example.myapplication2.core.common

import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.domain.model.UserProfile

/**
 * Maps user-selected country (from Settings/Profile) to regulatory jurisdiction context.
 * Used to tailor search and calendar prompts so results focus on the correct authority and regulations.
 */
object CountryRegulatoryContext {

    data class Context(
        val jurisdictionKey: String,
        val jurisdictionName: String,
        val systemFocus: String,
        val searchKeywords: List<String>,
        val documentExamples: String,
        /** Official resources (title to url) for Settings/Profile "Official resources" section. */
        val quickLinks: List<Pair<String, String>> = emptyList(),
    )

    /**
     * Strips emoji/punctuation and keeps letters + spaces, then resolves a stable key used in [forCountry].
     * Handles UI labels like "Ukraine 🇺🇦", "USA 🇺🇸", and glued emoji without a space.
     */
    private fun stripLetterText(country: String): String =
        buildString {
            for (ch in country) {
                when {
                    ch.isLetter() -> append(ch)
                    ch.isWhitespace() -> append(' ')
                }
            }
        }.trim().replace(Regex("\\s+"), " ").lowercase()

    private fun normalizeCountry(country: String): String {
        val cleaned = stripLetterText(country)
        if (cleaned.isBlank()) return ""
        return when {
            cleaned.contains("united states") || cleaned == "america" ->
                "usa"
            cleaned == "us" || cleaned.startsWith("usa ") || cleaned == "usa" ->
                "usa"
            cleaned.contains("netherlands") || cleaned == "holland" ->
                "netherlands"
            cleaned.contains("ukraine") ->
                "ukraine"
            cleaned.contains("poland") ->
                "poland"
            cleaned.contains("germany") ->
                "germany"
            cleaned.contains("france") ->
                "france"
            cleaned.contains("finland") ->
                "finland"
            cleaned == "other" || cleaned.startsWith("other ") ->
                "other"
            else ->
                cleaned.substringBefore(' ').ifBlank { cleaned }
        }
    }

    /**
     * One-line hint for LLM prompts: which regulators to cite for the selected profile country.
     * Avoids listing FDA + EC together when the user is in a single jurisdiction.
     */
    fun regulatoryAuthorityCitationHint(ctx: Context): String =
        when (ctx.jurisdictionKey) {
            "usa" ->
                "FDA, 21 CFR, CDRH guidance, and FDA-recognized consensus standards only"
            "ukraine" ->
                "Ukrainian State Expert Centre (SLC), CMU decrees, and national technical regulations only"
            "other" ->
                "authorities matching the user's jurisdiction name; use EU MDR/IVDR and FDA only as comparative references when clearly labeled as such"
            else -> when {
                ctx.jurisdictionName.contains("Poland", ignoreCase = true) ->
                    "URPL (Poland), MDCG, EU MDR/IVDR, and EC implementing acts — not FDA unless comparing frameworks"
                ctx.jurisdictionName.contains("Germany", ignoreCase = true) ->
                    "BfArM (Germany), MDCG, EU MDR/IVDR, and applicable German national rules — not FDA unless comparing frameworks"
                ctx.jurisdictionName.contains("France", ignoreCase = true) ->
                    "ANSM (France), MDCG, and EU MDR/IVDR — not FDA unless comparing frameworks"
                ctx.jurisdictionName.contains("Finland", ignoreCase = true) ->
                    "Fimea (Finland), MDCG, and EU MDR/IVDR — not FDA unless comparing frameworks"
                ctx.jurisdictionName.contains("Netherlands", ignoreCase = true) ->
                    "IGJ (Netherlands), MDCG, and EU MDR/IVDR — not FDA unless comparing frameworks"
                else ->
                    "EC/MDCG, notified bodies, EUDAMED, and EU MDR/IVDR — do not cite FDA or Ukraine SLC unless the user explicitly compares jurisdictions"
            }
        }

    fun forCountry(country: String): Context {
        if (country.isBlank()) return euContext()
        return when (normalizeCountry(country)) {
            "usa"         -> usaContext()
            "ukraine"     -> ukraineContext()
            "poland"      -> polandContext()
            "germany"     -> germanyContext()
            "france"      -> franceContext()
            "finland"     -> finlandContext()
            "netherlands" -> netherlandsContext()
            "other"       -> otherContext()
            else          -> euContext()
        }
    }

    private val euQuickLinks = listOf(
        "EUR-Lex MDR 2017/745" to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745",
        "MDCG Guidance Documents" to "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en",
        "EUDAMED" to "https://ec.europa.eu/tools/eudamed",
        "NANDO — Notified Bodies" to "https://ec.europa.eu/growth/tools-databases/nando/",
        "r/regulatoryaffairs" to "https://www.reddit.com/r/regulatoryaffairs/",
    )

    private val polandQuickLinks = listOf(
        "URPL (Poland)" to "https://www.urpl.gov.pl/",
    ) + euQuickLinks

    private val germanyQuickLinks = listOf(
        "BfArM (Germany)" to "https://www.bfarm.de/EN/Home/_node.html",
    ) + euQuickLinks

    private val franceQuickLinks = listOf(
        "ANSM (France)" to "https://www.ansm.sante.fr/",
    ) + euQuickLinks

    private val finlandQuickLinks = listOf(
        "Fimea (Finland)" to "https://www.fimea.fi/",
    ) + euQuickLinks

    private val netherlandsQuickLinks = listOf(
        "IGJ (Netherlands)" to "https://www.igj.nl/",
    ) + euQuickLinks

    private fun euContext() = Context(
        jurisdictionKey = "eu",
        jurisdictionName = "EU (MDR/IVDR)",
        systemFocus = """You are a senior EU MDR/IVDR regulatory expert. Focus on: Regulation (EU) 2017/745 (MDR), 2017/746 (IVDR), MDCG guidance, Notified Bodies, EUDAMED, NANDO. Cite MDR/IVDR articles, MDCG codes, ISO standards. All references in English.""",
        searchKeywords = listOf(
            "MDR 2017/745", "IVDR 2017/746", "MDCG", "EUDAMED", "Notified Body",
            "eur-lex.europa.eu", "health.ec.europa.eu", "NANDO",
        ),
        documentExamples = "MDR Art.XX, IVDR Art.XX, MDCG 20XX-XX, MEDDEV 2.7/1 Rev4, ISO XXXXX",
        quickLinks = euQuickLinks,
    )

    private val usaQuickLinks = listOf(
        "FDA Medical Devices" to "https://www.fda.gov/medical-devices",
        "21 CFR Part 820 (QSR)" to "https://www.ecfr.gov/current/title-21/chapter-I/subchapter-H/part-820",
        "CDRH Device Guidance" to "https://www.fda.gov/medical-devices/device-advice-comprehensive-regulatory-assistance/guidance-documents",
        "GUDID" to "https://accessgudid.nlm.nih.gov/",
        "r/regulatoryaffairs" to "https://www.reddit.com/r/regulatoryaffairs/",
    )

    private fun usaContext() = Context(
        jurisdictionKey = "usa",
        jurisdictionName = "USA (FDA)",
        systemFocus = """You are a senior US FDA medical device regulatory expert. Focus on: 21 CFR Part 820 (QSR), 21 CFR Part 807 (510(k)), PMA, De Novo, FDA guidance, CDRH, state laws where relevant. Cite FDA regulations, guidance documents, and recognized standards. All references in English.""",
        searchKeywords = listOf(
            "FDA 21 CFR", "510(k)", "PMA", "De Novo", "CDRH", "FDA guidance",
            "fda.gov", "regulations.gov", "Quality System Regulation",
        ),
        documentExamples = "21 CFR 820.30, 21 CFR 807.92, FDA guidance, ISO 13485, Recognized standards",
        quickLinks = usaQuickLinks,
    )

    private val ukraineQuickLinks = listOf(
        "State Expert Centre (SLC) — Ukraine" to "https://www.dlz.gov.ua/",
        "EUR-Lex MDR 2017/745" to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745",
        "MDCG Guidance" to "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en",
        "r/regulatoryaffairs" to "https://www.reddit.com/r/regulatoryaffairs/",
    )

    private fun ukraineContext() = Context(
        jurisdictionKey = "ukraine",
        jurisdictionName = "Ukraine",
        systemFocus = """You are a regulatory expert for medical devices in Ukraine. Focus on: the State Expert Centre (SLC; national medicines and medical devices authority), Cabinet of Ministers (CMU) decrees, technical regulations, registration and conformity procedures, and EU MDR/IVDR alignment where applicable. Cite Ukrainian legislation and official sources. Use English for all explanations, article references, and guidance titles in your replies.""",
        searchKeywords = listOf(
            "State Expert Centre Ukraine", "SLC Ukraine medical device", "Ukraine medical device registration",
            "dlz.gov.ua", "Ukraine technical regulation", "CMU Ukraine",
        ),
        documentExamples = "Ukrainian technical regulations, CMU decrees, SLC registration requirements, EU MDR alignment",
        quickLinks = ukraineQuickLinks,
    )

    private fun polandContext() = Context(
        jurisdictionKey = "eu",
        jurisdictionName = "Poland (EU + URPL)",
        systemFocus = """You are a regulatory expert for medical devices in Poland and the EU. Focus on: EU MDR/IVDR (full applicability), URPL (Urząd Rejestracji Produktów Leczniczych), national vigilance and registration nuances, Polish language requirements where relevant. Cite MDR/IVDR, MDCG, and URPL/Polish regulations. All references in English unless Polish-specific.""",
        searchKeywords = listOf(
            "URPL", "Poland medical device", "MDR", "IVDR", "MDCG", "EUDAMED",
            "urpl.gov.pl", "health.ec.europa.eu",
        ),
        documentExamples = "MDR Art.XX, MDCG 20XX-XX, URPL requirements, Polish national provisions",
        quickLinks = polandQuickLinks,
    )

    private fun germanyContext() = Context(
        jurisdictionKey = "eu",
        jurisdictionName = "Germany (EU + BfArM)",
        systemFocus = """You are a regulatory expert for medical devices in Germany and the EU. Focus on: EU MDR/IVDR, BfArM (Bundesinstitut für Arzneimittel und Medizinprodukte), MPG (Medizinproduktegesetz), DIMDI, German vigilance and market surveillance. Cite MDR/IVDR, MDCG, BfArM guidance. All references in English.""",
        searchKeywords = listOf(
            "BfArM", "MDR", "IVDR", "MDCG", "Germany MPG", "DIMDI", "EUDAMED",
            "bfarm.de", "health.ec.europa.eu",
        ),
        documentExamples = "MDR Art.XX, MDCG 20XX-XX, BfArM guidance, MPG",
        quickLinks = germanyQuickLinks,
    )

    private fun franceContext() = Context(
        jurisdictionKey = "eu",
        jurisdictionName = "France (EU + ANSM)",
        systemFocus = """You are a regulatory expert for medical devices in France and the EU. Focus on: EU MDR/IVDR, ANSM (Agence nationale de sécurité du médicament et des produits de santé), French vigilance and registration, LPPR. Cite MDR/IVDR, MDCG, ANSM. All references in English.""",
        searchKeywords = listOf(
            "ANSM", "France medical device", "MDR", "IVDR", "MDCG", "EUDAMED",
            "ansm.sante.fr", "health.ec.europa.eu",
        ),
        documentExamples = "MDR Art.XX, MDCG 20XX-XX, ANSM requirements",
        quickLinks = franceQuickLinks,
    )

    private fun finlandContext() = Context(
        jurisdictionKey = "eu",
        jurisdictionName = "Finland (EU + Fimea)",
        systemFocus = """You are a regulatory expert for medical devices in Finland and the EU. Focus on: EU MDR/IVDR, Fimea (Finnish Medicines Agency), national vigilance and market surveillance, Finnish language requirements where relevant. Cite MDR/IVDR, MDCG, Fimea. All references in English.""",
        searchKeywords = listOf(
            "Fimea", "Finland medical device", "MDR", "IVDR", "MDCG", "EUDAMED",
            "fimea.fi", "health.ec.europa.eu",
        ),
        documentExamples = "MDR Art.XX, MDCG 20XX-XX, Fimea requirements",
        quickLinks = finlandQuickLinks,
    )

    private fun netherlandsContext() = Context(
        jurisdictionKey = "eu",
        jurisdictionName = "Netherlands (EU + IGJ)",
        systemFocus = """You are a regulatory expert for medical devices in the Netherlands and the EU. Focus on: EU MDR/IVDR, IGJ (Inspectie Gezondheidszorg en Jeugd / Healthcare and Youth Inspectorate), Dutch vigilance and registration, CCMO where relevant. Cite MDR/IVDR, MDCG, IGJ. All references in English.""",
        searchKeywords = listOf(
            "IGJ", "Netherlands medical device", "MDR", "IVDR", "MDCG", "EUDAMED",
            "igj.nl", "health.ec.europa.eu",
        ),
        documentExamples = "MDR Art.XX, MDCG 20XX-XX, IGJ requirements",
        quickLinks = netherlandsQuickLinks,
    )

    private fun otherContext() = Context(
        jurisdictionKey = "other",
        jurisdictionName = "Multiple jurisdictions",
        systemFocus = """You are a senior medical device regulatory expert. The user works in a jurisdiction not specifically listed; prioritize EU MDR/IVDR and FDA as the two main reference frameworks, and mention other regions (UK MHRA, Canada, etc.) where relevant. Cite exact articles and guidance. All references in English.""",
        searchKeywords = listOf(
            "MDR", "IVDR", "FDA", "21 CFR", "MDCG", "medical device regulation",
            "eur-lex.europa.eu", "fda.gov", "ISO 13485",
        ),
        documentExamples = "MDR/IVDR Art.XX, FDA 21 CFR, MDCG, ISO standards",
        quickLinks = euQuickLinks + usaQuickLinks,
    )

    /**
     * Calendar list: show events tagged for this profile's jurisdiction (and compatible overlaps).
     * Blank [DashboardCard.jurisdictionKey] = legacy rows, still shown.
     */
    fun calendarEventMatchesProfile(cardJurisdictionKey: String, profileCountry: String): Boolean {
        if (cardJurisdictionKey.isBlank()) return true
        val p = forCountry(profileCountry).jurisdictionKey.lowercase()
        val c = cardJurisdictionKey.trim().lowercase()
        if (c == p) return true
        if (p == "eu" && c == "eu") return true
        if (p == "ukraine" && (c == "eu" || c == "ukraine")) return true
        if (p == "other") return true
        return false
    }

    /** When no single niche chip is selected: keep events that match any profile niche or General. */
    fun matchesProfileNiches(card: DashboardCard, profile: UserProfile): Boolean {
        val userNiches = profile.niches
        if (userNiches.isEmpty()) return true
        val cn = card.niche.trim()
        if (cn.isEmpty() || cn.equals("General", ignoreCase = true)) return true
        return userNiches.any { userN ->
            cn.equals(userN, ignoreCase = true) ||
                cn.contains(userN, ignoreCase = true) ||
                userN.contains(cn, ignoreCase = true)
        }
    }

    /**
     * Knowledge Base: same jurisdiction rules as calendar (legacy blank key = still shown).
     */
    fun knowledgeJurisdictionMatches(cardJurisdictionKey: String, profileCountry: String): Boolean =
        calendarEventMatchesProfile(cardJurisdictionKey, profileCountry)

    /**
     * Knowledge Base: card must belong to the user's regulatory [UserProfile.sector].
     * Unmapped / legacy niche strings are treated as medical-device content for backward compatibility.
     * Cards with niche "General" are treated as medical-device seed content.
     */
    fun knowledgeSectorMatches(card: DashboardCard, profile: UserProfile): Boolean {
        val profileSector = profile.sector.ifBlank { SectorCatalog.DEFAULT_KEY }
        val n = card.niche.trim()
        if (n.isEmpty() || n.equals("General", ignoreCase = true)) {
            return profileSector == SectorKeys.MEDICAL_DEVICES
        }
        val nicheEntry = NicheCatalog.findByKeyOrName(n)
            ?: NicheCatalog.findByPromptKey(n)
        if (nicheEntry == null) {
            return profileSector == SectorKeys.MEDICAL_DEVICES
        }
        return nicheEntry.sectorKey == profileSector
    }

    /**
     * Selected niche chip: [selectedNicheKey] null = "General" — show cards for any profile niche or General.
     */
    fun knowledgeNicheMatches(
        card: DashboardCard,
        profile: UserProfile,
        selectedNicheKey: String?,
    ): Boolean {
        if (selectedNicheKey == null) return matchesProfileNiches(card, profile)
        val sel = NicheCatalog.findByKeyOrName(selectedNicheKey)?.promptKey ?: selectedNicheKey.trim()
        val cardPk = NicheCatalog.findByKeyOrName(card.niche)?.promptKey ?: card.niche.trim()
        return sel.equals(cardPk, ignoreCase = true) ||
            card.niche.trim().equals("General", ignoreCase = true)
    }
}
