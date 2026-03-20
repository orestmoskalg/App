package com.example.myapplication2.core.common

import com.example.myapplication2.domain.model.UserProfile

/**
 * Deep prompt context from feedback v2 (22 regulatory “themes”), mapped onto [NicheCatalog] entries.
 * Appended to main research prompts — not a second niche system.
 */
data class NichePromptEnrichment(
    val promptContext: String,
    val officialSources: List<String>,
    val keyAuthorities: List<String>,
)

object NichePromptEnrichmentCatalog {

    private val byV2Id: Map<String, NichePromptEnrichment> = mapOf(
        "medtech_mdr_ivdr" to NichePromptEnrichment(
            promptContext = "Focus on: EU MDR 2017/745 transition deadlines, IVDR 2017/746 milestones, " +
                "Notified Body designation updates, EUDAMED module launches, UDI-DI registration deadlines, " +
                "CER submission windows, PMS/PMCF plan deadlines, MDCG guidance publications, " +
                "legacy device sell-off dates, Article 120 transition extensions. " +
                "Include FDA 510(k), PMA, De Novo; Health Canada MDEL; TGA Australia; PMDA Japan; NMPA China.",
            officialSources = listOf("EUR-Lex", "EUDAMED", "FDA CDRH", "MDCG guidelines"),
            keyAuthorities = listOf("European Commission", "FDA", "Health Canada", "TGA", "PMDA", "NMPA"),
        ),
        "medical_software_samd" to NichePromptEnrichment(
            promptContext = "Focus on: SaMD classification under MDR/IVDR, FDA Digital Health guidance, " +
                "IEC 62304 lifecycle deadlines, AI/ML-based SaMD PCCP plans, IMDRF SaMD framework updates, " +
                "cybersecurity requirements (FDA premarket), CDS exemption criteria, " +
                "interoperability standards (HL7 FHIR mandates), DiGA (Germany) approval changes.",
            officialSources = listOf("FDA Digital Health Center", "IMDRF", "IEC 62304", "BfArM DiGA"),
            keyAuthorities = listOf("FDA", "European Commission", "BfArM", "IMDRF"),
        ),
        "ivd_diagnostics" to NichePromptEnrichment(
            promptContext = "Focus on: IVDR classification rules (1-7), Common Specifications dates, " +
                "performance evaluation deadlines, companion diagnostics pathways, EU Reference Labs, " +
                "Annex VIII timeline, self-testing requirements, FDA IVD/LDT regulation, POCT changes.",
            officialSources = listOf("EUR-Lex IVDR", "FDA CDRH IVD", "EU Reference Labs"),
            keyAuthorities = listOf("European Commission", "FDA", "EU Reference Laboratories"),
        ),
        "pharma_drug_device" to NichePromptEnrichment(
            promptContext = "Focus on: Drug-device combination classification (FDA 21 CFR Part 3), " +
                "EU MDR Article 1(8-9) integral/non-integral, EMA-MDR coordination, " +
                "pre-filled syringe regulatory changes, ICH E6(R3) GCP updates, biosimilar device guidance, " +
                "orphan drug device requirements, ATMP milestones, FDA CBER/CDRH jurisdictional assignments.",
            officialSources = listOf("EMA", "FDA OCP", "ICH", "EUR-Lex"),
            keyAuthorities = listOf("EMA", "FDA OCP", "MHRA", "PMDA"),
        ),
        "orthopedics_trauma" to NichePromptEnrichment(
            promptContext = "Focus on: Class III implant reclassification under MDR, joint replacement registry reporting, " +
                "ODEP ratings, clinical investigation for high-risk implants, 3D-printed implant guidance, " +
                "metal-on-metal recall follow-ups, ISO 5835/ASTM F543 standards, UDI for implants, PMCF for implants.",
            officialSources = listOf("NJR", "FDA Ortho Guidance", "ISO 5835"),
            keyAuthorities = listOf("FDA", "European Commission", "NJR", "AOANJRR"),
        ),
        "cardiovascular_devices" to NichePromptEnrichment(
            promptContext = "Focus on: Transcatheter heart valve pathways, pacemaker/ICD cybersecurity, " +
                "coronary stent evidence expectations, LVAD PMA updates, heart failure remote monitoring, " +
                "Class III cardiovascular MDR transition, vascular graft standards, FDA Breakthrough Device pathways.",
            officialSources = listOf("FDA Cardiovascular", "EUR-Lex", "ACC/AHA"),
            keyAuthorities = listOf("FDA", "European Commission", "NICE"),
        ),
        "ophthalmic_dental" to NichePromptEnrichment(
            promptContext = "Focus on: Contact lens regulatory changes, IOL classification, refractive laser approvals, " +
                "dental implant ISO 14801, CAD/CAM dental regs, orthodontic aligner regulation, " +
                "AI retinal screening approvals, dental amalgam phase-down (Minamata Convention).",
            officialSources = listOf("FDA Ophthalmic", "ISO 14801", "ADA"),
            keyAuthorities = listOf("FDA", "European Commission", "ADA"),
        ),
        "fintech_banking" to NichePromptEnrichment(
            promptContext = "Focus on: PSD2/PSD3 implementation per EU member state, MiCA phased implementation, " +
                "Open Banking API deadlines (Berlin Group, UK Open Banking), Basel III/IV by jurisdiction, " +
                "DORA milestones, FCA sandbox rounds, BaFin licensing, AMF DeFi guidance, " +
                "MAS payment services updates, CFPB open banking rulemaking, OCC fintech charter. " +
                "Be SPECIFIC per jurisdiction — not only generic PSD2 deadlines.",
            officialSources = listOf("EBA", "FCA", "BaFin", "CFPB", "MAS", "OCC"),
            keyAuthorities = listOf("EBA", "ECB", "FCA", "BaFin", "AMF", "MAS", "CFPB", "OCC"),
        ),
        "cryptocurrency_blockchain" to NichePromptEnrichment(
            promptContext = "Focus on: MiCA CASP licensing deadlines, ART/EMT authorization, " +
                "travel rule implementation (FATF R16), SEC enforcement/rulemaking on digital assets, " +
                "CFTC crypto derivatives jurisdiction, UK FCA crypto registration, Japan FSA JVCEA, " +
                "DeFi regulatory frameworks, DAO legal status, CBDC pilots, NFT classification per jurisdiction.",
            officialSources = listOf("ESMA MiCA", "SEC", "CFTC", "FCA Crypto", "FSA Japan"),
            keyAuthorities = listOf("ESMA", "SEC", "CFTC", "FCA", "FSA", "MAS"),
        ),
        "ai_act_digital" to NichePromptEnrichment(
            promptContext = "Focus on: AI Act phased implementation — distinguish: " +
                "(1) Prohibited practices (Feb 2025), (2) GPAI rules (Aug 2025), " +
                "(3) High-risk obligations Annex III (Aug 2026), (4) Annex I product legislation (Aug 2027). " +
                "Include: AI Office milestones, CEN/CENELEC harmonised standards, codes of practice for GPAI, " +
                "sandbox regulations, conformity assessment deadlines. " +
                "DSA: VLOP compliance audits, transparency reports, systemic risk assessments, DSC appointments.",
            officialSources = listOf("EU AI Office", "EUR-Lex AI Act", "CEN/CENELEC", "DSA Transparency DB"),
            keyAuthorities = listOf("EU AI Office", "European Commission", "CEN/CENELEC", "National DSCs"),
        ),
        "gdpr_ccpa_privacy" to NichePromptEnrichment(
            promptContext = "Focus on: EDPB guidelines publication dates, DPA enforcement with case references, " +
                "adequacy decision renewals (EU-US DPF review), CCPA/CPRA enforcement (California AG, CPPA), " +
                "state privacy law effective dates, cookie consent campaigns, " +
                "children's privacy (COPPA 2.0, UK AADC), breach notification changes, SCCs review dates. " +
                "Cite specific DPA and case/decision reference numbers where possible.",
            officialSources = listOf("EDPB", "CNIL", "ICO", "CPPA", "EUR-Lex"),
            keyAuthorities = listOf("EDPB", "CNIL", "BfDI", "AEPD", "ICO", "CPPA", "FTC"),
        ),
        "cybersecurity_nis2" to NichePromptEnrichment(
            promptContext = "Focus on: NIS2 transposition per EU member state, Cyber Resilience Act phases " +
                "(separate software products from IoT device requirements — different timelines), " +
                "ENISA guidance, incident reporting timelines, ISO 27001/IEC 62443 updates, " +
                "NIST CSF updates, CISA directives, UK NIS Regulations, supply chain security, " +
                "critical infrastructure designation changes, penalty frameworks.",
            officialSources = listOf("ENISA", "NIST", "CISA", "BSI Germany"),
            keyAuthorities = listOf("ENISA", "NIST", "CISA", "BSI", "NCSC UK", "ANSSI"),
        ),
        "ai_ml_regulated" to NichePromptEnrichment(
            promptContext = "Focus on: AI in healthcare (FDA AI/ML Action Plan, Health Canada), " +
                "AI in financial services (SR 11-7, FCA/PRA guidance), algorithmic trading (MiFID II RTS), " +
                "AI bias audits (NYC LL 144, EU AI Act high-risk), explainability requirements, " +
                "AI safety institutes (UK AISI, US AISI), autonomous vehicle AI (UN ECE, NHTSA), " +
                "AI in employment (EEOC, EU Platform Workers Directive).",
            officialSources = listOf("FDA AI/ML", "FCA AI", "NIST AI RMF", "UK AISI"),
            keyAuthorities = listOf("FDA", "FCA", "PRA", "NIST", "UK AISI", "EEOC"),
        ),
        "digital_markets_act" to NichePromptEnrichment(
            promptContext = "Focus on: Gatekeeper designation decisions, compliance report deadlines, " +
                "interoperability implementation dates, EC non-compliance investigations, penalty decisions, " +
                "new service designation, app store sideloading, messaging interoperability, " +
                "data portability requirements, self-preferencing prohibition enforcement.",
            officialSources = listOf("European Commission DMA", "EUR-Lex"),
            keyAuthorities = listOf("European Commission DG CNECT", "National Competition Authorities"),
        ),
        "esg_sustainability" to NichePromptEnrichment(
            promptContext = "Focus on: CSRD phased roll-out (company sizes, years), " +
                "ESRS adoption dates, EU Taxonomy Delegated Acts, SFDR periodic reporting, " +
                "CBAM transitional phase milestones, SEC climate disclosure, " +
                "ISSB (IFRS S1/S2) adoption by jurisdiction, Swiss CO sustainability obligations, " +
                "UK SDR and green taxonomy, CSDDD implementation, greenwashing enforcement. " +
                "Include quarterly reporting deadlines where relevant.",
            officialSources = listOf("EFRAG", "EUR-Lex CSRD", "SEC", "ISSB", "FINMA"),
            keyAuthorities = listOf("EFRAG", "European Commission", "SEC", "ISSB", "FINMA", "FCA"),
        ),
        "sanctions_export" to NichePromptEnrichment(
            promptContext = "Focus on: EU sanctions packages (numbered, with deadlines), OFAC SDN list updates, " +
                "BIS Entity List additions, EU Dual-Use Regulation, Wassenaar updates, UK OFSI designations, " +
                "semiconductor export controls (US-China), wind-down period expirations, " +
                "humanitarian exemptions, enforcement actions with penalty amounts.",
            officialSources = listOf("EU Sanctions Map", "OFAC", "BIS", "UK OFSI"),
            keyAuthorities = listOf("European Commission", "OFAC", "BIS", "UK OFSI", "BAFA"),
        ),
        "labor_employment" to NichePromptEnrichment(
            promptContext = "Focus on: EU Platform Workers Directive timelines, pay transparency transposition, " +
                "whistleblower protection status per member state, minimum wage updates by country, " +
                "remote work legislation, US DOL overtime rules, EEOC guidance, UK Employment Rights Bill, " +
                "gig economy classification cases, OHS directive updates.",
            officialSources = listOf("EUR-Lex", "DOL", "EEOC", "ILO"),
            keyAuthorities = listOf("European Commission", "DOL", "EEOC", "HMRC", "ILO"),
        ),
        "automotive_unece" to NichePromptEnrichment(
            promptContext = "Focus on: Euro 7 implementation dates (by vehicle category), UN ECE WP.29 automated driving, " +
                "EU Battery Regulation phases, EV charging infrastructure deadlines, type-approval changes, " +
                "cybersecurity UN R155 / software update UN R156, NHTSA rulemaking, China NEV standards, " +
                "end-of-life vehicle directive, fleet CO2 targets.",
            officialSources = listOf("UN ECE WP.29", "EUR-Lex", "NHTSA", "MIIT China"),
            keyAuthorities = listOf("UN ECE", "European Commission", "NHTSA", "KBA", "MIIT"),
        ),
        "food_safety_haccp" to NichePromptEnrichment(
            promptContext = "Focus on: EFSA risk assessments, novel food authorization deadlines, " +
                "food contact material updates, allergen labelling changes, FSMA compliance dates, " +
                "Codex Alimentarius updates, farm-to-fork milestones, food fraud enforcement, " +
                "MRL updates, organic regulation, Nutri-Score mandate status, food supplement changes.",
            officialSources = listOf("EFSA", "FDA FSMA", "Codex Alimentarius", "EUR-Lex"),
            keyAuthorities = listOf("EFSA", "FDA", "FSA UK", "BVL Germany", "Codex"),
        ),
        "cosmetics_reach" to NichePromptEnrichment(
            promptContext = "Focus on: REACH registration deadlines, SVHC candidate list updates " +
                "(include specific substance names in event titles where relevant), restriction proposals with comment periods, " +
                "cosmetics ingredient bans (Annexes II-VI), ECHA opinions, animal testing ban enforcement, " +
                "microplastics restriction timeline, PFAS universal restriction phases, UK REACH divergence.",
            officialSources = listOf("ECHA", "EUR-Lex Cosmetics Reg", "SCCS opinions"),
            keyAuthorities = listOf("ECHA", "European Commission", "HSE UK", "MFDS Korea"),
        ),
        "chemicals_regulation" to NichePromptEnrichment(
            promptContext = "Focus on: CLP ATP adoption dates, GHS revision per jurisdiction, " +
                "REACH authorization sunset dates for specific substances, TSCA risk evaluations, " +
                "POPs Convention new listings, biocidal products updates, RoHS restrictions, " +
                "endocrine disruptor criteria, ECHA enforcement results, OEL updates.",
            officialSources = listOf("ECHA", "EPA TSCA", "EUR-Lex CLP", "Stockholm Convention"),
            keyAuthorities = listOf("ECHA", "EPA", "HSE", "BAuA"),
        ),
        "environmental_regulations" to NichePromptEnrichment(
            promptContext = "Focus on: EU ETS Phase 4 (aviation, maritime inclusion), CBAM transitional reporting, " +
                "IED revision, nature restoration milestones, water framework review, " +
                "EPA Clean Air/Water rulemaking, carbon credit regulation, deforestation regulation, " +
                "waste framework circular economy targets, plastic packaging tax/EPR per country.",
            officialSources = listOf("EUR-Lex", "EPA", "UNFCCC", "EEA"),
            keyAuthorities = listOf("European Commission DG ENV", "EPA", "Environment Agency UK", "UBA"),
        ),
    )

