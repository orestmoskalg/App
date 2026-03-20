package com.example.myapplication2.presentation.tools

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ══════════════════════════════════════════════════════════════════════════════
//  DATA MODELS
// ══════════════════════════════════════════════════════════════════════════════

data class GlossaryTerm(
    val abbreviation: String,
    val fullName: String,
    val definition: String,
    val reference: String = "",
    val category: GlossaryCategory,
    val seeAlso: List<String> = emptyList(),
    /** Active web links: (label, url) for official resources. Shown as clickable links in the glossary card. */
    val resourceLinks: List<Pair<String, String>> = emptyList(),
)

enum class GlossaryCategory(val label: String, val color: Color) {
    REGULATION("Regulation", AppGreen),
    DOCUMENT("Document", AppGreen),
    BODY("Body / Registry", AppGreen),
    PROCESS("Process", AppGreen),
    TECHNICAL("Technical", AppGreen),
    CLINICAL("Clinical", AppError),
}

private const val EURLEX_MDR = "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745"
private const val EURLEX_IVDR = "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0746"
private const val EC_MDCG = "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en"
private const val EC_EUDAMED = "https://ec.europa.eu/tools/eudamed"
private const val EC_NANDO = "https://ec.europa.eu/growth/tools-databases/nando/"
private const val FDA_DEVICES = "https://www.fda.gov/medical-devices"
private const val FDA_CFR820 = "https://www.ecfr.gov/current/title-21/chapter-I/subchapter-H/part-820"
private const val GUDID_URL = "https://accessgudid.nlm.nih.gov/"
private const val DLZ_UA = "https://www.dlz.gov.ua/"

