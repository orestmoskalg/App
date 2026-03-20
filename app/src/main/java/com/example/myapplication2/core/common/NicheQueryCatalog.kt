package com.example.myapplication2.core.common

/**
 * Niche-adaptive queries: medical and **general regulatory** sectors.
 * General buckets adapt to [SectorKeys] + country jurisdiction.
 */
object NicheQueryCatalog {

    data class QueryGroup(
        val category: String,
        val icon: String,
        val queries: List<String>,
    )

    // ── General MDR/IVDR (for all niches) ─────────────────────────────────────
    val general: List<QueryGroup> = listOf(
        QueryGroup("Device classification", "🏷️", listOf(
            "How to classify my device under MDR Annex VIII?",
            "Classification rules 1–22: which apply?",
            "Difference between Class IIa and IIb for active devices",
            "Classification of drug-device combination product",
            "Is reclassification of legacy device under MDR required?",
        )),
        QueryGroup("Technical documentation", "📋", listOf(
            "Full structure of technical documentation MDR Annex II",
            "GSPR checklist for Class III device",
            "Benefit-Risk Analysis methodology MDR",
            "How to compile Design History File per MDR?",
            "Software Lifecycle documentation per IEC 62304",
        )),
        QueryGroup("Clinical evaluation", "🔬", listOf(
            "CER: methodology and structure per MEDDEV 2.7/1 Rev4",
            "Literature search strategy for clinical evaluation",
            "Equivalence claim: MDR Article 61(4) requirements",
            "PMCF Plan: mandatory elements for Class IIb",
            "SSCP: what to include for implantable device?",
        )),
        QueryGroup("EUDAMED and UDI", "🗄️", listOf(
            "Manufacturer registration in EUDAMED: step by step",
            "UDI-DI vs UDI-PI: where to use which?",
            "Basic UDI-DI for product family",
            "EUDAMED modules: mandatory dates 2025",
            "UDI marking on device: Annex VI requirements",
        )),
        QueryGroup("Post-Market Surveillance", "📊", listOf(
            "PMS Plan: structure and mandatory elements",
            "PSUR vs SSCP: when to submit which?",
            "Serious Incident reporting: MDR timeframes",
            "Trend reporting: thresholds and methodology",
            "Field Safety Corrective Action (FSCA) procedure",
        )),
        QueryGroup("Notified Bodies", "🏛️", listOf(
            "How to choose a Notified Body for MDR?",
            "QMS audit: what does NB check under Annex IX?",
            "NB audit timelines in 2025: realistic expectations",
            "Unannounced audit: how to prepare?",
            "Changing NB: procedure and documents",
        )),
    )

    // ── General USA FDA (niche suggestions for USA) ──────────────────────────────
    private val generalUsa: List<QueryGroup> = listOf(
        QueryGroup("Device classification", "🏷️", listOf(
            "FDA device classification: Class I, II, III — how to determine?",
            "Product code and panel: how to find my device class?",
            "510(k) vs PMA vs De Novo: which pathway for my device?",
            "Class II exempt: when is 510(k) not required?",
            "Drug-device combination: FDA classification approach",
        )),
        QueryGroup("Technical documentation", "📋", listOf(
            "Design controls 21 CFR 820.30: documentation requirements",
            "DHF and DMR: FDA expectations for medical device",
            "Software documentation: FDA guidance and IEC 62304",
            "Risk management: ISO 14971 and FDA QSR",
        )),
        QueryGroup("Clinical evidence", "🔬", listOf(
            "Clinical data for 510(k): when is it required?",
            "PMA clinical trial: IDE and study design",
            "Real-world evidence for FDA submissions",
            "Substantial equivalence: predicate device selection",
        )),
        QueryGroup("UDI and GUDID", "🗄️", listOf(
            "FDA UDI requirements: GUDID submission steps",
            "UDI format and issuing agency for US market",
            "Direct marking requirements FDA",
        )),
        QueryGroup("Post-Market Surveillance", "📊", listOf(
            "MDR reporting: FDA timelines and MedWatch",
            "Quality System Regulation: post-market requirements",
            "Recalls: Class I, II, III and FDA process",
        )),
        QueryGroup("FDA pathways", "🏛️", listOf(
            "510(k) premarket notification: content and timeline",
            "PMA application: what does FDA expect?",
            "De Novo pathway: when and how to use?",
            "Pre-submission meeting with FDA: best practices",
        )),
    )

    // ── General Ukraine (niche suggestions for Ukraine) ───────────────────────────
    private val generalUkraine: List<QueryGroup> = listOf(
        QueryGroup("Device classification", "🏷️", listOf(
            "Medical device classification in Ukraine: State Expert Centre (SLC) rules",
            "Registration of medical device in Ukraine: steps",
            "Technical regulation alignment with EU MDR",
            "Legacy device re-registration requirements Ukraine",
        )),
        QueryGroup("Technical documentation", "📋", listOf(
            "Technical documentation for Ukrainian registration",
            "Quality management system: Ukraine and ISO 13485",
            "Declaration of conformity and national requirements",
        )),
        QueryGroup("Clinical evaluation", "🔬", listOf(
            "Clinical evaluation for Ukraine registration",
            "CER and PMS data: State Expert Centre (SLC) expectations",
        )),
        QueryGroup("Registration and vigilance", "🗄️", listOf(
            "Manufacturer registration with Ukrainian State Expert Centre (SLC)",
            "Vigilance and incident reporting in Ukraine",
            "Validity of registration and renewal",
        )),
        QueryGroup("Post-Market Surveillance", "📊", listOf(
            "PMS and vigilance requirements Ukraine",
            "Serious incident reporting: Ukrainian procedure",
        )),
        QueryGroup("National and EU alignment", "🏛️", listOf(
            "EU MDR/IVDR and Ukrainian technical regulations",
            "Notified Body certificate and Ukraine registration",
        )),
    )

    /** Medical devices: general groups by country (EU / USA / Ukraine). */
    private fun generalMedicalForCountry(country: String): List<QueryGroup> =
        when (CountryRegulatoryContext.forCountry(country).jurisdictionKey) {
            "usa" -> generalUsa
            "ukraine" -> generalUkraine
            else -> general
        }

