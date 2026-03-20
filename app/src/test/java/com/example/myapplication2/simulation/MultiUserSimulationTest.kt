package com.example.myapplication2.simulation

import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.core.common.SectorKeys
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Emulates 20 distinct user goals (country × sector × niches) and asserts regulatory routing + filters.
 */
@RunWith(Parameterized::class)
class MultiUserSimulationTest(private val scenario: Scenario) {

    data class Scenario(
        val id: Int,
        val goal: String,
        val profile: UserProfile,
        /** Expected [CountryRegulatoryContext.forCountry].jurisdictionKey */
        val expectedJurisdictionKey: String,
        /** Expected [CountryRegulatoryContext.regulatoryCountryBucket] */
        val expectedBucket: String,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<Scenario>> = listOf(
            s(
                1, "UA RA: SaMD under national + EU alignment",
                UserProfile("RA", SectorKeys.MEDICAL_DEVICES, listOf("Software as Medical Device SaMD AI ML"), emptyList(), "Ukraine 🇺🇦"),
                "ukraine", "ukraine",
            ),
            s(
                2, "PL QA: Food HACCP",
                UserProfile("QA", SectorKeys.FOOD_FEED, listOf("food_haccp_food_safety"), emptyList(), "Poland 🇵🇱"),
                "eu", "poland",
            ),
            s(
                3, "US: IVDR niche (EU rules) in FDA jurisdiction",
                UserProfile("RA", SectorKeys.MEDICAL_DEVICES, listOf("In Vitro Diagnostic Devices IVDR"), emptyList(), "USA 🇺🇸"),
                "usa", "usa",
            ),
            s(
                4, "DE: Pharma GMP",
                UserProfile("QA", SectorKeys.PHARMACEUTICALS, listOf("pharma_gmp_gdp_manufacturing"), emptyList(), "Germany 🇩🇪"),
                "eu", "germany",
            ),
            s(
                5, "FR: Cosmetics PIF",
                UserProfile("RA", SectorKeys.COSMETICS, listOf("cosmetic_pif_safety"), emptyList(), "France 🇫🇷"),
                "eu", "france",
            ),
            s(
                6, "FI: REACH",
                UserProfile("RA", SectorKeys.CHEMICALS, listOf("chem_reach_registration"), emptyList(), "Finland 🇫🇮"),
                "eu", "finland",
            ),
            s(
                7, "NL: Consumer safety",
                UserProfile("Compliance", SectorKeys.CONSUMER_PRODUCTS, listOf("consumer_gpsr_safety"), emptyList(), "Netherlands 🇳🇱"),
                "eu", "netherlands",
            ),
            s(
                8, "Spain (not in UI list): must map to generic EU context",
                UserProfile("RA", SectorKeys.MEDICAL_DEVICES, listOf("Software as Medical Device SaMD AI ML"), emptyList(), "Spain"),
                "eu", "eu",
            ),
            s(
                9, "Other / cross-border",
                UserProfile("Consultant", SectorKeys.OTHER, listOf("other_trade_sanctions"), emptyList(), "Other 🌍"),
                "other", "other",
            ),
            s(
                10, "UA: GDPR / digital",
                UserProfile("DPO", SectorKeys.DIGITAL_PRIVACY, listOf("dp_gdpr_processing"), emptyList(), "Ukraine 🇺🇦"),
                "ukraine", "ukraine",
            ),
            s(
                11, "USA: Automotive type approval",
                UserProfile("RA", SectorKeys.AUTOMOTIVE, listOf("auto_type_approval"), emptyList(), "USA 🇺🇸"),
                "usa", "usa",
            ),
            s(
                12, "FI: Workplace machinery",
                UserProfile("HSE", SectorKeys.WORKPLACE_SAFETY, listOf("ws_machinery"), emptyList(), "Finland 🇫🇮"),
                "eu", "finland",
            ),
            s(
                13, "NL: Agri seeds",
                UserProfile("RA", SectorKeys.AGRI_BIO, listOf("agri_seeds"), emptyList(), "Netherlands 🇳🇱"),
                "eu", "netherlands",
            ),
            s(
                14, "FR: Tobacco TPD",
                UserProfile("RA", SectorKeys.TOBACCO_ALCOHOL, listOf("tt_tpd"), emptyList(), "France 🇫🇷"),
                "eu", "france",
            ),
            s(
                15, "DE: Environment waste",
                UserProfile("EHS", SectorKeys.ENVIRONMENT, listOf("env_waste_circular"), emptyList(), "Germany 🇩🇪"),
                "eu", "germany",
            ),
            s(
                16, "PL: Construction CPR",
                UserProfile("RA", SectorKeys.CONSTRUCTION, listOf("cpr_ce_marking"), emptyList(), "Poland 🇵🇱"),
                "eu", "poland",
            ),
            s(
                17, "UA: Anti-bribery (other sector)",
                UserProfile("Legal", SectorKeys.OTHER, listOf("other_anti_bribery"), emptyList(), "Ukraine 🇺🇦"),
                "ukraine", "ukraine",
            ),
            s(
                18, "United States plain (normalization)",
                UserProfile("RA", SectorKeys.MEDICAL_DEVICES, listOf("Wound Care and Dressings"), emptyList(), "United States"),
                "usa", "usa",
            ),
            s(
                19, "Empty country → EU default context",
                UserProfile("RA", SectorKeys.MEDICAL_DEVICES, listOf("Diagnostic Imaging Equipment"), emptyList(), ""),
                "eu", "eu",
            ),
            s(
                20, "Knowledge filter: chip vs General card",
                UserProfile("QA", SectorKeys.FOOD_FEED, listOf("food_haccp_food_safety"), emptyList(), "Poland 🇵🇱"),
                "eu", "poland",
            ),
        )

        private fun s(
            id: Int,
            goal: String,
            profile: UserProfile,
            jk: String,
            bucket: String,
        ): Array<Scenario> = arrayOf(Scenario(id, goal, profile, jk, bucket))
    }