val MDR_GLOSSARY: List<GlossaryTerm> = listOf(
    // ── Regulations ──
    GlossaryTerm("MDR", "Medical Device Regulation", "EU Regulation 2017/745 on medical devices. Replaced MDD and AIMDD. Became applicable 26 May 2021. Covers classes I, IIa, IIb, III.", "EU 2017/745", GlossaryCategory.REGULATION, listOf("IVDR","NB","CE Mark"), listOf("EUR-Lex MDR 2017/745" to EURLEX_MDR)),
    GlossaryTerm("IVDR", "In Vitro Diagnostic Regulation", "EU Regulation 2017/746 on IVD diagnostics. Replaced IVDD. Transition period by class: A — 2022, B — 2025, C — 2026, D — 2027.", "EU 2017/746", GlossaryCategory.REGULATION, listOf("MDR","IVD","EUDAMED"), listOf("EUR-Lex IVDR 2017/746" to EURLEX_IVDR)),
    GlossaryTerm("MDD", "Medical Device Directive", "Legacy EU directive 93/42/EEC. Replaced by MDR. MDD certificates valid only under MDR Art.120 transition provisions.", "93/42/EEC", GlossaryCategory.REGULATION),
    GlossaryTerm("AIMDD", "Active Implantable Medical Device Directive", "Legacy directive 90/385/EEC for active implantable devices (pacemakers, etc.). Replaced by MDR.", "90/385/EEC", GlossaryCategory.REGULATION),
    GlossaryTerm("IVDD", "In Vitro Diagnostic Device Directive", "Legacy directive 98/79/EC for IVDs. Replaced by IVDR.", "98/79/EC", GlossaryCategory.REGULATION),
    GlossaryTerm("EU AI Act", "EU Artificial Intelligence Act", "EU Regulation 2024/1689 on artificial intelligence. AI-based SaMD may fall under 'high-risk AI' alongside MDR.", "EU 2024/1689", GlossaryCategory.REGULATION, listOf("SaMD","PCCP")),
    GlossaryTerm("CE Mark", "Conformité Européenne", "EU conformity marking. Device may be placed on the EU market after successful conformity assessment under MDR or IVDR.", "MDR Art. 20", GlossaryCategory.REGULATION),
    GlossaryTerm("REACH", "Registration, Evaluation, Authorisation of Chemicals", "EU regulation on chemicals. Relevant for medical devices containing hazardous substances (Annex I §10.4 MDR).", "EC 1907/2006", GlossaryCategory.REGULATION),

    // ── Documents ──
    GlossaryTerm("CER", "Clinical Evaluation Report", "Clinical evaluation report. Documents clinical evidence of safety and performance. Mandatory for all classes under MDR Art.61 and MEDDEV 2.7/1 Rev.4.", "MDR Art.61, Annex XIV", GlossaryCategory.DOCUMENT, listOf("PMCF","SSCP","MEDDEV")),
    GlossaryTerm("TD", "Technical Documentation", "Technical documentation (formerly Technical File). Contains device description, GSPR, design, manufacture, risks, CER, PMS. Annex II+III MDR.", "MDR Annex II & III", GlossaryCategory.DOCUMENT, listOf("GSPR","CER","PMS Plan")),
    GlossaryTerm("DoC", "Declaration of Conformity", "Declaration of conformity. Legally required document where the manufacturer confirms compliance with MDR/IVDR. Annex IV.", "MDR Art.19, Annex IV", GlossaryCategory.DOCUMENT),
    GlossaryTerm("IFU", "Instructions for Use", "Instructions for use. Mandatory for most devices. May be electronic (eIFU) under certain conditions. Annex I §23 MDR.", "MDR Annex I §23", GlossaryCategory.DOCUMENT),
    GlossaryTerm("SSCP", "Summary of Safety and Clinical Performance", "Summary of safety and clinical performance. Public document for Class III and implantable Class IIb. Uploaded to EUDAMED.", "MDR Art.32, MDCG 2019-9", GlossaryCategory.DOCUMENT, listOf("CER","EUDAMED")),
    GlossaryTerm("PSUR", "Periodic Safety Update Report", "Periodic safety update report. Mandatory for Class IIa (every 2 years), IIb and III (annually). Structure per MDCG 2020-7.", "MDR Art.86, MDCG 2020-7", GlossaryCategory.DOCUMENT, listOf("PMS","PMCF")),
    GlossaryTerm("PMS Plan", "Post-Market Surveillance Plan", "Mandatory PMS plan. Describes methods for collecting and analysing post-market data. Part of TD (Annex III).", "MDR Art.84, Annex III", GlossaryCategory.DOCUMENT, listOf("PMS","PSUR","PMCF")),
    GlossaryTerm("MEDDEV", "Medical Device Guidance (Old)", "Legacy EC guidance documents. MEDDEV 2.7/1 Rev.4 (CER) and MEDDEV 2.12 (Vigilance) still relevant.", "EC", GlossaryCategory.DOCUMENT),
    GlossaryTerm("FSCA Notice", "Field Safety Corrective Action Notice", "Official notice to end users when initiating FSCA (recall/correction).", "MDR Art.89", GlossaryCategory.DOCUMENT),
    GlossaryTerm("PER", "Performance Evaluation Report", "IVD equivalent of CER. Performance evaluation report for IVD devices under IVDR Annex XIII.", "IVDR Annex XIII", GlossaryCategory.DOCUMENT, listOf("CER","IVDR")),
    GlossaryTerm("RMP", "Risk Management Plan / File", "Risk management document per ISO 14971. Includes Plan, Report and Summary. Mandatory for all classes.", "ISO 14971", GlossaryCategory.DOCUMENT),

    // ── Bodies / Registries ──
    GlossaryTerm("NB", "Notified Body", "Organisation designated by an EU member state CA to perform conformity assessment. Required for Class IIa+. See NANDO database.", "MDR Art.36-50", GlossaryCategory.BODY, listOf("NANDO","CA","EUDAMED"), listOf("NANDO — NB register" to EC_NANDO)),
    GlossaryTerm("CA", "Competent Authority", "National regulator of an EU member state for medical devices. In non-EU countries, the equivalent national authority.", "MDR Art.101", GlossaryCategory.BODY),
    GlossaryTerm("EUDAMED", "European Database on Medical Devices", "EU-wide database for medical devices. Six modules: actors, UDI, NB/certificates, clinical investigations, PMS, vigilance.", "MDR Art.33", GlossaryCategory.BODY, listOf("UDI","NB","Vigilance"), listOf("EUDAMED portal" to EC_EUDAMED)),
    GlossaryTerm("MDCG", "Medical Device Coordination Group", "EU coordination group for medical devices. Issues guidance documents for MDR/IVDR.", "MDR Art.105", GlossaryCategory.BODY, emptyList(), listOf("MDCG guidance documents" to EC_MDCG)),
    GlossaryTerm("NANDO", "New Approach Notified and Designated Organisations", "Official register of all EU NBs under all directives and regulations. Check NB status here.", "EC", GlossaryCategory.BODY, emptyList(), listOf("NANDO database" to EC_NANDO)),
    GlossaryTerm("EMA", "European Medicines Agency", "EU agency for medicinal products. Advisory role for combination products and some SaMD.", "EC", GlossaryCategory.BODY),
    GlossaryTerm("ENISA", "EU Agency for Cybersecurity", "EU agency for cybersecurity. Issues guidance on medical device cybersecurity (with MDCG).", "EC", GlossaryCategory.BODY),
    GlossaryTerm("IMDRF", "International Medical Device Regulators Forum", "International forum of medical device regulators. FDA, EMA, Health Canada, TGA, etc. Develops global standards.", "IMDRF", GlossaryCategory.BODY),
    GlossaryTerm("ERN", "European Reference Network", "EU network for rare and complex diseases. Mandatory consultation for certain Class III devices.", "MDR Art.61(2)", GlossaryCategory.BODY),

    // ── Processes ──
    GlossaryTerm("QMS", "Quality Management System", "Quality management system. ISO 13485 is the standard for medical devices. Prerequisite for NB certification.", "ISO 13485", GlossaryCategory.PROCESS, listOf("CAPA","ISO 13485")),
    GlossaryTerm("PMS", "Post-Market Surveillance", "Proactive system for collecting and analysing post-market data. Covers complaints, vigilance, literature, social media.", "MDR Art.83-86", GlossaryCategory.PROCESS, listOf("PSUR","PMCF","Vigilance","FSCA")),
    GlossaryTerm("Vigilance", "Vigilance Reporting System", "Surveillance system: reporting serious incidents (24h–48h) and FSCA to CA. New terms and procedures in MDR Art.87-92.", "MDR Art.87-92", GlossaryCategory.PROCESS, listOf("FSCA","PMS","EUDAMED")),
    GlossaryTerm("FSCA", "Field Safety Corrective Action", "Corrective action: recall, modification, use restriction. Initiated by manufacturer when a risk is identified.", "MDR Art.89", GlossaryCategory.PROCESS, listOf("Vigilance","CA")),
    GlossaryTerm("PMCF", "Post-Market Clinical Follow-up", "Post-market clinical follow-up. Mandatory for Class IIa+. Methods: registries, PMCFS, literature, SSCP.", "MDR Annex XIV Part B", GlossaryCategory.PROCESS, listOf("PMS","CER","PSUR")),
    GlossaryTerm("CAPA", "Corrective and Preventive Action", "Corrective and preventive actions. Mandatory QMS element. CAPA is the response when receiving NC from NB.", "ISO 13485 §8.5", GlossaryCategory.PROCESS),
    GlossaryTerm("TPLC", "Total Product Lifecycle", "Regulatory approach across the full product lifecycle from concept to disposal. Especially important for SaMD/AI.", "IMDRF/MDR", GlossaryCategory.PROCESS, listOf("SaMD","PCCP")),
    GlossaryTerm("PCCP", "Predetermined Change Control Plan", "Plan for controlling predetermined changes. Allows algorithmic updates to AI/ML SaMD without re-certification.", "IMDRF/FDA", GlossaryCategory.PROCESS, listOf("SaMD","TPLC","EU AI Act")),

    // ── Technical ──
    GlossaryTerm("UDI", "Unique Device Identification", "Unique identification: UDI-DI (device type) + UDI-PI (serial/lot). AIDC code on packaging. Registration in EUDAMED.", "MDR Art.27, Annex VI", GlossaryCategory.TECHNICAL, listOf("EUDAMED","Basic UDI-DI")),
    GlossaryTerm("Basic UDI-DI", "Basic UDI Device Identifier", "Primary identifier for a device family or single device. Key for EUDAMED registration and reporting.", "MDR Art.27", GlossaryCategory.TECHNICAL, listOf("UDI")),
    GlossaryTerm("GSPR", "General Safety and Performance Requirements", "General safety and performance requirements (Annex I MDR). Main cross-reference table in TD demonstrates compliance.", "MDR Annex I", GlossaryCategory.TECHNICAL, listOf("TD","Harmonised Standards")),
    GlossaryTerm("SaMD", "Software as a Medical Device", "Software as a medical device. Classified under MDR Annex VIII Rule 11. IMDRF and MDCG 2019-11 are key guidance.", "MDR Rule 11, MDCG 2019-11", GlossaryCategory.TECHNICAL, listOf("IEC 62304","PCCP","EU AI Act")),
    GlossaryTerm("SBOM", "Software Bill of Materials", "List of all software components. Mandatory for SaMD per MDCG 2019-16 Rev.4 (GSPR §17). Formats: SPDX, CycloneDX.", "MDCG 2019-16 Rev.4", GlossaryCategory.TECHNICAL, listOf("SaMD","IEC 62304")),
    GlossaryTerm("ISO 13485", "QMS for Medical Devices", "QMS standard for medical devices. Based on ISO 9001 with MedDev-specific additions. Almost always required by NB.", "ISO 13485:2016", GlossaryCategory.TECHNICAL),
    GlossaryTerm("ISO 14971", "Risk Management Standard", "Risk management standard for medical devices. Mandatory via GSPR. Latest: ISO 14971:2019.", "ISO 14971:2019", GlossaryCategory.TECHNICAL),
    GlossaryTerm("IEC 62304", "Medical Device Software Lifecycle", "Software lifecycle standard for medical device software. SOUP management, unit testing, class A/B/C. Mandatory for SaMD.", "IEC 62304:2006+A1:2015", GlossaryCategory.TECHNICAL, listOf("SaMD","SBOM")),
    GlossaryTerm("IEC 62366", "Usability Engineering", "Usability standard for medical devices. Summative and formative evaluation. Mandatory via GSPR Annex I.", "IEC 62366-1:2015", GlossaryCategory.TECHNICAL),
    GlossaryTerm("Harmonised Standards", "EU Harmonised Standards", "Standards published in the EU OJ for MDR/IVDR. Application gives presumption of conformity for relevant GSPR.", "MDR Art.8", GlossaryCategory.TECHNICAL, listOf("GSPR","Common Specifications")),
    GlossaryTerm("Common Specifications", "Common Specifications (CS)", "EC technical specifications where Harmonised Standards are insufficient. Especially important for IVDR.", "MDR Art.9, IVDR Art.9", GlossaryCategory.TECHNICAL, listOf("Harmonised Standards")),
    GlossaryTerm("SOUP", "Software Of Unknown Provenance", "Software of unknown provenance — libraries, third-party components in SaMD. SOUP management is required by IEC 62304.", "IEC 62304", GlossaryCategory.TECHNICAL, listOf("SBOM","IEC 62304")),

    // ── Clinical ──
    GlossaryTerm("ClinEv", "Clinical Evidence", "Clinical evidence — body of clinical data and their evaluation. Basis for CER and demonstration of GSPR compliance.", "MDR Art.2(51)", GlossaryCategory.CLINICAL),
    GlossaryTerm("Equivalence", "Equivalent Device Assessment", "Equivalent device assessment. Allows using clinical data from another device if technical, biological and clinical equivalence are demonstrated.", "MDR Annex XIV", GlossaryCategory.CLINICAL, listOf("CER","MEDDEV")),
    GlossaryTerm("PMCFS", "Post-Market Clinical Follow-up Study", "Post-market clinical follow-up study. Conducted to gather additional clinical data. Requires ISO 14155 and GDPR compliance.", "MDR Annex XIV Part B", GlossaryCategory.CLINICAL, listOf("PMCF","ISO 14155")),
    GlossaryTerm("ISO 14155", "Clinical Investigation Standard", "Standard for clinical investigations of medical devices in humans. GCP equivalent for MedDev.", "ISO 14155:2020", GlossaryCategory.CLINICAL),
    GlossaryTerm("IVD", "In Vitro Diagnostic Device", "Device for diagnostics outside the body: blood, urine, tissue analysis, etc. Regulated by IVDR 2017/746.", "IVDR Art.2", GlossaryCategory.CLINICAL, listOf("IVDR","PER")),
    GlossaryTerm("Benefit-Risk", "Benefit-Risk Analysis", "Benefit-risk analysis. Mandatory part of TD and CER. Basis for compliance decision.", "MDR Annex I §1", GlossaryCategory.CLINICAL),
)

