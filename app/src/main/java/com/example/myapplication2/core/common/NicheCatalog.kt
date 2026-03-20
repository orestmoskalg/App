package com.example.myapplication2.core.common

data class Niche(
    val sectorKey: String,
    val name: String,
    val nameEn: String,
    val promptKey: String,
    val icon: String = "📋",
) {
    override fun equals(other: Any?) = other is Niche && promptKey == other.promptKey
    override fun hashCode() = promptKey.hashCode()
}

object NicheCatalog {

    val all: List<Niche> = buildList {
        addAll(medicalDevicesNiches())
        addAll(pharmaNiches())
        addAll(foodFeedNiches())
        addAll(cosmeticsNiches())
        addAll(chemicalsNiches())
        addAll(consumerNiches())
        addAll(agriNiches())
        addAll(environmentNiches())
        addAll(digitalNiches())
        addAll(workplaceNiches())
        addAll(constructionNiches())
        addAll(automotiveNiches())
        addAll(tobaccoAlcoholNiches())
        addAll(otherNiches())
    }

    private fun medicalDevicesNiches() = listOf(
        Niche(SectorKeys.MEDICAL_DEVICES, "Cardiovascular Devices", "Cardiovascular Devices", "Cardiovascular Devices (stents, pacemakers, valves)", "🫀"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Orthopedic & Trauma Devices", "Orthopedic & Trauma Devices", "Orthopedic and Trauma Devices (implants, prosthetics)", "🦴"),
        Niche(SectorKeys.MEDICAL_DEVICES, "In Vitro Diagnostics (IVDR)", "In Vitro Diagnostics (IVDR)", "In Vitro Diagnostic Devices IVDR", "🧪"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Software as Medical Device (SaMD)", "Software as Medical Device (SaMD)", "Software as Medical Device SaMD AI ML", "💻"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Active Implantable Devices", "Active Implantable Devices", "Active Implantable Medical Devices AIMD", "⚡"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Ophthalmic Devices", "Ophthalmic Devices", "Ophthalmic Devices (lenses, implants)", "👁️"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Dental & Maxillofacial", "Dental & Maxillofacial", "Dental and Maxillofacial Devices", "🦷"),
        Niche(SectorKeys.MEDICAL_DEVICES, "General Surgery Instruments", "General Surgery Instruments", "General Surgery Instruments", "🔬"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Wound Care", "Wound Care", "Wound Care and Dressings", "🩹"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Diagnostic Imaging", "Diagnostic Imaging", "Diagnostic Imaging Equipment", "📡"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Infusion & Injection", "Infusion & Injection", "Infusion Pumps and Injection Devices", "💉"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Home Healthcare", "Home Healthcare", "Home Healthcare Devices", "🏠"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Custom-Made Class III", "Custom-Made Class III", "Custom-Made Devices Class III", "🛠️"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Legacy Devices", "Legacy Devices", "Legacy Devices MDR transition", "📋"),
        Niche(SectorKeys.MEDICAL_DEVICES, "AI/ML Medical Devices", "AI/ML Medical Devices", "AI ML Medical Devices Regulation", "🤖"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Drug-Device Combination", "Drug-Device Combination", "Drug-Device Combination Products", "💊"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Patient Monitoring", "Patient Monitoring", "Patient Monitoring Systems", "📊"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Rehabilitation & Physiotherapy", "Rehabilitation & Physiotherapy", "Rehabilitation and Physiotherapy Devices", "🏃"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Sterile Single-Use", "Sterile Single-Use", "Sterile single-use medical devices and aseptic processing", "🧤"),
        Niche(SectorKeys.MEDICAL_DEVICES, "Radiation-Emitting Products", "Radiation-Emitting Products", "Medical lasers and radiation-emitting devices", "☢️"),
    )

    private fun pharmaNiches() = listOf(
        Niche(SectorKeys.PHARMACEUTICALS, "Small-molecule drugs", "Small-molecule drugs", "pharma_small_molecule_chemical", "🧬"),
        Niche(SectorKeys.PHARMACEUTICALS, "Biologics & vaccines", "Biologics & vaccines", "pharma_biologics_vaccines", "💉"),
        Niche(SectorKeys.PHARMACEUTICALS, "ATMPs & cell & gene", "ATMPs & cell & gene", "pharma_atmp_cgt", "🧫"),
        Niche(SectorKeys.PHARMACEUTICALS, "Biosimilars", "Biosimilars", "pharma_biosimilars", "↔️"),
        Niche(SectorKeys.PHARMACEUTICALS, "Generics & hybrid", "Generics & hybrid", "pharma_generics_hybrid", "📦"),
        Niche(SectorKeys.PHARMACEUTICALS, "OTC & self-care", "OTC & self-care", "pharma_otc_selfcare", "🏬"),
        Niche(SectorKeys.PHARMACEUTICALS, "GxP & manufacturing", "GxP & manufacturing", "pharma_gmp_gdp_manufacturing", "🏭"),
        Niche(SectorKeys.PHARMACEUTICALS, "Pharmacovigilance", "Pharmacovigilance", "pharma_pharmacovigilance", "📣"),
        Niche(SectorKeys.PHARMACEUTICALS, "Clinical trials — pharma", "Clinical trials — pharma", "pharma_clinical_trials", "📋"),
        Niche(SectorKeys.PHARMACEUTICALS, "Packaging & serialization", "Packaging & serialization", "pharma_packaging_serialization", "🏷️"),
        Niche(SectorKeys.PHARMACEUTICALS, "Radiopharmaceuticals", "Radiopharmaceuticals", "pharma_radiopharma", "☢️"),
        Niche(SectorKeys.PHARMACEUTICALS, "Veterinary medicines", "Veterinary medicines", "pharma_veterinary", "🐄"),
    )

    private fun foodFeedNiches() = listOf(
        Niche(SectorKeys.FOOD_FEED, "Food safety systems", "Food safety systems", "food_haccp_food_safety", "✅"),
        Niche(SectorKeys.FOOD_FEED, "Food labeling & claims", "Food labeling & claims", "food_labeling_claims", "🏷️"),
        Niche(SectorKeys.FOOD_FEED, "Novel food & ingredients", "Novel food & ingredients", "food_novel_food", "🆕"),
        Niche(SectorKeys.FOOD_FEED, "Food supplements", "Food supplements", "food_supplements", "💊"),
        Niche(SectorKeys.FOOD_FEED, "Infant & medical food", "Infant & medical food", "food_infant_medical", "🍼"),
        Niche(SectorKeys.FOOD_FEED, "Feed & pet food", "Feed & pet food", "feed_pet_food", "🐕"),
        Niche(SectorKeys.FOOD_FEED, "Contaminants & residues", "Contaminants & residues", "food_contaminants", "⚠️"),
        Niche(SectorKeys.FOOD_FEED, "Import & border controls", "Import & border controls", "food_import_border", "🛃"),
        Niche(SectorKeys.FOOD_FEED, "Food contact materials", "Food contact materials", "food_contact_materials", "🥤"),
        Niche(SectorKeys.FOOD_FEED, "Organic & sustainability claims", "Organic & sustainability claims", "food_organic_sustainability", "🌿"),
    )

    private fun cosmeticsNiches() = listOf(
        Niche(SectorKeys.COSMETICS, "Rinse-off & leave-on", "Rinse-off & leave-on", "cosmetic_rinse_leave_on", "🧴"),
        Niche(SectorKeys.COSMETICS, "Product Information File (PIF)", "Product Information File (PIF)", "cosmetic_pif_safety", "📁"),
        Niche(SectorKeys.COSMETICS, "CPNP & notifications", "CPNP & notifications", "cosmetic_cpnp_notification", "🌐"),
        Niche(SectorKeys.COSMETICS, "Claims & advertising", "Claims & advertising", "cosmetic_claims_advertising", "📢"),
        Niche(SectorKeys.COSMETICS, "Nanomaterials", "Nanomaterials", "cosmetic_nanomaterials", "🔬"),
        Niche(SectorKeys.COSMETICS, "Animal testing & alternatives", "Animal testing & alternatives", "cosmetic_animal_testing", "🐰"),
        Niche(SectorKeys.COSMETICS, "Fragrance & allergens", "Fragrance & allergens", "cosmetic_fragrance_allergens", "🌸"),
        Niche(SectorKeys.COSMETICS, "Natural & organic cosmetics", "Natural & organic cosmetics", "cosmetic_natural_organic", "🍃"),
    )

    private fun chemicalsNiches() = listOf(
        Niche(SectorKeys.CHEMICALS, "REACH registration", "REACH registration", "chem_reach_registration", "EU"),
        Niche(SectorKeys.CHEMICALS, "CLP & classification", "CLP & classification", "chem_clp_classification", "⚠️"),
        Niche(SectorKeys.CHEMICALS, "SDS & labels", "SDS & labels", "chem_sds_labels", "📄"),
        Niche(SectorKeys.CHEMICALS, "Biocidal products", "Biocidal products", "chem_biocides", "🦠"),
        Niche(SectorKeys.CHEMICALS, "Pesticides & PPP", "Pesticides & PPP", "chem_pesticides", "🌱"),
        Niche(SectorKeys.CHEMICALS, "Industrial chemicals & polymers", "Industrial chemicals & polymers", "chem_industrial_polymers", "🏭"),
        Niche(SectorKeys.CHEMICALS, "Detergents & cleaners", "Detergents & cleaners", "chem_detergents", "🧽"),
        Niche(SectorKeys.CHEMICALS, "Transport of dangerous goods", "Transport of dangerous goods", "chem_adr_transport", "🚛"),
    )

    private fun consumerNiches() = listOf(
        Niche(SectorKeys.CONSUMER_PRODUCTS, "General product safety", "General product safety", "consumer_gpsr_safety", "🛡️"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Toys & childcare", "Toys & childcare", "consumer_toys_childcare", "🧸"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Electrical & electronics", "Electrical & electronics", "consumer_electrical_lvd", "🔌"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Furniture & mattresses", "Furniture & mattresses", "consumer_furniture", "🛋️"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Textiles & footwear", "Textiles & footwear", "consumer_textiles", "👕"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Batteries & WEEE", "Batteries & WEEE", "consumer_batteries_weee", "🔋"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Recalls & RAPEX", "Recalls & RAPEX", "consumer_recalls_rapex", "📣"),
        Niche(SectorKeys.CONSUMER_PRODUCTS, "Metrology & pre-packaging", "Metrology & pre-packaging", "consumer_metrology", "⚖️"),
    )

    private fun agriNiches() = listOf(
        Niche(SectorKeys.AGRI_BIO, "Plant protection products", "Plant protection products", "agri_ppp", "🌾"),
        Niche(SectorKeys.AGRI_BIO, "Seeds & propagation", "Seeds & propagation", "agri_seeds", "🌱"),
        Niche(SectorKeys.AGRI_BIO, "GMO & NGT", "GMO & NGT", "agri_gmo_ngt", "🧬"),
        Niche(SectorKeys.AGRI_BIO, "Fertilizers & biostimulants", "Fertilizers & biostimulants", "agri_fertilizers", "🪴"),
        Niche(SectorKeys.AGRI_BIO, "Animal feed chain", "Animal feed chain", "agri_feed_chain", "🐄"),
        Niche(SectorKeys.AGRI_BIO, "Veterinary biologics", "Veterinary biologics", "agri_vet_biologics", "💉"),
    )

    private fun environmentNiches() = listOf(
        Niche(SectorKeys.ENVIRONMENT, "Waste & circular economy", "Waste & circular economy", "env_waste_circular", "♻️"),
        Niche(SectorKeys.ENVIRONMENT, "Packaging EPR", "Packaging EPR", "env_packaging_epr", "📦"),
        Niche(SectorKeys.ENVIRONMENT, "Industrial emissions", "Industrial emissions", "env_industrial_emissions", "🏭"),
        Niche(SectorKeys.ENVIRONMENT, "Water & wastewater", "Water & wastewater", "env_water", "💧"),
        Niche(SectorKeys.ENVIRONMENT, "Chemical accident prevention", "Chemical accident prevention", "env_seveso", "⚠️"),
        Niche(SectorKeys.ENVIRONMENT, "Carbon & climate reporting", "Carbon & climate reporting", "env_carbon_reporting", "🌍"),
    )

    private fun digitalNiches() = listOf(
        Niche(SectorKeys.DIGITAL_PRIVACY, "GDPR & processing", "GDPR & processing", "dp_gdpr_processing", "🇪🇺"),
        Niche(SectorKeys.DIGITAL_PRIVACY, "AI governance & AI Act", "AI governance & AI Act", "dp_ai_act", "🤖"),
        Niche(SectorKeys.DIGITAL_PRIVACY, "Cybersecurity & NIS2", "Cybersecurity & NIS2", "dp_nis2_cyber", "🔒"),
        Niche(SectorKeys.DIGITAL_PRIVACY, "Cross-border data transfers", "Cross-border data transfers", "dp_transfers_scc", "🌐"),
        Niche(SectorKeys.DIGITAL_PRIVACY, "Cookies & ePrivacy", "Cookies & ePrivacy", "dp_cookies_eprivacy", "🍪"),
        Niche(SectorKeys.DIGITAL_PRIVACY, "Sector IT validation (GxP)", "Sector IT validation (GxP)", "dp_gxp_csv", "💻"),
        Niche(SectorKeys.DIGITAL_PRIVACY, "Consumer IoT security", "Consumer IoT security", "dp_iot_security", "📡"),
    )

    private fun workplaceNiches() = listOf(
        Niche(SectorKeys.WORKPLACE_SAFETY, "Machinery safety", "Machinery safety", "ws_machinery", "⚙️"),
        Niche(SectorKeys.WORKPLACE_SAFETY, "PPE at workplace", "PPE at workplace", "ws_ppe", "🦺"),
        Niche(SectorKeys.WORKPLACE_SAFETY, "Chemical risk at work", "Chemical risk at work", "ws_chemical_oel", "☣️"),
        Niche(SectorKeys.WORKPLACE_SAFETY, "Noise & vibration", "Noise & vibration", "ws_noise_vibration", "🔊"),
        Niche(SectorKeys.WORKPLACE_SAFETY, "ATEX / explosive atmospheres", "ATEX / explosive atmospheres", "ws_atex", "💥"),
    )

    private fun constructionNiches() = listOf(
        Niche(SectorKeys.CONSTRUCTION, "CPR & CE marking", "CPR & CE marking", "cpr_ce_marking", "🏗️"),
        Niche(SectorKeys.CONSTRUCTION, "Fire performance", "Fire performance", "cpr_fire", "🔥"),
        Niche(SectorKeys.CONSTRUCTION, "Structural materials", "Structural materials", "cpr_structural", "🧱"),
        Niche(SectorKeys.CONSTRUCTION, "Insulation & energy", "Insulation & energy", "cpr_insulation_energy", "🌡️"),
        Niche(SectorKeys.CONSTRUCTION, "Facades & cladding", "Facades & cladding", "cpr_facades", "🏢"),
    )

    private fun automotiveNiches() = listOf(
        Niche(SectorKeys.AUTOMOTIVE, "Vehicle type approval", "Vehicle type approval", "auto_type_approval", "🚗"),
        Niche(SectorKeys.AUTOMOTIVE, "Emissions & fuel economy", "Emissions & fuel economy", "auto_emissions", "💨"),
        Niche(SectorKeys.AUTOMOTIVE, "ADR dangerous goods", "ADR dangerous goods", "auto_adr", "🛢️"),
        Niche(SectorKeys.AUTOMOTIVE, "Automotive electronics & software", "Automotive electronics & software", "auto_software_un", "💻"),
        Niche(SectorKeys.AUTOMOTIVE, "Two-wheel & L-category", "Two-wheel & L-category", "auto_l_category", "🏍️"),
        Niche(SectorKeys.AUTOMOTIVE, "Autonomous & ADAS", "Autonomous & ADAS", "auto_adas", "🛣️"),
    )

    private fun tobaccoAlcoholNiches() = listOf(
        Niche(SectorKeys.TOBACCO_ALCOHOL, "Tobacco & TPD", "Tobacco & TPD", "tt_tpd", "🚭"),
        Niche(SectorKeys.TOBACCO_ALCOHOL, "Novel tobacco & vaping", "Novel tobacco & vaping", "tt_novel_vaping", "💨"),
        Niche(SectorKeys.TOBACCO_ALCOHOL, "Alcohol labeling", "Alcohol labeling", "tt_alcohol_label", "🍷"),
        Niche(SectorKeys.TOBACCO_ALCOHOL, "Excise & track-and-trace", "Excise & track-and-trace", "tt_excise_trace", "📟"),
    )

    private fun otherNiches() = listOf(
        Niche(SectorKeys.OTHER, "Trade compliance & sanctions", "Trade compliance & sanctions", "other_trade_sanctions", "🌐"),
        Niche(SectorKeys.OTHER, "Export controls & dual-use", "Export controls & dual-use", "other_export_dual_use", "🛂"),
        Niche(SectorKeys.OTHER, "Anti-bribery & ethics", "Anti-bribery & ethics", "other_anti_bribery", "⚖️"),
        Niche(SectorKeys.OTHER, "Multi-site QA systems", "Multi-site QA systems", "other_multi_site_qms", "🏢"),
    )

    fun forSector(sectorKey: String): List<Niche> = all.filter { it.sectorKey == sectorKey }

    fun findByPromptKey(key: String): Niche? = all.firstOrNull { it.promptKey == key }

    fun findByKeyOrName(value: String): Niche? = all.firstOrNull {
        it.promptKey == value || it.name == value || it.nameEn == value
    }

    fun findByKeyOrNameIgnoreCase(value: String): Niche? = all.firstOrNull {
        it.promptKey.equals(value, ignoreCase = true) ||
            it.name.equals(value, ignoreCase = true) ||
            it.nameEn.equals(value, ignoreCase = true)
    }

    fun searchByName(query: String): List<Niche> {
        if (query.isBlank()) return all
        val q = query.trim().lowercase()
        return all.filter { it.name.lowercase().contains(q) || it.nameEn.lowercase().contains(q) }
    }

    /**
     * Canonical [Niche.promptKey] for filters (calendar, knowledge chips).
     * Empty / [General] → "" (treated as “no niche tag” when a specific chip is selected).
     */
    fun resolvePromptKey(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty() || t.equals("General", ignoreCase = true)) return ""
        return findByPromptKey(t)?.promptKey
            ?: findByKeyOrName(t)?.promptKey
            ?: findByKeyOrNameIgnoreCase(t)?.promptKey
            ?: t
    }
}
