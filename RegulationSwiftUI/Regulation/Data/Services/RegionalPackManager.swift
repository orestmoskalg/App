import Foundation
import os.log

// MARK: - RegionalPack

/// Curated, hand-verified regulatory events for regions where AI knowledge is thin.
///
/// **Why this exists:**
/// AI (Grok) has deep knowledge of EU/US/UK regulations but shallow coverage of:
/// - Middle East (UAE, Saudi Arabia, Qatar)
/// - South Asia (India)
/// - East Asia (Japan, South Korea specific)
/// - Southeast Asia (Singapore, Indonesia)
/// - Latin America (Brazil, Mexico specific)
///
/// Solution: hybrid data model — AI-generated events for EU/US/UK + curated packs
/// for underserved regions. Packs are updated via JSON files bundled with app updates
/// or fetched from a remote endpoint in the future.
///
/// Addresses feedback from:
/// - Amira: "UAE events were almost zero — half my job"
/// - Yuki: "Japan FSA events were only 3 out of 40"

struct RegionalPack: Identifiable, Codable {
    let id: String
    let region: String
    let displayName: String
    let description: String
    let countries: [String]
    let curatedEvents: [CuratedEvent]
    let standingRequirements: [CuratedStandingRequirement]
    let lastUpdated: Date
    let version: String

    var eventCount: Int { curatedEvents.count }
}

struct CuratedEvent: Identifiable, Codable {
    let id: String
    let title: String
    let dateString: String
    let endDateString: String?
    let eventType: String
    let niche: String
    let nicheCategory: String
    let country: String
    let authority: String
    let description: String
    let impact: String
    let sourceURL: String?
    let checklist: [String]
    let lastVerified: String

    /// Convert to RegulatoryEvent for unified display
    func toRegulatoryEvent(dateFormatter: DateFormatter) -> RegulatoryEvent? {
        guard let date = dateFormatter.date(from: dateString) else { return nil }
        let endDate = endDateString.flatMap { dateFormatter.date(from: $0) }

        return RegulatoryEvent(
            id: "curated_\(id)",
            title: title,
            date: date,
            endDate: endDate,
            eventType: RegulatoryEvent.EventType(rawValue: eventType) ?? .other,
            niche: niche,
            nicheCategory: nicheCategory,
            jurisdiction: RegulatoryEvent.Jurisdiction(
                region: country,
                subRegion: nil,
                authority: authority
            ),
            description: description,
            impact: RegulatoryEvent.ImpactLevel(rawValue: impact) ?? .medium,
            confidence: .verified,   // Curated = always verified
            sourceURL: sourceURL,
            sourceAuthority: authority,
            verificationHint: "Curated and verified by Regulatory Assistant team. Last verified: \(lastVerified)",
            checklist: checklist,
            risks: [],
            crossNicheImpacts: [],
            relatedTerms: [],
            recurringInfo: nil
        )
    }
}

struct CuratedStandingRequirement: Identifiable, Codable {
    let id: String
    let title: String
    let obligation: String
    let timeframe: String
    let legalBasis: String
    let country: String
    let authority: String
    let sourceURL: String?
}

// MARK: - RegionalPackManager

/// Loads and manages regional packs from bundled JSON + future remote source.
final class RegionalPackManager {
    static let shared = RegionalPackManager()

    private let logger = Logger(subsystem: "com.regulation.assistant", category: "RegionalPacks")
    private var loadedPacks: [RegionalPack] = []
    private let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    private init() {
        loadedPacks = loadAllPacks()
    }

    // MARK: - Public API

    var allPacks: [RegionalPack] { loadedPacks }

    /// Get curated events for a specific region, converted to RegulatoryEvent
    func events(for region: String) -> [RegulatoryEvent] {
        loadedPacks
            .filter { $0.region == region || $0.countries.contains(region) }
            .flatMap { $0.curatedEvents }
            .compactMap { $0.toRegulatoryEvent(dateFormatter: dateFormatter) }
    }

    /// Get all curated events across all packs
    func allCuratedEvents() -> [RegulatoryEvent] {
        loadedPacks
            .flatMap { $0.curatedEvents }
            .compactMap { $0.toRegulatoryEvent(dateFormatter: dateFormatter) }
    }

