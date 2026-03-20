import Foundation

// MARK: - Niche (Domain Entity v2.0)

/// A regulatory niche with deep prompt context.
///
/// **v2.0 change:** Each niche now carries `promptContext` — a detailed instruction
/// telling the AI exactly what kind of events to generate for this niche.
/// This fixes the #4 feedback issue: "non-MedTech niches feel shallow and bolted on."
struct Niche: Identifiable, Hashable, Codable {
    let id: String
    let promptKey: String
    let displayName: String
    let category: String
    let promptContext: String
    let officialSources: [String]
    let keyAuthorities: [String]

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: Niche, rhs: Niche) -> Bool { lhs.id == rhs.id }

    // MARK: - All Niches (data-driven, deep context)

    static let all: [Niche] = [

        // ═══════════════════════════════════════
        // MEDTECH
        // ═══════════════════════════════════════

        Niche(
            id: "medtech_mdr_ivdr",
            promptKey: "MedTech (MDR/IVDR)",
            displayName: "MedTech (MDR/IVDR)",
            category: "MedTech",
            promptContext: """
            Focus on: EU MDR 2017/745 transition deadlines, IVDR 2017/746 milestones, \
            Notified Body designation updates, EUDAMED module launches, UDI-DI registration deadlines, \
            clinical evaluation report (CER) submission windows, Post-Market Surveillance (PMS/PMCF) plan deadlines, \
            MDCG guidance publications, legacy device sell-off dates, Article 120 transition extensions. \
            Include both EU and non-EU MedTech: FDA 510(k), PMA, De Novo; Health Canada MDEL; TGA Australia; PMDA Japan; NMPA China.
            """,
            officialSources: ["EUR-Lex", "EUDAMED", "FDA CDRH", "MDCG guidelines"],
            keyAuthorities: ["European Commission", "FDA", "Health Canada", "TGA", "PMDA", "NMPA"]
        ),

        Niche(
            id: "medical_software_samd",
            promptKey: "Medical Software SaMD",
            displayName: "Medical Software (SaMD)",
            category: "MedTech",
            promptContext: """
            Focus on: Software as Medical Device (SaMD) classification under MDR/IVDR, \
            FDA Digital Health guidance updates, IEC 62304 lifecycle deadlines, \
            AI/ML-based SaMD predetermined change control plans (PCCP), \
            IMDRF SaMD classification framework updates, cybersecurity requirements (FDA premarket guidance), \
            Clinical Decision Support (CDS) exemption criteria changes, \
            interoperability standards (HL7 FHIR mandates), DiGA (Germany) approval process changes.
            """,
            officialSources: ["FDA Digital Health Center", "IMDRF", "IEC 62304", "BfArM DiGA"],
            keyAuthorities: ["FDA", "European Commission", "BfArM", "IMDRF"]
        ),

        Niche(
            id: "ivd_diagnostics",
            promptKey: "IVD Diagnostics (IVDR)",
            displayName: "IVD Diagnostics",
            category: "MedTech",
            promptContext: """
            Focus on: IVDR 2017/746 classification rule changes (Rule 1-7), \
            Common Specifications (CS) publication dates, performance evaluation deadlines, \
            companion diagnostics (CDx) regulatory pathways, EU Reference Laboratories designation, \
            Annex VIII classification timeline, self-testing device requirements, \
            FDA IVD guidance (LDT regulation, VALID Act status), \
            point-of-care testing (POCT) regulatory changes, proficiency testing requirements.
            """,
            officialSources: ["EUR-Lex IVDR", "FDA CDRH IVD", "EU Reference Labs"],
            keyAuthorities: ["European Commission", "FDA", "EU Reference Laboratories"]
        ),

        Niche(
            id: "pharma_drug_device",
            promptKey: "Pharma & Drug-Device Combinations",
            displayName: "Pharma & Drug-Device",
            category: "MedTech",
            promptContext: """
            Focus on: Drug-device combination product classification (FDA 21 CFR Part 3), \
            EU MDR Article 1(8-9) integral/non-integral combinations, \
            EMA-MDR coordination requirements, pre-filled syringe/auto-injector regulatory changes, \
            ICH E6(R3) GCP updates affecting device components, \
            biosimilar device interchangeability guidance, orphan drug device requirements, \
            ATMP regulatory milestones, FDA CBER/CDRH jurisdictional assignments.
            """,
            officialSources: ["EMA", "FDA OCP", "ICH", "EUR-Lex"],
            keyAuthorities: ["EMA", "FDA OCP", "MHRA", "PMDA"]
        ),

        Niche(
            id: "orthopedics_trauma",
            promptKey: "Orthopedics & Trauma Devices",
            displayName: "Orthopedics & Trauma",
            category: "MedTech",
            promptContext: """
            Focus on: Class III implant reclassification deadlines under MDR, \
            joint replacement registry reporting requirements, ODEP ratings updates, \
            clinical investigation requirements for high-risk implants, \
            3D-printed implant guidance (FDA/EU), metal-on-metal hip recall follow-ups, \
            trauma fixation device standards (ISO 5835, ASTM F543), \
            Unique Device Identification for implants, post-market clinical follow-up (PMCF) for implants.
            """,
            officialSources: ["NJR", "FDA Ortho Guidance", "ISO 5835"],
            keyAuthorities: ["FDA", "European Commission", "NJR", "AOANJRR"]
        ),

        Niche(
            id: "cardiovascular_devices",
            promptKey: "Cardiovascular Devices",
            displayName: "Cardiovascular Devices",
            category: "MedTech",
            promptContext: """
            Focus on: Transcatheter heart valve regulatory pathways, pacemaker/ICD cybersecurity requirements, \
            coronary stent clinical evidence expectations, left ventricular assist device (LVAD) PMA updates, \
            heart failure remote monitoring device regulations, \
            Class III cardiovascular device MDR transition, vascular graft standards updates, \
            FDA Breakthrough Device pathways for cardiac devices.
            """,
            officialSources: ["FDA Cardiovascular Devices", "EUR-Lex", "ACC/AHA"],
            keyAuthorities: ["FDA", "European Commission", "NICE"]
        ),

        Niche(
            id: "ophthalmic_dental",
            promptKey: "Ophthalmic & Dental Devices",
            displayName: "Ophthalmic & Dental",
            category: "MedTech",
            promptContext: """
            Focus on: Contact lens regulatory changes, IOL (intraocular lens) classification, \
            refractive surgery laser device approvals, dental implant standards (ISO 14801), \
            CAD/CAM dental restoration regulations, orthodontic aligner regulations, \
            AI-powered retinal screening device approvals, \
            dental amalgam phase-down regulations (Minamata Convention).
            """,
            officialSources: ["FDA Ophthalmic", "ISO 14801", "ADA"],
            keyAuthorities: ["FDA", "European Commission", "ADA"]
        ),

        // ═══════════════════════════════════════
        // FINTECH
        // ═══════════════════════════════════════

        Niche(
            id: "fintech_banking",
            promptKey: "FinTech & Banking Regulation",
            displayName: "FinTech & Banking",
            category: "FinTech",
            promptContext: """
            Focus on: PSD2/PSD3 implementation timelines per EU member state, \
            MiCA (Markets in Crypto-Assets) phased implementation dates, \
            Open Banking API standards deadlines (Berlin Group, UK Open Banking), \
            Basel III/IV implementation by jurisdiction, DORA (Digital Operational Resilience Act) milestones, \
            FCA (UK) regulatory sandbox rounds, BaFin (Germany) licensing deadlines, \
            AMF (France) DeFi guidance, MAS (Singapore) payment services act updates, \
            CFPB (US) open banking rulemaking, OCC fintech charter developments. \
            Be SPECIFIC per jurisdiction — not just "PSD2 deadline" but which requirement and which country.
            """,
            officialSources: ["EBA", "FCA", "BaFin", "CFPB", "MAS", "OCC"],
            keyAuthorities: ["EBA", "ECB", "FCA", "BaFin", "AMF", "MAS", "CFPB", "OCC"]
        ),

        Niche(
            id: "cryptocurrency_blockchain",
            promptKey: "Cryptocurrency & Blockchain Regulation",
            displayName: "Crypto & Blockchain",
            category: "FinTech",
            promptContext: """
            Focus on: MiCA licensing deadlines for CASPs, stablecoin (ARTs/EMTs) authorization requirements, \
            travel rule implementation (FATF Recommendation 16), \
            SEC enforcement actions and rulemaking on digital assets, \
            CFTC jurisdiction over crypto derivatives, \
            UK FCA crypto registration requirements, Japan FSA JVCEA standards, \
            DeFi regulatory frameworks emerging globally, DAO legal status developments, \
            CBDCs pilot programs and regulatory sandboxes, NFT classification guidance per jurisdiction.
            """,
            officialSources: ["ESMA MiCA", "SEC", "CFTC", "FCA Crypto", "FSA Japan"],
            keyAuthorities: ["ESMA", "SEC", "CFTC", "FCA", "FSA", "MAS"]
        ),

        // ═══════════════════════════════════════
        // DIGITAL
        // ═══════════════════════════════════════

        Niche(
            id: "ai_act_digital",
            promptKey: "EU AI Act & Digital Services Act",
            displayName: "AI Act & DSA",
            category: "Digital",
            promptContext: """
            Focus on: AI Act phased implementation — distinguish clearly between: \
            (1) Prohibited practices (Feb 2025), (2) GPAI rules (Aug 2025), \
            (3) High-risk obligations Annex III (Aug 2026), (4) Annex I product legislation (Aug 2027). \
            Include: AI Office establishment milestones, harmonised standards development (CEN/CENELEC), \
            codes of practice for GPAI, sandbox regulations, conformity assessment deadlines. \
            DSA: very large platform (VLOP) compliance audits, transparency report deadlines, \
            systemic risk assessments, Digital Services Coordinator appointments per member state.
            """,
            officialSources: ["EU AI Office", "EUR-Lex AI Act", "CEN/CENELEC", "DSA Transparency DB"],
            keyAuthorities: ["EU AI Office", "European Commission", "CEN/CENELEC", "National DSCs"]
        ),

        Niche(
            id: "gdpr_ccpa_privacy",
            promptKey: "GDPR/CCPA Data Privacy",
            displayName: "Data Privacy",
            category: "Digital",
            promptContext: """
            Focus on: EDPB guidelines publication dates, DPA enforcement actions with case references, \
            adequacy decision renewals (EU-US Data Privacy Framework review), \
            CCPA/CPRA enforcement milestones (California AG and CPPA), \
            state privacy law effective dates (Texas TDPSA, Oregon, Montana, etc.), \
            cookie consent enforcement campaigns, children's privacy (COPPA 2.0, UK Age Appropriate Design Code), \
            data breach notification requirement changes, Standard Contractual Clauses (SCCs) review dates, \
            GDPR delegated/implementing act updates. \
            ALWAYS cite the specific DPA and case/decision reference number.
            """,
            officialSources: ["EDPB", "CNIL", "ICO", "CPPA", "EUR-Lex"],
            keyAuthorities: ["EDPB", "CNIL", "BfDI", "AEPD", "ICO", "CPPA", "FTC"]
        ),

        Niche(
            id: "cybersecurity_nis2",
            promptKey: "Cybersecurity & NIS2 Directive",
            displayName: "Cybersecurity & NIS2",
            category: "Digital",
            promptContext: """
            Focus on: NIS2 Directive transposition deadlines per EU member state, \
            Cyber Resilience Act (CRA) implementation phases, \
            ENISA guidance publications, incident reporting timeline requirements, \
            sector-specific cybersecurity standards (ISO 27001 updates, IEC 62443 for OT), \
            US NIST Cybersecurity Framework updates, CISA directives, \
            UK NIS Regulations updates, supply chain security requirements, \
            critical infrastructure designation changes per country, penalty frameworks.
            """,
            officialSources: ["ENISA", "NIST", "CISA", "BSI Germany"],
            keyAuthorities: ["ENISA", "NIST", "CISA", "BSI", "NCSC UK", "ANSSI"]
        ),

        Niche(
            id: "ai_ml_regulated",
            promptKey: "AI/ML in Regulated Industries",
            displayName: "AI/ML in Regulated",
            category: "Digital",
            promptContext: """
            Focus on: AI in healthcare (FDA AI/ML Action Plan, Health Canada guidance), \
            AI in financial services (model risk management SR 11-7 updates, FCA/PRA AI guidance), \
            algorithmic trading regulations (MiFID II RTS updates), \
            AI bias audit requirements (NYC Local Law 144, EU AI Act high-risk), \
            explainability requirements per sector, AI safety institutes (UK AISI, US AISI), \
            autonomous vehicle AI regulations (UN ECE, NHTSA), \
            AI in employment (EEOC guidance, EU Platform Workers Directive).
            """,
            officialSources: ["FDA AI/ML", "FCA AI", "NIST AI RMF", "UK AISI"],
            keyAuthorities: ["FDA", "FCA", "PRA", "NIST", "UK AISI", "EEOC"]
        ),

        Niche(
            id: "digital_markets_act",
            promptKey: "Digital Markets Act (DMA)",
            displayName: "Digital Markets Act",
            category: "Digital",
            promptContext: """
            Focus on: Gatekeeper designation decisions, compliance report deadlines, \
            interoperability requirements implementation dates, \
            European Commission non-compliance investigations, \
            DMA penalty decisions, new service designation proceedings, \
            app store sideloading implementation, messaging interoperability timelines, \
            data portability requirements, self-preferencing prohibition enforcement.
            """,
            officialSources: ["European Commission DMA", "EUR-Lex"],
            keyAuthorities: ["European Commission DG CNECT", "National Competition Authorities"]
        ),

        // ═══════════════════════════════════════
        // LEGAL
        // ═══════════════════════════════════════

        Niche(
            id: "esg_sustainability",
            promptKey: "ESG & Sustainability Reporting",
            displayName: "ESG & Sustainability",
            category: "Legal",
            promptContext: """
            Focus on: CSRD reporting obligation phased roll-out (which company sizes, which years), \
            ESRS (European Sustainability Reporting Standards) adoption dates, \
            EU Taxonomy Delegated Acts updates, SFDR periodic reporting deadlines, \
            CBAM (Carbon Border Adjustment Mechanism) transitional phase milestones, \
            SEC climate disclosure rules (status and deadlines), \
            ISSB (IFRS S1/S2) adoption by jurisdiction, \
            Swiss CO sustainability obligations, UK SDR and green taxonomy, \
            corporate due diligence directive (CSDDD) implementation, \
            greenwashing enforcement actions per DPA/authority. \
            Include QUARTERLY reporting deadlines — not just annual.
            """,
            officialSources: ["EFRAG", "EUR-Lex CSRD", "SEC", "ISSB", "FINMA"],
            keyAuthorities: ["EFRAG", "European Commission", "SEC", "ISSB", "FINMA", "FCA"]
        ),

        Niche(
            id: "sanctions_export",
            promptKey: "Sanctions & Export Controls",
            displayName: "Sanctions & Export",
            category: "Legal",
            promptContext: """
            Focus on: EU sanctions packages (numbered, with specific deadlines), \
            OFAC SDN list updates, BIS Entity List additions, \
            EU Dual-Use Regulation implementation, Wassenaar Arrangement updates, \
            UK OFSI sanctions designations, screening requirement changes, \
            semiconductor export control restrictions (US-China), \
            wind-down period expirations, humanitarian exemption frameworks, \
            enforcement actions with penalty amounts.
            """,
            officialSources: ["EU Sanctions Map", "OFAC", "BIS", "UK OFSI"],
            keyAuthorities: ["European Commission", "OFAC", "BIS", "UK OFSI", "BAFA"]
        ),

        Niche(
            id: "labor_employment",
            promptKey: "Labor & Employment Law",
            displayName: "Labor & Employment",
            category: "Legal",
            promptContext: """
            Focus on: EU Platform Workers Directive implementation timelines, \
            pay transparency directive transposition deadlines, \
            whistleblower protection directive status per member state, \
            minimum wage updates by country, remote work legislation changes, \
            US DOL overtime rule changes, EEOC guidance updates, \
            UK Employment Rights Bill milestones, gig economy classification cases, \
            occupational health and safety directive updates.
            """,
            officialSources: ["EUR-Lex", "DOL", "EEOC", "ILO"],
            keyAuthorities: ["European Commission", "DOL", "EEOC", "HMRC", "ILO"]
        ),

        // ═══════════════════════════════════════
        // INDUSTRIAL
        // ═══════════════════════════════════════

        Niche(
            id: "automotive_unece",
            promptKey: "Automotive Regulation (UN ECE, Euro 7)",
            displayName: "Automotive",
            category: "Industrial",
            promptContext: """
            Focus on: Euro 7 emission standards implementation dates (by vehicle category), \
            UN ECE WP.29 automated driving regulation updates, \
            battery regulation (EU Battery Regulation) implementation phases, \
            EV charging infrastructure directive deadlines, \
            type-approval regulation changes, cybersecurity (UN R155) and software update (UN R156) compliance, \
            NHTSA rulemaking (US), China NEV standards, \
            end-of-life vehicle directive revision, fleet CO2 targets milestones.
            """,
            officialSources: ["UN ECE WP.29", "EUR-Lex", "NHTSA", "MIIT China"],
            keyAuthorities: ["UN ECE", "European Commission", "NHTSA", "KBA", "MIIT"]
        ),

        Niche(
            id: "food_safety_haccp",
            promptKey: "Food Safety & HACCP",
            displayName: "Food Safety",
            category: "Industrial",
            promptContext: """
            Focus on: EFSA risk assessment publications, novel food authorization deadlines, \
            food contact material regulation updates, allergen labelling changes, \
            FSMA (US) compliance dates, Codex Alimentarius updates, \
            EU farm-to-fork strategy milestones, food fraud enforcement actions, \
            maximum residue limits (MRL) updates, organic regulation changes, \
            front-of-pack labelling implementation (Nutri-Score mandate status), \
            food supplement regulation changes per country.
            """,
            officialSources: ["EFSA", "FDA FSMA", "Codex Alimentarius", "EUR-Lex"],
            keyAuthorities: ["EFSA", "FDA", "FSA UK", "BVL Germany", "Codex"]
        ),

        Niche(
            id: "cosmetics_reach",
            promptKey: "Cosmetics & REACH Chemical Regulation",
            displayName: "Cosmetics & REACH",
            category: "Industrial",
            promptContext: """
            Focus on: REACH registration deadlines, SVHC (Substances of Very High Concern) candidate list updates, \
            restriction proposals and their comment periods, \
            cosmetics regulation ingredient bans/restrictions (Annexes II-VI updates), \
            ECHA opinions and recommendations, EU cosmetics safety assessment requirements, \
            animal testing ban enforcement, microplastics restriction timeline, \
            PFAS universal restriction proposal phases, \
            UK REACH divergence from EU REACH, K-beauty/J-beauty regulatory changes.
            """,
            officialSources: ["ECHA", "EUR-Lex Cosmetics Reg", "SCCS opinions"],
            keyAuthorities: ["ECHA", "European Commission", "HSE UK", "MFDS Korea"]
        ),

        Niche(
            id: "chemicals_regulation",
            promptKey: "Chemicals Regulation (CLP, REACH, GHS)",
            displayName: "Chemicals",
            category: "Industrial",
            promptContext: """
            Focus on: CLP Regulation ATP (Adaptations to Technical Progress) adoption dates, \
            GHS revision implementation per jurisdiction, \
            REACH authorization sunset dates for specific substances, \
            TSCA (US) risk evaluations and regulations, \
            POPs Convention new listings, biocidal products regulation updates, \
            RoHS substance restriction updates, endocrine disruptor criteria changes, \
            ECHA enforcement project results, occupational exposure limit updates.
            """,
            officialSources: ["ECHA", "EPA TSCA", "EUR-Lex CLP", "Stockholm Convention"],
            keyAuthorities: ["ECHA", "EPA", "HSE", "BAuA"]
        ),

        Niche(
            id: "environmental_regulations",
            promptKey: "Environmental Regulations (ETS, CBAM, EIA)",
            displayName: "Environmental",
            category: "Industrial",
            promptContext: """
            Focus on: EU ETS Phase 4 changes (aviation, maritime inclusion dates), \
            CBAM transitional period reporting deadlines, \
            Industrial Emissions Directive (IED) revision, \
            nature restoration regulation milestones, water framework directive review, \
            EPA (US) Clean Air Act/Clean Water Act rulemaking, \
            carbon credit market regulation, deforestation regulation implementation, \
            waste framework directive circular economy targets, plastic packaging tax/EPR changes per country.
            """,
            officialSources: ["EUR-Lex", "EPA", "UNFCCC", "EEA"],
            keyAuthorities: ["European Commission DG ENV", "EPA", "Environment Agency UK", "UBA"]
        )
    ]

    // MARK: - Grouped Access

    static let byCategory: [(category: String, niches: [Niche])] = {
        let grouped = Dictionary(grouping: all, by: { $0.category })
        let order = ["MedTech", "FinTech", "Digital", "Legal", "Industrial"]
        return order.compactMap { cat in
            guard let items = grouped[cat], !items.isEmpty else { return nil }
            return (category: cat, niches: items)
        }
    }()

    static let byID: [String: Niche] = {
        Dictionary(uniqueKeysWithValues: all.map { ($0.id, $0) })
    }()
}