// ── FDA (USA) Glossary ──────────────────────────────────────────────────────
private val FDA_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("510(k)", "Premarket Notification", "FDA pathway for Class II devices. Demonstrates substantial equivalence to a predicate device. No clinical trial required in many cases.", "21 CFR 807.92", GlossaryCategory.REGULATION, listOf("PMA","De Novo","Predicate"), listOf("FDA Medical Devices" to FDA_DEVICES)),
    GlossaryTerm("PMA", "Premarket Approval", "FDA pathway for Class III devices. Requires clinical data and full review. Most stringent pathway.", "21 CFR 814", GlossaryCategory.REGULATION, listOf("510(k)","IDE","De Novo"), listOf("FDA Medical Devices" to FDA_DEVICES)),
    GlossaryTerm("De Novo", "De Novo Classification", "Pathway for novel low-to-moderate risk devices with no predicate. Leads to new product classification.", "21 CFR 860", GlossaryCategory.REGULATION, listOf("510(k)","PMA"), listOf("FDA Medical Devices" to FDA_DEVICES)),
    GlossaryTerm("QSR", "Quality System Regulation", "FDA quality system requirements for medical devices. Design controls, CAPA, documentation. 21 CFR Part 820.", "21 CFR 820", GlossaryCategory.REGULATION, listOf("DHF","DMR","ISO 13485"), listOf("21 CFR Part 820" to FDA_CFR820)),
    GlossaryTerm("CDRH", "Center for Devices and Radiological Health", "FDA center that regulates medical devices. Issues guidance, reviews submissions, oversees compliance.", "FDA", GlossaryCategory.BODY, listOf("510(k)","PMA","GUDID"), listOf("FDA CDRH" to FDA_DEVICES)),
    GlossaryTerm("GUDID", "Global Unique Device Identification Database", "FDA database for UDI. Manufacturers must submit device identification data. Public database.", "FDA", GlossaryCategory.BODY, listOf("UDI","510(k)"), listOf("GUDID database" to GUDID_URL)),
    GlossaryTerm("DHF", "Design History File", "Design history file. Contains design and development documentation. Required under 21 CFR 820.30.", "21 CFR 820.30", GlossaryCategory.DOCUMENT, listOf("DMR","QSR")),
    GlossaryTerm("DMR", "Device Master Record", "Device master record. Reference for manufacturing and specifications. 21 CFR 820.181.", "21 CFR 820.181", GlossaryCategory.DOCUMENT, listOf("DHF","QSR")),
    GlossaryTerm("MDR", "Medical Device Reporting", "FDA requirement to report device-related deaths, serious injuries, malfunctions. MedWatch reporting.", "21 CFR 803", GlossaryCategory.PROCESS, listOf("Vigilance","Recall")),
    GlossaryTerm("Pre-Sub", "Pre-Submission Meeting", "Optional meeting with FDA before submission. Discuss strategy, data requirements, and expectations.", "FDA", GlossaryCategory.PROCESS, listOf("510(k)","PMA")),
    GlossaryTerm("IDE", "Investigational Device Exemption", "Exemption to allow clinical investigation of unapproved device. Required before PMA clinical study.", "21 CFR 812", GlossaryCategory.REGULATION, listOf("PMA","Clinical trial")),
    GlossaryTerm("UDI", "Unique Device Identification", "FDA UDI rule. UDI-DI and UDI-PI. Submission to GUDID. Applies to most devices.", "21 CFR 830", GlossaryCategory.TECHNICAL, listOf("GUDID")),
    GlossaryTerm("SaMD", "Software as a Medical Device", "Software as medical device. FDA guidance on clinical evaluation, cybersecurity, and change control. Often 510(k) or De Novo.", "FDA Guidance", GlossaryCategory.TECHNICAL, listOf("PCCP","IEC 62304")),
    GlossaryTerm("ISO 13485", "QMS for Medical Devices", "QMS standard. FDA may accept for QSR compliance. Often required by contract or for international markets.", "ISO 13485:2016", GlossaryCategory.TECHNICAL),
    GlossaryTerm("CAPA", "Corrective and Preventive Action", "Corrective and preventive actions. Required by QSR. Response to nonconformities and quality issues.", "21 CFR 820.100", GlossaryCategory.PROCESS),
)

// ── Ukraine Glossary ────────────────────────────────────────────────────────
private val UKRAINE_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("SLC", "State Expert Centre", "Ukraine's national authority for medicines and medical devices. Registration and oversight of medical devices.", "Ukraine", GlossaryCategory.BODY, listOf("Registration","CE"), listOf("State Expert Centre (official site)" to DLZ_UA)),
    GlossaryTerm("Registration", "Registration", "Medical device registration in Ukraine through the State Expert Centre (SLC). Technical documentation and conformity evidence are required.", "Ukraine", GlossaryCategory.PROCESS, listOf("SLC","CE")),
    GlossaryTerm("SLC (authority)", "State Expert Centre", "National authority for medicines and medical devices in Ukraine (often referred to as SLC or Derzhliksluzhba in local sources).", "Ukraine", GlossaryCategory.BODY),
    GlossaryTerm("CE", "CE Marking", "European conformity marking. CE-marked devices may support alignment with Ukrainian technical regulations for national registration.", "EU", GlossaryCategory.REGULATION, listOf("MDR","Registration")),
    GlossaryTerm("MDR", "Medical Device Regulation", "EU Regulation 2017/745. Ukraine often aligns national registration and documentation expectations with MDR.", "EU 2017/745", GlossaryCategory.REGULATION, listOf("IVDR","CE"), listOf("EUR-Lex MDR 2017/745" to EURLEX_MDR)),
    GlossaryTerm("IVDR", "In Vitro Diagnostic Regulation", "EU Regulation 2017/746 for IVD. Relevant for import and IVD registration in Ukraine.", "EU 2017/746", GlossaryCategory.REGULATION),
    GlossaryTerm("Technical Documentation", "Technical Documentation", "Documentation for registration: device description, GSPR, risk, clinical data. Often structured similarly to MDR Annex II.", "Ukraine", GlossaryCategory.DOCUMENT, listOf("CER","Registration")),
    GlossaryTerm("CER", "Clinical Evaluation Report", "Clinical evaluation report. Typically required for registration. Methodology MEDDEV 2.7/1 or national requirements.", "MDR Art.61", GlossaryCategory.DOCUMENT),
    GlossaryTerm("Vigilance", "Vigilance", "Serious incident and recall reporting. Reporting to the State Expert Centre (SLC).", "Ukraine", GlossaryCategory.PROCESS, listOf("SLC")),
    GlossaryTerm("UDI", "Unique Device Identification", "Unique device identification. May be required for registration and reporting.", "MDR Art.27", GlossaryCategory.TECHNICAL),
    GlossaryTerm("ISO 13485", "QMS for Medical Devices", "Quality management system. Often required for registration and contracts.", "ISO 13485:2016", GlossaryCategory.TECHNICAL),
    GlossaryTerm("PMS", "Post-Market Surveillance", "Post-market surveillance: data collection and analysis after registration. State Expert Centre (SLC) requirements apply.", "Ukraine", GlossaryCategory.PROCESS),
)

