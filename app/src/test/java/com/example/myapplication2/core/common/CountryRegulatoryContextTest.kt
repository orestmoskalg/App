package com.example.myapplication2.core.common

import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.domain.model.UserProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountryRegulatoryContextTest {

    @Test
    fun forCountry_matchesUiLabelsWithEmoji() {
        assertThat(CountryRegulatoryContext.forCountry("Ukraine 🇺🇦").jurisdictionKey).isEqualTo("ukraine")
        assertThat(CountryRegulatoryContext.forCountry("USA 🇺🇸").jurisdictionKey).isEqualTo("usa")
        assertThat(CountryRegulatoryContext.forCountry("Poland 🇵🇱").jurisdictionName).contains("Poland")
    }

    @Test
    fun forCountry_stripsEmojiWithoutSpace() {
        assertThat(CountryRegulatoryContext.forCountry("Ukraine🇺🇦").jurisdictionKey).isEqualTo("ukraine")
        assertThat(CountryRegulatoryContext.forCountry("USA🇺🇸").jurisdictionKey).isEqualTo("usa")
    }

    @Test
    fun forCountry_plainEnglishNames() {
        assertThat(CountryRegulatoryContext.forCountry("Ukraine").jurisdictionKey).isEqualTo("ukraine")
        assertThat(CountryRegulatoryContext.forCountry("United States").jurisdictionKey).isEqualTo("usa")
    }

    @Test
    fun regulatoryAuthorityCitationHint_usaExcludesEu() {
        val ctx = CountryRegulatoryContext.forCountry("USA 🇺🇸")
        val hint = CountryRegulatoryContext.regulatoryAuthorityCitationHint(ctx)
        assertThat(hint).contains("FDA")
        assertThat(hint).doesNotContain("MDCG")
    }

    @Test
    fun regulatoryAuthorityCitationHint_polandMentionsUrpl() {
        val ctx = CountryRegulatoryContext.forCountry("Poland 🇵🇱")
        val hint = CountryRegulatoryContext.regulatoryAuthorityCitationHint(ctx)
        assertThat(hint).contains("URPL")
    }

    @Test
    fun knowledgeSectorMatches_generalNicheIsNotMedicalOnly() {
        val card = DashboardCard.create(
            type = CardType.INSIGHT,
            title = "t",
            subtitle = "s",
            body = "b",
            dateMillis = 1000L,
            niche = "General",
            priority = Priority.MEDIUM,
        )
        val medical = UserProfile(
            role = "RA",
            sector = SectorKeys.MEDICAL_DEVICES,
            niches = listOf("Software as Medical Device SaMD AI ML"),
            country = "Ukraine 🇺🇦",
        )
        val food = UserProfile(
            role = "RA",
            sector = SectorKeys.FOOD_FEED,
            niches = listOf("food_haccp_food_safety"),
            country = "Ukraine 🇺🇦",
        )
        assertThat(CountryRegulatoryContext.knowledgeSectorMatches(card, medical)).isTrue()
        assertThat(CountryRegulatoryContext.knowledgeSectorMatches(card, food)).isTrue()
    }

    @Test
    fun knowledgeSectorMatches_promptKeyAlignedWithProfileSector() {
        val card = DashboardCard.create(
            type = CardType.INSIGHT,
            title = "t",
            subtitle = "s",
            body = "b",
            dateMillis = 1000L,
            niche = "food_haccp_food_safety",
            priority = Priority.MEDIUM,
        )
        val food = UserProfile(
            role = "RA",
            sector = SectorKeys.FOOD_FEED,
            niches = listOf("food_haccp_food_safety"),
            country = "Poland 🇵🇱",
        )
        val medical = UserProfile(
            role = "RA",
            sector = SectorKeys.MEDICAL_DEVICES,
            niches = listOf("Software as Medical Device SaMD AI ML"),
            country = "Poland 🇵🇱",
        )
        assertThat(CountryRegulatoryContext.knowledgeSectorMatches(card, food)).isTrue()
        assertThat(CountryRegulatoryContext.knowledgeSectorMatches(card, medical)).isFalse()
    }
}