    @Test
    fun runScenario() {
        val p = scenario.profile
        val ctx = CountryRegulatoryContext.forCountry(p.country)
        assertThat(ctx.jurisdictionKey).isEqualTo(scenario.expectedJurisdictionKey)
        assertThat(CountryRegulatoryContext.regulatoryCountryBucket(p.country)).isEqualTo(scenario.expectedBucket)

        for (niche in p.niches) {
            val pk = NicheCatalog.resolvePromptKey(niche)
            assertThat(pk).isNotEmpty()
        }

        assertThat(NicheCatalog.forSector(p.sector)).isNotEmpty()

        val cardMatching = DashboardCard.create(
            type = CardType.INSIGHT,
            title = "t",
            subtitle = "s",
            body = "b",
            dateMillis = 1L,
            niche = p.niches.firstOrNull() ?: "General",
            priority = Priority.MEDIUM,
            jurisdictionKey = ctx.jurisdictionKey,
        )
        assertThat(CountryRegulatoryContext.knowledgeJurisdictionMatches(cardMatching.jurisdictionKey, p.country)).isTrue()
        assertThat(CountryRegulatoryContext.knowledgeSectorMatches(cardMatching, p)).isTrue()
        assertThat(CountryRegulatoryContext.knowledgeNicheMatches(cardMatching, p, null)).isTrue()

        if (scenario.id == 20) {
            val generalCard = DashboardCard.create(
                type = CardType.INSIGHT,
                title = "g",
                subtitle = "s",
                body = "b",
                dateMillis = 1L,
                niche = "General",
                priority = Priority.MEDIUM,
            )
            assertThat(
                CountryRegulatoryContext.knowledgeNicheMatches(generalCard, p, "food_haccp_food_safety"),
            ).isFalse()
        }
    }
}

/** One-off integrity checks (not parameterized). */
class NicheCatalogIntegrityTest {

    @Test
    fun allSectorsHaveNiches_and_promptKeysUnique() {
        for (sector in SectorCatalog.all) {
            assertThat(NicheCatalog.forSector(sector.key)).isNotEmpty()
        }
        assertThat(NicheCatalog.all.map { it.promptKey }.toSet().size).isEqualTo(NicheCatalog.all.size)
    }
}