// ── Non-medical sector glossaries (English) ───────────────────────────────────
private val FOOD_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("GFL", "General Food Law", "EU Regulation 178/2002 — general principles of food safety, traceability, RASFF, official controls.", "EU 178/2002", GlossaryCategory.REGULATION),
    GlossaryTerm("HACCP", "Hazard Analysis Critical Control Points", "Systematic preventive approach to food safety; prerequisite programs + CCP monitoring.", "Codex / national", GlossaryCategory.PROCESS),
    GlossaryTerm("FSMA", "Food Safety Modernization Act", "US law shifting focus to preventive controls, FSVP for importers, supply-chain rules.", "USA FDA", GlossaryCategory.REGULATION),
    GlossaryTerm("FSVP", "Foreign Supplier Verification Program", "US importer program: verify foreign suppliers produce food to US safety standards.", "21 CFR 1", GlossaryCategory.PROCESS),
    GlossaryTerm("RASFF", "Rapid Alert System Food & Feed", "EU system for notification of serious food/feed risks between member states.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("EFSA", "European Food Safety Authority", "EU agency providing scientific opinions on food/feed risks, novel foods, contaminants.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("Novel food", "Novel Food", "Food or ingredient without significant EU consumption before May 1997; requires authorization.", "EU 2015/2283", GlossaryCategory.REGULATION),
    GlossaryTerm("FIC", "Food Information to Consumers", "EU Regulation 1169/2011 on labelling, allergens, nutrition declaration.", "EU 1169/2011", GlossaryCategory.REGULATION),
    GlossaryTerm("PRP", "Prerequisite Program", "Basic hygiene and operational conditions (GMP for food) before HACCP.", "Codex", GlossaryCategory.PROCESS),
    GlossaryTerm("VACCP / TACCP", "Food Fraud / Food Defence", "Vulnerability assessment (economically motivated adulteration) and threat analysis (intentional contamination).", "Industry", GlossaryCategory.PROCESS),
)

private val PHARMA_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("MA", "Marketing Authorization", "Approval to place a medicinal product on the market (national, MRP, DCP, or centralised).", "EU/ICH", GlossaryCategory.REGULATION),
    GlossaryTerm("GMP", "Good Manufacturing Practice", "EU GMP Guide Part I–IV; manufacturing and quality system for human/veterinary medicines.", "Eudralex Vol 4", GlossaryCategory.PROCESS),
    GlossaryTerm("GDP", "Good Distribution Practice", "Requirements for wholesale distribution of medicinal products (storage, transport, traceability).", "EU 2013/C 343/01", GlossaryCategory.PROCESS),
    GlossaryTerm("PSMF", "Pharmacovigilance System Master File", "EU requirement describing the PV system and EU QPPV location.", "Dir 2001/83", GlossaryCategory.DOCUMENT),
    GlossaryTerm("QPPV", "Qualified Person PV", "EU responsible person for pharmacovigilance for authorised products.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("ASMF", "Active Substance Master File", "DMF-like dossier for API submitted to authority; referenced by MA applicants.", "EMA", GlossaryCategory.DOCUMENT),
    GlossaryTerm("CTD", "Common Technical Document", "ICH format for quality, nonclinical, clinical modules of registration dossiers.", "ICH M4", GlossaryCategory.DOCUMENT),
    GlossaryTerm("ATMP", "Advanced Therapy Medicinal Product", "Gene/cell/tissue engineered products under specific EU framework (Reg 1394/2007).", "EU", GlossaryCategory.REGULATION),
    GlossaryTerm("PQR", "Product Quality Review", "Annual review of quality of authorised medicinal products (EU GMP Chapter 1).", "EU GMP", GlossaryCategory.PROCESS),
    GlossaryTerm("Serialization", "Track & trace / Serialization", "Unique identifiers on packs; EU Delegated Regulation 2016/161 (safety features).", "EU", GlossaryCategory.TECHNICAL),
)

private val CHEMICALS_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("REACH", "Registration, Evaluation, Authorisation of Chemicals", "EU regulation on registration of substances, authorisation of SVHC, restrictions.", "EC 1907/2006", GlossaryCategory.REGULATION),
    GlossaryTerm("CLP", "Classification, Labelling and Packaging", "EU regulation implementing GHS for substance and mixture classification and labelling.", "EC 1272/2008", GlossaryCategory.REGULATION),
    GlossaryTerm("SDS", "Safety Data Sheet", "16-section document for hazardous chemicals per REACH Annex II.", "REACH", GlossaryCategory.DOCUMENT),
    GlossaryTerm("SVHC", "Substance of Very High Concern", "Candidate for authorisation under REACH; listed on ECHA candidate list.", "REACH", GlossaryCategory.TECHNICAL),
    GlossaryTerm("ECHA", "European Chemicals Agency", "Agency managing REACH/CLP registration, evaluation, authorisation.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("BPR", "Biocidal Products Regulation", "EU Regulation 528/2012 on biocidal active substances and products.", "EU 528/2012", GlossaryCategory.REGULATION),
    GlossaryTerm("PCN", "Poison Centre Notification", "EU harmonised poison centre notification for hazardous mixtures (Annex VIII CLP).", "EU", GlossaryCategory.PROCESS),
    GlossaryTerm("UFI", "Unique Formula Identifier", "Code on label linking to poison centre information for hazardous mixtures.", "EU", GlossaryCategory.TECHNICAL),
)

private val COSMETICS_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("CPR", "Cosmetics Regulation", "EU Regulation 1223/2009 — safety, responsible person, PIF, CPNP, claims.", "EU 1223/2009", GlossaryCategory.REGULATION),
    GlossaryTerm("PIF", "Product Information File", "Full dossier including CPSR, formula, manufacturing, evidence of compliance.", "CPR Art. 11", GlossaryCategory.DOCUMENT),
    GlossaryTerm("CPSR", "Cosmetic Product Safety Report", "Safety assessment documented by qualified safety assessor.", "CPR Annex I", GlossaryCategory.DOCUMENT),
    GlossaryTerm("CPNP", "Cosmetic Products Notification Portal", "EU portal for notification before placing cosmetic on market.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("RP", "Responsible Person", "Legal or natural person in EU ensuring compliance for each cosmetic product.", "CPR Art. 4", GlossaryCategory.BODY),
    GlossaryTerm("MoCRA", "Modernization of Cosmetics Regulation Act", "US framework updating FDA cosmetic facility registration, listings, adverse events.", "USA FDA", GlossaryCategory.REGULATION),
)

