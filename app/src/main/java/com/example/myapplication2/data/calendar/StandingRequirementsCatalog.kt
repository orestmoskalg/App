package com.example.myapplication2.data.calendar

import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.domain.model.StandingRequirement
import com.example.myapplication2.domain.model.UserProfile

/**
 * Curated catalog of standing (non-calendar-date) obligations (ported from iOS v3).
 * Filtered by profile country (jurisdiction), sector, and selected niche prompt keys.
 */
object StandingRequirementsCatalog {

    val all: List<StandingRequirement> = listOf(
        StandingRequirement(
            id = "mdr_pms_continuous",
            title = "Post-Market Surveillance system",
            obligation = "Manufacturers must proactively collect and evaluate data on the safety and performance of their devices throughout the entire product lifecycle.",
            timeframe = "Continuous — throughout device lifecycle",
            legalBasis = "MDR Article 83, Annex III Section 1.1",
            nicheID = "medtech_mdr_ivdr",
            nicheCategory = "MedTech",
            jurisdictions = listOf("EU"),
            authority = "National Competent Authorities",
            practicalSteps = listOf(
                "Establish PMS plan per Annex III before placing device on market",
                "Collect and analyze complaint data continuously",
                "Submit PSUR per class schedule",
            ),
            sourceURL = "https://eur-lex.europa.eu/eli/reg/2017/745/oj",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
        StandingRequirement(
            id = "mdr_vigilance_serious",
            title = "Serious incident reporting",
            obligation = "Report any serious incident involving a medical device to the relevant competent authority within 15 days (2 days for life-threatening).",
            timeframe = "Within 15 days of awareness",
            legalBasis = "MDR Article 87",
            nicheID = "medtech_mdr_ivdr",
            nicheCategory = "MedTech",
            jurisdictions = listOf("EU"),
            authority = "National Competent Authority + EUDAMED",
            practicalSteps = listOf(
                "Establish 24/7 vigilance reporting capability",
                "Report to competent authority within 15 days",
            ),
            sourceURL = "https://ec.europa.eu/health/md_sector/new_regulations_en",
            urgency = StandingRequirement.Urgency.Triggered,
        ),
        StandingRequirement(
            id = "nis2_incident_24h",
            title = "NIS2 incident reporting",
            obligation = "Essential and important entities must report significant cybersecurity incidents: early warning 24h, notification 72h, final report 1 month.",
            timeframe = "24h → 72h → 1 month",
            legalBasis = "NIS2 Directive Article 23",
            nicheID = "cybersecurity_nis2",
            nicheCategory = "Digital",
            jurisdictions = listOf("EU"),
            authority = "National CSIRT",
            practicalSteps = listOf(
                "Submit early warning to CSIRT within 24 hours",
                "Provide incident notification within 72 hours",
            ),
            sourceURL = "https://eur-lex.europa.eu/eli/dir/2022/2555/oj",
            urgency = StandingRequirement.Urgency.Triggered,
        ),
        StandingRequirement(
            id = "gdpr_breach_72h",
            title = "GDPR personal data breach notification",
            obligation = "Notify the supervisory authority within 72 hours of becoming aware of a breach likely to result in a risk to individuals.",
            timeframe = "72 hours to supervisory authority",
            legalBasis = "GDPR Articles 33-34",
            nicheID = "gdpr_ccpa_privacy",
            nicheCategory = "Digital",
            jurisdictions = listOf("EU", "UK"),
            authority = "Lead Supervisory Authority",
            practicalSteps = listOf(
                "Assess risk to individuals within first hours",
                "Notify supervisory authority within 72 hours",
            ),
            sourceURL = "https://eur-lex.europa.eu/eli/reg/2016/679/oj",
            urgency = StandingRequirement.Urgency.Triggered,
        ),
        StandingRequirement(
            id = "psd2_sca",
            title = "Strong Customer Authentication (SCA)",
            obligation = "Payment service providers must apply SCA when a payer initiates an electronic payment or accesses their payment account online.",
            timeframe = "Every qualifying transaction",
            legalBasis = "PSD2 Article 97, RTS on SCA",
            nicheID = "fintech_banking",
            nicheCategory = "FinTech",
            jurisdictions = listOf("EU", "UK"),
            authority = "EBA / NCA",
            practicalSteps = listOf(
                "Implement 2 of 3 factors: knowledge, possession, inherence",
                "Review TRA exemptions against EBA fraud rates",
            ),
            sourceURL = "https://www.eba.europa.eu/",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
        StandingRequirement(
            id = "csrd_quarterly",
            title = "CSRD / ESRS data collection",
            obligation = "Companies subject to CSRD must collect sustainability data throughout the year for accurate annual ESRS disclosure.",
            timeframe = "Quarterly data collection; annual reporting",
            legalBasis = "CSRD 2022/2464, ESRS",
            nicheID = "esg_sustainability",
            nicheCategory = "Legal",
            jurisdictions = listOf("EU"),
            authority = "National transposition + auditor",
            practicalSteps = listOf(
                "Collect Q1–Q4 metrics for GHG, workforce, supply chain",
                "Consolidate annual sustainability statement per ESRS",
            ),
            sourceURL = "https://eur-lex.europa.eu/eli/dir/2022/2464/oj",
            urgency = StandingRequirement.Urgency.Periodic,
        ),
        StandingRequirement(
            id = "csrd_double_materiality",
            title = "CSRD: Double materiality assessment",
            obligation = "Companies in scope of CSRD must conduct a double materiality assessment to identify sustainability topics that are (1) material to the company's financial performance and (2) where the company has material impacts on people and environment. This assessment drives ESRS disclosure requirements.",
            timeframe = "Ongoing; review when business model or reporting scope changes",
            legalBasis = "CSRD 2022/2464, ESRS (materiality)",
            nicheID = "esg_sustainability",
            nicheCategory = "Legal",
            jurisdictions = listOf("EU"),
            authority = "National transposing authority",
            practicalSteps = listOf(
                "Conduct stakeholder engagement for materiality",
                "Assess both impact materiality and financial materiality",
                "Document materiality methodology",
                "Review annually and when business model changes",
            ),
            sourceURL = "https://eur-lex.europa.eu/eli/dir/2022/2464/oj",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
        StandingRequirement(
            id = "reach_sds_update",
            title = "REACH Safety Data Sheet maintenance",
            obligation = "Suppliers must provide and keep SDS up to date when new hazard information becomes available.",
            timeframe = "Without delay upon new information",
            legalBasis = "REACH Article 31, Annex II",
            nicheID = "chemicals_regulation",
            nicheCategory = "Industrial",
            jurisdictions = listOf("EU", "UK"),
            authority = "ECHA / HSE",
            practicalSteps = listOf(
                "Update SDS when classification or hazards change",
                "Send updated SDS to all recipients from prior 12 months",
            ),
            sourceURL = "https://echa.europa.eu/safety-data-sheets",
            urgency = StandingRequirement.Urgency.Triggered,
        ),
        StandingRequirement(
            id = "gdpr_dpia",
            title = "GDPR: DPIA required for high-risk processing",
            obligation = "A Data Protection Impact Assessment must be conducted before processing likely to result in high risk to individuals. This includes systematic monitoring, large-scale special categories, or automated decision-making with legal effects.",
            timeframe = "Before high-risk processing begins; review when change in risk",
            legalBasis = "GDPR Article 35",
            nicheID = "gdpr_ccpa_privacy",
            nicheCategory = "Digital",
            jurisdictions = listOf("EU", "UK"),
            authority = "Lead Supervisory Authority",
            practicalSteps = listOf(
                "Identify processing that requires a DPIA (WP248 criteria, EDPB lists)",
                "Document necessity, proportionality, measures, residual risk",
            ),
            sourceURL = "https://gdpr-info.eu/art-35-gdpr/",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
        StandingRequirement(
            id = "nis2_management_body",
            title = "NIS2: Management body personal liability",
            obligation = "Members of management bodies of essential and important entities can be held personally liable for non-compliance with cybersecurity risk management obligations. Management must approve and oversee cybersecurity measures and undergo regular training.",
            timeframe = "Ongoing governance; evidence at supervisory inspections",
            legalBasis = "NIS2 Directive Article 20",
            nicheID = "cybersecurity_nis2",
            nicheCategory = "Digital",
            jurisdictions = listOf("EU"),
            authority = "National competent authority",
            practicalSteps = listOf(
                "Board-level cybersecurity training annually",
                "Document management approval of cyber measures",
                "Ensure D&O insurance covers NIS2 liability where applicable",
            ),
            sourceURL = "https://eur-lex.europa.eu/eli/dir/2022/2555/oj",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
        StandingRequirement(
            id = "sanctions_screening",
            title = "Sanctions: Continuous customer and transaction screening",
            obligation = "Financial institutions and obliged entities must screen customers and transactions against applicable sanctions lists (EU, OFAC SDN, UK OFSI, UN) on an ongoing basis. Reflect new designations without undue delay.",
            timeframe = "Continuous — on onboarding and material changes",
            legalBasis = "EU sanctions regimes; dual-use Regulation 2021/821; national implementations",
            nicheID = "sanctions_export",
            nicheCategory = "Legal",
            jurisdictions = listOf("EU", "UK", "US"),
            authority = "Competent national authorities / OFAC / OFSI",
            practicalSteps = listOf(
                "Automate sanctions list update ingestion where possible",
                "Screen at onboarding, per-transaction, and on list updates",
                "Document false positive resolution",
                "Test screening effectiveness periodically",
            ),
            sourceURL = "https://www.sanctionsmap.eu/",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
        StandingRequirement(
            id = "haccp_ccp_monitoring",
            title = "HACCP: Continuous monitoring at critical control points",
            obligation = "Food business operators must identify Critical Control Points (CCPs) and continuously monitor them. Critical limits must be established, monitored, and corrective actions taken immediately when limits are exceeded. Records must be maintained.",
            timeframe = "Continuous during operations",
            legalBasis = "Codex Alimentarius HACCP; EU food hygiene (e.g. 852/2004) national rules",
            nicheID = "food_safety_haccp",
            nicheCategory = "Food",
            jurisdictions = listOf("EU", "UK", "US"),
            authority = "Food business operator / competent authority",
            practicalSteps = listOf(
                "Identify all CCPs in the production process",
                "Set critical limits for each CCP",
                "Establish monitoring procedures and frequency",
                "Define corrective actions for deviations",
                "Maintain monitoring records for audit",
            ),
            sourceURL = "https://www.fao.org/fao-who-codexalimentarius/",
            urgency = StandingRequirement.Urgency.Continuous,
        ),
    )

    /**
     * Requirements relevant to [profile] country, regulatory sector, and selected niche prompt keys
     * (sub-niches from [UserProfile.niches]).
     */
    fun forProfile(profile: UserProfile?): List<StandingRequirement> {
        if (profile == null) return all
        return all
            .filter { matchesCountryJurisdiction(profile.country, it) }
            .filter { matchesSectorAndNiches(profile, it) }
    }

    /** EU/UK/US/UA tags on each requirement vs profile country. */
    private fun matchesCountryJurisdiction(profileCountry: String, req: StandingRequirement): Boolean {
        val tags = req.jurisdictions.map { it.uppercase() }
        if (tags.isEmpty()) return true
        val jk = CountryRegulatoryContext.forCountry(profileCountry).jurisdictionKey.lowercase()
        return when (jk) {
            "usa" -> tags.any { it == "US" || it == "USA" }
            "ukraine" -> tags.any { it == "EU" || it == "UA" || it == "UKRAINE" }
            "other" -> true
            else -> tags.any { it == "EU" || it == "EEA" || it == "UK" }
        }
    }

    /**
     * When niches are selected: requirement must match at least one [UserProfile.niches] entry.
     * When none: fall back to sector-only alignment with [StandingRequirement.nicheID].
     */
    private fun matchesSectorAndNiches(profile: UserProfile, req: StandingRequirement): Boolean {
        val sectorKey = profile.sector.ifBlank { SectorKeys.MEDICAL_DEVICES }
        val nicheKeys = profile.niches.map { it.trim() }.filter { it.isNotEmpty() }
        if (nicheKeys.isEmpty()) {
            return matchesSectorOnly(req, sectorKey)
        }
        return nicheKeys.any { userKey -> requirementMatchesUserNiche(req, userKey, sectorKey) }
    }

    private fun matchesSectorOnly(req: StandingRequirement, sectorKey: String): Boolean {
        return when (req.nicheID) {
            "medtech_mdr_ivdr" -> sectorKey == SectorKeys.MEDICAL_DEVICES
            "cybersecurity_nis2", "gdpr_ccpa_privacy" -> sectorKey == SectorKeys.DIGITAL_PRIVACY
            "fintech_banking" -> false
            "esg_sustainability", "csrd_double_materiality" ->
                sectorKey == SectorKeys.ENVIRONMENT || sectorKey == SectorKeys.OTHER
            "chemicals_regulation" -> sectorKey == SectorKeys.CHEMICALS
            "sanctions_export" -> sectorKey == SectorKeys.OTHER
            "food_safety_haccp" -> sectorKey == SectorKeys.FOOD_FEED
            else -> false
        }
    }

    private fun requirementMatchesUserNiche(
        req: StandingRequirement,
        userNicheKey: String,
        profileSectorKey: String,
    ): Boolean {
        val niche = NicheCatalog.findByPromptKey(userNicheKey)
            ?: NicheCatalog.findByKeyOrName(userNicheKey)
        val pk = (niche?.promptKey ?: userNicheKey).trim()
        val pkLower = pk.lowercase()
        val sectorOfNiche = niche?.sectorKey ?: profileSectorKey

        return when (req.nicheID) {
            "medtech_mdr_ivdr" ->
                sectorOfNiche == SectorKeys.MEDICAL_DEVICES ||
                    pkLower.contains("ivdr") || pkLower.contains("device") ||
                    pkLower.contains("samd") || pkLower.contains("mdr") ||
                    userNicheKey.contains("Medical", ignoreCase = true) ||
                    userNicheKey.contains("IVD", ignoreCase = true)

            "cybersecurity_nis2" ->
                pk == "dp_nis2_cyber" ||
                    pkLower.contains("nis2") || pkLower.contains("cyber") ||
                    (sectorOfNiche == SectorKeys.DIGITAL_PRIVACY && (pkLower.contains("security") || pkLower.contains("iot")))

            "gdpr_ccpa_privacy" ->
                pk == "dp_gdpr_processing" ||
                    pkLower.contains("gdpr") || pkLower.contains("privacy") ||
                    pkLower.contains("cookie") || pkLower.contains("eprivacy") ||
                    pkLower.contains("transfer") || pk == "dp_gxp_csv" ||
                    pk == "dp_ai_act" || pkLower.contains("ai act")

            "fintech_banking" ->
                pkLower.contains("psd") || pkLower.contains("bank") || pkLower.contains("payment") ||
                    pkLower.contains("mica") || pkLower.contains("crypto") || pkLower.contains("fintech")

            "esg_sustainability", "csrd_double_materiality" ->
                sectorOfNiche == SectorKeys.ENVIRONMENT ||
                    pkLower.contains("csrd") || pkLower.contains("esg") ||
                    pkLower.contains("carbon") || pkLower.contains("waste") ||
                    pkLower.contains("organic") || pkLower.startsWith("env_") ||
                    userNicheKey.contains("sanction", ignoreCase = true) ||
                    userNicheKey.contains("trade", ignoreCase = true)

            "chemicals_regulation" ->
                sectorOfNiche == SectorKeys.CHEMICALS ||
                    pkLower.startsWith("chem_") || pkLower.contains("reach") ||
                    pkLower.contains("clp") || pkLower.contains("sds") ||
                    pkLower.contains("biocide") || pkLower.contains("pesticide")

            "sanctions_export" ->
                pk == "other_trade_sanctions" ||
                    pk == "other_export_dual_use" ||
                    pkLower.contains("sanction") || pkLower.contains("dual") ||
                    pkLower.contains("export control")

            "food_safety_haccp" ->
                sectorOfNiche == SectorKeys.FOOD_FEED ||
                    pkLower.startsWith("food_") || pkLower.startsWith("feed_")

            else -> false
        }
    }

    fun filterByUrgency(
        list: List<StandingRequirement>,
        urgency: StandingRequirement.Urgency?,
    ): List<StandingRequirement> {
        if (urgency == null) return list
        return list.filter { it.urgency == urgency }
    }
}