    private val generalFoodFeed: List<QueryGroup> = listOf(
        QueryGroup("Food safety & systems", "🍎", listOf(
            "Prerequisite programs vs HACCP: what to document first?",
            "Allergen management and cross-contact: regulatory expectations",
            "Import controls and border rejections: how to prepare",
            "Food defense and food fraud mitigation plans",
        )),
        QueryGroup("Labeling & claims", "🏷️", listOf(
            "Nutrition and health claims: substantiation checklist",
            "Allergen labeling formats for my jurisdiction",
            "Novel food or new ingredient: authorization pathway overview",
        )),
        QueryGroup("Supplements & special categories", "💊", listOf(
            "Food supplements: borderline with medicinal product — criteria",
            "Maximum levels for vitamins/minerals: how to verify",
        )),
    )

    private val generalPharma: List<QueryGroup> = listOf(
        QueryGroup("Authorization & lifecycle", "💊", listOf(
            "Marketing authorization variations: typology and timelines",
            "Orphan designation: when it applies and evidence needs",
            "GMP inspection readiness: critical systems to rehearse",
        )),
        QueryGroup("Quality & PV", "📋", listOf(
            "Pharmacovigilance master file (PSMF) — key updates to track",
            "Root cause analysis for quality deviations: GMP expectations",
            "Serialization and supply-chain integrity: common audit findings",
        )),
    )

    private val generalCosmetics: List<QueryGroup> = listOf(
        QueryGroup("Product compliance", "✨", listOf(
            "Safety assessment and CPSR/PIF structure for my formula",
            "Claims support: cosmetic vs drug borderline",
            "Nanomaterials: notification and documentation",
        )),
    )

    private val generalChemicals: List<QueryGroup> = listOf(
        QueryGroup("Chemical compliance", "⚗️", listOf(
            "REACH registration vs authorization: which applies to my substance?",
            "CLP classification and label elements: workflow",
            "SDS sections: common mistakes under EU vs US rules",
        )),
    )

    private val generalConsumer: List<QueryGroup> = listOf(
        QueryGroup("Product safety", "🛒", listOf(
            "GPSR / product safety: economic operator obligations",
            "Recall simulation: roles and timelines",
            "Standards and presumption of conformity: how to cite",
        )),
    )

    private val generalDigital: List<QueryGroup> = listOf(
        QueryGroup("Data & AI", "🔐", listOf(
            "DPIA: when mandatory and how to document",
            "AI Act risk class for my system: first-pass assessment",
            "International transfers: SCCs and transfer impact assessment",
        )),
    )

    private val generalEnv: List<QueryGroup> = listOf(
        QueryGroup("Environment", "🌍", listOf(
            "EPR / packaging: registration and reporting obligations",
            "Waste codes and shipment documentation: compliance checklist",
        )),
    )

    private val generalGeneric: List<QueryGroup> = listOf(
        QueryGroup("Regulatory navigation", "📚", listOf(
            "Map competent authorities for my sector in this jurisdiction",
            "Identify binding vs non-binding instruments for my product",
            "Prepare a gap analysis against new guidance or decree",
            "Market surveillance risk: how authorities prioritize inspections",
        )),
    )