private val DIGITAL_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("GDPR", "General Data Protection Regulation", "EU 2016/679 — lawful basis, rights, DPIA, DPO, transfers, fines.", "EU 2016/679", GlossaryCategory.REGULATION),
    GlossaryTerm("DPIA", "Data Protection Impact Assessment", "Assessment of high-risk processing; mandatory when criteria in Art. 35 GDPR met.", "GDPR Art. 35", GlossaryCategory.PROCESS),
    GlossaryTerm("DPA", "Data Protection Authority", "Supervisory authority (e.g. ICO, CNIL, UODO) enforcing GDPR nationally.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("SCC", "Standard Contractual Clauses", "EU-approved clauses for international data transfers (2021 versions).", "EU Commission", GlossaryCategory.DOCUMENT),
    GlossaryTerm("NIS2", "Network and Information Security 2", "EU directive strengthening cybersecurity for essential/important entities.", "EU 2022/2555", GlossaryCategory.REGULATION),
    GlossaryTerm("AI Act", "EU Artificial Intelligence Act", "EU 2024/1689 — risk-based rules for AI systems and GPAI.", "EU 2024/1689", GlossaryCategory.REGULATION),
    GlossaryTerm("DPO", "Data Protection Officer", "Mandatory for certain controllers/processors under GDPR Art. 37–39.", "GDPR", GlossaryCategory.BODY),
)

private val CONSUMER_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("GPSR", "General Product Safety Regulation", "EU 2023/988 — economic operators, accident reporting, online marketplaces.", "EU 2023/988", GlossaryCategory.REGULATION),
    GlossaryTerm("RAPEX", "Rapid Exchange of dangerous products", "EU system for dangerous non-food consumer products recalls and alerts.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("Harmonised standard", "Harmonised standard", "Standard cited in EU OJ giving presumption of conformity to GPSR/sector directives when applicable.", "EU", GlossaryCategory.TECHNICAL),
    GlossaryTerm("Economic operator", "Economic operator", "Manufacturer, importer, distributor, or fulfilment service provider with defined obligations.", "EU", GlossaryCategory.PROCESS),
    GlossaryTerm("CPSC", "Consumer Product Safety Commission", "US federal agency for consumer product safety and recalls.", "USA", GlossaryCategory.BODY),
)

private val GENERAL_SECTOR_GLOSSARY: List<GlossaryTerm> = listOf(
    GlossaryTerm("MS", "Market Surveillance", "Authority activities to ensure products comply; GPSR, sector regulations, border checks.", "EU", GlossaryCategory.PROCESS),
    GlossaryTerm("DoC", "Declaration of Conformity", "Document stating product meets applicable requirements (many product regulations).", "Various", GlossaryCategory.DOCUMENT),
    GlossaryTerm("TCF", "Technical Construction File", "Technical evidence for product compliance (name varies by directive/regulation).", "Various", GlossaryCategory.DOCUMENT),
    GlossaryTerm("Notified body", "Notified Body (NB)", "Conformity assessment body for EU modules where third party required.", "EU", GlossaryCategory.BODY),
    GlossaryTerm("CE marking", "CE marking", "Marking indicating conformity with applicable EU harmonisation legislation.", "EU", GlossaryCategory.REGULATION),
    GlossaryTerm("ISO management", "ISO 9001 / ISO standards", "Management system standards often used alongside sector law (not a substitute).", "ISO", GlossaryCategory.TECHNICAL),
)

/** Medical devices: glossary by jurisdiction (EU MDR / FDA / Ukraine). */
private fun getGlossaryForCountry(country: String): List<GlossaryTerm> =
    when (CountryRegulatoryContext.forCountry(country).jurisdictionKey) {
        "usa" -> FDA_GLOSSARY
        "ukraine" -> UKRAINE_GLOSSARY
        else -> MDR_GLOSSARY
    }

