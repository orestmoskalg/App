import Foundation

// MARK: - StandingRequirement

/// A persistent regulatory obligation that is NOT a calendar event.
///
/// Examples:
/// - "NIS2: Report incidents to CSIRT within 24 hours"
/// - "GDPR: Respond to data subject requests within 30 days"
/// - "MDR: Maintain Post-Market Surveillance system continuously"
///
/// These are reference cards visible at all times, not calendar entries.
/// Addresses Pawel's feedback: "Not all compliance is dates."
struct StandingRequirement: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let obligation: String
    let timeframe: String?
    let legalBasis: String
    let nicheID: String
    let nicheCategory: String
    let jurisdictions: [String]
    let authority: String
    let penaltyInfo: String?
    let practicalSteps: [String]
    let sourceURL: String?
    let relatedEventTypes: [RegulatoryEvent.EventType]
    let lastUpdated: Date
    let effectiveFrom: Date?

    /// Urgency classification for visual display
    enum Urgency: String, Codable {
        case continuous    // Always active (e.g., "maintain PMS system")
        case triggered     // Activated by an event (e.g., "report breach within 72h")
        case periodic      // Recurring obligation (e.g., "annual PSUR submission")
    }

    let urgency: Urgency

    /// Human-readable timeframe badge
    var timeframeBadge: String {
        timeframe ?? (urgency == .continuous ? "Ongoing" : "When triggered")
    }
}

// MARK: - StandingRequirementsCatalog

/// Curated catalog of standing requirements across ALL regulatory niches.
/// These are hand-verified, not AI-generated — addressing the trust concern.
struct StandingRequirementsCatalog {