    /** General query groups for sector × country (non-medical sectors use sector buckets). */
    private fun generalForSectorAndCountry(sectorKey: String, country: String): List<QueryGroup> {
        val sk = sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }
        if (sk == SectorKeys.MEDICAL_DEVICES) return generalMedicalForCountry(country)
        return when (sk) {
            SectorKeys.FOOD_FEED -> generalFoodFeed
            SectorKeys.PHARMACEUTICALS -> generalPharma
            SectorKeys.COSMETICS -> generalCosmetics
            SectorKeys.CHEMICALS -> generalChemicals
            SectorKeys.CONSUMER_PRODUCTS -> generalConsumer
            SectorKeys.DIGITAL_PRIVACY -> generalDigital
            SectorKeys.ENVIRONMENT -> generalEnv
            else -> generalGeneric
        }
    }

    // ── Niche-specific queries ──────────────────────────────────────────────────
    private val nicheSpecific: Map<String, List<QueryGroup>> = mapOf(

        "Cardiovascular Devices (stents, pacemakers, valves)" to listOf(
            QueryGroup("Cardio: Class and documentation", "🫀", listOf(
                "Stent classification: coronary vs peripheral MDR rules",
                "Clinical evaluation for cardiovascular implants Class III",
                "ISO 5840 heart valves: how to apply under MDR?",
                "Haemocompatibility testing: ISO 10993-4 for stents",
                "PMCF registry for cardiac devices: best practices",
            )),
            QueryGroup("Cardio: Deadlines and risks", "⚡", listOf(
                "Legacy cardiovascular devices: MDR deadline 2025-2027",
                "Is clinical investigation required for new stent?",
                "EUDAMED registration for cardiac device manufacturer",
            )),
        ),

        "Orthopedic and Trauma Devices (implants, prosthetics)" to listOf(
            QueryGroup("Orthopedics: Classification and documents", "🦴", listOf(
                "Orthopedic implant classification: which Annex VIII rules?",
                "ISO 14801 fatigue tests for bone screws: MDR requirements",
                "CER for knee/hip endoprosthesis",
                "PMCF arthroplasty registries: recognised EC registries",
                "Osseointegration testing: standards and documentation",
            )),
            QueryGroup("Orthopedics: Transition period", "🦴", listOf(
                "Legacy orthopedic devices Class III: MDR deadline?",
                "Custom-made orthopedic devices: Annex XIII requirements",
            )),
        ),

        "In Vitro Diagnostic Devices IVDR" to listOf(
            QueryGroup("IVDR: Classification and timelines", "🧪", listOf(
                "IVD device classification under IVDR Annex VIII: Rules 1-7",
                "IVDR phase-in: timelines for classes A, B, C, D",
                "Performance Evaluation Report (PEE/PER): IVDR structure",
                "Scientific Validity vs Analytical Performance vs Clinical Performance",
                "IVDR Companion Diagnostics: specific requirements",
            )),
            QueryGroup("IVDR: Technical documentation", "🧪", listOf(
                "IVDR Annex II vs MDR Annex II technical documentation: difference",
                "Common Specifications for Class D IVD: what to apply?",
                "EUDAMED registration for IVD devices: module and timelines",
                "Reference Laboratories for Class D IVD: where to find?",
            )),
        ),

        "Software as Medical Device SaMD AI ML" to listOf(
            QueryGroup("SaMD: Classification and lifecycle", "💻", listOf(
                "SaMD classification: IMDRF + MDR Annex VIII Rule 11",
                "IEC 62304 Software Lifecycle: mandatory MDR artifacts",
                "SaMD cybersecurity: MDCG 2019-16 Rev4 requirements 2024",
                "Software Bill of Materials (SBOM): how to implement?",
                "Continuous Delivery and MDR: how to validate software changes?",
            )),
            QueryGroup("AI/ML in medical devices", "🤖", listOf(
                "Algorithmic drift (model drift): what does MDR require?",
                "AI/ML SaMD: TPLC approach and post-market monitoring",
                "Transparency and explainability for AI Medical Device",
                "Training data requirements for AI SaMD: what does NB check?",
                "De Novo vs 510k equivalent for AI SaMD in EU?",
            )),
        ),

        "Active Implantable Medical Devices AIMD" to listOf(
            QueryGroup("AIMD: Directives and requirements", "⚡", listOf(
                "AIMD transition from AIMD Directive to MDR: key changes",
                "Pacemakers: clinical evaluation and ISO 14708",
                "Electromagnetic compatibility for AIMD: IEC 60601-1-2",
                "Cybersecurity for implantable devices with wireless",
                "Battery longevity claims: how to substantiate for AIMD?",
            )),
        ),

        "Ophthalmic Devices (lenses, implants)" to listOf(
            QueryGroup("Ophthalmology: Classification and documents", "👁️", listOf(
                "IOL (intraocular lens) classification: Class III MDR",
                "ISO 11979 for IOL: which parts are mandatory?",
                "Contact lenses: Class IIb or III MDR? Classification rules",
                "Clinical investigation for ophthalmic implants",
                "Ophthalmic drug-device combinations: separate regulatory path",
            )),
        ),

        "Dental and Maxillofacial Devices" to listOf(
            QueryGroup("Dental: MDR requirements", "🦷", listOf(
                "Dental implants Class IIb: MDR documentation",
                "Custom-made dental prosthetics: Annex XIII requirements",
                "ISO 22674 metallic materials: application under MDR",
                "Digital dental workflow: SaMD classification CAD/CAM",
                "Biocompatibility of dental materials: ISO 10993 series",
            )),
        ),

        "General Surgery Instruments" to listOf(
            QueryGroup("Surgery: Classification and documents", "🔬", listOf(
                "Surgical instruments Class I vs IIa: classification rules",
                "Reusable surgical instruments: reprocessing validation",
                "Sterile Class I devices: MDR QMS requirements",
                "Endoscopic instruments: special MDR requirements?",
                "Single-use vs reusable labelling: what to state?",
            )),
        ),

        "Wound Care and Dressings" to listOf(
            QueryGroup("Wounds and dressings: Classification", "🩹", listOf(
                "Wound dressing classification: active vs passive MDR",
                "Advanced wound care (skin substitutes): Class III?",
                "Antimicrobial dressings with silver/iodine: ancillary action",
                "Wound care combination products: drug-device MDR path",
                "Clinical evidence for wound care: what is sufficient?",
            )),
        ),

        "Diagnostic Imaging Equipment" to listOf(
            QueryGroup("Diagnostics: IEC 60601 and MDR", "📡", listOf(
                "MRI scanner Class IIb: MDR technical documentation",
                "IEC 60601-1 Ed.3.1: mandatory safety tests",
                "Radiation dose management for CT/X-Ray: MDR requirements",
                "DICOM and cybersecurity for imaging equipment",
                "AI-assisted diagnostic imaging: SaMD Rule 11 classification",
            )),
        ),

        "Infusion Pumps and Injection Devices" to listOf(
            QueryGroup("Infusion: Safety and documents", "💉", listOf(
                "Infusion pumps Class IIb: software and alarm requirements",
                "Drug delivery accuracy: ISO 8536 and clinical validation",
                "Cybersecurity for network-connected infusion pumps",
                "Auto-injector Class III: combination product path?",
                "Dose error reduction: IEC 60601-2-24 requirements",
            )),
        ),

        "Home Healthcare Devices" to listOf(
            QueryGroup("Home use: Usability and MDR", "🏠", listOf(
                "Usability Engineering for home-use devices: IEC 62366-1",
                "Home glucose monitors classification: IVDR Class C",
                "Lay person labelling: MDR Annex I section 23 requirements",
                "Remote monitoring: SaMD + hardware combination requirements",
                "Patient safety for non-specialists: human factors study",
            )),
        ),

        "Custom-Made Devices Class III" to listOf(
            QueryGroup("Custom-Made: Annex XIII requirements", "🛠️", listOf(
                "Custom-made devices: definition MDR Article 2(3)",
                "Annex XIII Statement: what do doctor and manufacturer sign?",
                "Custom-made Class III: is NB involvement required?",
                "3D-printed custom implants: MDR regulatory path",
                "Clinical follow-up for custom-made: how to organise?",
            )),
        ),

        "Legacy Devices MDR transition" to listOf(
            QueryGroup("Legacy: Deadlines and strategy", "📋", listOf(
                "MDR transition deadline 2026-2028: full schedule by class",
                "What changed: MDD vs MDR technical documentation gap",
                "Strategy: extend MDD or transition to MDR?",
                "Sell-off period after MDD certificate expiry",
                "NB capacity 2025: will I get MDR cert in time?",
            )),
        ),

        "AI ML Medical Devices Regulation" to listOf(
            QueryGroup("AI Act + MDR: Interaction", "🤖", listOf(
                "EU AI Act and MDR: how do they interact for medical AI?",
                "High-risk AI system under AI Act: what is it for medical software?",
                "MDCG 2021-6 SaMD guidance: key requirements for AI",
                "Post-market performance monitoring for AI/ML SaMD",
                "Foundation models in medical devices: regulatory status",
            )),
        ),

        "Drug-Device Combination Products" to listOf(
            QueryGroup("Combination: Definition and path", "💊", listOf(
                "Drug-device combination: MDR vs ATMP — which path?",
                "Integral vs co-packaged combination: different MDR regulation",
                "Ancillary medicinal substance in device: Annex I sec 14",
                "Consultation procedure with EMA for combination products",
                "Pre-filled syringe: drug or device? Classification rule",
            )),
        ),

        "Patient Monitoring Systems" to listOf(
            QueryGroup("Monitoring: Class and connectivity", "📊", listOf(
                "Wearable patient monitor: Class IIb MDR requirements",
                "Alarm management in patient monitoring: IEC 60601-1-8",
                "Continuous glucose monitor (CGM): IVDR Class C requirements",
                "ECG remote monitoring SaMD: Class IIb or IIa?",
                "Data integrity and cybersecurity for monitoring systems",
            )),
        ),

        "Rehabilitation and Physiotherapy Devices" to listOf(
            QueryGroup("Rehabilitation: Classification and clinical data", "🏃", listOf(
                "Physiotherapy equipment: Class I vs IIa MDR",
                "Exoskeleton rehabilitation device: MDR classification",
                "Clinical evidence for electrical stimulation devices",
                "Robotics rehabilitation: SaMD + hardware classification",
                "Home physiotherapy devices: usability and labelling requirements",
            )),
        ),

        // ── Non-medical niches (promptKey = NicheCatalog.promptKey) ─────────────
        "food_haccp_food_safety" to listOf(
            QueryGroup("Food: HACCP", "✅", listOf(
                "CCP determination: validation vs verification for my process",
                "Allergen CCP vs PRP: when to monitor allergens at a CCP?",
                "HACCP plan review frequency and trigger events",
                "Third-party audit (BRC, FSSC): gap vs regulatory minimum",
            )),
        ),
        "pharma_gmp_gdp_manufacturing" to listOf(
            QueryGroup("Pharma: GMP operations", "🏭", listOf(
                "Batch release: QP responsibilities EU vs national",
                "Cross-site manufacturing: variation and supply chain mapping",
                "Data integrity ALCOA+: audit findings and remediation",
                "Media fill / process simulation: frequency after line changes",
            )),
        ),
        "chem_reach_registration" to listOf(
            QueryGroup("REACH: Registration", "⚗️", listOf(
                "Joint submission vs lead registrant: roles and SIEF",
                "Tonnage band update: when to notify increase",
                "Only Representative (OR): contract and compliance scope",
                "PPORD vs full registration for R&D quantities",
            )),
        ),
        "cosmetic_pif_safety" to listOf(
            QueryGroup("Cosmetics: Safety", "✨", listOf(
                "CPSR structure: what assessor must sign off on",
                "Fragrance allergens labelling: INCI and concentration thresholds",
                "Nanomaterials in cosmetics: notification and PIF evidence",
                "Animal testing ban: alternatives acceptable in EU CPSR",
            )),
        ),
        "dp_gdpr_processing" to listOf(
            QueryGroup("GDPR: Processing", "🔐", listOf(
                "Lawful basis for health data in clinical operations",
                "ROPA content: granularity expected by DPAs",
                "Processor vs controller: subcontractor checklist",
                "DPIA trigger examples for our use case",
            )),
        ),
        "consumer_gpsr_safety" to listOf(
            QueryGroup("Consumer: Safety", "🛡️", listOf(
                "GPSR economic operator obligations for online sales",
                "Presumption of conformity: when harmonised standard is enough",
                "Accident notification: timelines and authority contact",
                "Recall simulation: roles importer vs manufacturer",
            )),
        ),

        "Sterile single-use medical devices and aseptic processing" to listOf(
            QueryGroup("Sterile processing", "🧤", listOf(
                "Terminal sterilization vs aseptic processing: validation scope",
                "SAL and bioburden control for single-use sterile devices",
                "ISO 11135 / 11137: EO vs radiation dossier expectations",
                "Reprocessing IFU: when mandatory under MDR for reusables",
            )),
        ),
        "Medical lasers and radiation-emitting devices" to listOf(
            QueryGroup("Radiation devices", "☢️", listOf(
                "Laser / radiation device classification vs MDR class",
                "IEC 60601-2-22 and clinical evaluation for therapeutic lasers",
                "Dose management and ALARA documentation expectations",
                "National radiation safety licensing vs CE marking",
            )),
        ),

        "pharma_small_molecule_chemical" to listOf(
            QueryGroup("Small molecules", "🧬", listOf(
                "ICH stability and impurity thresholds: update triggers",
                "Starting materials vs intermediates: GMP scope",
                "Polymorph / salt form change: variation pathway",
                "Elemental impurities ICH Q3D: risk assessment template",
            )),
        ),
        "pharma_biologics_vaccines" to listOf(
            QueryGroup("Biologics", "💉", listOf(
                "Viral clearance validation: scale-down model acceptance",
                "Biosafety and adventitious agents: EMA expectations",
                "Cold chain deviations: batch disposition rules",
                "Vaccine antigen changes: clinical bridging study design",
            )),
        ),
        "pharma_atmp_cgt" to listOf(
            QueryGroup("ATMP / CGT", "🧫", listOf(
                "ATMP classification: somatic vs tissue-engineered criteria",
                "Hospital exemption vs MAH pathway: documentation delta",
                "Traceability from donor to patient: audit trail",
                "Long-term follow-up for gene therapies: PMC analogues",
            )),
        ),
        "pharma_biosimilars" to listOf(
            QueryGroup("Biosimilars", "↔️", listOf(
                "Biosimilar extrapolation: indication vs population",
                "Immunogenicity comparability: study design",
                "Reference product switching: national substitution rules",
                "Interchangeable designation (where applicable): evidence bar",
            )),
        ),
        "pharma_generics_hybrid" to listOf(
            QueryGroup("Generics", "📦", listOf(
                "Bioequivalence vs therapeutic equivalence for complex generics",
                "Hybrid applications: when clinical data still needed",
                "BCS biowaiver: documentation checklist",
                "API supplier change: variation class and stability",
            )),
        ),
        "pharma_otc_selfcare" to listOf(
            QueryGroup("OTC", "🏬", listOf(
                "Switch Rx-to-OTC: evidence and labelling hurdles",
                "OTC combination products: borderline with medical device",
                "National classification of OTC vs pharmacy-only",
                "Advertising claims substantiation for self-care",
            )),
        ),
        "pharma_pharmacovigilance" to listOf(
            QueryGroup("Pharmacovigilance", "📣", listOf(
                "PSMF inspection readiness: top findings",
                "Signal detection: disproportionality vs clinical review",
                "RMP update triggers: when mandatory",
                "Literature screening: validated workflow for MAH",
            )),
        ),
        "pharma_clinical_trials" to listOf(
            QueryGroup("Clinical trials", "📋", listOf(
                "CTIS vs legacy national pathways: transition tips",
                "Investigational medicinal product labeling: Annex XIII",
                "Serious breach reporting: timelines and content",
                "Comparator selection: ethical and scientific justification",
            )),
        ),
        "pharma_packaging_serialization" to listOf(
            QueryGroup("Serialization", "🏷️", listOf(
                "EU FMD vs US DSCSA: decommissioning rules",
                "Aggregation levels: when required by NMVS",
                "Artwork change: variation when serialization layout moves",
                "Returns and rework: serial number integrity",
            )),
        ),
        "pharma_radiopharma" to listOf(
            QueryGroup("Radiopharma", "☢️", listOf(
                "Radiopharmaceutical production: GMP Annex 3 highlights",
                "Half-life and batch release timing",
                "Transport index and type A/B packaging",
                "Clinical trial PET tracer: IMPD content",
            )),
        ),
        "pharma_veterinary" to listOf(
            QueryGroup("Veterinary", "🐄", listOf(
                "VMP vs biocide borderline for animal health products",
                "MRL residues: withdrawal periods documentation",
                "Antimicrobial resistance: national stewardship reporting",
                "Cascade use: when allowed for minor species",
            )),
        ),

        "food_labeling_claims" to listOf(
            QueryGroup("Food labeling", "🏷️", listOf(
                "Front-of-pack vs back-of-pack: mandatory elements",
                "Health claims authorization: EFSA-style substantiation",
                "Country-of-origin and QR digital labelling",
                "Allergen precautionary statements: when justified",
            )),
        ),
        "food_novel_food" to listOf(
            QueryGroup("Novel food", "🆕", listOf(
                "Novel food application: history of use vs safety data",
                "Traditional food from third country: notification path",
                "CBD / hemp ingredients: evolving national rules",
                "Novel food catalogue updates: monitoring obligations",
            )),
        ),
        "food_supplements" to listOf(
            QueryGroup("Supplements", "💊", listOf(
                "Maximum permitted levels for vitamins and minerals",
                "Botanicals safety: EFSA BELT and national lists",
                "Borderline with medicinal product: assessment criteria",
                "Third-country import: certificates and lab testing",
            )),
        ),
        "food_infant_medical" to listOf(
            QueryGroup("Infant / medical food", "🍼", listOf(
                "FSMP vs infant formula: compositional rules",
                "Medical food: prescription and reimbursement documentation",
                "Cronobacter controls for PIF: environmental monitoring",
                "Labelling warnings for specialised nutrition",
            )),
        ),
        "feed_pet_food" to listOf(
            QueryGroup("Feed & pet food", "🐕", listOf(
                "Feed additive approval vs premixture registration",
                "Pet food labelling: analytical constituents declaration",
                "BSE/TSE safeguards: chain of custody",
                "Import of feed from third countries: health certificates",
            )),
        ),
        "food_contaminants" to listOf(
            QueryGroup("Contaminants", "⚠️", listOf(
                "Mycotoxin sampling plans: EU regulation alignment",
                "Heavy metals in food: MLs and enforcement focus",
                "Process contaminants (e.g. acrylamide): ALARA measures",
                "RASFF-style escalation: internal decision tree",
            )),
        ),
        "food_import_border" to listOf(
            QueryGroup("Border controls", "🛃", listOf(
                "Imported food of non-animal origin: documentary checks",
                "BIP vs BCP: which border post for my product",
                "ICS2 / advance cargo: data elements for food loads",
                "Detention and re-export: dispute handling",
            )),
        ),
        "food_contact_materials" to listOf(
            QueryGroup("FCM", "🥤", listOf(
                "Plastics Regulation migration limits: testing strategy",
                "Declaration of compliance: downstream obligations",
                "Functional barriers: when required for multilayer",
                "Recycled content in FCM: suitability assessment",
            )),
        ),
        "food_organic_sustainability" to listOf(
            QueryGroup("Organic / sustainability", "🌿", listOf(
                "EU organic logo: conversion periods and controls",
                "Green claims vs substantiation: unfair commercial practices",
                "Carbon footprint on label: ISO 14067 pitfalls",
                "Chain of custody for organic imports",
            )),
        ),

        "cosmetic_rinse_leave_on" to listOf(
            QueryGroup("Rinse / leave-on", "🧴", listOf(
                "Exposure assessment: rinse-off vs leave-on factors",
                "Preservative efficacy challenge testing expectations",
                "Rinse-off hair dyes: allergen and warnings",
                "Microbiological limits: ISO 17516 application",
            )),
        ),
        "cosmetic_cpnp_notification" to listOf(
            QueryGroup("CPNP", "🌐", listOf(
                "Responsible person: legal anchor in EU",
                "CPNP notification timing: before placing on market",
                "PIF number linkage and updates after formula change",
                "Serious undesirable effects: reporting via CPNP",
            )),
        ),
        "cosmetic_claims_advertising" to listOf(
            QueryGroup("Claims", "📢", listOf(
                "Cosmetic vs drug claim: borderline checklist",
                "Substantiation dossier: consumer perception testing",
                "Influencer posts: RP liability for online claims",
                "Before/after imagery: truthful presentation rules",
            )),
        ),
        "cosmetic_nanomaterials" to listOf(
            QueryGroup("Nanomaterials", "🔬", listOf(
                "Nanomaterial definition vs conventional pigments",
                "Notification to Commission: timing and data set",
                "Safety assessor sign-off for nano UV filters",
                "Labelling ‘nano’: INCI list requirements",
            )),
        ),
        "cosmetic_animal_testing" to listOf(
            QueryGroup("Animal testing", "🐰", listOf(
                "EU marketing ban: third-country testing implications",
                "Alternatives OECD TG acceptance for eye/skin",
                "Imported ingredients: supplier declarations",
                "China animal testing updates: supply chain risk",
            )),
        ),
        "cosmetic_fragrance_allergens" to listOf(
            QueryGroup("Fragrance", "🌸", listOf(
                "Annex III allergen labelling: concentration thresholds",
                "IFRA standards vs mandatory EU labelling",
                "Natural complex substances: SDS and allergen mapping",
                "Refill and bulk: consumer information duties",
            )),
        ),
        "cosmetic_natural_organic" to listOf(
            QueryGroup("Natural / organic", "🍃", listOf(
                "COSMOS / NATRUE vs legal minimum for ‘organic’ claims",
                "Organic agriculture ingredients: certification chain",
                "Greenwashing risk: natural origin % claims",
                "Preservation challenges in ‘clean’ formulations",
            )),
        ),

        "chem_clp_classification" to listOf(
            QueryGroup("CLP", "⚠️", listOf(
                "Self-classification vs REACH dossier harmonised class",
                "ATE calculation for mixtures: bridging principles",
                "Label elements: pictogram selection when multiple hazards",
                "UFI and poison centre notification: scope",
            )),
        ),
        "chem_sds_labels" to listOf(
            QueryGroup("SDS / labels", "📄", listOf(
                "Section 2 GHS alignment: EU vs US H-codes",
                "SDS revision triggers: new toxicological data",
                "Small packaging exemptions: fold-out labels",
                "Language requirements for multilingual markets",
            )),
        ),
        "chem_biocides" to listOf(
            QueryGroup("Biocides", "🦠", listOf(
                "BPR product-type authorisation: data requirements",
                "Treated articles: claims allowed vs biocidal product",
                "Union list of active substances: renewal deadlines",
                "In situ generated substances: dossier strategy",
            )),
        ),
        "chem_pesticides" to listOf(
            QueryGroup("PPP", "🌱", listOf(
                "Active substance approval vs product authorisation",
                "Endocrine disruption assessment: EFSA approach",
                "Buffer zones and drift: operator obligations",
                "MRL setting: import tolerance requests",
            )),
        ),
        "chem_industrial_polymers" to listOf(
            QueryGroup("Polymers", "🏭", listOf(
                "REACH polymer registration: monomer two-tonnage rule",
                "Microplastics restriction: product categories affected",
                "Recycled polymers: traceability and contamination",
                "SVHC in articles: SCIP and communication",
            )),
        ),
        "chem_detergents" to listOf(
            QueryGroup("Detergents", "🧽", listOf(
                "Detergents Regulation: biodegradability evidence",
                "Ingredient disclosure via digital label",
                "Biocidal claims in cleaners: BPR overlap",
                "CLP + detergents: combined label layout",
            )),
        ),
        "chem_adr_transport" to listOf(
            QueryGroup("ADR transport", "🚛", listOf(
                "UN number vs proper shipping name selection",
                "Limited quantity and excepted quantity reliefs",
                "Multimodal vs road-only documentation",
                "Tank and bulk: periodic inspection intervals",
            )),
        ),

        "consumer_toys_childcare" to listOf(
            QueryGroup("Toys", "🧸", listOf(
                "Toy Safety Directive: small parts and cords",
                "Chemical requirements in toys: REACH entries",
                "Warnings for functional toys resembling real vehicles",
                "Track & trace for recalls: distributor lists",
            )),
        ),
        "consumer_electrical_lvd" to listOf(
            QueryGroup("Electrical", "🔌", listOf(
                "LVD + RED overlap for connected devices",
                "Harmonised standards presumption: when NB needed",
                "Battery passport vs product passport interplay",
                "Market surveillance: technical file availability",
            )),
        ),
        "consumer_furniture" to listOf(
            QueryGroup("Furniture", "🛋️", listOf(
                "Flammability testing: national vs harmonised approach",
                "Furniture GPSR: stability and entrapment risks",
                "Formaldehyde in wood-based panels: emission classes",
                "Online sales: importer obligations for flat-pack",
            )),
        ),
        "consumer_textiles" to listOf(
            QueryGroup("Textiles", "👕", listOf(
                "REACH restricted substances in textiles (azo dyes, etc.)",
                "Fibre composition labelling: tolerances",
                "Microplastic release from washing: upcoming rules",
                "PPE vs fashion: when PPE Regulation applies",
            )),
        ),
        "consumer_batteries_weee" to listOf(
            QueryGroup("Batteries / WEEE", "🔋", listOf(
                "Battery Regulation: removable vs replaceable design",
                "EPR registration: tonnage reporting cadence",
                "WEEE categories: B2B vs B2C evidence",
                "Collection targets: distributor take-back",
            )),
        ),
        "consumer_recalls_rapex" to listOf(
            QueryGroup("Recalls", "📣", listOf(
                "RAPEX notification: when mandatory vs voluntary",
                "Traceability one-up one-down for consumer goods",
                "Corrective action vs withdrawal: messaging",
                "Insurance and product liability after recall",
            )),
        ),
        "consumer_metrology" to listOf(
            QueryGroup("Metrology", "⚖️", listOf(
                "Pre-packed quantity: e-mark and Tolerable Negative Error",
                "Average quantity system: sampling plan",
                "Price display and unit pricing rules",
                "Measuring instruments in trade: MID/NTEP alignment",
            )),
        ),

        "agri_ppp" to listOf(
            QueryGroup("PPP", "🌾", listOf(
                "Zonal authorisation vs national addenda",
                "Operator certification for professional use",
                "Drift reduction: buffer zones and LERAP",
                "Resistance management: label stewardship",
            )),
        ),
        "agri_seeds" to listOf(
            QueryGroup("Seeds", "🌱", listOf(
                "Variety registration DUS and VCU trials",
                "Seed labelling: category and certification marks",
                "Organic seed availability: derogation limits",
                "Novel traits: coexistence measures",
            )),
        ),
        "agri_gmo_ngt" to listOf(
            QueryGroup("GMO / NGT", "🧬", listOf(
                "EU GMO vs NGT proposal: product scope",
                "Traceability and labelling for GM feed/food",
                "Contained use vs deliberate release authorisations",
                "Third-country approval vs EU import tolerance",
            )),
        ),
        "agri_fertilizers" to listOf(
            QueryGroup("Fertilizers", "🪴", listOf(
                "CE-marked fertilising products: PFC categories",
                "Organic farming: permitted inputs list",
                "Contaminants (cadmium) limits: monitoring plan",
                "Biostimulant claims: efficacy trials",
            )),
        ),
        "agri_feed_chain" to listOf(
            QueryGroup("Feed chain", "🐄", listOf(
                "HACCP for feed mills vs food safety systems",
                "Undesirable substances in feed: monitoring",
                "GMO labelling thresholds for compound feed",
                "Cross-contamination with medicated feed",
            )),
        ),
        "agri_vet_biologics" to listOf(
            QueryGroup("Vet biologics", "💉", listOf(
                "Vaccine batch release: national lab role",
                "Cold chain for veterinary vaccines",
                "Autogenous vaccines: when allowed",
                "Pharmacovigilance for veterinary medicines",
            )),
        ),

        "env_waste_circular" to listOf(
            QueryGroup("Waste / circular", "♻️", listOf(
                "Waste codes: hazardous vs non-hazardous classification",
                "End-of-waste criteria: when material becomes product",
                "Extended producer responsibility: financial guarantees",
                "Shipments of waste: notification vs consent",
            )),
        ),
        "env_packaging_epr" to listOf(
            QueryGroup("Packaging EPR", "📦", listOf(
                "Modulated fees: eco-design criteria",
                "Reporting by material fractions: data quality",
                "Re-use systems vs single-use bans",
                "Import obligations for distance sellers",
            )),
        ),
        "env_industrial_emissions" to listOf(
            QueryGroup("Industrial emissions", "🏭", listOf(
                "IED permit: BAT-AEL vs local ELV",
                "Monitoring plan: CEMS vs periodic stack tests",
                "Accident prevention: Seveso tier thresholds",
                "Odour nuisance: best available techniques",
            )),
        ),
        "env_water" to listOf(
            QueryGroup("Water", "💧", listOf(
                "Urban wastewater directive: emerging substances",
                "Industrial discharge permits: mixing zones",
                "Drinking water safety: risk assessment plans",
                "Groundwater pollution: remediation liability",
            )),
        ),
        "env_seveso" to listOf(
            QueryGroup("Seveso", "⚠️", listOf(
                "COMAH / Seveso: threshold calculations for mixtures",
                "Safety report update triggers",
                "Land-use planning: consultation distances",
                "Inspection preparation: top agency questions",
            )),
        ),
        "env_carbon_reporting" to listOf(
            QueryGroup("Carbon", "🌍", listOf(
                "CBAM: embedded emissions calculation methodology",
                "CSRD vs CBAM data overlap for installations",
                "Product carbon footprint: ISO 14067 for claims",
                "Offset claims: additionality and permanence",
            )),
        ),

        "dp_ai_act" to listOf(
            QueryGroup("AI Act", "🤖", listOf(
                "High-risk AI system Annex III: sector mapping",
                "FRIA vs DPIA: when both needed",
                "Foundation models: transparency obligations",
                "Conformity assessment: internal vs notified body",
            )),
        ),
        "dp_nis2_cyber" to listOf(
            QueryGroup("NIS2 / cyber", "🔒", listOf(
                "Essential vs important entities: sector lists",
                "Supply-chain security: vendor due diligence",
                "Incident reporting: early warning vs notification",
                "Board oversight: training and accountability",
            )),
        ),
        "dp_transfers_scc" to listOf(
            QueryGroup("Transfers", "🌐", listOf(
                "SCC Module 2 vs 3: processor-to-processor chains",
                "Schrems II: TIA template elements",
                "Adequacy decisions: monitoring after Brexit/US changes",
                "BCR approval: multinational rollout",
            )),
        ),
        "dp_cookies_eprivacy" to listOf(
            QueryGroup("Cookies", "🍪", listOf(
                "Consent vs legitimate interest for analytics",
                "Cookie banner UX: valid consent criteria",
                "TCF 2.2 alignment for ad tech",
                "Workplace devices: BYOD monitoring limits",
            )),
        ),
        "dp_gxp_csv" to listOf(
            QueryGroup("GxP CSV", "💻", listOf(
                "GAMP 5 category vs risk-based validation",
                "21 CFR Part 11 vs Annex 11: hybrid cloud",
                "Audit trail review frequency for critical systems",
                "Vendor qualification for SaaS in regulated ops",
            )),
        ),
        "dp_iot_security" to listOf(
            QueryGroup("IoT security", "📡", listOf(
                "Consumer IoT RED delegated act: baseline requirements",
                "SBOM for connected devices: EU CRA expectations",
                "Security update support period: disclosure",
                "Coordinated vulnerability disclosure policy",
            )),
        ),

        "ws_machinery" to listOf(
            QueryGroup("Machinery", "⚙️", listOf(
                "Machinery Regulation vs old MD: timeline",
                "Risk assessment ISO 12100: documentation depth",
                "Guards and interlocks: performance level PLr",
                "Instructions for assembly vs use: language rules",
            )),
        ),
        "ws_ppe" to listOf(
            QueryGroup("PPE", "🦺", listOf(
                "PPE Regulation categories: I II III mapping",
                "EU type-examination vs module B+C2",
                "Workplace risk assessment driving PPE selection",
                "Importers marking PPE from UK/CH: post-Brexit",
            )),
        ),
        "ws_chemical_oel" to listOf(
            QueryGroup("Chemical OEL", "☣️", listOf(
                "Binding OEL vs indicative: compliance hierarchy",
                "Exposure measurement strategy: SEG definition",
                "Substitution hierarchy: REACH vs workplace law",
                "Biological monitoring: consent and limits",
            )),
        ),
        "ws_noise_vibration" to listOf(
            QueryGroup("Noise / vibration", "🔊", listOf(
                "HAVS prevention: EAV and ELV thresholds",
                "Noise action values: hearing protection triggers",
                "Whole-body vibration: seat and suspension controls",
                "Night-time noise limits: community complaints",
            )),
        ),
        "ws_atex" to listOf(
            QueryGroup("ATEX", "💥", listOf(
                "Zone 0/1/2 vs equipment categories",
                "Ignition hazard assessment: dust vs gas",
                "ATEX vs IECEx: mutual recognition limits",
                "Static electricity controls in EX areas",
            )),
        ),

        "cpr_ce_marking" to listOf(
            QueryGroup("CPR / CE", "🏗️", listOf(
                "DoP and CE marking: manufacturer obligations",
                "AVCP system 1+ vs 4: when testing by NB",
                "Harmonised standards: presumption of performance",
                "UKCA vs CE for construction products",
            )),
        ),
        "cpr_fire" to listOf(
            QueryGroup("Fire", "🔥", listOf(
                "Reaction to fire classes: testing vs classification",
                "External wall systems: large-scale test evidence",
                "Penetration seals: EI classification maintenance",
                "Smoke toxicity: additional national requirements",
            )),
        ),
        "cpr_structural" to listOf(
            QueryGroup("Structural", "🧱", listOf(
                "Structural bearings and joints: ETA route",
                "CE for steel vs concrete: product standards",
                "Design assisted by testing: documentation",
                "Third-party inspection: national building codes",
            )),
        ),
        "cpr_insulation_energy" to listOf(
            QueryGroup("Insulation", "🌡️", listOf(
                "Lambda values and U-value claims: evidence",
                "Moisture and mould: hygrothermal modelling",
                "EPBD renovation wave: product performance",
                "Recycled insulation: fire performance reassessment",
            )),
        ),
        "cpr_facades" to listOf(
            QueryGroup("Facades", "🏢", listOf(
                "Cladding subframe: structural and thermal interaction",
                "Wind load and fixing patterns: CWCT",
                "Fire breaks in rainscreen cavities",
                "Maintenance access and anchor corrosion",
            )),
        ),

        "auto_type_approval" to listOf(
            QueryGroup("Type approval", "🚗", listOf(
                "Whole vehicle type approval vs small series",
                "CoP (conformity of production) audits",
                "National plates and registration: type approval link",
                "Retrofit kits: separate approval path",
            )),
        ),
        "auto_emissions" to listOf(
            QueryGroup("Emissions", "💨", listOf(
                "Euro 7 / future limits: test cycle changes",
                "RDE vs WLTP: real-world compliance",
                "Defeat device rules: software update governance",
                "CO2 fleet targets: pooling and eco-innovation",
            )),
        ),
        "auto_adr" to listOf(
            QueryGroup("ADR auto", "🛢️", listOf(
                "Tank vehicles: periodic inspection intervals",
                "Driver ADR training categories vs cargo class",
                "Equipment compatibility: hose and coupling standards",
                "Multimodal tank: TIR / customs seals",
            )),
        ),
        "auto_software_un" to listOf(
            QueryGroup("Auto software", "💻", listOf(
                "UN R156 software update and cybersecurity",
                "OTA updates: safety case and rollback",
                "SOTIF for ADAS: validation metrics",
                "Supplier software: liability chain OEM–tier1",
            )),
        ),
        "auto_l_category" to listOf(
            QueryGroup("L-category", "🏍️", listOf(
                "L-category vs M-category: approval differences",
                "Helmet and PPE integration for PTW",
                "Noise limits: stationary vs pass-by tests",
                "Electric L-vehicles: battery safety UN 38.3",
            )),
        ),
        "auto_adas" to listOf(
            QueryGroup("ADAS", "🛣️", listOf(
                "ALKS UN regulation: ODD definition",
                "Sensor calibration after repair: OEM procedures",
                "Driver monitoring: privacy vs safety trade-offs",
                "Type approval for retrofit ADAS kits",
            )),
        ),

        "tt_tpd" to listOf(
            QueryGroup("TPD", "🚭", listOf(
                "TPD II pack warnings and track & trace",
                "Cross-border distance sales: national bans",
                "Novel tobacco authorisation vs notification",
                "Nicotine concentration limits: compliance testing",
            )),
        ),
        "tt_novel_vaping" to listOf(
            QueryGroup("Vaping", "💨", listOf(
                "TPD notification for e-liquids: emissions testing",
                "Refill containers and leak-free mechanisms",
                "Youth access: online age verification",
                "Battery safety for disposable vapes",
            )),
        ),
        "tt_alcohol_label" to listOf(
            QueryGroup("Alcohol label", "🍷", listOf(
                "EU nutrition declaration for alcoholic beverages",
                "Pregnancy pictogram: national variations",
                "Low/no alcohol claims: thresholds",
                "Geographical indications for spirits",
            )),
        ),
        "tt_excise_trace" to listOf(
            QueryGroup("Excise", "📟", listOf(
                "Excise movement under suspension: EMCS",
                "Tax warehouse authorisation: controls",
                "Track & trace unique identifiers: tobacco vs alcohol",
                "Import excise: deferred payment guarantees",
            )),
        ),

        "other_trade_sanctions" to listOf(
            QueryGroup("Sanctions", "🌐", listOf(
                "EU vs US OFAC: blocking vs sectoral sanctions",
                "Ownership and control screening: 50% rule nuances",
                "Wind-down licences: record keeping",
                "Sanctions clauses in sales contracts",
            )),
        ),
        "other_export_dual_use" to listOf(
            QueryGroup("Dual-use", "🛂", listOf(
                "Dual-use list classification: technology vs product",
                "Broker registration and intra-EU transfers",
                "End-user statements: red flags",
                "Encryption mass-market exception: catch-all review",
            )),
        ),
        "other_anti_bribery" to listOf(
            QueryGroup("Anti-bribery", "⚖️", listOf(
                "Third-party due diligence: risk tiers",
                "Gifts & hospitality: monetary thresholds",
                "Books & records: adequate procedures defence",
                "M&A reps & warranties: corruption warranties",
            )),
        ),
        "other_multi_site_qms" to listOf(
            QueryGroup("Multi-site QMS", "🏢", listOf(
                "Centralised vs site scopes in multi-site certification",
                "Transfer of processes between sites: change control",
                "Remote audits: evidence and limitations",
                "Group quality agreements: roles and KPIs",
            )),
        ),
    )

    /**
     * Returns query groups adapted to the user profile and selected country.
     * General queries (classification, documentation, etc.) match the country's jurisdiction (EU, USA, Ukraine).
     * If niches are set — niche queries + general. If none — general only.
     */
    fun getQueriesForProfile(
        nichePromptKeys: List<String>,
        country: String = "",
        sectorKey: String = SectorCatalog.DEFAULT_KEY,
    ): List<QueryGroup> {
        val generalBySector = generalForSectorAndCountry(sectorKey, country)
        val result = mutableListOf<QueryGroup>()

        // Add niche queries (first 2 niches, up to 3 groups each)
        nichePromptKeys.take(2).forEach { key ->
            nicheSpecific[key]?.take(2)?.let { result.addAll(it) }
        }

        // General for selected country (most used sections)
        result.addAll(generalBySector.take(3))

        // If more niches — add more niche queries
        if (nichePromptKeys.size > 2) {
            nichePromptKeys.drop(2).take(2).forEach { key ->
                nicheSpecific[key]?.take(1)?.let { result.addAll(it) }
            }
        }

        // Add remaining general
        result.addAll(generalBySector.drop(3))

        return result.distinctBy { it.category }
    }

    /**
     * All niche queries for a given niche (for filter display).
     */
    fun getQueriesForNiche(nichePromptKey: String): List<QueryGroup> =
        nicheSpecific[nichePromptKey] ?: emptyList()

    /**
     * Search suggestions (autocomplete) for the input field. Matches country so queries are jurisdiction-relevant.
     */
    fun getAutocompleteSuggestions(
        nichePromptKeys: List<String>,
        country: String = "",
        sectorKey: String = SectorCatalog.DEFAULT_KEY,
    ): List<String> =
        getQueriesForProfile(nichePromptKeys, country, sectorKey).flatMap { it.queries }
}