/** Glossary for regulatory sector × country (medical uses jurisdiction-specific lists). */
fun getGlossaryForSectorAndCountry(sectorKey: String, country: String): List<GlossaryTerm> {
    val sk = sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }
    return when (sk) {
        SectorKeys.MEDICAL_DEVICES -> getGlossaryForCountry(country)
        SectorKeys.FOOD_FEED -> FOOD_SECTOR_GLOSSARY
        SectorKeys.PHARMACEUTICALS -> PHARMA_SECTOR_GLOSSARY
        SectorKeys.CHEMICALS -> CHEMICALS_SECTOR_GLOSSARY
        SectorKeys.COSMETICS -> COSMETICS_SECTOR_GLOSSARY
        SectorKeys.DIGITAL_PRIVACY -> DIGITAL_SECTOR_GLOSSARY
        SectorKeys.CONSUMER_PRODUCTS -> CONSUMER_SECTOR_GLOSSARY
        SectorKeys.ENVIRONMENT, SectorKeys.AGRI_BIO, SectorKeys.WORKPLACE_SAFETY,
        SectorKeys.CONSTRUCTION, SectorKeys.AUTOMOTIVE, SectorKeys.TOBACCO_ALCOHOL,
        -> GENERAL_SECTOR_GLOSSARY
        SectorKeys.OTHER -> GENERAL_SECTOR_GLOSSARY
        else -> GENERAL_SECTOR_GLOSSARY
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  GLOSSARY SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GlossaryScreen(onBack: () -> Unit, container: AppContainer) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<GlossaryCategory?>(null) }
    var expandedTerm by remember { mutableStateOf<String?>(null) }
    var country by remember { mutableStateOf("") }
    var sectorKey by remember { mutableStateOf(SectorCatalog.DEFAULT_KEY) }

    LaunchedEffect(container) {
        runCatching {
            withContext(Dispatchers.IO) {
                val p = container.userProfileRepository.getUserProfile()
                country = p?.country ?: ""
                sectorKey = p?.sector?.ifBlank { SectorCatalog.DEFAULT_KEY } ?: SectorCatalog.DEFAULT_KEY
            }
        }.getOrElse { }
    }

    val glossary = remember(country, sectorKey) { getGlossaryForSectorAndCountry(sectorKey, country) }
    val ctx = remember(country) { CountryRegulatoryContext.forCountry(country) }
    val sectorLabel = remember(sectorKey) { SectorCatalog.labelOrKey(sectorKey) }

    val filtered = remember(query, selectedCategory, glossary) {
        glossary.filter { term ->
            val q = query.lowercase().trim()
            val matchesQ = q.isBlank() ||
                term.abbreviation.lowercase().contains(q) ||
                term.fullName.lowercase().contains(q) ||
                term.definition.lowercase().contains(q)
            val matchesCat = selectedCategory == null || term.category == selectedCategory
            matchesQ && matchesCat
        }.sortedBy { it.abbreviation }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Regulatory Glossary", fontWeight = FontWeight.Bold)
                        Text("${ctx.jurisdictionName} • $sectorLabel • ${glossary.size} terms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBackIosNew, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { pad ->
        val uriHandler = LocalUriHandler.current
        val focusManager = LocalFocusManager.current
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Official resources for current country (all jurisdictions from Settings)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = PrimaryGreen.copy(alpha = 0.06f),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Official resources — ${ctx.jurisdictionName}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(ctx.quickLinks.size) { index ->
                            val (label, url) = ctx.quickLinks[index]
                            if (url.isNotBlank()) {
                                SuggestionChip(
                                    onClick = { runCatching { uriHandler.openUri(url) } },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                            icon = { Icon(Icons.Filled.OpenInNew, null, Modifier.size(14.dp), tint = PrimaryGreen) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = PrimaryGreen.copy(alpha = 0.12f), labelColor = PrimaryGreen),
                                )
                            }
                        }
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                placeholder = { Text("Search term or abbreviation...") },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = PrimaryGreen) },
                trailingIcon = if (query.isNotBlank()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, null) } }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(14.dp),
            )

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${glossary.size})") },
                        leadingIcon = if (selectedCategory == null) { { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) } } else null,
                    )
                }
                items(GlossaryCategory.values().toList()) { cat ->
                    val count = glossary.count { it.category == cat }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text("${cat.label} ($count)", color = if (selectedCategory == cat) cat.color else MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = if (selectedCategory == cat) { { Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = cat.color) } } else null,
                    )
                }
            }

            // Results count
            if (query.isNotBlank() || selectedCategory != null) {
                Text(
                    "Found: ${filtered.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.SearchOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No term found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Clear search") }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(filtered, key = { it.abbreviation }) { term ->
                        GlossaryTermCard(
                            term = term,
                            expanded = expandedTerm == term.abbreviation,
                            onToggle = { expandedTerm = if (expandedTerm == term.abbreviation) null else term.abbreviation },
                            defaultResourceLink = ctx.quickLinks.firstOrNull()?.takeIf { (_, url) -> url.isNotBlank() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossaryTermCard(
    term: GlossaryTerm,
    expanded: Boolean,
    onToggle: () -> Unit,
    defaultResourceLink: Pair<String, String>? = null,
) {
    val catColor = term.category.color
    val uriHandler = LocalUriHandler.current
    val linksToShow = if (term.resourceLinks.isNotEmpty()) term.resourceLinks else listOfNotNull(defaultResourceLink).filter { (_, url) -> url.isNotBlank() }
    Column(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = catColor.copy(alpha = 0.12f)) {
                        Text(term.abbreviation, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = catColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Text(term.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            },
            supportingContent = if (!expanded) {
                { Text(term.definition, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else null,
            trailingContent = {
                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = catColor.copy(alpha = 0.08f)) {
                        Text(term.category.label, style = MaterialTheme.typography.labelSmall, color = catColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp))
                }
            },
            modifier = Modifier.clickable { onToggle() },
        )

        if (linksToShow.isNotEmpty()) {
            val (label, url) = linksToShow.first()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { runCatching { uriHandler.openUri(url) } },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, Modifier.size(14.dp), tint = PrimaryGreen)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Surface(modifier = Modifier.fillMaxWidth(), color = catColor.copy(alpha = 0.04f)) {
                val uriHandler = LocalUriHandler.current
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(term.definition, style = MaterialTheme.typography.bodyMedium)
                    if (term.reference.isNotBlank()) {
                        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Article, null, Modifier.size(13.dp), tint = catColor)
                            Text("Reference: ${term.reference}", style = MaterialTheme.typography.labelSmall, color = catColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (term.resourceLinks.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.OpenInNew, null, Modifier.size(14.dp), tint = catColor)
                            Text("Resources:", style = MaterialTheme.typography.labelSmall, color = catColor, fontWeight = FontWeight.SemiBold)
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            items(term.resourceLinks.size) { index ->
                                val (label, url) = term.resourceLinks[index]
                                if (url.isNotBlank()) {
                                    SuggestionChip(
                                        onClick = { runCatching { uriHandler.openUri(url) } },
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                        icon = { Icon(Icons.Filled.Link, null, Modifier.size(14.dp), tint = catColor) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = catColor.copy(alpha = 0.12f), labelColor = catColor),
                                    )
                                }
                            }
                        }
                    }
                    if (term.seeAlso.isNotEmpty()) {
                        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Link, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("See also: ${term.seeAlso.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  COMPLIANCE CHECKLIST SCREEN
// ══════════════════════════════════════════════════════════════════════════════

data class ChecklistItem(
    val id: String,
    val text: String,
    val hint: String = "",
    val isCritical: Boolean = false,
)

data class ChecklistSection(
    val title: String,
    val subtitle: String,
    val color: Color,
    val applicableClasses: List<String>,
    val items: List<ChecklistItem>,
)

val COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Basics (all classes)",
        subtitle = "Class I, IIa, IIb, III",
        color = PrimaryGreen,
        applicableClasses = listOf("I", "IIa", "IIb", "III"),
        items = listOf(
            ChecklistItem("b1", "Device class determined per MDR Annex VIII", "Rules 1–22. Verify with NB or RA consultant", true),
            ChecklistItem("b2", "QMS ISO 13485 implemented and certified", "Mandatory for Class IIa+. For Class I — required but NB audit not mandatory", true),
            ChecklistItem("b3", "Technical documentation Annex II and III prepared", "TD must cover full GSPR Annex I with cross-reference", true),
            ChecklistItem("b4", "Declaration of Conformity (DoC) signed", "Annex IV. Signed by authorised person — PRRC or CEO"),
            ChecklistItem("b5", "CE marking applied correctly", "MDR Art.20. Without CE Mark — no EU market access"),
            ChecklistItem("b6", "IFU prepared in accordance with Annex I §23", "Check all mandatory elements: name, manufacturer, intended use, warnings, UDI"),
            ChecklistItem("b7", "UDI-DI and UDI-PI assigned and applied", "GS1 or HIBCC system. AIDC code on packaging", true),
            ChecklistItem("b8", "EUDAMED registration completed", "Actor + device + UDI registration", true),
            ChecklistItem("b9", "Risk analysis per ISO 14971 completed", "Risk Management File: Plan, Report, Summary of Residual Risks"),
            ChecklistItem("b10", "GSPR Annex I reviewed — all applicable requirements closed", "GSPR cross-reference table in TD with evidence of compliance"),
        )
    ),
    ChecklistSection(
        title = "Clinical evaluation",
        subtitle = "All classes (scope depends on class)",
        color = ErrorRed,
        applicableClasses = listOf("I", "IIa", "IIb", "III"),
        items = listOf(
            ChecklistItem("c1", "Clinical Evaluation Report (CER) prepared", "MEDDEV 2.7/1 Rev.4. Literature search + appraisal + conclusions", true),
            ChecklistItem("c2", "Literature search strategy documented", "PubMed, Embase, Cochrane. PICO format. Search date recorded"),
            ChecklistItem("c3", "State of the Art defined and documented", "Comparison with benchmark devices and best alternatives"),
            ChecklistItem("c4", "Equivalence assessment completed (if applicable)", "Technical + biological + clinical equivalence. Agreed with NB"),
            ChecklistItem("c5", "Benefit-Risk Analysis included in CER and TD", "Explicit comparison of benefit and risk. Conclusion positive"),
            ChecklistItem("c6", "PMCF Plan developed (IIa+)", "Clear objectives, methods, timelines, KPIs", true),
            ChecklistItem("c7", "SSCP prepared (Class III and implantable IIb)", "Public document. Uploaded to EUDAMED"),
        )
    ),
    ChecklistSection(
        title = "Notified Body (IIa, IIb, III)",
        subtitle = "Class IIa, IIb, III",
        color = WarningAmber,
        applicableClasses = listOf("IIa", "IIb", "III"),
        items = listOf(
            ChecklistItem("n1", "Notified Body selected and contract signed", "Check NANDO. NB must be designated for your device type", true),
            ChecklistItem("n2", "Pre-Submission Meeting held (recommended)", "Early consultation reduces risk of deficiency reports"),
            ChecklistItem("n3", "Application file submitted to NB", "Full document package + fee. Receipt confirmation obtained"),
            ChecklistItem("n4", "Technical Documentation Review passed", "Responses to all NB requests (deficiency reports) submitted"),
            ChecklistItem("n5", "QMS audit by NB completed (on-site or remote)", "ISO 13485 + MDR Annex IX audit"),
            ChecklistItem("n6", "EU Type Examination (Annex X) completed (IIb/III)", "For Class IIb and III. Prototype or TD review depending on type"),
            ChecklistItem("n7", "Certificate of conformity from NB obtained", "EU certificate. Check validity and scope", true),
            ChecklistItem("n8", "Certificate registered in EUDAMED", "NB uploads certificate. Verify visibility"),
        )
    ),
    ChecklistSection(
        title = "Post-Market Surveillance",
        subtitle = "All classes (PMS mandatory)",
        color = InfoBlue,
        applicableClasses = listOf("I", "IIa", "IIb", "III"),
        items = listOf(
            ChecklistItem("p1", "PMS Plan developed and approved", "MDR Art.84, Annex III. Covers all data sources", true),
            ChecklistItem("p2", "Complaint Management System in place", "SOP for receiving, investigating and reporting complaints"),
            ChecklistItem("p3", "Vigilance SOP developed", "Timeframes: serious incident — 15/30 days; public health threat — 2 days; FSCA — 15 days", true),
            ChecklistItem("p4", "Literature Search Alerts set up", "PubMed/Embase automatic alerts for key terms"),
            ChecklistItem("p5", "PSUR prepared (IIa — every 2 years, IIb/III — annually)", "Structure per MDCG 2020-7", true),
            ChecklistItem("p6", "PMCF activities started (IIa+)", "Patient registry, PMCFS or structured literature review"),
            ChecklistItem("p7", "Post-Market Performance Follow-up (IVD, IVDR)", "IVDR equivalent of PMCF for IVD devices"),
            ChecklistItem("p8", "FSCA procedure tested", "Drill or simulation. Team knows their role"),
        )
    ),
    ChecklistSection(
        title = "SaMD & Cybersecurity",
        subtitle = "Software as a Medical Device",
        color = AppGreen,
        applicableClasses = listOf("SaMD"),
        items = listOf(
            ChecklistItem("s1", "SaMD classified per MDR Rule 11", "MDCG 2019-11. Consider patient state and significance of decision", true),
            ChecklistItem("s2", "Software Lifecycle documented per IEC 62304", "Class A/B/C software lifecycle. SOUP management"),
            ChecklistItem("s3", "SBOM (Software Bill of Materials) generated", "MDCG 2019-16 Rev.4. Format: SPDX or CycloneDX", true),
            ChecklistItem("s4", "CVE vulnerability monitoring in place", "Dependency-Track or similar. Response process"),
            ChecklistItem("s5", "Penetration Testing completed", "External cybersecurity audit. Report in TD"),
            ChecklistItem("s6", "Cybersecurity Management Plan developed", "MDCG 2019-16. Incident response, vulnerability disclosure policy"),
            ChecklistItem("s7", "TD §17 (Cybersecurity) updated", "GSPR Annex I §17 — network security, data protection"),
            ChecklistItem("s8", "Usability Engineering per IEC 62366 completed", "Summative and formative evaluation. Documents in TD"),
            ChecklistItem("s9", "EU AI Act high-risk category checked (if AI/ML)", "EU 2024/1689. Possible dual regulation MDR + AI Act"),
        )
    ),
)

private val FOOD_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Food safety foundation",
        subtitle = "PRPs & management",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("fd1", "Food safety policy and food safety culture documented", "Top management commitment; GFL / FSMA preventive mindset", true),
            ChecklistItem("fd2", "Prerequisite programs (PRPs) established", "Premises, water, cleaning, pest control, supplier approval", true),
            ChecklistItem("fd3", "Allergen management and cross-contact controls", "Segregation, scheduling, labelling verification", true),
            ChecklistItem("fd4", "Traceability one step back / one step forward tested", "Lot recall simulation; mass balance", true),
        )
    ),
    ChecklistSection(
        title = "HACCP",
        subtitle = "Plan",
        color = WarningAmber,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("fd5", "HACCP team and product scope defined", "Flow diagrams verified on site", true),
            ChecklistItem("fd6", "Hazard analysis and CCPs with critical limits", "Validation and monitoring records", true),
            ChecklistItem("fd7", "Corrective actions and verification procedures", "Internal audit of HACCP annually", true),
        )
    ),
    ChecklistSection(
        title = "Import & labelling",
        subtitle = "Market-specific",
        color = InfoBlue,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("fd8", "Food labelling compliant (allergens, language, nutrition)", "EU FIC / FDA requirements", true),
            ChecklistItem("fd9", "Importer / FSVP or equivalent responsibilities assigned", "When third-country sourcing", true),
        )
    ),
)

private val PHARMA_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Quality system",
        subtitle = "GMP / GDP",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("ph1", "GMP certification or inspection history current", "EudraGMP / national / FDA", true),
            ChecklistItem("ph2", "Data integrity and computerised systems validated", "Annex 11 / 21 CFR Part 11 where applicable", true),
            ChecklistItem("ph3", "Change control, deviations, CAPA, and annual product review", "Quality unit oversight", true),
        )
    ),
    ChecklistSection(
        title = "Authorisation & PV",
        subtitle = "Lifecycle",
        color = ErrorRed,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("ph4", "Marketing authorisation variations and renewals tracked", "Sunset clause / renewals", true),
            ChecklistItem("ph5", "Pharmacovigilance system and PSMF in place", "QPPV, literature, ICSRs, PSUR/PBRER", true),
            ChecklistItem("ph6", "Artwork and labelling aligned with SmPC", "Version control and batch release", true),
        )
    ),
)