    /// Merge curated events with AI-generated events
    func mergeWithAIEvents(aiEvents: [RegulatoryEvent], selectedRegions: Set<String>) -> [RegulatoryEvent] {
        var merged = aiEvents

        // Add curated events for selected regions
        let curated = selectedRegions.flatMap { events(for: $0) }

        // Deduplicate by title similarity
        let existingTitles = Set(aiEvents.map { $0.title.lowercased() })
        let uniqueCurated = curated.filter { !existingTitles.contains($0.title.lowercased()) }

        merged.append(contentsOf: uniqueCurated)
        return merged.sorted { $0.date < $1.date }
    }

    /// Available regions with pack counts
    var availableRegions: [(region: String, packName: String, eventCount: Int)] {
        loadedPacks.map { ($0.region, $0.displayName, $0.eventCount) }
    }

    // MARK: - Loading

    private func loadAllPacks() -> [RegionalPack] {
        // Load from bundled JSON files
        var packs: [RegionalPack] = []

        // Try to load each known pack
        let packNames = ["mena_pack", "south_asia_pack", "east_asia_pack", "latam_pack"]
        for name in packNames {
            if let pack = loadPack(named: name) {
                packs.append(pack)
                logger.info("Loaded regional pack: \(pack.displayName) (\(pack.eventCount) events)")
            }
        }

        // If no bundled packs found, use hardcoded seed data
        if packs.isEmpty {
            packs = seedPacks()
            logger.info("Using seed regional packs (\(packs.count) packs)")
        }

        return packs
    }