    /** Maps [NicheCatalog] entry to a v2 enrichment id, or null if no thematic match. */
    fun v2IdForNiche(niche: Niche): String? {
        when (niche.promptKey) {
            "Cardiovascular Devices (stents, pacemakers, valves)" -> return "cardiovascular_devices"
            "Orthopedic and Trauma Devices (implants, prosthetics)" -> return "orthopedics_trauma"
            "In Vitro Diagnostic Devices IVDR" -> return "ivd_diagnostics"
            "Software as Medical Device SaMD AI ML" -> return "medical_software_samd"
            "Ophthalmic Devices (lenses, implants)" -> return "ophthalmic_dental"
            "Dental and Maxillofacial Devices" -> return "ophthalmic_dental"
            "AI ML Medical Devices Regulation" -> return "medical_software_samd"
            "Drug-Device Combination Products" -> return "pharma_drug_device"
        }
        when (niche.sectorKey) {
            SectorKeys.MEDICAL_DEVICES -> return "medtech_mdr_ivdr"
            SectorKeys.PHARMACEUTICALS -> return "pharma_drug_device"
            SectorKeys.FOOD_FEED -> return "food_safety_haccp"
            SectorKeys.COSMETICS -> return "cosmetics_reach"
            SectorKeys.CHEMICALS -> return "chemicals_regulation"
            SectorKeys.DIGITAL_PRIVACY -> {
                return when (niche.promptKey) {
                    "dp_nis2_cyber", "dp_iot_security" -> "cybersecurity_nis2"
                    "dp_ai_act" -> "ai_act_digital"
                    else -> "gdpr_ccpa_privacy"
                }
            }
            SectorKeys.ENVIRONMENT -> {
                return if (niche.promptKey == "env_carbon_reporting") "esg_sustainability" else "environmental_regulations"
            }
            SectorKeys.AUTOMOTIVE -> return "automotive_unece"
            SectorKeys.WORKPLACE_SAFETY -> return "labor_employment"
            SectorKeys.OTHER -> {
                val pk = niche.promptKey.lowercase()
                return if (
                    pk.contains("sanction") || pk.contains("export") || pk.contains("dual")
                ) {
                    "sanctions_export"
                } else {
                    null
                }
            }
            SectorKeys.AGRI_BIO -> {
                val pk = niche.promptKey.lowercase()
                return when {
                    pk.contains("ppp") || pk.contains("pesticide") -> "chemicals_regulation"
                    pk.contains("feed") || pk.contains("food") -> "food_safety_haccp"
                    else -> "environmental_regulations"
                }
            }
            else -> return null
        }
    }

    fun promptBlockForProfile(profile: UserProfile): String {
        val keys = profile.niches
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { raw ->
                val pk = NicheCatalog.resolvePromptKey(raw).ifBlank { null } ?: return@mapNotNull null
                NicheCatalog.findByPromptKey(pk) ?: NicheCatalog.findByKeyOrName(raw)
            }
            .mapNotNull { v2IdForNiche(it) }
            .distinct()
        if (keys.isEmpty()) return ""

        val blocks = keys.mapNotNull { id ->
            val e = byV2Id[id] ?: return@mapNotNull null
            buildString {
                append("— Theme: ").append(id.replace('_', ' ')).append('\n')
                append(e.promptContext).append('\n')
                append("Official sources to prefer: ").append(e.officialSources.joinToString(", ")).append('\n')
                append("Key authorities: ").append(e.keyAuthorities.joinToString(", "))
            }
        }
        if (blocks.isEmpty()) return ""

        return buildString {
            append("\nNICHE-SPECIFIC AI INSTRUCTIONS (apply when relevant to the query; stay within MANDATORY SCOPE):\n")
            append(blocks.joinToString("\n\n"))
        }
    }
}
