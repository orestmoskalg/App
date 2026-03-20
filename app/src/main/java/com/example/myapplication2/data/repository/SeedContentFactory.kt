package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.RegulatoryPrepLinks
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.core.model.*
import com.example.myapplication2.domain.model.UserProfile
import java.util.Calendar

object SeedContentFactory {

    const val MIN_KNOWLEDGE_ITEMS = 20

    /** Increment when default KB seed content changes; app clears insight/strategy/learning once and re-seeds. */
    const val KNOWLEDGE_SEED_VERSION = 1

    fun ensureMinimumKnowledgeItems(
        items: List<DashboardCard>,
        niches: List<String>,
        jurisdictionKey: String,
        padding: (Int, List<String>, String) -> List<DashboardCard>,
    ): List<DashboardCard> {
        if (items.size >= MIN_KNOWLEDGE_ITEMS) return items
        return items + padding(MIN_KNOWLEDGE_ITEMS - items.size, niches, jurisdictionKey)
    }

    fun syntheticInsightPadding(count: Int, niches: List<String>, jurisdictionKey: String): List<DashboardCard> {
        val nicheLabel = NicheCatalog.findByKeyOrName(niches.firstOrNull() ?: "")?.nameEn ?: niches.firstOrNull() ?: "General"
        return (1..count).map { i ->
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "Offline insight $i — $nicheLabel",
                subtitle = "Placeholder • $jurisdictionKey",
                body = "This item was added offline so your Knowledge Base reaches at least $MIN_KNOWLEDGE_ITEMS entries. Tap Refresh when online to generate full AI content for your jurisdiction and niche.",
                niche = nicheLabel,
                dateMillis = System.currentTimeMillis() - i * 86_400_000L,
                priority = Priority.MEDIUM,
                jurisdictionKey = jurisdictionKey,
            )
        }
    }

    fun syntheticStrategyPadding(count: Int, niches: List<String>, jurisdictionKey: String): List<DashboardCard> {
        val nicheLabel = NicheCatalog.findByKeyOrName(niches.firstOrNull() ?: "")?.nameEn ?: niches.firstOrNull() ?: "General"
        return (1..count).map { i ->
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "Offline strategy $i — $nicheLabel",
                subtitle = "Placeholder • $jurisdictionKey",
                body = "Offline supplement. Refresh with network to generate 20 tailored strategies for $jurisdictionKey and your niche.",
                niche = nicheLabel,
                dateMillis = System.currentTimeMillis() - i * 86_400_000L,
                priority = Priority.MEDIUM,
                jurisdictionKey = jurisdictionKey,
            )
        }
    }

    fun syntheticLearningPadding(count: Int, niches: List<String>, jurisdictionKey: String): List<DashboardCard> {
        val nicheLabel = NicheCatalog.findByKeyOrName(niches.firstOrNull() ?: "")?.nameEn ?: niches.firstOrNull() ?: "General"
        return (1..count).map { i ->
            DashboardCard.create(
                type = CardType.LEARNING_MODULE,
                title = "Offline module $i — $nicheLabel",
                subtitle = "Placeholder • $jurisdictionKey",
                body = "Offline supplement. Tap Refresh when online for a full curriculum for $jurisdictionKey and $nicheLabel.",
                niche = nicheLabel,
                dateMillis = System.currentTimeMillis() - i * 86_400_000L,
                priority = Priority.MEDIUM,
                jurisdictionKey = jurisdictionKey,
                resources = listOf("Official regulatory sources for $jurisdictionKey"),
                actionChecklist = listOf("Regenerate when online", "Review with RA team"),
            )
        }
    }

    private fun nicheLabelForKnowledgeSeed(profile: UserProfile): String {
        val k = profile.niches.firstOrNull().orEmpty()
        return NicheCatalog.findByPromptKey(k)?.nameEn
            ?: NicheCatalog.findByKeyOrName(k)?.nameEn
            ?: k.ifBlank { "Regulatory" }
    }

    private fun sectorLabelForSeed(profile: UserProfile): String =
        SectorCatalog.labelOrKey(profile.sector.ifBlank { SectorCatalog.DEFAULT_KEY })

    /**
     * First-run Knowledge Base: MDR-centric seed for medical devices; generic sector-aware starter for other spheres.
     */
    fun knowledgeSeedCards(profile: UserProfile, jurisdictionKey: String): List<DashboardCard> {
        val sk = profile.sector.ifBlank { SectorCatalog.DEFAULT_KEY }
        if (sk == SectorKeys.MEDICAL_DEVICES) {
            return insightCards(profile.niches, jurisdictionKey) +
                strategyCards(profile.niches, jurisdictionKey) +
                learningCards(profile.niches, jurisdictionKey)
        }
        return nonMedicalInsightCards(profile, jurisdictionKey) +
            nonMedicalStrategyCards(profile, jurisdictionKey) +
            nonMedicalLearningCards(profile, jurisdictionKey)
    }

    private fun nonMedicalInsightCards(profile: UserProfile, jurisdictionKey: String): List<DashboardCard> {
        val label = nicheLabelForKnowledgeSeed(profile)
        val sectorName = sectorLabelForSeed(profile)
        val jk = jurisdictionKey
        val core = listOf(
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "Starter insight: $sectorName landscape",
                subtitle = "Offline seed • $jk",
                body = "This app seeds a non–medical-device Knowledge Base for $sectorName. Priorities typically include licensing, product conformity, market surveillance, and documentation for your jurisdiction ($jk). Refresh when online to generate deeper, niche-specific AI content.",
                expertOpinion = "Use official registers and published guidance for $jk; cross-check transitional measures and national gold-plating.",
                niche = label,
                dateMillis = System.currentTimeMillis(),
                priority = Priority.HIGH,
                impactAreas = listOf("Orientation", "Sector scope", "Jurisdiction"),
                jurisdictionKey = jk,
            ),
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "Compliance signals to watch",
                subtitle = "Sector trends • $sectorName",
                body = "Across regulators, themes include digital submissions, supply-chain traceability, and stricter enforcement of economic operator duties. Map obligations to your product categories and markets.",
                niche = label,
                dateMillis = System.currentTimeMillis() - 86400000L,
                priority = Priority.MEDIUM,
                jurisdictionKey = jk,
            ),
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "Documentation and evidence",
                subtitle = "What assessors expect",
                body = "Regardless of sector, align technical files, risk analysis, and post-market monitoring with the instruments cited in your jurisdiction. Evidence proportionate to risk speeds approvals and inspections.",
                niche = label,
                dateMillis = System.currentTimeMillis() - 86400000L * 3,
                priority = Priority.MEDIUM,
                jurisdictionKey = jk,
            ),
        )
        return ensureMinimumKnowledgeItems(core, profile.niches, jurisdictionKey, ::syntheticInsightPadding)
    }

    private fun nonMedicalStrategyCards(profile: UserProfile, jurisdictionKey: String): List<DashboardCard> {
        val label = nicheLabelForKnowledgeSeed(profile)
        val sectorName = sectorLabelForSeed(profile)
        val jk = jurisdictionKey
        val core = listOf(
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "90-day regulatory roadmap ($sectorName)",
                subtitle = "Practical sequence • $jk",
                body = "Week 1–2: inventory products and applicable acts; Week 3–5: gap analysis vs obligations; Week 6–8: documentation and labelling; Week 9–12: surveillance plan and authority touchpoints. Adjust to your niche within $sectorName.",
                expertOpinion = "Front-load classification and borderline decisions — they cascade into testing budgets and timelines.",
                niche = label,
                dateMillis = System.currentTimeMillis(),
                priority = Priority.HIGH,
                actionChecklist = listOf(
                    "Map competent authorities and registers",
                    "List harmonised or designated standards",
                    "Define economic operator roles in supply chain",
                    "Set PMS / vigilance analogues for your sector",
                ),
                jurisdictionKey = jk,
            ),
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "Inspection readiness",
                subtitle = "Systems and records",
                body = "Prepare a traceable QMS or control system narrative: change control, training, CAPA, and supplier oversight. For $sectorName, stress batch/lot traceability where applicable.",
                niche = label,
                dateMillis = System.currentTimeMillis() - 86400000L * 4,
                priority = Priority.MEDIUM,
                jurisdictionKey = jk,
            ),
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "Multi-market expansion",
                subtitle = "Reuse evidence wisely",
                body = "Reuse technical documentation where regimes recognize equivalence, but revalidate labelling, authorised representative roles, and vigilance reporting for each jurisdiction including $jk.",
                niche = label,
                dateMillis = System.currentTimeMillis() - 86400000L * 8,
                priority = Priority.MEDIUM,
                jurisdictionKey = jk,
            ),
        )
        return ensureMinimumKnowledgeItems(core, profile.niches, jurisdictionKey, ::syntheticStrategyPadding)
    }

    private fun nonMedicalLearningCards(profile: UserProfile, jurisdictionKey: String): List<DashboardCard> {
        val label = nicheLabelForKnowledgeSeed(profile)
        val sectorName = sectorLabelForSeed(profile)
        val jk = jurisdictionKey
        val core = listOf(
            DashboardCard.create(
                type = CardType.LEARNING_MODULE,
                title = "Module 1: Sector framework ($sectorName)",
                subtitle = "45 min • beginner",
                body = "How to read hierarchy of norms for $sectorName: regulations, delegated acts, harmonised standards, and national gold-plating in $jk.",
                niche = label,
                dateMillis = System.currentTimeMillis(),
                priority = Priority.HIGH,
                resources = listOf("Official gazette / consolidated texts", "Sector regulator guidance index"),
                actionChecklist = listOf("List mandatory vs voluntary instruments", "Note upcoming consultations"),
                jurisdictionKey = jk,
            ),
            DashboardCard.create(
                type = CardType.LEARNING_MODULE,
                title = "Module 2: Product & dossier structure",
                subtitle = "60 min • intermediate",
                body = "Typical dossier elements: specifications, test summaries, labelling drafts, and risk file — tailored to $sectorName and market.",
                niche = label,
                dateMillis = System.currentTimeMillis() - 86400000L * 2,
                priority = Priority.HIGH,
                resources = listOf("Sector-specific annexes and forms"),
                actionChecklist = listOf("Cross-check with your niche checklists in Tools"),
                jurisdictionKey = jk,
            ),
            DashboardCard.create(
                type = CardType.LEARNING_MODULE,
                title = "Module 3: Post-market & vigilance analogues",
                subtitle = "45 min • intermediate",
                body = "Monitoring complaints, recalls, serious incidents, and authority reporting — concepts translate across sectors even when names differ from MDR.",
                niche = label,
                dateMillis = System.currentTimeMillis() - 86400000L * 5,
                priority = Priority.MEDIUM,
                resources = listOf("National incident portals where applicable"),
                actionChecklist = listOf("Define signal detection for your product line"),
                jurisdictionKey = jk,
            ),
        )
        return ensureMinimumKnowledgeItems(core, profile.niches, jurisdictionKey, ::syntheticLearningPadding)
    }

    private fun absoluteDate(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

    private fun daysFromNow(days: Int): Long =
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

    fun calendarCards(niches: List<String>, jurisdictionKey: String = "eu"): List<DashboardCard> = buildList {

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 2, 27),
            title = "Completed: ISO 13485:2016 Surveillance Audit",
            subtitle = "QMS Audit • Annual surveillance",
            body = "Annual ISO 13485:2016 surveillance audit. Covers management review, CAPA effectiveness, complaint trends, MDR requirements integration in QMS.",
            niche = "General", priority = Priority.MEDIUM,
            actionChecklist = listOf("Close all CAPAs from previous audit", "Management Review protocol for last year", "Supplier qualification updated", "Complaint trend analysis prepared"),
            impactAreas = listOf("QMS", "ISO 13485", "CAPA"),
            urgencyLabel = "Past", confidenceLabel = "ISO 13485:2016",
            links = RegulatoryPrepLinks.diversifyForSeed("Completed: ISO 13485:2016 Surveillance Audit", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 3, 19),
            title = "EMA Webinar: AI as a medical device",
            subtitle = "EMA • Online • Free",
            body = "Official EMA webinar on AI/ML regulation in medical devices: SaMD Rule 11, TPLC approach, algorithmic drift and validation strategies per MDCG 2019-11.",
            niche = "Software as Medical Device SaMD AI ML", priority = Priority.MEDIUM,
            actionChecklist = listOf("Register for EMA webinar", "Prepare questions on your SaMD", "Invite development and RA team"),
            resources = listOf("ema.europa.eu/events", "MDCG 2019-11 SaMD Guidance"),
            urgencyLabel = "19 March", confidenceLabel = "EMA",
            links = RegulatoryPrepLinks.diversifyForSeed(
                "EMA Webinar: AI as a medical device",
                "Software as Medical Device SaMD AI ML",
                extra = listOf(CardLink("EMA — Human medicines", "https://www.ema.europa.eu/en/human-regulatory-overview/medical-devices", "EMA", true)),
            ),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 3, 22),
            title = "UDI registration deadline in EUDAMED (Class I)",
            subtitle = "EU MDR Art.27 • All Class I manufacturers",
            body = "Deadline for UDI-DI registration in EUDAMED for Class I devices. Without registration the device cannot be placed on the EU market.",
            niche = "General", priority = Priority.CRITICAL,
            actionChecklist = listOf("Check UDI status in EUDAMED", "Prepare Basic UDI-DI and UDI-PI", "Contact EUDAMED Support if issues", "Update Declaration of Conformity"),
            riskFlags = listOf("Sales ban without UDI in system", "Fines from competent authorities"),
            urgencyLabel = "22 March", confidenceLabel = "EC official",
            resources = listOf("EUDAMED: ec.europa.eu/tools/eudamed", "MDCG 2021-23: UDI Guide"),
            links = RegulatoryPrepLinks.diversifyForSeed(
                "UDI registration deadline in EUDAMED (Class I)",
                "General",
                extra = listOf(CardLink("EUDAMED", "https://ec.europa.eu/tools/eudamed", "EC", true)),
            ),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 3, 31),
            title = "IVDR Class D — Transition deadline",
            subtitle = "EU IVDR 2017/746 Art.110 • Highest risk IVD",
            body = "End of transition period for Class D IVD (HIV, hepatitis, blood donation). NB certificate and Common Specifications compliance mandatory.",
            niche = "In Vitro Diagnostic Devices IVDR", priority = Priority.CRITICAL,
            actionChecklist = listOf("Verify NB certificate in EUDAMED", "Common Specifications for Class D", "Performance Study documentation", "Update IFU per IVDR"),
            riskFlags = listOf("Without NB certificate — sales ban", "Withdrawal from EU market"),
            urgencyLabel = "31 March", confidenceLabel = "IVDR Art.110",
            links = RegulatoryPrepLinks.diversifyForSeed("IVDR Class D — Transition deadline", "In Vitro Diagnostic Devices IVDR"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 4, 1),
            title = "PSUR Q1 2026 — Class IIb and III",
            subtitle = "EU MDR Art.86 • Quarterly",
            body = "Periodic Safety Update Report submission deadline for Class IIb and III. Covers 12 months PMS data, PMCF analysis and benefit-risk reassessment.",
            niche = "General", priority = Priority.HIGH,
            actionChecklist = listOf("Collect PMS data for 12 months", "Update Benefit-Risk Analysis", "Complaint trends analysis", "Literature search for CER update", "Submit PSUR to NB"),
            impactAreas = listOf("PMS System", "PSUR", "PMCF", "Clinical Evaluation"),
            urgencyLabel = "1 April", confidenceLabel = "MDR Art.86",
            links = RegulatoryPrepLinks.diversifyForSeed("PSUR Q1 2026 — Class IIb and III", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 4, 15),
            title = "GSPR Cybersecurity — New MDCG requirements",
            subtitle = "MDCG 2019-16 Rev.4 • SaMD and connected devices",
            body = "Updated GSPR §17 requirements: SBOM, Incident Response Plan, Vulnerability Disclosure Policy and continuous monitoring.",
            niche = if (niches.any { "SaMD" in it || "AI" in it }) "Software as Medical Device SaMD AI ML" else "General",
            priority = Priority.HIGH,
            actionChecklist = listOf("Review MDCG 2019-16 Rev 4", "Cybersecurity Gap Analysis", "Implement SBOM generation in CI/CD", "Update Technical Documentation §17"),
            impactAreas = listOf("Technical Documentation", "GSPR §17", "IEC 62304"),
            urgencyLabel = "15 April", confidenceLabel = "MDCG official",
            links = RegulatoryPrepLinks.diversifyForSeed(
                "GSPR Cybersecurity — New MDCG requirements",
                if (niches.any { "SaMD" in it || "AI" in it }) "Software as Medical Device SaMD AI ML" else "General",
            ),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 5, 26),
            title = "IVDR Phase-in: Class B deadline",
            subtitle = "EU IVDR 2017/746 • IVD Class B",
            body = "End of transition period for Class B IVD. All new devices require full IVDR compliance and NB certificate.",
            niche = "In Vitro Diagnostic Devices IVDR", priority = Priority.CRITICAL,
            actionChecklist = listOf("Audit all Class B IVD", "Performance Evaluation Report (PER)", "NB certificate if not yet obtained", "Update DoC under IVDR", "EUDAMED registration"),
            riskFlags = listOf("Sales ban without IVDR certificate", "Market withdrawal"),
            urgencyLabel = "26 May", confidenceLabel = "EC Regulation",
            links = RegulatoryPrepLinks.diversifyForSeed("IVDR Phase-in: Class B deadline", "In Vitro Diagnostic Devices IVDR"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 7, 1),
            title = "EUDAMED Actor Registration — Mandatory",
            subtitle = "EU MDR Art.31 • SRN for all manufacturers",
            body = "Without SRN (Single Registration Number) in EUDAMED the manufacturer cannot place new devices on the EU market.",
            niche = "General", priority = Priority.CRITICAL,
            actionChecklist = listOf("Activate EU Login for EUDAMED", "Complete Actor Registration Form", "Confirm SRN in EUDAMED Dashboard", "Update DoC with new SRN"),
            riskFlags = listOf("Without SRN — new devices cannot be placed on market"),
            urgencyLabel = "1 July", confidenceLabel = "MDR Art.31",
            links = RegulatoryPrepLinks.diversifyForSeed("EUDAMED Actor Registration — Mandatory", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 7, 15),
            title = "PSUR Q2 2026 — Class IIb and III",
            subtitle = "EU MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR for Class IIb and III. New complaints, FSCA, literature updates and PMCF progress.",
            niche = "General", priority = Priority.HIGH,
            actionChecklist = listOf("Quarterly PMS data review", "New complaints and trends", "FSCA for quarter", "PMCF progress update"),
            urgencyLabel = "15 July", confidenceLabel = "MDR Art.86",
            links = RegulatoryPrepLinks.diversifyForSeed("PSUR Q2 2026 — Class IIb and III", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 10, 1),
            title = "PSUR Q3 2026 — Class IIb and III",
            subtitle = "EU MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR for Class IIb and III. Focus on complaint trending and signal detection.",
            niche = "General", priority = Priority.HIGH,
            actionChecklist = listOf("Q3 PMS data compilation", "Signal detection analysis", "PMCF effectiveness review"),
            urgencyLabel = "1 October", confidenceLabel = "MDR Art.86",
            links = RegulatoryPrepLinks.diversifyForSeed("PSUR Q3 2026 — Class IIb and III", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 11, 1),
            title = "ISO 13485:2025 — Expected revision release",
            subtitle = "ISO TC 210 • QMS Standard Update",
            body = "ISO is preparing an update to ISO 13485 incorporating MDR/IVDR requirements: PMS integration, software validation updates and risk management alignment with ISO 14971:2019.",
            niche = "General", priority = Priority.MEDIUM,
            actionChecklist = listOf("Follow ISO TC 210 publications", "Preliminary gap assessment", "Plan QMS update after publication"),
            urgencyLabel = "Q4 2026",
            links = RegulatoryPrepLinks.diversifyForSeed("ISO 13485:2025 — Expected revision release", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2026, 12, 31),
            title = "Annual Compliance Review — MDR/IVDR 2026",
            subtitle = "Annual Review • Mandatory for all",
            body = "Year-end — time for full compliance review. Management Review, PMS Plan update, CAPA closure, verification of all certificates.",
            niche = "General", priority = Priority.MEDIUM,
            actionChecklist = listOf("Management Review Q4", "Update PMS Plan for 2027", "Close all 2026 CAPAs", "Verify status of all certificates"),
            urgencyLabel = "31 December",
            links = RegulatoryPrepLinks.diversifyForSeed("Annual Compliance Review — MDR/IVDR 2026", "General"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2027, 5, 26),
            title = "IVDR Full Implementation — Final deadline",
            subtitle = "EU IVDR 2017/746 • All Classes",
            body = "Final IVDR deadline for all remaining transition devices. After this date all IVDs without IVDR certificate are fully withdrawn from the EU market.",
            niche = "In Vitro Diagnostic Devices IVDR", priority = Priority.CRITICAL,
            actionChecklist = listOf("Audit all IVD devices in portfolio", "Verify IVDR certificates in EUDAMED", "Retire or replace non-compliant devices"),
            riskFlags = listOf("Full ban on non-IVDR devices", "Fines and withdrawal"),
            urgencyLabel = "26 May 2027", confidenceLabel = "EC Regulation",
            links = RegulatoryPrepLinks.diversifyForSeed("IVDR Full Implementation — Final deadline", "In Vitro Diagnostic Devices IVDR"),
        ))

        add(DashboardCard.create(
            type = CardType.REGULATORY_EVENT, dateMillis = absoluteDate(2027, 7, 1),
            title = "EUDAMED Vigilance Module — Mandatory",
            subtitle = "MDR Art.87-92 • Vigilance Reporting via EUDAMED",
            body = "All Vigilance reports (SAE, FSN, FSCA) move to mandatory submission via EUDAMED. New timeframes: 24h for serious incidents.",
            niche = "General", priority = Priority.HIGH,
            actionChecklist = listOf("Set up EUDAMED Vigilance Module", "Update Vigilance SOP", "Train team on new process"),
            urgencyLabel = "1 July 2027", confidenceLabel = "MDR Art.87",
            links = RegulatoryPrepLinks.diversifyForSeed("EUDAMED Vigilance Module — Mandatory", "General"),
        ))
    }.map { it.copy(jurisdictionKey = jurisdictionKey) }

    fun insightCards(niches: List<String>, jurisdictionKey: String): List<DashboardCard> {
        val nicheDisplay = NicheCatalog.findByKeyOrName(niches.firstOrNull() ?: "")?.nameEn ?: niches.firstOrNull() ?: "General"
        val core = listOf(
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "Notified Bodies: queues grown to 28 months",
                subtitle = "Market analysis 2025 • Critical situation",
                body = "BSI, TÜV SÜD, SGS and Dekra have announced limits on new applications. Average wait time for Class III: 22-28 months. BSI has stopped accepting new clients until Q3 2025.",
                expertOpinion = "Initiate NB discussions 24-30 months before market entry. Pre-Submission Meeting is mandatory.",
                analytics = "78% of Class III manufacturers — delays >12 months (COCIR 2024). 43% of NBs have limited new applications.",
                niche = nicheDisplay,
                dateMillis = System.currentTimeMillis(), priority = Priority.HIGH,
                impactAreas = listOf("Time-to-Market", "NB Strategy", "Budget Planning"),
                actionChecklist = listOf("Check application status with your NB", "Explore alternative NBs in NANDO", "Replan market entry roadmap", "Obtain Pre-Submission Meeting with NB"),
                links = listOf(CardLink("NANDO Database", "https://ec.europa.eu/growth/tools-databases/nando/", "European Commission", true)),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "SaMD: Mandatory SBOM from 2025",
                subtitle = "MDCG 2019-16 Rev.4 + ENISA 2024",
                body = "New cybersecurity requirements: mandatory Software Bill of Materials (SBOM), continuous vulnerability monitoring, Incident Response Plan and scheduled penetration testing.",
                expertOpinion = "SBOM is becoming the regulator standard. Tools: Syft, CycloneDX, SPDX. Integrate Dependency-Track for CVE monitoring.",
                analytics = "87% of SaMD manufacturers lack a full SBOM process (ENISA 2024).",
                niche = "Software as Medical Device SaMD AI ML",
                dateMillis = System.currentTimeMillis() - 86400000L, priority = Priority.HIGH,
                actionChecklist = listOf("SBOM generation (Syft/CycloneDX) in CI/CD", "CVE monitoring (Dependency-Track)", "Cybersecurity Management Plan", "Penetration Testing (external)", "Update TD §17"),
                impactAreas = listOf("GSPR §17", "IEC 62304", "Technical Documentation"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "EU AI Act + MDR: dual regulation for AI SaMD",
                subtitle = "EU AI Act 2024/1689 + MDR • AI Regulation",
                body = "EU AI Act entered into force August 2024. AI/ML medical devices fall under both MDR and AI Act high-risk category. Mandatory: explainability, PCCP, ongoing monitoring.",
                expertOpinion = "AI Act conformity assessment mandatory by August 2026 for new products. Technical documentation effectively doubles.",
                analytics = "95% of AI medical applications — high-risk AI under EU AI Act. Only 12% of SaMD manufacturers have a PCCP strategy.",
                niche = "Software as Medical Device SaMD AI ML",
                dateMillis = System.currentTimeMillis() - 86400000L * 10, priority = Priority.HIGH,
                actionChecklist = listOf("Determine AI Act high-risk category", "Develop PCCP (Change Control Plan)", "Implement ongoing performance monitoring", "Add AI Act compliance to technical documentation"),
                impactAreas = listOf("EU AI Act", "SaMD Classification", "PCCP", "MDR Compliance"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "FDA-EMA Parallel Review: new opportunity",
                subtitle = "FDA/EMA Joint Meetings 2025 • Expanded Program",
                body = "FDA and EMA have expanded the parallel scientific consultation program for innovative medical devices. Coordinated advice from both regulators at once.",
                expertOpinion = "Parallel consultation is the gold standard for Class III innovations. Cost: €50-100K, but savings on redundant clinical studies $2-5M.",
                analytics = "Average savings: 8-14 months on EU+US launch. Approval rate after Joint Meeting: 89%",
                niche = nicheDisplay,
                dateMillis = System.currentTimeMillis() - 86400000L * 5, priority = Priority.MEDIUM,
                actionChecklist = listOf("Assess whether device is suitable for Parallel Review", "Prepare Briefing Document", "Request to EMA and FDA"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.INSIGHT,
                title = "MDCG Q&A 2024: UDI for combination products and SaMD",
                subtitle = "MDCG 2024-08 • November 2024",
                body = "MDCG published clarifications on UDI for combination products, software-only devices and custom-made devices. New UDI-PI rules for devices with variable configurations.",
                expertOpinion = "Q3.7 on SaMD with modular architecture will affect most software manufacturers. Review your UDI strategy.",
                niche = nicheDisplay,
                dateMillis = System.currentTimeMillis() - 86400000L * 3, priority = Priority.MEDIUM,
                actionChecklist = listOf("Read MDCG 2024-08", "Check UDI strategy for portfolio", "Update UDI Assignment SOP"),
                jurisdictionKey = jurisdictionKey,
            ),
        )
        return ensureMinimumKnowledgeItems(core, niches, jurisdictionKey, ::syntheticInsightPadding)
    }

    fun strategyCards(niches: List<String>, jurisdictionKey: String): List<DashboardCard> {
        val nicheDisplay = NicheCatalog.findByKeyOrName(niches.firstOrNull() ?: "")?.nameEn ?: niches.firstOrNull() ?: "General"
        val core = listOf(
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "90-day MDR certification plan",
                subtitle = "Class IIa/IIb • Step-by-step plan",
                body = "Proven plan for technical documentation preparation and CE marking under MDR in 90 days (for Class IIa with QMS in place). Critical path, resources, KPIs and milestones.",
                expertOpinion = "The most common mistake is underestimating time for clinical evaluation. Allocate 40% of time and budget to CER/PMCF.",
                analytics = "MDR certification: Class IIa — 10 mo., IIb — 18 mo., III — 26 mo. (NB survey 2024)",
                niche = nicheDisplay,
                dateMillis = System.currentTimeMillis(), priority = Priority.HIGH,
                actionChecklist = listOf(
                    "Weeks 1-2: MDR documentation Gap Analysis",
                    "Weeks 2-4: Classification + MDR team",
                    "Weeks 4-7: Annex II (TD) + Annex I (GSPR)",
                    "Weeks 7-10: Clinical Evaluation + Literature Search",
                    "Weeks 10-12: Risk Management (ISO 14971)",
                    "Week 12: Submit to NB",
                ),
                impactAreas = listOf("Technical Documentation", "Clinical Evaluation", "QMS", "Risk Management"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "PMCF strategy for Class IIb/III implants",
                subtitle = "Post-Market Clinical Follow-up • MDR Annex XIV",
                body = "Comprehensive PMCF strategy for implantable devices: patient registries, clinical studies, SSCP and PSUR updates.",
                expertOpinion = "Patient registry is the most effective PMCF method for implants. Requires 2-3 years for clinically significant data.",
                analytics = "68% of NBs refuse due to non-specific PMCF Plan objectives. Successful registry: n≥200 over 2 years",
                niche = nicheDisplay,
                dateMillis = System.currentTimeMillis() - 86400000L * 5, priority = Priority.MEDIUM,
                actionChecklist = listOf("Define PMCF clinical endpoints", "Choose method: registry, study or literature", "Partnership with clinical centre", "PMCF Plan with measurable objectives"),
                impactAreas = listOf("Clinical Evidence", "PMCF Plan", "PSUR", "NB Satisfaction"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.STRATEGY,
                title = "MedTech startup: from prototype to CE Mark",
                subtitle = "Market Entry Strategy • EU MedTech",
                body = "Practical roadmap for MedTech startups from idea to commercial EU market entry. Design Controls, QMS build, NB selection, regulatory fundraising.",
                expertOpinion = "Three mistakes that kill startups: (1) QMS as afterthought; (2) NB by price not experience; (3) ignoring PMS until launch.",
                analytics = "83% of MedTech startups — major deficiency at first NB submission. MDR Class IIb cost: €180K-€450K",
                niche = nicheDisplay,
                dateMillis = System.currentTimeMillis() - 86400000L * 14, priority = Priority.MEDIUM,
                actionChecklist = listOf("Device class (Annex VIII) — determine early", "QMS ISO 13485 from the start", "NB with experience in your niche", "Regulatory budget = 20-40% of R&D"),
                impactAreas = listOf("QMS Build", "Design Controls", "NB Selection", "Go-to-Market"),
                jurisdictionKey = jurisdictionKey,
            ),
        )
        return ensureMinimumKnowledgeItems(core, niches, jurisdictionKey, ::syntheticStrategyPadding)
    }

    fun learningCards(niches: List<String>, jurisdictionKey: String): List<DashboardCard> {
        val nicheDisplay = NicheCatalog.findByKeyOrName(niches.firstOrNull() ?: "")?.nameEn ?: niches.firstOrNull() ?: "General"
        val core = listOf(
            DashboardCard.create(
                type = CardType.LEARNING_MODULE, dateMillis = System.currentTimeMillis(),
                title = "Module 1: MDR device classification",
                subtitle = "MDR basics • 45 min • Beginner",
                body = "Classification under MDR Annex VIII: Rules 1-22, classes I/IIa/IIb/III, special rules for active, implantable and SaMD. Rule 11 for Software.",
                niche = nicheDisplay, priority = Priority.HIGH,
                resources = listOf("EU MDR Annex VIII", "MDCG 2021-24: Classification Manual", "IMDRF Classification Guidance"),
                actionChecklist = listOf("Read MDR Annex VIII", "Take EC classification test", "Classify 3 own devices", "Verify with RA consultant"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.LEARNING_MODULE, dateMillis = System.currentTimeMillis() - 86400000L * 2,
                title = "Module 2: Technical documentation Annex II/III",
                subtitle = "Technical Documentation • 90 min • Intermediate",
                body = "Full TD structure: Annex II (description, GSPR cross-reference, design, benefit-risk), Annex III (PMS Plan, PSUR, PMCF). Practical examples and common mistakes.",
                niche = nicheDisplay, priority = Priority.HIGH,
                resources = listOf("MDR Annex II and III", "MDCG 2019-9: SSCP Guide", "ISO 14971: Risk Management"),
                actionChecklist = listOf("Read MDR Annex II and III", "GSPR Checklist for your device", "Gap analysis of current documentation"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.LEARNING_MODULE, dateMillis = System.currentTimeMillis() - 86400000L * 7,
                title = "Module 3: Clinical evaluation (CER)",
                subtitle = "Clinical Evaluation • 2 hr • Advanced",
                body = "CER methodology per MDR Art.61 and MEDDEV 2.7/1 Rev.4. PICO literature search, Appraisal, Equivalence Assessment, Benefit-Risk Analysis.",
                niche = nicheDisplay, priority = Priority.HIGH,
                resources = listOf("MEDDEV 2.7/1 Rev.4", "MDR Art.61 + Annex XIV", "MDCG 2020-13", "ISO 14155: Clinical Investigations"),
                actionChecklist = listOf("Study MEDDEV 2.7/1 Rev.4", "Trial literature search in PubMed", "Identify Equivalent Device", "Draft CER Scope"),
                jurisdictionKey = jurisdictionKey,
            ),
            DashboardCard.create(
                type = CardType.LEARNING_MODULE, dateMillis = System.currentTimeMillis() - 86400000L * 14,
                title = "Module 4: Post-Market Surveillance System",
                subtitle = "PMS System • 75 min • Intermediate",
                body = "Building a PMS system: PMS Plan, complaint management, signal detection, FSCA, Vigilance Reporting. PSUR and PMCF Report structure.",
                niche = nicheDisplay, priority = Priority.MEDIUM,
                resources = listOf("MDR Art.83-92", "MDCG 2021-1: PMS Guidelines", "MDCG 2022-21: PMS FAQ"),
                actionChecklist = listOf("Does PMS Plan cover all MDR Art.83 sources?", "Vigilance SOP: 24h/48h timeframes", "Test complaint handling end-to-end"),
                jurisdictionKey = jurisdictionKey,
            ),
        )
        return ensureMinimumKnowledgeItems(core, niches, jurisdictionKey, ::syntheticLearningPadding)
    }
}