private val CHEMICALS_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "REACH / CLP",
        subtitle = "Substances & mixtures",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("ch1", "Substance inventory and registration status", "REACH tonnage bands; only representative if non-EU", true),
            ChecklistItem("ch2", "CLP classification and labelling for all mixtures", "SDS and UFI / PCN where required", true),
            ChecklistItem("ch3", "Authorisation / restriction obligations checked", "SVHC in articles; Annex XVII", true),
        )
    ),
)

private val COSMETICS_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Cosmetic product",
        subtitle = "EU / international",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("cs1", "Responsible Person designated (EU)", "PIF available at RP address", true),
            ChecklistItem("cs2", "CPSR and PIF complete before placing on market", "Formula, GMP, evidence, CPNP", true),
            ChecklistItem("cs3", "Claims substantiated and IFU/label compliant", "Annex III–VI ingredients checked", true),
        )
    ),
)

private val DIGITAL_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Privacy & security",
        subtitle = "GDPR / NIS2",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("dg1", "Lawful basis and records of processing (ROPA)", "Art. 6 / Art. 30 GDPR", true),
            ChecklistItem("dg2", "DPIA for high-risk processing", "Art. 35; consult DPA if required", true),
            ChecklistItem("dg3", "Transfer mechanisms for non-EEA transfers", "SCC, TIA, supplementary measures", true),
            ChecklistItem("dg4", "Security measures and incident response", "NIS2 / breach notification timelines", true),
        )
    ),
    ChecklistSection(
        title = "AI governance",
        subtitle = "AI Act",
        color = InfoBlue,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("dg5", "AI system risk classification documented", "High-risk checklist; human oversight", true),
        )
    ),
)

private val CONSUMER_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Product safety",
        subtitle = "GPSR / market surveillance",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("cn1", "Economic operator roles identified (manufacturer/importer)", "EU address and contact", true),
            ChecklistItem("cn2", "Technical documentation and risk assessment", "Standards used; test reports", true),
            ChecklistItem("cn3", "Incident and recall procedure", "RAPEX / national notification", true),
        )
    ),
)

