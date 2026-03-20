package com.example.myapplication2.core.common

// Term list aligned with Desktop `files (2)/cursor-package/.../RegulatoryGlossary.kt` (V2).

data class GlossaryTerm(
    val term: String,
    val definition: String,
    val niches: List<String>,
    val relatedTerms: List<String>,
)

object RegulatoryGlossary {
    val terms: List<GlossaryTerm> by lazy { buildTerms() }

    fun termsForCategory(category: String): List<GlossaryTerm> =
        terms.filter { it.niches.contains(category) }

    fun search(query: String): List<GlossaryTerm> {
        if (query.isBlank()) return terms
        val q = query.lowercase()
        return terms.filter { it.term.lowercase().contains(q) || it.definition.lowercase().contains(q) }
    }

    fun definitionFor(term: String): GlossaryTerm? =
        terms.find { it.term.equals(term, ignoreCase = true) }

    private fun buildTerms(): List<GlossaryTerm> = listOf(
        GlossaryTerm("MDR", "Medical Device Regulation (EU) 2017/745 — EU framework governing medical devices, replacing MDD. Sets requirements for clinical evidence, post-market surveillance, and device traceability.", listOf("MedTech"), listOf("IVDR", "MDD", "Notified Body", "CE marking")),
        GlossaryTerm("IVDR", "In Vitro Diagnostic Regulation (EU) 2017/746 — governs IVD medical devices. Introduces risk-based classification (Rules 1-7) and requires Notified Body involvement for higher-risk devices.", listOf("MedTech"), listOf("MDR", "IVD", "Companion Diagnostic")),
        GlossaryTerm("510(k)", "FDA premarket notification demonstrating substantial equivalence to an already marketed device. Most common US medical device clearance path.", listOf("MedTech"), listOf("PMA", "De Novo", "FDA")),
        GlossaryTerm("PMA", "Premarket Approval — most rigorous FDA pathway for Class III devices. Requires clinical trial data proving safety and effectiveness.", listOf("MedTech"), listOf("510(k)", "De Novo")),
        GlossaryTerm("Notified Body", "Organization designated by EU member state to assess manufacturer compliance. Examples: BSI, TUV SUD. Required for CE marking of higher-risk devices.", listOf("MedTech"), listOf("CE marking", "MDR")),
        GlossaryTerm("EUDAMED", "European Database on Medical Devices — centralized EU IT system for device registration, vigilance reporting, clinical investigations.", listOf("MedTech"), listOf("UDI", "MDR")),
        GlossaryTerm("UDI", "Unique Device Identification — system to identify medical devices throughout distribution. Consists of Device Identifier (UDI-DI) and Production Identifier (UDI-PI).", listOf("MedTech"), listOf("EUDAMED", "GUDID")),
        GlossaryTerm("SaMD", "Software as Medical Device — software intended for medical purposes not part of hardware. Classified by significance of information and healthcare situation.", listOf("MedTech", "Digital"), listOf("IEC 62304", "IMDRF")),
        GlossaryTerm("CER", "Clinical Evaluation Report — living document under MDR summarizing clinical evidence for device safety, performance, and clinical benefits.", listOf("MedTech"), listOf("PMCF", "Clinical Investigation")),
        GlossaryTerm("PMCF", "Post-Market Clinical Follow-up — active process to collect clinical data from marketed devices, required under MDR.", listOf("MedTech"), listOf("PMS", "CER", "PSUR")),
        GlossaryTerm("PSD2", "Payment Services Directive 2 — EU directive regulating payment services. Introduced Strong Customer Authentication and Open Banking. PSD3 in development.", listOf("FinTech"), listOf("SCA", "Open Banking", "PSD3")),
        GlossaryTerm("MiCA", "Markets in Crypto-Assets Regulation — EU comprehensive framework for crypto-asset markets. Covers CASPs, stablecoins (ARTs/EMTs), and market abuse.", listOf("FinTech"), listOf("CASP", "ART", "EMT")),
        GlossaryTerm("DORA", "Digital Operational Resilience Act — EU regulation requiring financial entities to withstand ICT disruptions and threats.", listOf("FinTech", "Digital"), listOf("NIS2", "ICT Risk")),
        GlossaryTerm("Basel III/IV", "International regulatory framework for banks: capital, liquidity, leverage requirements. Basel IV tightens risk-weighted asset rules.", listOf("FinTech"), listOf("CRR", "CRD")),
        GlossaryTerm("CASP", "Crypto-Asset Service Provider — entity providing crypto services that must be authorized under MiCA.", listOf("FinTech"), listOf("MiCA", "ART", "EMT")),
        GlossaryTerm("AI Act", "EU Artificial Intelligence Act — world's first comprehensive AI law. Classifies AI by risk level with corresponding obligations.", listOf("Digital"), listOf("High-risk AI", "GPAI", "AI Office")),
        GlossaryTerm("GPAI", "General-Purpose AI — models trained with large data using self-supervision, capable of wide range of tasks. Subject to AI Act transparency obligations.", listOf("Digital"), listOf("AI Act", "Systemic Risk")),
        GlossaryTerm("GDPR", "General Data Protection Regulation (EU) 2016/679 — EU data protection framework. Data subject rights, lawful bases, breach notification. Fines up to 4% global turnover.", listOf("Digital", "Legal"), listOf("DPA", "DPIA", "SCCs")),
        GlossaryTerm("NIS2", "Network and Information Security Directive 2 — expands cybersecurity obligations to more sectors. Requires 24h incident reporting and management liability.", listOf("Digital"), listOf("ENISA", "DORA", "CRA")),
        GlossaryTerm("DSA", "Digital Services Act — governs digital intermediaries. Content moderation transparency, risk assessments for VLOPs, algorithmic transparency.", listOf("Digital"), listOf("DMA", "VLOP")),
        GlossaryTerm("DMA", "Digital Markets Act — targets gatekeepers to ensure fair competition. Requires interoperability, data portability, prohibits self-preferencing.", listOf("Digital"), listOf("DSA", "Gatekeeper")),
        GlossaryTerm("DPA", "Data Protection Authority — independent public authority supervising data protection. Each EU state has one (CNIL, BfDI, ICO).", listOf("Digital"), listOf("GDPR", "EDPB")),
        GlossaryTerm("CCPA/CPRA", "California privacy laws giving consumers rights over personal data. CPRA created CPPA enforcement agency.", listOf("Digital"), listOf("GDPR", "CPPA")),
        GlossaryTerm("CSRD", "Corporate Sustainability Reporting Directive — requires sustainability reporting using ESRS. Phased implementation starting 2024.", listOf("Legal"), listOf("ESRS", "SFDR", "EU Taxonomy")),
        GlossaryTerm("EU Taxonomy", "Classification system defining environmentally sustainable economic activities. Used in financial disclosures. 6 environmental objectives.", listOf("Legal"), listOf("CSRD", "SFDR")),
        GlossaryTerm("SFDR", "Sustainable Finance Disclosure Regulation — requires financial participants to disclose sustainability risks. Classifies funds as Article 6/8/9.", listOf("Legal"), listOf("EU Taxonomy", "CSRD")),
        GlossaryTerm("CBAM", "Carbon Border Adjustment Mechanism — imposes carbon price on imports to prevent carbon leakage. Currently in transitional reporting phase.", listOf("Legal", "Industrial"), listOf("ETS", "Carbon Leakage")),
        GlossaryTerm("OFAC", "Office of Foreign Assets Control — US Treasury agency administering economic sanctions. Maintains SDN list.", listOf("Legal"), listOf("SDN", "Sanctions", "BIS")),
        GlossaryTerm("REACH", "Registration, Evaluation, Authorisation and Restriction of Chemicals — EU regulation requiring manufacturers to register and manage chemical risks.", listOf("Industrial"), listOf("ECHA", "SVHC", "CLP")),
        GlossaryTerm("CLP", "Classification, Labelling and Packaging — EU implementation of GHS for chemical hazard classification.", listOf("Industrial"), listOf("GHS", "REACH", "SDS")),
        GlossaryTerm("HACCP", "Hazard Analysis and Critical Control Points — preventive food safety approach identifying physical, chemical, biological hazards.", listOf("Industrial"), listOf("FSMA", "EFSA")),
        GlossaryTerm("ETS", "Emissions Trading System — EU cap-and-trade for greenhouse gas emissions. Cap decreases over time.", listOf("Industrial", "Legal"), listOf("CBAM", "Carbon Credits")),
        GlossaryTerm("Euro 7", "Upcoming EU vehicle emission standard covering exhaust pollutants, brake/tyre particles, battery durability.", listOf("Industrial"), listOf("UN ECE", "Type Approval")),
        GlossaryTerm("CE Marking", "Conformité Européenne — indicates EU health/safety/environment compliance. Required for medical devices, machinery, electronics.", listOf("MedTech", "Industrial"), listOf("Conformity Assessment", "Notified Body")),
        GlossaryTerm("Conformity Assessment", "Process demonstrating product meets regulatory requirements. May involve self-assessment, third-party testing, QMS audit.", listOf("MedTech", "Industrial", "Digital"), listOf("CE Marking", "Notified Body")),
        GlossaryTerm("Transition Period", "Window between regulation entry into force and full application. Allows gradual compliance. Dates vary by product class.", listOf("MedTech", "FinTech", "Digital", "Legal", "Industrial"), listOf("Grandfathering", "Sell-off Date")),
    )
}
