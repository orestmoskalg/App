import Foundation

// MARK: - RegulatoryGlossary

/// Universal glossary covering terms from ALL regulatory niches.
/// Addresses feedback: "I wish there was a glossary for regulatory terms" (David, junior RA)
struct GlossaryTerm: Identifiable, Codable {
    var id: String { term }
    let term: String
    let definition: String
    let niches: [String]   // Which niches this term is relevant to
    let relatedTerms: [String]
}

struct RegulatoryGlossary {

    static let terms: [GlossaryTerm] = [

        // ── MedTech ──
        GlossaryTerm(term: "MDR", definition: "Medical Device Regulation (EU) 2017/745 — the EU framework governing medical devices, replacing the MDD. Sets requirements for clinical evidence, post-market surveillance, and device traceability.", niches: ["MedTech"], relatedTerms: ["IVDR", "MDD", "Notified Body", "CE marking"]),
        GlossaryTerm(term: "IVDR", definition: "In Vitro Diagnostic Regulation (EU) 2017/746 — governs IVD medical devices in the EU. Introduces risk-based classification (Rules 1-7) and requires Notified Body involvement for higher-risk devices.", niches: ["MedTech"], relatedTerms: ["MDR", "IVD", "Companion Diagnostic"]),
        GlossaryTerm(term: "510(k)", definition: "FDA premarket notification pathway demonstrating a device is substantially equivalent to an already legally marketed device (predicate). Most common US medical device clearance path.", niches: ["MedTech"], relatedTerms: ["PMA", "De Novo", "FDA"]),
        GlossaryTerm(term: "PMA", definition: "Premarket Approval — the most rigorous FDA device marketing pathway, required for Class III devices. Requires clinical trial data proving safety and effectiveness.", niches: ["MedTech"], relatedTerms: ["510(k)", "De Novo", "Clinical Investigation"]),
        GlossaryTerm(term: "Notified Body", definition: "An organization designated by an EU member state to assess whether manufacturers meet regulatory requirements. Examples: BSI, TUV SUD, SGS. Required for CE marking of higher-risk devices.", niches: ["MedTech"], relatedTerms: ["CE marking", "MDR", "Conformity Assessment"]),
        GlossaryTerm(term: "EUDAMED", definition: "European Database on Medical Devices — the EU's centralized IT system for device registration, vigilance reporting, clinical investigations, and market surveillance.", niches: ["MedTech"], relatedTerms: ["UDI", "MDR", "Vigilance"]),
        GlossaryTerm(term: "UDI", definition: "Unique Device Identification — a system to identify medical devices throughout distribution and use. Consists of a Device Identifier (UDI-DI) and Production Identifier (UDI-PI).", niches: ["MedTech"], relatedTerms: ["EUDAMED", "GUDID"]),
        GlossaryTerm(term: "SaMD", definition: "Software as a Medical Device — software intended for medical purposes that is not part of a hardware device. Classified based on the significance of the information it provides and the healthcare situation.", niches: ["MedTech", "Digital"], relatedTerms: ["IEC 62304", "AI/ML", "IMDRF"]),
        GlossaryTerm(term: "CER", definition: "Clinical Evaluation Report — a living document required under MDR that summarizes clinical evidence demonstrating a device's safety, performance, and clinical benefits.", niches: ["MedTech"], relatedTerms: ["PMCF", "Clinical Investigation", "Literature Review"]),
        GlossaryTerm(term: "PMCF", definition: "Post-Market Clinical Follow-up — active and systematic process to collect and evaluate clinical data from devices already on the market, required under MDR.", niches: ["MedTech"], relatedTerms: ["PMS", "CER", "PSUR"]),

        // ── FinTech ──
        GlossaryTerm(term: "PSD2", definition: "Payment Services Directive 2 — EU directive regulating payment services and providers. Introduced Strong Customer Authentication (SCA) and Open Banking requirements. PSD3 is in development.", niches: ["FinTech"], relatedTerms: ["SCA", "Open Banking", "PSD3"]),
        GlossaryTerm(term: "MiCA", definition: "Markets in Crypto-Assets Regulation — the EU's comprehensive framework for crypto-asset markets. Covers CASPs, stablecoins (ARTs and EMTs), and market abuse for crypto.", niches: ["FinTech"], relatedTerms: ["CASP", "ART", "EMT", "TFR"]),
        GlossaryTerm(term: "DORA", definition: "Digital Operational Resilience Act — EU regulation requiring financial entities to withstand, respond to, and recover from ICT-related disruptions and threats.", niches: ["FinTech", "Digital"], relatedTerms: ["NIS2", "ICT Risk Management"]),
        GlossaryTerm(term: "Basel III/IV", definition: "International regulatory framework for banks, setting requirements for capital, liquidity, and leverage. Basel IV (finalization) tightens rules on risk-weighted assets.", niches: ["FinTech"], relatedTerms: ["CRR", "CRD", "Capital Requirements"]),
        GlossaryTerm(term: "CASP", definition: "Crypto-Asset Service Provider — any entity providing crypto services (exchange, custody, transfer, advisory) that must be authorized under MiCA.", niches: ["FinTech"], relatedTerms: ["MiCA", "ART", "EMT"]),

        // ── Digital ──
        GlossaryTerm(term: "AI Act", definition: "EU Artificial Intelligence Act — the world's first comprehensive AI law. Classifies AI systems by risk level (unacceptable, high, limited, minimal) with corresponding obligations.", niches: ["Digital"], relatedTerms: ["High-risk AI", "GPAI", "AI Office", "Conformity Assessment"]),
        GlossaryTerm(term: "GPAI", definition: "General-Purpose AI — AI models trained with large amounts of data using self-supervision, capable of performing a wide range of tasks. Subject to transparency obligations under the AI Act, with additional rules for systemic risk models.", niches: ["Digital"], relatedTerms: ["AI Act", "Foundation Model", "Systemic Risk"]),
        GlossaryTerm(term: "GDPR", definition: "General Data Protection Regulation (EU) 2016/679 — the EU's data protection framework. Establishes data subject rights, lawful bases for processing, data breach notification, and DPA enforcement with fines up to 4% of global turnover.", niches: ["Digital", "Legal"], relatedTerms: ["DPA", "DPIA", "SCCs", "Adequacy Decision"]),
        GlossaryTerm(term: "NIS2", definition: "Network and Information Security Directive 2 — EU directive expanding cybersecurity obligations to more sectors and entity types. Requires incident reporting within 24 hours and introduces personal liability for management.", niches: ["Digital"], relatedTerms: ["ENISA", "DORA", "CRA", "Incident Reporting"]),
        GlossaryTerm(term: "DSA", definition: "Digital Services Act — EU regulation governing digital intermediaries. Requires content moderation transparency, risk assessments for Very Large Platforms (VLOPs), and algorithmic transparency.", niches: ["Digital"], relatedTerms: ["DMA", "VLOP", "Digital Services Coordinator"]),
        GlossaryTerm(term: "DMA", definition: "Digital Markets Act — EU regulation targeting 'gatekeepers' (large digital platforms) to ensure fair competition. Requires interoperability, data portability, and prohibits self-preferencing.", niches: ["Digital"], relatedTerms: ["DSA", "Gatekeeper", "Interoperability"]),
        GlossaryTerm(term: "DPA", definition: "Data Protection Authority — independent public authority supervising data protection law compliance. Each EU member state has one (e.g., CNIL in France, BfDI in Germany, ICO in UK).", niches: ["Digital"], relatedTerms: ["GDPR", "EDPB", "Enforcement"]),
        GlossaryTerm(term: "CCPA/CPRA", definition: "California Consumer Privacy Act / California Privacy Rights Act — California's data privacy laws giving consumers rights over personal data. CPRA amended CCPA and created the CPPA enforcement agency.", niches: ["Digital"], relatedTerms: ["GDPR", "State Privacy Laws", "CPPA"]),

        // ── Legal ──
        GlossaryTerm(term: "CSRD", definition: "Corporate Sustainability Reporting Directive — EU directive requiring companies to report on sustainability using European Sustainability Reporting Standards (ESRS). Phased implementation starting 2024.", niches: ["Legal"], relatedTerms: ["ESRS", "SFDR", "EU Taxonomy", "ESG"]),
        GlossaryTerm(term: "EU Taxonomy", definition: "EU classification system defining which economic activities are environmentally sustainable. Used in financial disclosures and investment decisions. Covers 6 environmental objectives.", niches: ["Legal"], relatedTerms: ["CSRD", "SFDR", "Green Bond"]),
        GlossaryTerm(term: "SFDR", definition: "Sustainable Finance Disclosure Regulation — EU regulation requiring financial market participants to disclose sustainability risks and impacts. Classifies funds as Article 6, 8, or 9.", niches: ["Legal"], relatedTerms: ["EU Taxonomy", "CSRD", "ESG", "PAI"]),
        GlossaryTerm(term: "CBAM", definition: "Carbon Border Adjustment Mechanism — EU tool imposing a carbon price on imported goods to prevent carbon leakage. Currently in transitional reporting phase before full implementation.", niches: ["Legal", "Industrial"], relatedTerms: ["ETS", "Carbon Leakage", "Scope 3"]),
        GlossaryTerm(term: "OFAC", definition: "Office of Foreign Assets Control — US Treasury agency administering and enforcing economic and trade sanctions. Maintains the SDN (Specially Designated Nationals) list.", niches: ["Legal"], relatedTerms: ["SDN", "Sanctions", "BIS"]),

        // ── Industrial ──
        GlossaryTerm(term: "REACH", definition: "Registration, Evaluation, Authorisation and Restriction of Chemicals — EU regulation requiring manufacturers to register chemical substances and manage their risks. Administered by ECHA.", niches: ["Industrial"], relatedTerms: ["ECHA", "SVHC", "CLP", "Authorisation"]),
        GlossaryTerm(term: "CLP", definition: "Classification, Labelling and Packaging Regulation — EU implementation of the GHS for classifying and labelling chemical hazards. Requires hazard pictograms and safety data sheets.", niches: ["Industrial"], relatedTerms: ["GHS", "REACH", "SDS"]),
        GlossaryTerm(term: "HACCP", definition: "Hazard Analysis and Critical Control Points — systematic preventive approach to food safety identifying physical, chemical, and biological hazards. Required by EU and US food safety regulations.", niches: ["Industrial"], relatedTerms: ["FSMA", "EFSA", "Codex Alimentarius"]),
        GlossaryTerm(term: "ETS", definition: "Emissions Trading System — EU's cap-and-trade system for greenhouse gas emissions. Companies receive or buy emission allowances; total cap decreases over time to reduce emissions.", niches: ["Industrial", "Legal"], relatedTerms: ["CBAM", "Carbon Credits", "Green Deal"]),
        GlossaryTerm(term: "Euro 7", definition: "Upcoming EU vehicle emission standard covering exhaust pollutants, brake and tyre particles, and battery durability. Applies to all motor vehicles sold in the EU.", niches: ["Industrial"], relatedTerms: ["UN ECE", "Type Approval", "Battery Regulation"]),
        GlossaryTerm(term: "CE Marking", definition: "Conformité Européenne — indicates a product meets EU health, safety, and environmental requirements. Required for many products sold in the EU/EEA including medical devices, machinery, and electronics.", niches: ["MedTech", "Industrial"], relatedTerms: ["Conformity Assessment", "Notified Body", "Declaration of Conformity"]),

        // ── Cross-cutting ──
        GlossaryTerm(term: "Conformity Assessment", definition: "Process demonstrating whether a product meets regulatory requirements. May involve self-assessment, third-party testing, quality management system audit, or type examination depending on risk class.", niches: ["MedTech", "Industrial", "Digital"], relatedTerms: ["CE Marking", "Notified Body", "ISO 13485"]),
        GlossaryTerm(term: "Transition Period", definition: "Time window between a regulation's entry into force and its full application. Allows existing products/services to comply gradually. Key dates vary by product class and regulation.", niches: ["MedTech", "FinTech", "Digital", "Legal", "Industrial"], relatedTerms: ["Grandfathering", "Legacy Device", "Sell-off Date"])
    ]

    /// Lookup terms by niche
    static func terms(for nicheCategory: String) -> [GlossaryTerm] {
        terms.filter { $0.niches.contains(nicheCategory) }
    }

    /// Search glossary
    static func search(_ query: String) -> [GlossaryTerm] {
        guard !query.isEmpty else { return terms }
        let q = query.lowercased()
        return terms.filter {
            $0.term.lowercased().contains(q) ||
            $0.definition.lowercased().contains(q)
        }
    }

    /// Find definition for a specific term (used by event's relatedTerms)
    static func definition(for term: String) -> GlossaryTerm? {
        let q = term.lowercased()
        return terms.first { $0.term.lowercased() == q }
    }
}