    static let all: [StandingRequirement] = [

        // ═══════════════════════════════════════
        // MEDTECH
        // ═══════════════════════════════════════

        StandingRequirement(
            id: "mdr_pms_continuous",
            title: "Post-Market Surveillance system",
            obligation: "Manufacturers must proactively collect and evaluate data on the safety and performance of their devices throughout the entire product lifecycle. This includes complaint handling, trend analysis, and corrective actions.",
            timeframe: "Continuous — throughout device lifecycle",
            legalBasis: "MDR Article 83, Annex III Section 1.1",
            nicheID: "medtech_mdr_ivdr",
            nicheCategory: "MedTech",
            jurisdictions: ["EU"],
            authority: "National Competent Authorities",
            penaltyInfo: "Device suspension, market withdrawal, fines up to EUR 5M or 2% of annual turnover (varies by member state)",
            practicalSteps: [
                "Establish PMS plan per Annex III before placing device on market",
                "Collect and analyze complaint data continuously",
                "Monitor published literature and similar device reports",
                "Submit PSUR (Periodic Safety Update Report) — annually for Class III/IIb, every 2 years for IIa/I",
                "Update clinical evaluation based on PMS data",
                "Report serious incidents via vigilance system within 15 days"
            ],
            sourceURL: "https://eur-lex.europa.eu/eli/reg/2017/745/oj",
            relatedEventTypes: [.deadline, .auditWindow, .marketSurveillance],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .continuous
        ),

        StandingRequirement(
            id: "mdr_vigilance_serious",
            title: "Serious incident reporting",
            obligation: "Report any serious incident involving a medical device to the relevant competent authority. A serious incident includes death, serious deterioration of health, or serious public health threat.",
            timeframe: "Within 15 days (2 days for life-threatening, 10 days for near-miss)",
            legalBasis: "MDR Article 87, MEDDEV 2.12/1 rev 8",
            nicheID: "medtech_mdr_ivdr",
            nicheCategory: "MedTech",
            jurisdictions: ["EU"],
            authority: "National Competent Authority + EUDAMED",
            penaltyInfo: "Fines, device suspension, potential criminal liability in some member states",
            practicalSteps: [
                "Establish 24/7 vigilance reporting capability",
                "Report to competent authority within 15 days of awareness",
                "Life-threatening events: initial report within 2 days",
                "Submit follow-up reports until investigation is complete",
                "File Field Safety Corrective Actions (FSCA) if needed",
                "Record all incidents in technical documentation"
            ],
            sourceURL: "https://ec.europa.eu/health/md_sector/new_regulations_en",
            relatedEventTypes: [.enforcement, .marketSurveillance],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .triggered
        ),

        StandingRequirement(
            id: "fda_mdr_reporting",
            title: "FDA Medical Device Reporting (MDR)",
            obligation: "Report deaths, serious injuries, and malfunctions associated with medical devices to the FDA. Manufacturers, importers, and device user facilities have different reporting obligations.",
            timeframe: "Within 30 calendar days (5 days for events requiring remedial action)",
            legalBasis: "21 CFR Part 803",
            nicheID: "medtech_mdr_ivdr",
            nicheCategory: "MedTech",
            jurisdictions: ["USA"],
            authority: "FDA CDRH",
            penaltyInfo: "Warning letters, consent decrees, civil penalties up to $15,000 per violation",
            practicalSteps: [
                "Submit via FDA eMDR (electronic Medical Device Reporting)",
                "30-day reports for deaths and serious injuries",
                "5-day reports for events requiring remedial action to prevent unreasonable risk",
                "Annual baseline reports for registered devices",
                "Maintain complaint files per 21 CFR 820.198"
            ],
            sourceURL: "https://www.accessdata.fda.gov/scripts/cdrh/cfdocs/cfmaude/search.cfm",
            relatedEventTypes: [.enforcement, .guidance],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .triggered
        ),

        // ═══════════════════════════════════════
        // DIGITAL / CYBERSECURITY
        // ═══════════════════════════════════════

        StandingRequirement(
            id: "nis2_incident_24h",
            title: "NIS2 incident reporting",
            obligation: "Essential and important entities must report significant cybersecurity incidents to their national CSIRT or competent authority. Early warning within 24 hours, full notification within 72 hours, final report within 1 month.",
            timeframe: "24h early warning → 72h notification → 1 month final report",
            legalBasis: "NIS2 Directive Article 23",
            nicheID: "cybersecurity_nis2",
            nicheCategory: "Digital",
            jurisdictions: ["EU"],
            authority: "National CSIRT (e.g., CSIRT NASK in Poland, BSI in Germany)",
            penaltyInfo: "Essential entities: up to EUR 10M or 2% of global turnover. Important entities: up to EUR 7M or 1.4%",
            practicalSteps: [
                "Establish 24/7 incident detection and response capability",
                "Submit early warning to CSIRT within 24 hours of becoming aware",
                "Provide incident notification with initial assessment within 72 hours",
                "Submit final report with root cause analysis within 1 month",
                "Inform service recipients if incident may affect them",
                "Management body must approve and oversee cybersecurity risk measures (personal liability)"
            ],
            sourceURL: "https://eur-lex.europa.eu/eli/dir/2022/2555/oj",
            relatedEventTypes: [.enforcement, .deadline],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .triggered
        ),

        StandingRequirement(
            id: "gdpr_breach_72h",
            title: "GDPR personal data breach notification",
            obligation: "Notify the supervisory authority within 72 hours of becoming aware of a personal data breach likely to result in a risk to individuals' rights and freedoms. Inform affected individuals without undue delay if high risk.",
            timeframe: "72 hours to supervisory authority; without undue delay to individuals",
            legalBasis: "GDPR Articles 33-34",
            nicheID: "gdpr_ccpa_privacy",
            nicheCategory: "Digital",
            jurisdictions: ["EU", "UK"],
            authority: "Lead Supervisory Authority (CNIL, ICO, BfDI, etc.)",
            penaltyInfo: "Up to EUR 10M or 2% of global annual turnover for failure to notify",
            practicalSteps: [
                "Maintain breach detection and assessment procedures",
                "Document ALL breaches (even non-reportable ones) in internal register",
                "Assess risk to individuals within first hours",
                "Notify supervisory authority within 72 hours (include nature, categories, likely consequences, measures taken)",
                "If high risk: notify affected individuals directly and without undue delay",
                "If notification is delayed beyond 72h, provide reasons for delay"
            ],
            sourceURL: "https://edpb.europa.eu/our-work-tools/documents/public-consultations_en",
            relatedEventTypes: [.enforcement, .guidance],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .triggered
        ),

        StandingRequirement(
            id: "gdpr_dsar_30d",
            title: "Data subject access request response",
            obligation: "Respond to data subject requests (access, erasure, rectification, portability, restriction, objection) within one calendar month. Can extend by two months for complex requests with notification.",
            timeframe: "1 month (extendable to 3 months for complex cases)",
            legalBasis: "GDPR Article 12(3)",
            nicheID: "gdpr_ccpa_privacy",
            nicheCategory: "Digital",
            jurisdictions: ["EU", "UK"],
            authority: "National DPA",
            penaltyInfo: "Up to EUR 20M or 4% of global annual turnover",
            practicalSteps: [
                "Verify identity of requester before processing",
                "Acknowledge receipt and assess request scope",
                "Respond within 1 calendar month of receipt",
                "If extension needed: notify requester within first month with reasons",
                "Provide information in a concise, intelligible, easily accessible form",
                "Free of charge for first request; reasonable fee allowed for excessive/repetitive requests"
            ],
            sourceURL: "https://eur-lex.europa.eu/eli/reg/2016/679/oj",
            relatedEventTypes: [.enforcement],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .triggered
        ),

        StandingRequirement(
            id: "ai_act_transparency",
            title: "AI Act transparency obligations",
            obligation: "Providers of AI systems interacting with natural persons must ensure the system is designed so users know they are interacting with AI. Applies to chatbots, emotion recognition, biometric categorization, and deepfake generators.",
            timeframe: "Continuous — from deployment onwards",
            legalBasis: "AI Act Article 50",
            nicheID: "ai_act_digital",
            nicheCategory: "Digital",
            jurisdictions: ["EU"],
            authority: "National Market Surveillance Authority + EU AI Office",
            penaltyInfo: "Up to EUR 15M or 3% of global annual turnover",
            practicalSteps: [
                "Label all AI-generated or manipulated content (text, audio, video, images)",
                "Inform users clearly that they are interacting with an AI system",
                "For emotion recognition / biometric categorization: inform exposed persons",
                "For deepfakes: disclose that content has been artificially generated/manipulated",
                "Machine-readable marking for AI-generated content where technically feasible",
                "Document transparency measures in technical documentation"
            ],
            sourceURL: "https://eur-lex.europa.eu/eli/reg/2024/1689/oj",
            relatedEventTypes: [.enforcement, .guidance, .transitionPeriod],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .continuous
        ),

        // ═══════════════════════════════════════
        // FINTECH
        // ═══════════════════════════════════════

        StandingRequirement(
            id: "psd2_sca",
            title: "Strong Customer Authentication (SCA)",
            obligation: "Payment service providers must apply Strong Customer Authentication when a payer initiates an electronic payment, accesses their online account, or carries out any action through a remote channel that may imply a risk of fraud.",
            timeframe: "Every qualifying transaction — continuous",
            legalBasis: "PSD2 Article 97, RTS on SCA (EU 2018/389)",
            nicheID: "fintech_banking",
            nicheCategory: "FinTech",
            jurisdictions: ["EU", "UK"],
            authority: "EBA / National Competent Authority",
            penaltyInfo: "Varies by member state; up to withdrawal of authorization",
            practicalSteps: [
                "Implement 2 of 3 authentication factors: knowledge, possession, inherence",
                "Apply to all electronic payments above EUR 0 (exemptions below)",
                "Exemptions: low-value (< EUR 30, cumulative < EUR 100), recurring, trusted beneficiaries, TRA",
                "Transaction Risk Analysis (TRA) exemptions require fraud rates below EBA thresholds",
                "SCA must generate dynamic authentication code linked to amount and payee",
                "Review exemption usage quarterly against EBA reference fraud rates"
            ],
            sourceURL: "https://www.eba.europa.eu/regulation-and-policy/payment-services-and-electronic-money",
            relatedEventTypes: [.enforcement, .standardUpdate],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .continuous
        ),

        StandingRequirement(
            id: "mica_casp_ongoing",
            title: "MiCA CASP ongoing obligations",
            obligation: "Authorized Crypto-Asset Service Providers must maintain organizational requirements including governance, complaint handling, conflict of interest policies, outsourcing rules, and prudential safeguards continuously.",
            timeframe: "Continuous — from authorization onwards",
            legalBasis: "MiCA Title V, Articles 59-73",
            nicheID: "cryptocurrency_blockchain",
            nicheCategory: "FinTech",
            jurisdictions: ["EU"],
            authority: "National Competent Authority (per home member state)",
            penaltyInfo: "Up to EUR 5M or 3-12.5% of annual turnover depending on violation type",
            practicalSteps: [
                "Maintain minimum prudential safeguards (own funds or insurance)",
                "Segregate client crypto-assets and funds from own assets",
                "Implement robust governance with fit-and-proper management",
                "Maintain complaint handling procedure and publish it",
                "Identify, prevent, manage, and disclose conflicts of interest",
                "Comply with AML/CFT obligations including travel rule"
            ],
            sourceURL: "https://eur-lex.europa.eu/eli/reg/2023/1114/oj",
            relatedEventTypes: [.enforcement, .auditWindow],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .continuous
        ),

        // ═══════════════════════════════════════
        // LEGAL / ESG
        // ═══════════════════════════════════════

        StandingRequirement(
            id: "csrd_quarterly",
            title: "CSRD quarterly data collection",
            obligation: "Companies subject to CSRD must collect sustainability data throughout the year — not just at annual reporting. ESRS requires quantitative metrics (GHG emissions, water usage, workforce data) that need quarterly tracking to ensure accurate annual disclosure.",
            timeframe: "Quarterly data collection; annual reporting",
            legalBasis: "CSRD Directive 2022/2464, ESRS (Delegated Reg 2023/2772)",
            nicheID: "esg_sustainability",
            nicheCategory: "Legal",
            jurisdictions: ["EU"],
            authority: "National Transposition Authority + Auditor",
            penaltyInfo: "Varies by member state; liability for directors who sign off on inaccurate reports",
            practicalSteps: [
                "Q1: Collect Scope 1, 2, 3 GHG emissions data for prior quarter",
                "Q2: Update workforce metrics (diversity, training hours, H&S incidents)",
                "Q3: Review supply chain due diligence data (CSDDD alignment)",
                "Q4: Consolidate annual sustainability statement per ESRS",
                "Engage auditor for limited assurance (evolving to reasonable assurance)",
                "Ensure data quality is auditable — maintain documentation trail"
            ],
            sourceURL: "https://eur-lex.europa.eu/eli/dir/2022/2464/oj",
            relatedEventTypes: [.reportingDue, .deadline],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .periodic
        ),

        StandingRequirement(
            id: "sanctions_screening",
            title: "Sanctions screening obligation",
            obligation: "All EU/US/UK persons and entities must screen business relationships, transactions, and counterparties against applicable sanctions lists. No transactions with sanctioned persons or in sanctioned sectors/countries.",
            timeframe: "Continuous — every transaction and new business relationship",
            legalBasis: "EU Council Regulations (various), OFAC regulations (31 CFR 500-599), UK Sanctions Act 2018",
            nicheID: "sanctions_export",
            nicheCategory: "Legal",
            jurisdictions: ["EU", "USA", "UK"],
            authority: "OFAC (US), National Competent Authorities (EU), OFSI (UK)",
            penaltyInfo: "Criminal penalties including imprisonment. Civil fines: OFAC up to $330,000/violation or twice transaction value; EU varies by member state",
            practicalSteps: [
                "Screen all customers, vendors, and counterparties against: EU Consolidated List, OFAC SDN + non-SDN, UK Sanctions List",
                "Re-screen existing relationships whenever lists are updated",
                "Screen at onboarding, transaction execution, and periodic review",
                "Document all screening results (including false positives)",
                "Escalate potential matches to compliance officer within 24 hours",
                "File blocking reports (OFAC) or asset freeze reports (EU/UK) for confirmed matches"
            ],
            sourceURL: "https://www.sanctionsmap.eu/",
            relatedEventTypes: [.enforcement, .lawUpdate],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .continuous
        ),

        // ═══════════════════════════════════════
        // INDUSTRIAL
        // ═══════════════════════════════════════

        StandingRequirement(
            id: "reach_sds_update",
            title: "REACH Safety Data Sheet maintenance",
            obligation: "Suppliers of hazardous substances/mixtures must provide and keep Safety Data Sheets (SDS) up to date. SDS must be revised without delay when new information on hazards or risk management becomes available.",
            timeframe: "Without delay upon new information; review recommended annually",
            legalBasis: "REACH Article 31, Annex II",
            nicheID: "chemicals_regulation",
            nicheCategory: "Industrial",
            jurisdictions: ["EU", "UK"],
            authority: "ECHA (EU), HSE (UK)",
            penaltyInfo: "Varies by member state; typically EUR 5,000-50,000 per violation; potential product withdrawal",
            practicalSteps: [
                "Update SDS when new classification or hazard information becomes available",
                "Update when new risk management measures are identified",
                "Update when authorization is granted or refused",
                "Update when a restriction is imposed",
                "Provide updated SDS to all recipients who received the substance in prior 12 months",
                "Include exposure scenarios for registered substances > 10 tonnes/year"
            ],
            sourceURL: "https://echa.europa.eu/safety-data-sheets",
            relatedEventTypes: [.standardUpdate, .enforcement],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .triggered
        ),

        StandingRequirement(
            id: "haccp_monitoring",
            title: "HACCP monitoring at critical control points",
            obligation: "Food business operators must monitor Critical Control Points (CCPs) continuously or at specified intervals. Monitoring records must be maintained and available for official controls at all times.",
            timeframe: "Continuous or per-batch — at every CCP",
            legalBasis: "Regulation (EC) 852/2004 Article 5, Codex Alimentarius",
            nicheID: "food_safety_haccp",
            nicheCategory: "Industrial",
            jurisdictions: ["EU", "USA", "International"],
            authority: "National food safety authority (e.g., EFSA, FDA, FSA)",
            penaltyInfo: "Product recall, facility closure, criminal penalties for serious violations",
            practicalSteps: [
                "Monitor each CCP at the frequency specified in your HACCP plan",
                "Record all monitoring results in real-time (temperature, pH, time, etc.)",
                "Verify that monitoring is effective through periodic validation",
                "Take corrective actions immediately when critical limits are exceeded",
                "Document all corrective actions and their outcomes",
                "Retain records for at least 2 years (longer for specific products)"
            ],
            sourceURL: "https://www.efsa.europa.eu/en/topics/topic/hazard-analysis-critical-control-point-haccp",
            relatedEventTypes: [.auditWindow, .enforcement],
            lastUpdated: Date(),
            effectiveFrom: nil,
            urgency: .continuous
        )
    ]

    // MARK: - Queries

    static func requirements(for nicheID: String) -> [StandingRequirement] {
        all.filter { $0.nicheID == nicheID }
    }

    static func requirements(for category: String) -> [StandingRequirement] {
        all.filter { $0.nicheCategory == category }
    }

    static func requirements(for jurisdictions: Set<String>) -> [StandingRequirement] {
        guard !jurisdictions.isEmpty else { return all }
        return all.filter { req in
            !Set(req.jurisdictions).isDisjoint(with: jurisdictions)
        }
    }

    static func triggered() -> [StandingRequirement] {
        all.filter { $0.urgency == .triggered }
    }

    static func continuous() -> [StandingRequirement] {
        all.filter { $0.urgency == .continuous }
    }

    static func periodic() -> [StandingRequirement] {
        all.filter { $0.urgency == .periodic }
    }
}