    private func loadPack(named name: String) -> RegionalPack? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json") else { return nil }
        do {
            let data = try Data(contentsOf: url)
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode(RegionalPack.self, from: data)
        } catch {
            logger.error("Failed to load pack \(name): \(error.localizedDescription)")
            return nil
        }
    }

    // MARK: - Seed Data (hardcoded fallback)

    private func seedPacks() -> [RegionalPack] {
        [
            menaPack(),
            southAsiaPack(),
            eastAsiaPack()
        ]
    }

    private func menaPack() -> RegionalPack {
        RegionalPack(
            id: "mena_v1",
            region: "MENA",
            displayName: "Middle East & North Africa",
            description: "UAE, Saudi Arabia, Qatar, Bahrain — financial regulation, labor law, data protection",
            countries: ["UAE", "Saudi Arabia", "Qatar", "Bahrain"],
            curatedEvents: [
                CuratedEvent(id: "uae_pdpl_001", title: "UAE Federal Data Protection Law (PDPL) — full enforcement begins", dateString: "2026-06-01", endDateString: nil, eventType: "enforcement", niche: "GDPR/CCPA Data Privacy", nicheCategory: "Digital", country: "UAE", authority: "UAE Data Office", description: "The UAE Federal Decree-Law No. 45/2021 on Personal Data Protection reaches full enforcement. All data controllers and processors operating in the UAE must be fully compliant, including registration with the UAE Data Office and implementation of data subject rights.", impact: "critical", sourceURL: "https://u.ae/en/about-the-uae/digital-uae/data/data-protection-laws", checklist: ["Register with UAE Data Office", "Appoint Data Protection Officer if required", "Implement data subject request procedures", "Review cross-border data transfer mechanisms", "Conduct DPIA for high-risk processing"], lastVerified: "2026-03-15"),

                CuratedEvent(id: "uae_cbuae_001", title: "CBUAE Stored Value Facility (SVF) regulation update", dateString: "2026-04-15", endDateString: nil, eventType: "law_update", niche: "FinTech & Banking Regulation", nicheCategory: "FinTech", country: "UAE", authority: "CBUAE", description: "Central Bank of UAE issues updated regulations for Stored Value Facilities covering e-wallets, prepaid cards, and digital payment tokens. New capital requirements and consumer protection measures take effect.", impact: "high", sourceURL: "https://www.centralbank.ae/en/", checklist: ["Review updated capital requirements", "Assess compliance with new consumer protection measures", "Update KYC/AML procedures per CBUAE guidance", "Submit compliance report to CBUAE"], lastVerified: "2026-03-10"),

                CuratedEvent(id: "uae_difc_001", title: "DIFC Data Protection Law Amendment No. 1 — effective", dateString: "2026-07-01", endDateString: nil, eventType: "law_update", niche: "GDPR/CCPA Data Privacy", nicheCategory: "Digital", country: "UAE", authority: "DIFC Commissioner of Data Protection", description: "The Dubai International Financial Centre implements amendments to DIFC Data Protection Law No. 5 of 2020, aligning more closely with GDPR standards. Introduces mandatory breach notification within 72 hours and expanded data subject rights.", impact: "high", sourceURL: "https://www.difc.ae/business/operating/data-protection/", checklist: ["Review DIFC DP Law amendments", "Update breach notification procedures to 72h", "Train staff on expanded data subject rights", "Review cross-border transfer impact"], lastVerified: "2026-03-12"),

                CuratedEvent(id: "saudi_pdpl_001", title: "Saudi Arabia PDPL — implementing regulations published", dateString: "2026-05-01", endDateString: nil, eventType: "guidance", niche: "GDPR/CCPA Data Privacy", nicheCategory: "Digital", country: "Saudi Arabia", authority: "SDAIA (Saudi Data & AI Authority)", description: "SDAIA publishes the detailed implementing regulations for the Saudi Personal Data Protection Law (Royal Decree M/19). Specifies requirements for consent mechanisms, data breach procedures, and cross-border transfers from KSA.", impact: "high", sourceURL: "https://sdaia.gov.sa/", checklist: ["Review implementing regulations in detail", "Map data flows originating from KSA", "Implement consent mechanisms per SDAIA requirements", "Establish breach notification process"], lastVerified: "2026-03-14"),

                CuratedEvent(id: "uae_labor_001", title: "UAE Unemployment Insurance scheme — annual renewal", dateString: "2026-10-01", endDateString: nil, eventType: "deadline", niche: "Labor & Employment Law", nicheCategory: "Legal", country: "UAE", authority: "MOHRE", description: "Annual renewal deadline for UAE mandatory unemployment insurance scheme (Federal Decree-Law No. 13/2022). All private sector and government employees must ensure their policy is active. Non-compliance results in AED 400 fine.", impact: "medium", sourceURL: "https://www.mohre.gov.ae/", checklist: ["Verify all employees have active unemployment insurance", "Process renewals before October 1 deadline", "Check for new employee enrollments in prior quarter", "Budget AED 5-10 per month per employee"], lastVerified: "2026-03-10"),

                CuratedEvent(id: "qatar_qdpsp_001", title: "Qatar National Data Classification Policy — compliance audit window", dateString: "2026-08-01", endDateString: "2026-09-30", eventType: "audit_window", niche: "Cybersecurity & NIS2 Directive", nicheCategory: "Digital", country: "Qatar", authority: "National Cyber Security Agency (NCSA)", description: "Qatar NCSA opens compliance audit window for the National Data Classification Policy. Organizations processing government or critical infrastructure data must demonstrate proper classification, handling, and storage.", impact: "high", sourceURL: "https://www.ncsa.gov.qa/", checklist: ["Classify all data assets per NCSA framework", "Implement handling procedures for each classification level", "Prepare documentation for NCSA audit team", "Train staff on data classification obligations"], lastVerified: "2026-03-08")
            ],
            standingRequirements: [
                CuratedStandingRequirement(id: "uae_aml_goaml", title: "UAE goAML suspicious transaction reporting", obligation: "All Designated Non-Financial Businesses and Professions (DNFBPs) and Financial Institutions must file Suspicious Transaction Reports (STRs) via the goAML portal within 24 hours of suspicion.", timeframe: "Within 24 hours of forming suspicion", legalBasis: "Federal Decree-Law No. 20/2018, Cabinet Decision No. 10/2019", country: "UAE", authority: "Financial Intelligence Unit (FIU)", sourceURL: "https://www.uaefiu.gov.ae/")
            ],
            lastUpdated: Date(),
            version: "1.0"
        )
    }

    private func southAsiaPack() -> RegionalPack {
        RegionalPack(
            id: "south_asia_v1",
            region: "South Asia",
            displayName: "South Asia",
            description: "India, Pakistan — RBI, SEBI, digital regulation",
            countries: ["India"],
            curatedEvents: [
                CuratedEvent(id: "india_dpdpa_001", title: "India DPDP Act 2023 — Rules notification expected", dateString: "2026-06-30", endDateString: nil, eventType: "law_update", niche: "GDPR/CCPA Data Privacy", nicheCategory: "Digital", country: "India", authority: "Ministry of Electronics and IT (MeitY)", description: "India's Digital Personal Data Protection Act 2023 rules expected to be notified by MeitY. Will specify consent manager registration, Data Protection Board procedures, cross-border transfer whitelist, and significant data fiduciary obligations.", impact: "critical", sourceURL: "https://www.meity.gov.in/", checklist: ["Monitor MeitY for Rules publication", "Map personal data processing activities in India", "Assess consent mechanism requirements", "Evaluate cross-border transfer impact", "Plan for Data Protection Board registration if significant data fiduciary"], lastVerified: "2026-03-15"),

                CuratedEvent(id: "india_rbi_001", title: "RBI Digital Lending Guidelines — compliance review", dateString: "2026-04-01", endDateString: nil, eventType: "enforcement", niche: "FinTech & Banking Regulation", nicheCategory: "FinTech", country: "India", authority: "Reserve Bank of India (RBI)", description: "RBI conducts compliance review of digital lending guidelines issued in September 2022. Focus areas: First Loss Default Guarantee (FLDG) arrangements, customer disclosure requirements, and Lending Service Provider (LSP) registration.", impact: "high", sourceURL: "https://www.rbi.org.in/", checklist: ["Review FLDG arrangements for compliance (5% cap)", "Verify all customer disclosures are per RBI format", "Ensure LSP agreements include required clauses", "Check grievance redressal mechanism compliance"], lastVerified: "2026-03-12"),

                CuratedEvent(id: "india_sebi_esg_001", title: "SEBI BRSR Core — mandatory assurance for top 150 listed companies", dateString: "2026-04-01", endDateString: nil, eventType: "deadline", niche: "ESG & Sustainability Reporting", nicheCategory: "Legal", country: "India", authority: "SEBI", description: "SEBI's Business Responsibility and Sustainability Report (BRSR) Core framework becomes mandatory for top 150 listed companies by market capitalization. Requires reasonable assurance on specified ESG KPIs including GHG emissions, water, waste, and social indicators.", impact: "high", sourceURL: "https://www.sebi.gov.in/", checklist: ["Identify if company is in top 150 by market cap", "Engage assurance provider for BRSR Core KPIs", "Collect auditable ESG data per SEBI format", "Submit BRSR Core with annual report"], lastVerified: "2026-03-14"),

                CuratedEvent(id: "india_labor_001", title: "India Labour Codes — state-level implementation status check", dateString: "2026-07-01", endDateString: nil, eventType: "law_update", niche: "Labor & Employment Law", nicheCategory: "Legal", country: "India", authority: "Ministry of Labour & Employment", description: "Periodic check on state-level implementation of India's 4 consolidated Labour Codes (Wages, Social Security, Industrial Relations, OSH). Implementation varies dramatically by state — Karnataka, Uttar Pradesh, Madhya Pradesh have notified Rules, others pending.", impact: "medium", sourceURL: "https://labour.gov.in/", checklist: ["Check which Labour Codes are implemented in your operating states", "Review state-specific Rules where notified", "Update payroll systems for Code on Wages compliance", "Assess Social Security Code impact on PF/ESIC contributions"], lastVerified: "2026-03-10")
            ],
            standingRequirements: [
                CuratedStandingRequirement(id: "india_rbi_kyc", title: "RBI KYC periodic update", obligation: "Banks and regulated entities must perform periodic KYC updates: every 2 years for high-risk customers, every 8 years for medium-risk, and every 10 years for low-risk customers.", timeframe: "2/8/10 years based on risk category", legalBasis: "RBI Master Direction on KYC 2016 (updated 2023)", country: "India", authority: "RBI", sourceURL: "https://www.rbi.org.in/")
            ],
            lastUpdated: Date(),
            version: "1.0"
        )
    }

    private func eastAsiaPack() -> RegionalPack {
        RegionalPack(
            id: "east_asia_v1",
            region: "East Asia",
            displayName: "East Asia",
            description: "Japan, South Korea — FSA, MFDS, financial and health regulation",
            countries: ["Japan", "South Korea"],
            curatedEvents: [
                CuratedEvent(id: "japan_fsa_001", title: "Japan FSA stablecoin framework — JVCEA self-regulatory rules effective", dateString: "2026-06-01", endDateString: nil, eventType: "law_update", niche: "Cryptocurrency & Blockchain Regulation", nicheCategory: "FinTech", country: "Japan", authority: "FSA / JVCEA", description: "Japan Virtual and Crypto Assets Exchange Association (JVCEA) implements revised self-regulatory rules for stablecoin issuance and distribution under the amended Payment Services Act. Electronic Payment Means Transfer Service Providers must register.", impact: "high", sourceURL: "https://www.fsa.go.jp/en/", checklist: ["Register as Electronic Payment Means Transfer Service Provider if applicable", "Review JVCEA self-regulatory rules", "Implement reserve asset management per FSA requirements", "Submit compliance documentation to JVCEA"], lastVerified: "2026-03-14"),

                CuratedEvent(id: "japan_appi_001", title: "Japan APPI — triennial review published", dateString: "2026-09-01", endDateString: nil, eventType: "guidance", niche: "GDPR/CCPA Data Privacy", nicheCategory: "Digital", country: "Japan", authority: "PPC (Personal Information Protection Commission)", description: "Japan PPC publishes the results of the triennial review of the Act on the Protection of Personal Information (APPI). Expected to propose amendments on AI-generated personal data, children's data, and cross-border transfer mechanisms following EU adequacy review.", impact: "medium", sourceURL: "https://www.ppc.go.jp/en/", checklist: ["Review PPC triennial review findings", "Assess impact on cross-border transfers (EU adequacy status)", "Evaluate AI/children's data handling changes", "Update privacy notices if required"], lastVerified: "2026-03-12"),

                CuratedEvent(id: "korea_pipa_001", title: "South Korea PIPA Amendment — strengthened consent requirements", dateString: "2026-05-15", endDateString: nil, eventType: "law_update", niche: "GDPR/CCPA Data Privacy", nicheCategory: "Digital", country: "South Korea", authority: "PIPC (Personal Information Protection Commission)", description: "Amended Personal Information Protection Act strengthens consent requirements, introduces mandatory Privacy Impact Assessments for AI systems, and expands extraterritorial application. South Korea maintains EU adequacy status.", impact: "high", sourceURL: "https://www.pipc.go.kr/eng/", checklist: ["Review amended PIPA consent requirements", "Conduct Privacy Impact Assessment for AI systems", "Update data processing notices", "Verify compliance maintains EU adequacy alignment"], lastVerified: "2026-03-11"),

                CuratedEvent(id: "korea_mfds_001", title: "South Korea MFDS — AI-based medical device approval pathway update", dateString: "2026-04-01", endDateString: nil, eventType: "guidance", niche: "Medical Software SaMD", nicheCategory: "MedTech", country: "South Korea", authority: "MFDS (Ministry of Food and Drug Safety)", description: "MFDS updates its regulatory pathway for AI-based medical devices, introducing a dedicated review track for continuously learning AI/ML devices. Aligns partially with FDA's PCCP framework but with Korea-specific clinical evidence requirements.", impact: "medium", sourceURL: "https://www.mfds.go.kr/eng/", checklist: ["Review updated MFDS AI/ML device guidance", "Compare with FDA PCCP requirements for cross-filing", "Prepare Korea-specific clinical evidence if needed", "Assess if existing approval needs updating"], lastVerified: "2026-03-09")
            ],
            standingRequirements: [
                CuratedStandingRequirement(id: "japan_fsa_aml", title: "Japan FSA AML/CFT transaction monitoring", obligation: "Financial institutions must implement risk-based transaction monitoring and file Suspicious Transaction Reports (STRs) to JAFIC. Enhanced due diligence for high-risk customers and PEPs.", timeframe: "Continuous — every transaction", legalBasis: "Act on Prevention of Transfer of Criminal Proceeds (APTCP)", country: "Japan", authority: "FSA / JAFIC", sourceURL: "https://www.npa.go.jp/sosikihanzai/jafic/en/")
            ],
            lastUpdated: Date(),
            version: "1.0"
        )
    }
}
