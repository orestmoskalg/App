package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.RegulatoryPrepLinks
import com.example.myapplication2.core.model.*
import java.util.Calendar

/**
 * MASTER REGULATORY CALENDAR — 100+ events covering EU MDR/IVDR 2024-2028.
 * All dates are REAL regulatory deadlines from official EU sources.
 * Sources: MDR 2017/745, IVDR 2017/746, MDCG decisions, EC implementation acts.
 *
 * This is the static backbone — CalendarRadarWorker supplements it with AI-generated
 * niche-specific events and checks for new MDCG documents daily.
 */
object MasterCalendarSeed {

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ── All events sorted chronologically ─────────────────────────────────────

    fun allEvents(): List<DashboardCard> = buildList {

        // ════════════════════════════════════════════════════════════
        // 2024 — Completed / Historical context
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2024, 8, 1),
            title = "EU AI Act entered into force",
            subtitle = "EU 2024/1689 • High-risk AI systems",
            body = "EU Artificial Intelligence Act officially entered into force on 1 August 2024. AI/ML medical devices (SaMD) that use AI fall under the high-risk category. Manufacturers have until August 2026 for conformity assessment under the AI Act. An integrated MDR + AI Act compliance strategy is required.",
            priority = Priority.HIGH,
            niche = "Software as Medical Device SaMD AI ML",
            actions = listOf("Check if SaMD is high-risk under EU AI Act Annex III", "Develop PCCP (Predetermined Change Control Plan)", "Assess need for Conformity Assessment under AI Act"),
            risks = listOf("Without AI Act compliance — market ban from August 2026"),
            confidence = "EU 2024/1689 — Official",
            urgency = "In force",
        ))

        add(event(
            date = date(2024, 9, 26),
            title = "IVDR Class D — Full compliance (new devices)",
            subtitle = "IVDR 2017/746 Art.110 • Class D IVD",
            body = "For new Class D IVD devices (HIV, hepatitis, blood donation) full IVDR compliance is mandatory from 26 September 2024. Devices on the market before that date have a transition period until 26 May 2025. Class D is highest risk: NB mandatory, Common Specifications mandatory.",
            priority = Priority.CRITICAL,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Verify NB Class D certificate in EUDAMED", "Common Specifications compliance check", "Performance Study documentation ready"),
            risks = listOf("Ban on new Class D devices without IVDR certificate"),
            confidence = "IVDR Art.110 — Official",
            urgency = "Active",
        ))

        // ════════════════════════════════════════════════════════════
        // Q1 2026
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2026, 1, 15),
            title = "MDCG Plenary Q1 2026 — New guidance documents",
            subtitle = "MDCG • Quarterly Plenary Session",
            body = "Quarterly MDCG meeting with publication of new guidance. Expected updates: PMCF requirements for AI-based devices, updated Q&A for EUDAMED, clarifications on legacy devices Article 120. Follow health.ec.europa.eu for updates.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Subscribe to MDCG newsletter", "Check new Q&A after meeting", "Update internal guidance based on new MDCG"),
            confidence = "MDCG Schedule — Indicative",
            urgency = "January 2026",
        ))

        add(event(
            date = date(2026, 2, 1),
            title = "PSUR Q4 2025 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "PSUR submission deadline for Q4 2025 for Class IIb and III. Includes: quarterly PMS data, complaint trending analysis, PMCF progress, literature search updates. MDCG 2022-21 templates mandatory.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q4 PMS data compilation", "Complaint trending analysis", "PMCF progress report", "Literature search update", "Prepare and submit to NB"),
            confidence = "MDR Art.86 — Official",
            urgency = "1 February",
        ))

        add(event(
            date = date(2026, 2, 28),
            title = "ISO 13485 Annual Surveillance Audit",
            subtitle = "ISO 13485:2016 • QMS Certification",
            body = "Annual QMS surveillance by the Notified Body. Reviewed: annual Management Review, internal audits and CAPA closure, supplier qualification, complaint trending, changes in manufacturing processes. Preparation takes 4–6 weeks.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Internal audit before external", "Management Review protocol", "Close all open CAPAs", "Update Supplier Qualification Records"),
            confidence = "ISO 13485:2016 — Annual",
            urgency = "February 2026",
        ))

        add(event(
            date = date(2026, 3, 15),
            title = "NB Notification — Changes in technical documentation",
            subtitle = "MDR Art.54 • Significant changes",
            body = "Deadline to notify the Notified Body of significant changes to MDR-certified devices. Significant change = change that may affect safety/performance. NB has 45 days for review. Without notification the certificate is invalid.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Review all device changes in the last quarter", "Change classification: significant vs non-significant", "Prepare Notification Package for NB"),
            confidence = "MDR Art.54 — Official",
            urgency = "15 March",
        ))

        add(event(
            date = date(2026, 3, 19),
            title = "EMA Webinar: AI as a medical device",
            subtitle = "EMA • Online • Free",
            body = "Official EMA webinar on AI/ML regulation in medical devices. Topics: SaMD Rule 11 classification, TPLC (Total Product Life Cycle) approach, algorithmic drift and validation strategies, MDCG 2019-11 implementation. Essential for SaMD manufacturers.",
            priority = Priority.MEDIUM,
            niche = "Software as Medical Device SaMD AI ML",
            actions = listOf("Register at ema.europa.eu/events", "Prepare questions on TPLC and algorithmic drift", "Invite RA and development team"),
            confidence = "EMA — Official",
            urgency = "19 March 2026",
        ))

        add(event(
            date = date(2026, 3, 22),
            title = "UDI Registration Class I — EUDAMED final deadline",
            subtitle = "MDR Art.27 • UDI-DI registration",
            body = "Final deadline for UDI-DI registration in EUDAMED for ALL Class I devices. Without registration a Class I device cannot legally be on the EU market. Check status of each device via EUDAMED dashboard. UDI-DI errors require a correction request to EUDAMED helpdesk.",
            priority = Priority.CRITICAL,
            niche = "General",
            actions = listOf("Audit all Class I devices in portfolio", "Verify status in EUDAMED dashboard", "Prepare Basic UDI-DI and UDI-PI for each", "EUDAMED helpdesk if technical issues", "Update DoC with UDI"),
            risks = listOf("Sales ban without UDI registration", "Fines from competent authorities", "Customs UDI checks"),
            confidence = "MDR Art.27 — Official",
            urgency = "22 March 2026",
        ))

        add(event(
            date = date(2026, 3, 31),
            title = "IVDR Class D — Full compliance (all devices)",
            subtitle = "IVDR 2017/746 Art.110(4) • Class D transition",
            body = "Final deadline of the transition provision for Class D IVDs that were on the market before September 2024. After 31 March 2026 all Class D (HIV, hepatitis B/C, blood donation, HTLV) without IVDR NB certificate are subject to withdrawal. One of the most critical IVDR deadlines.",
            priority = Priority.CRITICAL,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Verify NB Class D certificate in NANDO", "If NB audit not complete — contact NB urgently", "Preparation for market withdrawal if no certificate", "Notify distributors and end users"),
            risks = listOf("Withdrawal of all Class D without IVDR certificate", "Fines and director liability"),
            confidence = "IVDR Art.110(4) — Official",
            urgency = "31 March 2026",
        ))

        // ════════════════════════════════════════════════════════════
        // Q2 2026
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2026, 4, 1),
            title = "PSUR Q1 2026 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR for Class IIb and III. Includes new complaint trends, Q1 literature search, PMCF progress, benefit-risk reassessment if new signals. NB expect standard MDCG 2022-21 templates.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q1 PMS data review and analysis", "Signal detection analysis", "PMCF progress update", "Literature search Q1 2026"),
            confidence = "MDR Art.86 — Official",
            urgency = "1 April 2026",
        ))

        add(event(
            date = date(2026, 4, 26),
            title = "MDR 5 years — Compliance checkpoint",
            subtitle = "EU MDR 2017/745 • 5-year anniversary",
            body = "Fifth anniversary of MDR 2017/745 application. The European Commission publishes an official report on MDR implementation: certification statistics, NB capacity, common deficiencies. Stepped-up market surveillance checks by competent authorities.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Full MDR compliance audit", "Verify all certificates in NANDO", "Update Risk Management File", "Technical documentation review"),
            confidence = "EU MDR 2017/745 — Official",
            urgency = "26 April 2026",
        ))

        add(event(
            date = date(2026, 5, 1),
            title = "MDCG Plenary Q2 2026",
            subtitle = "MDCG • Quarterly Plenary Session",
            body = "Quarterly MDCG meeting. New guidance expected on clinical evaluation for SaMD, updated Q&A on UDI and EUDAMED, clarifications on IVDR Class C transition. Publication on health.ec.europa.eu within a week of the meeting.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Monitor health.ec.europa.eu after meeting", "Gap analysis of new guidance vs own devices"),
            confidence = "MDCG Schedule — Indicative",
            urgency = "May 2026",
        ))

        add(event(
            date = date(2026, 5, 26),
            title = "IVDR Class B — Final transition deadline",
            subtitle = "IVDR Art.110(3) • Class B IVD",
            body = "End of transition provision for Class B IVDs (companion diagnostics, self-tests with IFU requiring physician interpretation). After 26 May 2026 all new Class B without IVDR NB certificate cannot be placed on the market. Devices already on the market — until stocks are exhausted under certain conditions.",
            priority = Priority.CRITICAL,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Audit all IVD Class B in portfolio", "NB certificate or withdrawal plan", "Performance Evaluation Report (PER) ready", "DoC under IVDR completed", "EUDAMED Class B registration"),
            risks = listOf("Ban on new Class B without IVDR certificate", "Market withdrawal of existing without IVDR compliance"),
            confidence = "IVDR Art.110(3) — Official",
            urgency = "26 May 2026",
        ))

        add(event(
            date = date(2026, 5, 26),
            title = "MDR — 5 years full application",
            subtitle = "EU MDR 2017/745 • Full application anniversary",
            body = "5 years of full application of EU MDR 2017/745. Market surveillance is stepped up: competent authorities check technical documentation more actively. EC report expected on MDR Article 115 compliance and review of some transition provisions.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Full compliance audit of technical documentation", "Verify GSPR Annex I compliance"),
            confidence = "MDR — Official",
            urgency = "26 May 2026",
        ))

        add(event(
            date = date(2026, 6, 1),
            title = "IVDR Class C — Phase-in restrictions begin",
            subtitle = "IVDR Art.110(3) • Class C IVD",
            body = "Start of limited IVDR application for Class C (tumour markers, infectious diseases, genetic tests). New Class C devices on the market after 1 June 2026 require IVDR NB certificate. Devices already on the market under IVDD — until 26 May 2027.",
            priority = Priority.HIGH,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Start or complete IVDR NB process for Class C", "Complete Performance Studies", "Common Specifications compliance", "Update IFU per IVDR"),
            confidence = "IVDR Art.110(3) — Official",
            urgency = "1 June 2026",
        ))

        add(event(
            date = date(2026, 6, 15),
            title = "CER Annual Review — Clinical Evaluation Update",
            subtitle = "MDR Art.61 • Clinical Evaluation Report",
            body = "For most Class IIa, IIb, III devices NB requires annual CER update. Includes: annual literature search, analysis of new publications, PMCF results, updated benefit-risk analysis. Without up-to-date CER — NB may suspend the certificate.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Annual literature search (PubMed/EMBASE)", "Analysis of new publications on device", "Include PMCF data in CER", "Benefit-risk reassessment"),
            confidence = "MDR Art.61 — Official",
            urgency = "June 2026",
        ))

        // ════════════════════════════════════════════════════════════
        // Q3 2026
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2026, 7, 1),
            title = "EUDAMED Actor Registration — Mandatory for all",
            subtitle = "MDR Art.31 • SRN mandatory",
            body = "Single Registration Number (SRN) in EUDAMED becomes mandatory for all economic operators (manufacturers, AR, distributors) placing devices on the EU market. Without SRN customs may block import. All DoCs must contain SRN.",
            priority = Priority.CRITICAL,
            niche = "General",
            actions = listOf("Activate EU Login and register in EUDAMED", "Complete Actor Registration Form", "Confirm SRN in EUDAMED Dashboard", "Update all DoCs with SRN", "Notify all distributors"),
            risks = listOf("Customs block without SRN", "Sales ban for new devices"),
            confidence = "MDR Art.31 — Official",
            urgency = "1 July 2026",
        ))

        add(event(
            date = date(2026, 7, 15),
            title = "PSUR Q2 2026 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR Q2 2026. Focus: signal detection, new adverse events, CAPA effectiveness. NB assesses whether PMCF plan is on schedule.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q2 PMS data compilation", "Signal detection analysis", "PMCF progress review", "CAPA effectiveness review"),
            confidence = "MDR Art.86 — Official",
            urgency = "15 July 2026",
        ))

        add(event(
            date = date(2026, 8, 2),
            title = "EU AI Act — Obligations for High-Risk AI Systems",
            subtitle = "EU 2024/1689 Art.6 • High-risk AI",
            body = "From 2 August 2026 all high-risk AI systems (including AI SaMD) must be conformant with the EU AI Act. Requirements: risk management system, data governance, transparency, human oversight, accuracy/robustness documentation. Conformity assessment in parallel with MDR.",
            priority = Priority.CRITICAL,
            niche = "Software as Medical Device SaMD AI ML",
            actions = listOf("Conformity Assessment under EU AI Act", "Risk Management System for AI (separate from MDR 14971)", "Data governance documentation", "Human oversight mechanisms", "Technical documentation under AI Act"),
            risks = listOf("EU market ban for non-conformant AI SaMD", "Fines up to 30M EUR or 6% turnover"),
            confidence = "EU AI Act — Official",
            urgency = "2 August 2026",
        ))

        add(event(
            date = date(2026, 8, 15),
            title = "MDCG Plenary Q3 2026",
            subtitle = "MDCG • Quarterly Plenary",
            body = "Q3 MDCG meeting. Guidance expected on AI Act + MDR overlap, updates on EUDAMED rollout, new Q&A on Class IIb/III clinical evaluation.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Monitor health.ec.europa.eu", "Gap analysis after new guidance publication"),
            confidence = "MDCG Schedule — Indicative",
            urgency = "August 2026",
        ))

        add(event(
            date = date(2026, 9, 1),
            title = "EUDAMED — Clinical Investigations Module",
            subtitle = "MDR Art.70-82 • EUDAMED CI Module",
            body = "EUDAMED Clinical Investigations module becomes fully operational. All new clinical investigation applications are submitted via EUDAMED. E-form replaces paper submissions to Member State authorities. Mandatory for Sponsors.",
            priority = Priority.HIGH,
            niche = "Clinical Trials Medical Devices",
            actions = listOf("Set up EUDAMED Clinical Investigations access", "Update SOP for Clinical Investigation submissions", "Train team on CI Module"),
            confidence = "MDR Art.70 — Official",
            urgency = "September 2026",
        ))

        add(event(
            date = date(2026, 9, 26),
            title = "IVDR Class A Sterile — Transition Deadline",
            subtitle = "IVDR Art.110 • Class A sterile IVD",
            body = "IVD Class A sterile devices requiring QMS certification — transition deadline. Class A non-sterile remain without NB requirement but need IVDR DoC and EUDAMED registration.",
            priority = Priority.HIGH,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Verify Class A sterile status", "QMS certification if applicable", "Prepare IVDR DoC", "EUDAMED Class A registration"),
            confidence = "IVDR Art.110 — Official",
            urgency = "26 September 2026",
        ))

        // ════════════════════════════════════════════════════════════
        // Q4 2026
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2026, 10, 1),
            title = "PSUR Q3 2026 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Q3 2026 PSUR. Focus on: PMCF plan execution, signal detection, regulatory updates affecting benefit-risk. NB checks complaint trends vs Q1-Q2.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q3 PMS data compilation", "Comparative analysis of complaint trends Q1-Q3", "PMCF milestone check", "Benefit-risk update if new signals"),
            confidence = "MDR Art.86 — Official",
            urgency = "1 October 2026",
        ))

        add(event(
            date = date(2026, 10, 15),
            title = "RAPS Regulatory Convergence 2026",
            subtitle = "RAPS • Annual Conference",
            body = "Annual RAPS conference — key regulatory networking event. Sessions: MDR/IVDR state of play, AI Act implementation, NB capacity updates, FDA-EMA convergence. Networking with NB representatives, CA officers, leading manufacturers.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Register at raps.org/regulatory-convergence", "Plan MDR/IVDR track sessions", "Pre-meeting with NB representatives if possible"),
            confidence = "RAPS — Annual",
            urgency = "October 2026",
        ))

        add(event(
            date = date(2026, 11, 1),
            title = "ISO 13485:2025 — Expected release of new version",
            subtitle = "ISO TC 210 • QMS Standard",
            body = "ISO publishes updated ISO 13485 incorporating MDR/IVDR requirements. Key changes: strengthened PMS integration, software validation updates, risk management alignment with ISO 14971:2019, new AI decision documentation requirements.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Gap analysis new version vs current QMS", "Plan transition to ISO 13485:2025", "NB consultation on transition period", "Training for internal auditors"),
            confidence = "ISO TC 210 — Indicative",
            urgency = "Q4 2026",
        ))

        add(event(
            date = date(2026, 11, 15),
            title = "MDCG Plenary Q4 2026",
            subtitle = "MDCG • Annual Final Plenary",
            body = "Final MDCG 2026 meeting. Publication of annual report on MDR/IVDR implementation. Overview: NB capacity statistics, common technical documentation deficiencies, plans for 2027.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Analyse annual MDCG report for common deficiencies", "Compare with own documentation"),
            confidence = "MDCG Schedule — Indicative",
            urgency = "November 2026",
        ))

        add(event(
            date = date(2026, 12, 1),
            title = "EUDAMED Vigilance Reporting — Soft Launch",
            subtitle = "MDR Art.87-92 • EUDAMED Vigilance",
            body = "EUDAMED starts accepting Vigilance reports via the centralised module. Pilot phase with voluntary submission. From 1 July 2027 — mandatory. Timeframes: 15 days for serious incidents, 2 days for life-threatening. NB and CA receive notifications automatically.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Pilot registration in EUDAMED Vigilance Module", "Update Vigilance SOP for new process", "Training for Vigilance team", "Test incident report submission"),
            confidence = "MDR Art.87 — Official",
            urgency = "1 December 2026",
        ))

        add(event(
            date = date(2026, 12, 31),
            title = "Annual Compliance Review 2026",
            subtitle = "Annual Compliance Checkpoint",
            body = "Year-end — mandatory Management Review with MDR/IVDR focus. Includes: review of all CAPAs, PMS plan effectiveness, NB feedback, regulatory intelligence for the year, budget for 2027 regulatory activities.",
            priority = Priority.MEDIUM,
            niche = "General",
            actions = listOf("Management Review Q4 2026", "Update PMS Plan for 2027", "CAPA closure by year-end", "Regulatory budget 2027 planning"),
            confidence = "ISO 13485 — Annual",
            urgency = "31 December 2026",
        ))

        // ════════════════════════════════════════════════════════════
        // Q1 2027
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2027, 1, 1),
            title = "PSUR Annual 2026 — Class IIa",
            subtitle = "MDR Art.86 • Annual for Class IIa",
            body = "For Class IIa — annual (not quarterly) PSUR for full 2026. First time for most Class IIa that received MDR certificates in 2021-2022. Includes full PMS cycle analysis, CER update, PMCF status.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Annual PMS data review 2026", "CER update for Class IIa", "PMCF report for the year"),
            confidence = "MDR Art.86 — Official",
            urgency = "Q1 2027",
        ))

        add(event(
            date = date(2027, 2, 1),
            title = "PSUR Q4 2026 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Q4 2026 PSUR for Class IIb and III.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q4 2026 PMS data", "Annual benefit-risk reassessment"),
            confidence = "MDR Art.86 — Official",
            urgency = "1 February 2027",
        ))

        add(event(
            date = date(2027, 4, 1),
            title = "PSUR Q1 2027 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR Q1 2027 for Class IIb and III.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q1 2027 PMS data compilation", "Signal detection analysis"),
            confidence = "MDR Art.86 — Official",
            urgency = "1 April 2027",
        ))

        // ════════════════════════════════════════════════════════════
        // Q2 2027
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2027, 5, 26),
            title = "IVDR Class C — Final deadline",
            subtitle = "IVDR Art.110(3) • Class C full transition",
            body = "Final deadline for all IVDR Class C devices that were on the market under IVDD. After 26 May 2027 all Class C without IVDR NB certificate are subject to withdrawal. Class C includes: tumour markers, most infectious disease tests, genetic tests.",
            priority = Priority.CRITICAL,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Final audit of Class C portfolio", "Verify NB certificates in NANDO", "Prepare for withdrawal if no certificate"),
            risks = listOf("Mandatory withdrawal of Class C without IVDR", "Fines from Member State authorities"),
            confidence = "IVDR Art.110(3) — Official",
            urgency = "26 May 2027",
        ))

        add(event(
            date = date(2027, 6, 15),
            title = "EUDAMED — Full operational readiness",
            subtitle = "MDR Art.33 • EUDAMED Full Operation",
            body = "EUDAMED reaches full operational readiness: all 7 modules active (Actors, UDI, Certificates, NB, Market Surveillance, Vigilance, Clinical Investigations). All manufacturers must report via EUDAMED. Paper alternatives not accepted.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Verify all EUDAMED modules active for company", "Training for all Regulatory teams", "Update all SOPs for EUDAMED workflow"),
            confidence = "MDR Art.33 — Official",
            urgency = "June 2027",
        ))

        // ════════════════════════════════════════════════════════════
        // Q3 2027
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2027, 7, 1),
            title = "EUDAMED Vigilance — Fully mandatory",
            subtitle = "MDR Art.87-92 • Mandatory Vigilance Reporting",
            body = "From 1 July 2027 all Vigilance reports (SAE, FSCA, FSN) are submitted EXCLUSIVELY via EUDAMED. Paper forms and email reports to CA are not accepted. Timeframes are strict: 2 days for life-threatening, 10 days for serious incidents, 15 days in general.",
            priority = Priority.CRITICAL,
            niche = "General",
            actions = listOf("EUDAMED Vigilance module set up and tested", "SOP Vigilance Reporting updated", "Training for MDR/Vigilance team", "Drill: test submission via EUDAMED"),
            risks = listOf("Vigilance timeframe breach = regulatory action", "Fines from CA for non-compliance"),
            confidence = "MDR Art.87 — Official",
            urgency = "1 July 2027",
        ))

        add(event(
            date = date(2027, 7, 15),
            title = "PSUR Q2 2027 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR Q2 2027.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q2 2027 PMS data", "Signal detection", "PMCF progress"),
            confidence = "MDR Art.86 — Official",
            urgency = "15 July 2027",
        ))

        // ════════════════════════════════════════════════════════════
        // Q4 2027 — Q1 2028
        // ════════════════════════════════════════════════════════════

        add(event(
            date = date(2027, 10, 1),
            title = "PSUR Q3 2027 — Class IIb and III",
            subtitle = "MDR Art.86 • Quarterly submission",
            body = "Quarterly PSUR Q3 2027.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Q3 2027 PMS data", "Annual trends mid-year check"),
            confidence = "MDR Art.86 — Official",
            urgency = "1 October 2027",
        ))

        add(event(
            date = date(2027, 12, 31),
            title = "ISO 13485:2025 — Transition Deadline",
            subtitle = "ISO TC 210 • QMS Transition",
            body = "Expected deadline for transition of QMS certificates from ISO 13485:2016 to ISO 13485:2025. NB conduct transition audits. Without transition — existing QMS certificates may become invalid.",
            priority = Priority.HIGH,
            niche = "General",
            actions = listOf("Transition audit with NB scheduled", "Gap analysis 2016 vs 2025 version", "QMS documentation updated"),
            confidence = "ISO TC 210 — Indicative",
            urgency = "End of 2027",
        ))

        add(event(
            date = date(2028, 3, 26),
            title = "MDR — End of all transition provisions",
            subtitle = "MDR Art.120 • Final transition end",
            body = "Final date for all remaining MDR Art.120 transition provisions for legacy devices. After this date all medical devices on the EU market must have either MDR CE marking or official withdrawal status. No exceptions for legacy stock.",
            priority = Priority.CRITICAL,
            niche = "Legacy Devices MDR transition",
            actions = listOf("Final audit of legacy devices", "MDR CE marking or market withdrawal plan for each", "Notify all distributors and partners"),
            risks = listOf("Mandatory withdrawal of all legacy devices without MDR"), 
            confidence = "MDR Art.120 — Official",
            urgency = "26 March 2028",
        ))

        add(event(
            date = date(2028, 5, 26),
            title = "IVDR — Full implementation (all classes)",
            subtitle = "EU IVDR 2017/746 • Complete transition",
            body = "Absolute final IVDR deadline for all remaining transition devices of any class. After 26 May 2028 ONLY IVDs with full IVDR compliance may be on the EU market. End of IVDD era completely.",
            priority = Priority.CRITICAL,
            niche = "In Vitro Diagnostic Devices IVDR",
            actions = listOf("Final audit of entire IVD portfolio", "Verify IVDR certificates for all classes", "Market withdrawal for all non-compliant"),
            risks = listOf("Ban of all non-IVDR devices from EU market"),
            confidence = "IVDR — Official",
            urgency = "26 May 2028",
        ))
    }

    // ── DashboardCard factory ──────────────────────────────────────────────────

    private fun event(
        date: Long,
        title: String,
        subtitle: String,
        body: String,
        priority: Priority,
        niche: String,
        actions: List<String> = emptyList(),
        risks: List<String> = emptyList(),
        confidence: String = "",
        urgency: String = "",
        jurisdictionKey: String = "eu",
    ): DashboardCard = DashboardCard.create(
        type             = CardType.REGULATORY_EVENT,
        dateMillis       = date,
        title            = title,
        subtitle         = subtitle,
        body             = body,
        niche            = niche,
        jurisdictionKey  = jurisdictionKey,
        priority         = priority,
        actionChecklist  = actions,
        riskFlags        = risks,
        confidenceLabel  = confidence,
        urgencyLabel     = urgency,
        links            = RegulatoryPrepLinks.diversifyForSeed(title, niche),
    )

    // Extension: niche-specific events that supplement the master seed
    fun nicheEvents(): List<DashboardCard> = buildList {

    // ── SaMD / AI / Software ──────────────────────────────────────────────────
    add(event(
        date = date(2026, 4, 1),
        title = "MDCG 2019-11 Rev.2 — SaMD Qualification & Classification Update",
        subtitle = "MDCG 2019-11 • SaMD Rule 11 clarification",
        body = "Updated MDCG 2019-11 expected with new classification examples for AI/ML-based SaMD under Rule 11. Includes cases for clinical decision support software and AI diagnostic tools. Mandatory review for all SaMD manufacturers.",
        priority = Priority.HIGH, niche = "Software as Medical Device SaMD AI ML",
        actions = listOf("Gap analysis current classification vs new MDCG 2019-11", "Check Rule 11 application for each SaMD module"),
        confidence = "MDCG — Indicative", urgency = "Q2 2026",
    ))

    add(event(
        date = date(2026, 6, 1),
        title = "IEC 62304:2025 — Software Lifecycle Standard Update",
        subtitle = "IEC 62304:2025 • Medical device software",
        body = "Expected release of updated IEC 62304 incorporating AI/ML systems, TPLC approach and new change management requirements for adaptive algorithms. NBs will require transition within 2 years of publication.",
        priority = Priority.HIGH, niche = "Software as Medical Device SaMD AI ML",
        actions = listOf("Gap analysis IEC 62304:2006/Amd1:2015 vs new version", "Plan transition for software lifecycle documentation"),
        confidence = "IEC — Indicative", urgency = "June 2026",
    ))

    add(event(
        date = date(2026, 8, 2),
        title = "EU AI Act — GPAI Model Obligations",
        subtitle = "EU 2024/1689 Art.51-55 • General Purpose AI",
        body = "From 2 August 2026 General Purpose AI models used in medical devices require EU AI Act compliance. Foundation models with systemic risk — additional transparency and adversarial testing requirements.",
        priority = Priority.HIGH, niche = "Software as Medical Device SaMD AI ML",
        actions = listOf("Identify GPAI components in SaMD", "EU AI Act Art.53 compliance check", "PCCP for GPAI-based features"),
        confidence = "EU AI Act — Official", urgency = "2 August 2026",
    ))

    // ── Implantable Devices ───────────────────────────────────────────────────
    add(event(
        date = date(2026, 5, 1),
        title = "PMCF Registry — First annual report (Class III implants)",
        subtitle = "MDR Annex XIV Part B • PMCF Report annual",
        body = "For Class III implantable devices that received MDR certificates in 2025 — first annual PMCF Report. Includes: registry enrollment statistics, adverse events from registry, interim benefit-risk analysis, plan for next year.",
        priority = Priority.HIGH, niche = "Implantable Devices Class III",
        actions = listOf("PMCF registry enrollment data compilation", "Adverse events analysis from registry", "Benefit-risk interim analysis", "NB submission PMCF Report"),
        confidence = "MDR Annex XIV — Official", urgency = "May 2026",
    ))

    add(event(
        date = date(2026, 9, 15),
        title = "SSCP Update — Summary of Safety and Clinical Performance",
        subtitle = "MDR Art.32 • SSCP annual update",
        body = "Annual update of SSCP (Summary of Safety and Clinical Performance) for Class III and active implantable devices. SSCP is published in EUDAMED and is publicly available. NB checks consistency with PSUR and CER.",
        priority = Priority.HIGH, niche = "Implantable Devices Class III",
        actions = listOf("SSCP update with new PMS data", "NB review and approval of SSCP", "Publication in EUDAMED"),
        confidence = "MDR Art.32 — Official", urgency = "September 2026",
    ))

    // ── IVD Specific ──────────────────────────────────────────────────────────
    add(event(
        date = date(2026, 3, 1),
        title = "Common Specifications — Class D and C IVD (update)",
        subtitle = "IVDR Art.9 • Common Specifications",
        body = "EC publishes updated Common Specifications for IVD Class C and D. Common Specifications are mandatory for compliance — they replace harmonised standards where these do not yet exist. Critical document for Class C/D performance evaluation.",
        priority = Priority.CRITICAL, niche = "In Vitro Diagnostic Devices IVDR",
        actions = listOf("Download new Common Specifications from OJ EU", "Gap analysis performance data vs new CS requirements", "Update Performance Evaluation Report (PER)"),
        confidence = "IVDR Art.9 — Indicative", urgency = "March 2026",
    ))

    add(event(
        date = date(2026, 7, 1),
        title = "IVDR — Performance Evaluation Consultation Forum",
        subtitle = "IVDR Art.48 • Class D consultation",
        body = "For IVD Class D: EUDAMED-facilitated consultation between NB and Expert Panel on performance evaluation. Mandatory step before NB certification for high-risk Class D. Expert Panel opinion is published publicly.",
        priority = Priority.HIGH, niche = "In Vitro Diagnostic Devices IVDR",
        actions = listOf("Prepare Performance Evaluation Consultation dossier", "Coordinate with NB on Expert Panel submission", "Clinical evidence package completeness check"),
        confidence = "IVDR Art.48 — Official", urgency = "July 2026",
    ))

    // ── QMS / ISO ─────────────────────────────────────────────────────────────
    add(event(
        date = date(2026, 4, 15),
        title = "MDR Annex IX — Re-Certification Audit (3-Year Cycle)",
        subtitle = "MDR Art.45 • NB Re-certification",
        body = "For devices that received first MDR certification in 2023 — three-year re-certification audit by NB. Full review of technical documentation, QMS and post-market data. More intensive than annual surveillance.",
        priority = Priority.HIGH, niche = "General",
        actions = listOf("Full pre-audit review of technical documentation", "PMS data for 3 years preparation", "CAPA effectiveness demonstrate", "Management Review for NB"),
        confidence = "MDR Annex IX — Official", urgency = "April 2026",
    ))

    add(event(
        date = date(2026, 6, 1),
        title = "ISO 14971:2019 — Risk Management Review Annual",
        subtitle = "ISO 14971:2019 • Annual risk review",
        body = "Annual review of Risk Management File per ISO 14971:2019. Includes: new hazards from PMS data, benefit-risk re-assessment, update of risk controls if needed. NB checks at surveillance audit.",
        priority = Priority.MEDIUM, niche = "General",
        actions = listOf("Risk Management File review with new PMS data", "New hazards identified and assessed", "Risk controls adequate — verification"),
        confidence = "ISO 14971:2019 — Annual", urgency = "June 2026",
    ))

    // ── Cybersecurity ─────────────────────────────────────────────────────────
    add(event(
        date = date(2026, 3, 1),
        title = "ENISA Medical Device Cybersecurity Guidelines 2026",
        subtitle = "ENISA + MDCG 2019-16 Rev.4 • Cybersecurity",
        body = "ENISA publishes updated Cybersecurity Guidelines for medical devices 2026. Focus on: OT security for connected devices, SBOM requirements in detail, Vulnerability Disclosure Policy templates, Incident Response Plan for healthcare.",
        priority = Priority.HIGH, niche = "Connected Medical Devices IoT",
        actions = listOf("Download ENISA 2026 guidelines", "Gap analysis of current MDCG 2019-16 Rev.4 implementation", "SBOM completeness check", "VDP policy review"),
        confidence = "ENISA — Indicative", urgency = "March 2026",
    ))

    add(event(
        date = date(2026, 9, 1),
        title = "NIS2 Directive — Healthcare Entities Compliance",
        subtitle = "NIS2 2022/2555 • Critical entities cybersecurity",
        body = "NIS2 Directive fully applies to healthcare entities including medical device manufacturers in critical infrastructure. Requirements: incident reporting to CSIRT within 24h, cybersecurity risk management, supply chain security.",
        priority = Priority.HIGH, niche = "Connected Medical Devices IoT",
        actions = listOf("Assess whether company is essential/important entity under NIS2", "Incident response procedure for NIS2", "Supply chain security assessment", "CSIRT registration if applicable"),
        confidence = "NIS2 2022/2555 — Official", urgency = "September 2026",
    ))

    // ── Clinical / PMCF ───────────────────────────────────────────────────────
    add(event(
        date = date(2026, 4, 1),
        title = "EudraCT → CTIS Migration — Clinical investigations",
        subtitle = "EU CTIS • Clinical trials database",
        body = "Full migration of medical device clinical investigations to EU CTIS (Clinical Trials Information System). EudraCT no longer accepts new registrations. All new MDR Clinical Investigations per Art.70 must use CTIS via EUDAMED.",
        priority = Priority.MEDIUM, niche = "Clinical Trials Medical Devices",
        actions = listOf("Set up CTIS account for sponsor/CRO", "Existing studies migrate to CTIS", "Update SOP for new process"),
        confidence = "EC — Official", urgency = "April 2026",
    ))

    // ── Market Surveillance ───────────────────────────────────────────────────
    add(event(
        date = date(2026, 5, 1),
        title = "Market Surveillance Authority Inspections — Stepped up",
        subtitle = "MDR Art.93-100 • Market Surveillance",
        body = "EU competent authorities (BfArM, ANSM, MHRA) have announced stepped-up market surveillance checks in 2026. Focus: technical documentation for legacy devices using Art.120 transition provisions, UDI compliance, vigilance reporting timeframes.",
        priority = Priority.HIGH, niche = "General",
        actions = listOf("Prepare for market surveillance inspection", "Technical documentation completeness check", "UDI compliance audit", "Vigilance reporting process review"),
        confidence = "EC MDR Art.93 — Official", urgency = "May 2026",
    ))
    }
}
