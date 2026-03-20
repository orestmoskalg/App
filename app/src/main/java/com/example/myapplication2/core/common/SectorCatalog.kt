package com.example.myapplication2.core.common

/**
 * Regulatory **sector** (sphere): the broad compliance domain — not limited to medical devices.
 * User picks one sector during onboarding; niches are filtered by sector.
 */
data class Sector(
    val key: String,
    val label: String,
    val icon: String,
    /** Short English line for prompts — what this sector covers. */
    val promptSummary: String,
)

object SectorKeys {
    const val MEDICAL_DEVICES = "medical_devices"
    const val PHARMACEUTICALS = "pharmaceuticals"
    const val FOOD_FEED = "food_feed"
    const val COSMETICS = "cosmetics"
    const val CHEMICALS = "chemicals_reach"
    const val CONSUMER_PRODUCTS = "consumer_products"
    const val AGRI_BIO = "agri_biotech"
    const val ENVIRONMENT = "environment_waste"
    const val DIGITAL_PRIVACY = "digital_privacy"
    const val WORKPLACE_SAFETY = "workplace_safety"
    const val CONSTRUCTION = "construction_products"
    const val AUTOMOTIVE = "automotive_transport"
    const val TOBACCO_ALCOHOL = "tobacco_alcohol"
    const val OTHER = "other_general"
}

object SectorCatalog {

    /** Legacy default when migrating old profiles without [UserProfile.sector]. */
    const val DEFAULT_KEY: String = SectorKeys.MEDICAL_DEVICES

    val all: List<Sector> = listOf(
        Sector(
            SectorKeys.MEDICAL_DEVICES,
            "Medical devices & IVD",
            "🏥",
            "Medical devices, IVD, SaMD, digital health — MDR/IVDR/FDA/510(k)/national registration.",
        ),
        Sector(
            SectorKeys.PHARMACEUTICALS,
            "Pharmaceuticals & biologics",
            "💊",
            "Human/veterinary medicines, biologics, ATMPs, GMP, pharmacovigilance, clinical trials.",
        ),
        Sector(
            SectorKeys.FOOD_FEED,
            "Food, feed & supplements",
            "🍎",
            "Food/feed safety, FSMA/HACCP, novel food, supplements, contaminants, labeling, import.",
        ),
        Sector(
            SectorKeys.COSMETICS,
            "Cosmetics & personal care",
            "✨",
            "Cosmetic products, safety assessment, labeling, CPNP/notification, claims, nanomaterials.",
        ),
        Sector(
            SectorKeys.CHEMICALS,
            "Chemicals & hazardous substances",
            "⚗️",
            "REACH/CLP, SDS, biocides, pesticides, detergents, workplace chemical safety.",
        ),
        Sector(
            SectorKeys.CONSUMER_PRODUCTS,
            "Consumer & general products",
            "🛒",
            "General product safety, toys, electrical goods, PPE (consumer), recalls, market surveillance.",
        ),
        Sector(
            SectorKeys.AGRI_BIO,
            "Agriculture & biotechnology",
            "🌾",
            "Seeds, plant protection, GMO/NGT, fertilizers, animal health feed chain.",
        ),
        Sector(
            SectorKeys.ENVIRONMENT,
            "Environment & waste",
            "🌍",
            "Waste, packaging EPR, emissions, water, chemicals in environment, circular economy rules.",
        ),
        Sector(
            SectorKeys.DIGITAL_PRIVACY,
            "Digital, AI & privacy",
            "🔐",
            "GDPR, AI Act, cybersecurity, ePrivacy, data transfers, sector-specific IT rules.",
        ),
        Sector(
            SectorKeys.WORKPLACE_SAFETY,
            "Workplace & machinery safety",
            "🦺",
            "Machinery directive, workplace risks, PPE at work, industrial equipment conformity.",
        ),
        Sector(
            SectorKeys.CONSTRUCTION,
            "Construction products",
            "🏗️",
            "CPR, structural materials, fire, CE marking of construction products, national building codes.",
        ),
        Sector(
            SectorKeys.AUTOMOTIVE,
            "Automotive & transport",
            "🚗",
            "Vehicle type approval, ADR dangerous goods, emissions, autonomous driving rules.",
        ),
        Sector(
            SectorKeys.TOBACCO_ALCOHOL,
            "Tobacco, alcohol & excise",
            "🚭",
            "TPD, alcohol labeling, track & trace, excise and licensing.",
        ),
        Sector(
            SectorKeys.OTHER,
            "Other / cross-sector",
            "📚",
            "Cross-cutting compliance, trade sanctions, export controls, multi-sector programs.",
        ),
    )

    fun find(key: String): Sector? = all.firstOrNull { it.key == key }

    fun labelOrKey(key: String): String = find(key)?.label ?: key
}