private val GENERAL_SECTOR_COMPLIANCE_CHECKLIST: List<ChecklistSection> = listOf(
    ChecklistSection(
        title = "Compliance basics",
        subtitle = "Cross-sector",
        color = PrimaryGreen,
        applicableClasses = listOf("All"),
        items = listOf(
            ChecklistItem("gn1", "Applicable legal framework mapped for your product", "Directives, regulations, national law", true),
            ChecklistItem("gn2", "Declaration of conformity and technical documentation", "Updated with design changes", true),
            ChecklistItem("gn3", "Internal audit and management review", "Corrective actions closed", true),
            ChecklistItem("gn4", "Supplier qualification and contracts", "Regulatory clauses for critical suppliers", true),
        )
    ),
)

/** Compliance checklist sections for the user’s regulatory sector. */
fun complianceChecklistForSector(sectorKey: String): List<ChecklistSection> =
    when (sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }) {
        SectorKeys.MEDICAL_DEVICES -> COMPLIANCE_CHECKLIST
        SectorKeys.FOOD_FEED -> FOOD_COMPLIANCE_CHECKLIST
        SectorKeys.PHARMACEUTICALS -> PHARMA_COMPLIANCE_CHECKLIST
        SectorKeys.CHEMICALS -> CHEMICALS_COMPLIANCE_CHECKLIST
        SectorKeys.COSMETICS -> COSMETICS_COMPLIANCE_CHECKLIST
        SectorKeys.DIGITAL_PRIVACY -> DIGITAL_COMPLIANCE_CHECKLIST
        SectorKeys.CONSUMER_PRODUCTS -> CONSUMER_COMPLIANCE_CHECKLIST
        else -> GENERAL_SECTOR_COMPLIANCE_CHECKLIST
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun ComplianceChecklistScreen(
    onBack: () -> Unit,
    container: com.example.myapplication2.di.AppContainer? = null,
) {
    var sectorKey by remember { mutableStateOf(SectorCatalog.DEFAULT_KEY) }
    LaunchedEffect(container) {
        runCatching {
            withContext(Dispatchers.IO) {
                sectorKey = container?.userProfileRepository?.getUserProfile()
                    ?.sector?.ifBlank { SectorCatalog.DEFAULT_KEY }
                    ?: SectorCatalog.DEFAULT_KEY
            }
        }
    }

    val allSections = remember(sectorKey) { complianceChecklistForSector(sectorKey) }
    val checkStates = remember(sectorKey) {
        mutableStateMapOf<String, Boolean>().also { map ->
            allSections.forEach { section ->
                section.items.forEach { item -> map[item.id] = false }
            }
        }
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(sectorKey) {
        val saved = container?.appSettingsRepository?.getChecklistStates() ?: emptyMap()
        allSections.forEach { s ->
            s.items.forEach { item ->
                checkStates[item.id] = saved[item.id] ?: false
            }
        }
    }

    // Debounced save — only saves 800ms after last change
    var saveJob by remember { mutableStateOf<Job?>(null) }
    fun onToggle(id: String) {
        checkStates[id] = !(checkStates[id] ?: false)
        saveJob?.cancel()
        saveJob = scope.launch {
            kotlinx.coroutines.delay(800)
            container?.appSettingsRepository?.saveChecklistStates(checkStates.toMap())
        }
    }

    var classFilter by remember { mutableStateOf("All") }
    val classes = listOf("All", "I", "IIa", "IIb", "III", "SaMD")
    val sectorLabel = remember(sectorKey) { SectorCatalog.labelOrKey(sectorKey) }
    val effectiveClassFilter =
        if (sectorKey != SectorKeys.MEDICAL_DEVICES) "All" else classFilter

    val filteredSections = remember(effectiveClassFilter, allSections) {
        if (effectiveClassFilter == "All") allSections
        else allSections.filter { it.applicableClasses.contains(effectiveClassFilter) }
    }

    val total = filteredSections.sumOf { it.items.size }
    val done = filteredSections.sumOf { s -> s.items.count { checkStates[it.id] == true } }
    val progress = if (total > 0) done.toFloat() / total else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("Compliance checklist", fontWeight = FontWeight.Bold)
                    Text("$sectorLabel • $done / $total done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBackIosNew, "Back") } },
                actions = {
                    IconButton(onClick = {
                        checkStates.keys.forEach { checkStates[it] = false }
                        scope.launch {
                            container?.appSettingsRepository?.saveChecklistStates(emptyMap())
                        }
                    }) {
                        Icon(Icons.Filled.RestartAlt, "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Progress bar
            Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Overall progress", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = PrimaryGreen,
                        trackColor = PrimaryGreen.copy(alpha = 0.15f),
                    )
                    val critTotal = filteredSections.sumOf { s -> s.items.count { it.isCritical } }
                    val critDone = filteredSections.sumOf { s -> s.items.count { it.isCritical && checkStates[it.id] == true } }
                    Text("Critical: $critDone/$critTotal ✓", style = MaterialTheme.typography.labelMedium, color = if (critDone == critTotal) PrimaryGreen else ErrorRed)
                }
            }

            // MDR device-class filter (medical sector only)
            if (sectorKey == SectorKeys.MEDICAL_DEVICES) {
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(classes) { cls ->
                        FilterChip(
                            selected = classFilter == cls,
                            onClick = { classFilter = cls },
                            label = { Text(cls) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.12f), selectedLabelColor = PrimaryGreen),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                filteredSections.forEach { section ->
                    val sectionDone = section.items.count { checkStates[it.id] == true }
                    val sectionTotal = section.items.size
                    val sectionProg = if (sectionTotal > 0) sectionDone.toFloat() / sectionTotal else 0f

                    item(key = "sec_${section.title}") {
                        ChecklistSectionHeader(section, sectionDone, sectionTotal, sectionProg)
                    }
                    items(section.items, key = { it.id }) { item ->
                        ChecklistItemRow(
                            item = item,
                            checked = checkStates[item.id] == true,
                            color = section.color,
                            onToggle = { onToggle(item.id) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ChecklistSectionHeader(section: ChecklistSection, done: Int, total: Int, progress: Float) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), RoundedCornerShape(12.dp),
        color = section.color.copy(alpha = 0.07f), border = BorderStroke(1.dp, section.color.copy(alpha = 0.2f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = section.color)
                    Text(section.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$done/$total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = section.color)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = section.color,
                trackColor = section.color.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun ChecklistItemRow(item: ChecklistItem, checked: Boolean, color: Color, onToggle: () -> Unit) {
    var showHint by remember { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
        RoundedCornerShape(10.dp),
        color = if (checked) color.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 10.dp),
                Arrangement.spacedBy(10.dp), Alignment.Top,
            ) {
                Checkbox(
                    checked = checked, onCheckedChange = { onToggle() },
                    modifier = Modifier.size(20.dp).offset(y = 1.dp),
                    colors = CheckboxDefaults.colors(checkedColor = color),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (item.isCritical) FontWeight.SemiBold else FontWeight.Normal,
                            textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.isCritical && !checked) {
                            Surface(shape = RoundedCornerShape(4.dp), color = ErrorRed.copy(alpha = 0.12f)) {
                                Text("!", style = MaterialTheme.typography.labelSmall, color = ErrorRed, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
                if (item.hint.isNotBlank()) {
                    IconButton(onClick = { showHint = !showHint }, modifier = Modifier.size(24.dp)) {
                        Icon(if (showHint) Icons.Filled.ExpandLess else Icons.Outlined.Info, null,
                            Modifier.size(14.dp), tint = color.copy(alpha = 0.7f))
                    }
                }
            }
            AnimatedVisibility(visible = showHint && item.hint.isNotBlank()) {
                Text(item.hint, style = MaterialTheme.typography.bodySmall, color = color,
                    fontStyle = FontStyle.Italic, modifier = Modifier.padding(start = 42.dp, end = 12.dp, bottom = 10.dp))
            }
        }
    }
}
