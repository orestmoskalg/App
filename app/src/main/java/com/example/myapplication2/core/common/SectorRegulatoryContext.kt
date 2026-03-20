package com.example.myapplication2.core.common

import com.example.myapplication2.domain.model.UserProfile

/**
 * Builds **sector × jurisdiction** narrative for LLM prompts (general regulations, not medical-only).
 */
object SectorRegulatoryContext {

    fun sectorKeyOrDefault(profile: UserProfile): String =
        profile.sector.ifBlank { SectorCatalog.DEFAULT_KEY }

    /**
     * Full domain + jurisdiction block for system prompts (insights, strategies, learning, calendar).
     */
    fun combinedExpertAndJurisdictionPrompt(sectorKey: String, country: String): String {
        val sk = sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }
        val ctx = CountryRegulatoryContext.forCountry(country)
        val sectorLine = SectorCatalog.find(sk)?.promptSummary
            ?: "General regulatory compliance in the user's selected sector."
        val domainExpert = expertDomainLine(sk)
        val jurisdictionBlock = when (sk) {
            SectorKeys.MEDICAL_DEVICES -> ctx.systemFocus
            else -> nonMedicalJurisdictionNarrative(sk, ctx)
        }
        return """
$domainExpert
Sector scope: $sectorLine
$jurisdictionBlock
""".trimIndent()
    }

    private fun expertDomainLine(sectorKey: String): String = when (sectorKey) {
        SectorKeys.MEDICAL_DEVICES ->
            "You are a senior regulatory affairs expert in medical devices, IVD, diagnostics, and digital health products."
        SectorKeys.PHARMACEUTICALS ->
            "You are a senior regulatory affairs expert in human and veterinary medicinal products, biologics, ATMPs, and GxP compliance."
        SectorKeys.FOOD_FEED ->
            "You are a senior regulatory expert in food and feed law, food safety systems, hygiene, contaminants, labeling, and import controls."
        SectorKeys.COSMETICS ->
            "You are a senior regulatory expert in cosmetic products, safety assessments, product information files, and market notifications."
        SectorKeys.CHEMICALS ->
            "You are a senior regulatory expert in chemicals law, classification, labeling, SDS, biocides, and hazardous substances."
        SectorKeys.CONSUMER_PRODUCTS ->
            "You are a senior regulatory expert in general product safety, consumer goods conformity, recalls, and market surveillance."
        SectorKeys.AGRI_BIO ->
            "You are a senior regulatory expert in agriculture inputs, plant protection, seeds, GMO/NGT, and related authorizations."
        SectorKeys.ENVIRONMENT ->
            "You are a senior regulatory expert in environmental permitting, waste, packaging EPR, emissions, and circular economy rules."
        SectorKeys.DIGITAL_PRIVACY ->
            "You are a senior regulatory expert in data protection, AI governance, cybersecurity, and digital product compliance."
        SectorKeys.WORKPLACE_SAFETY ->
            "You are a senior regulatory expert in occupational health and safety, machinery, and workplace equipment conformity."
        SectorKeys.CONSTRUCTION ->
            "You are a senior regulatory expert in construction products regulation, performance declarations, and building safety."
        SectorKeys.AUTOMOTIVE ->
            "You are a senior regulatory expert in vehicle type approval, transport of dangerous goods, and automotive safety/emissions."
        SectorKeys.TOBACCO_ALCOHOL ->
            "You are a senior regulatory expert in tobacco, novel tobacco, alcohol, and excise-related product rules."
        SectorKeys.OTHER ->
            "You are a senior regulatory expert in cross-sector compliance, trade compliance, and multi-domain regulatory programs."
        else ->
            "You are a senior regulatory affairs expert in the user's selected compliance sector."
    }

    private fun nonMedicalJurisdictionNarrative(sectorKey: String, ctx: CountryRegulatoryContext.Context): String {
        val jName = ctx.jurisdictionName
        return when (ctx.jurisdictionKey) {
            "usa" -> usaSectorRemit(sectorKey, jName)
            "ukraine" -> ukraineSectorRemit(sectorKey, jName)
            "other" -> "Operate within $jName. Use the main national frameworks and internationally recognized standards for this sector; label cross-border comparisons."
            else -> euSectorRemit(sectorKey, jName)
        }
    }

    private fun euSectorRemit(sectorKey: String, jName: String): String = when (sectorKey) {
        SectorKeys.PHARMACEUTICALS ->
            "In $jName focus on EU pharmaceutical legislation (EMA where relevant), national competent authorities, GMP/GDP, pharmacovigilance, and clinical trial rules."
        SectorKeys.FOOD_FEED ->
            "In $jName focus on EU General Food Law, hygiene packages, contaminants, novel food, food supplements, RASFF, and EFSA scientific opinions where applicable."
        SectorKeys.COSMETICS ->
            "In $jName focus on EU Cosmetics Regulation (EC) 1223/2009, PIF, CPNP, safety assessment, and responsible person obligations."
        SectorKeys.CHEMICALS ->
            "In $jName focus on REACH/CLP, biocidal products, pesticides, detergents, and national enforcement for chemicals."
        SectorKeys.CONSUMER_PRODUCTS ->
            "In $jName focus on EU General Product Safety Regulation, harmonized standards, recalls, and national market surveillance."
        SectorKeys.AGRI_BIO ->
            "In $jName focus on EU plant protection, seeds, animal feed, GMO/NGT, and national agriculture authority requirements."
        SectorKeys.ENVIRONMENT ->
            "In $jName focus on EU environmental law (waste, packaging, EPR, industrial emissions) and national permits."
        SectorKeys.DIGITAL_PRIVACY ->
            "In $jName focus on GDPR, AI Act, NIS2, ePrivacy where relevant, and national cybersecurity authorities."
        SectorKeys.WORKPLACE_SAFETY ->
            "In $jName focus on EU machinery/OHS directives, workplace risk limits, and PPE/work equipment conformity."
        SectorKeys.CONSTRUCTION ->
            "In $jName focus on CPR, harmonized CE marking for construction products, and national building regulations."
        SectorKeys.AUTOMOTIVE ->
            "In $jName focus on EU vehicle type-approval frameworks, UN/ECE regulations where adopted, and national transport rules."
        SectorKeys.TOBACCO_ALCOHOL ->
            "In $jName focus on EU tobacco products (TPD), excise, labeling, and national alcohol licensing rules."
        SectorKeys.OTHER ->
            "In $jName focus on the most relevant EU acts and national transposition for the user's sector."
        else ->
            "In $jName apply the applicable EU regulations and national provisions for this sector; do not default to medical device rules unless the niche requires it."
    }

    private fun usaSectorRemit(sectorKey: String, jName: String): String = when (sectorKey) {
        SectorKeys.PHARMACEUTICALS ->
            "In $jName focus on FDA (CDER/CBER/VM), DEA where relevant, USP, GMP, pharmacovigilance, and clinical trial rules."
        SectorKeys.FOOD_FEED ->
            "In $jName focus on FDA FSMA, HACCP, USDA where relevant, food labeling, import alerts, and FSMA supply-chain programs."
        SectorKeys.COSMETICS ->
            "In $jName focus on FDA cosmetic regulation (FD&C Act), MoCRA, labeling, facility registration, and adverse event reporting."
        SectorKeys.CHEMICALS ->
            "In $jName focus on EPA TSCA/FIFRA, OSHA HAZCOM, OSHA chemical standards, and DOT/PHMSA transport rules where relevant."
        SectorKeys.CONSUMER_PRODUCTS ->
            "In $jName focus on CPSC, CPSIA where relevant, FCC/consumer electronics, recalls, and federal/state product safety."
        SectorKeys.AGRI_BIO ->
            "In $jName focus on EPA pesticidal products, USDA APHIS, FDA animal feed/food, and state-level agriculture rules."
        SectorKeys.ENVIRONMENT ->
            "In $jName focus on EPA environmental programs (air, water, waste), RCRA, EPCRA, and state environmental agencies."
        SectorKeys.DIGITAL_PRIVACY ->
            "In $jName focus on FTC privacy/security, sector HIPAA where relevant, state privacy laws, and federal AI risk management guidance."
        SectorKeys.WORKPLACE_SAFETY ->
            "In $jName focus on OSHA standards, machine guarding, and NIST/ANSI references for equipment safety."
        SectorKeys.CONSTRUCTION ->
            "In $jName focus on federal building codes, OSHA construction, and state/local building product acceptance."
        SectorKeys.AUTOMOTIVE ->
            "In $jName focus on NHTSA/FMVSS, EPA emissions, and PHMSA/DOT for hazardous materials transport."
        SectorKeys.TOBACCO_ALCOHOL ->
            "In $jName focus on FDA tobacco products center rules, TTB for alcohol, excise, and state licensing."
        SectorKeys.OTHER ->
            "In $jName focus on the most relevant federal agencies and state programs for this sector."
        else ->
            "In $jName apply US federal and state rules for this sector; do not assume EU medical device frameworks."
    }

    private fun ukraineSectorRemit(sectorKey: String, jName: String): String = when (sectorKey) {
        SectorKeys.PHARMACEUTICALS ->
            "In $jName focus on Ukrainian medicines/medical devices authorities, GMP/GDP alignment, national registration, and pharmacovigilance."
        SectorKeys.FOOD_FEED ->
            "In $jName focus on Ukrainian food safety law, official controls, hygiene, labeling, and import requirements."
        SectorKeys.COSMETICS ->
            "In $jName focus on Ukrainian cosmetic product regulation, notification/registration, and safety documentation."
        SectorKeys.CHEMICALS ->
            "In $jName focus on Ukrainian chemical safety, SDS, classification, and national environmental chemical rules."
        SectorKeys.CONSUMER_PRODUCTS ->
            "In $jName focus on Ukrainian product safety and market surveillance, aligned with EU GPSR-style expectations where applicable."
        else ->
            "In $jName apply Ukrainian national legislation and official guidance for this sector; cite State Expert Centre (SLC) only where it applies to medicines/medical devices."
    }

    /**
     * Citation hint for insight prompts — sector-aware; medical uses existing country hint.
     */
    fun regulatoryAuthorityCitationHint(sectorKey: String, ctx: CountryRegulatoryContext.Context): String {
        val sk = sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }
        if (sk == SectorKeys.MEDICAL_DEVICES) {
            return CountryRegulatoryContext.regulatoryAuthorityCitationHint(ctx)
        }
        return when (ctx.jurisdictionKey) {
            "usa" -> when (sk) {
                SectorKeys.FOOD_FEED -> "FDA, USDA, FSMA, HACCP, and import requirements — not EU MDCG"
                SectorKeys.PHARMACEUTICALS -> "FDA, ICH, USP, GMP regulations — not EU MDR"
                SectorKeys.COSMETICS -> "FDA, MoCRA, cosmetic labeling — not EU MDR"
                SectorKeys.CHEMICALS -> "EPA, OSHA, TSCA/FIFRA, DOT/PHMSA as applicable — not EU MDR"
                SectorKeys.DIGITAL_PRIVACY -> "FTC, HIPAA (if health data), state privacy laws, NIST — not EU MDR"
                else -> "US federal/state agencies relevant to this sector — not EU medical device bodies unless comparing"
            }
            "ukraine" -> "Ukrainian national authorities and official acts for this sector; SLC only for medicines/medical devices"
            "other" -> "Authorities that match the user's jurisdiction; label EU vs US comparisons"
            else -> when (sk) {
                SectorKeys.FOOD_FEED -> "EC food law, RASFF, national food authority — not MDCG medical device guidance"
                SectorKeys.PHARMACEUTICALS -> "EMA/national competent authorities, EU pharmaceutical directives — not MDR device rules"
                SectorKeys.COSMETICS -> "EU Cosmetics Regulation, responsible person, CPNP — not MDR"
                SectorKeys.CHEMICALS -> "ECHA, REACH/CLP, national chemicals enforcement — not MDR"
                SectorKeys.DIGITAL_PRIVACY -> "EDPB, GDPR, AI Act, national DPA — not MDR"
                else -> "EU sector regulations and national competent authorities for ${ctx.jurisdictionName} — avoid FDA/MDR unless explicitly comparing"
            }
        }
    }

    fun jurisdictionReviewProcessPhrase(ctx: CountryRegulatoryContext.Context, sectorKey: String): String {
        val sk = sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }
        return when (sk) {
            SectorKeys.MEDICAL_DEVICES -> when (ctx.jurisdictionKey) {
                "usa" -> "FDA submission, review, and inspection processes"
                "ukraine" -> "State Expert Centre (SLC) registration and market surveillance processes"
                "other" -> "the competent authority process appropriate to ${ctx.jurisdictionName}"
                else -> "Notified Body and national competent authority processes where applicable in ${ctx.jurisdictionName}"
            }
            SectorKeys.PHARMACEUTICALS -> "marketing authorization, GMP inspections, and pharmacovigilance processes"
            SectorKeys.FOOD_FEED -> "FDA/USDA or EU food authority inspections, registrations, and import controls"
            SectorKeys.COSMETICS -> "product safety assessment, notification, and market surveillance processes"
            SectorKeys.CHEMICALS -> "REACH/CLP registration, authorization, and enforcement processes"
            SectorKeys.DIGITAL_PRIVACY -> "DPA supervision, DPIA, AI system conformity, and breach notification processes"
            else -> "competent authority review, permits, and market surveillance in ${ctx.jurisdictionName}"
        }
    }
}
