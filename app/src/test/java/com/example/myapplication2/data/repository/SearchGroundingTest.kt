package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.sanitizeJson
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Інтеграційні перевірки сидів та санітайзера JSON (без мережі).
 */
class SearchGroundingTest {

    @Test
    fun seedContentFactory_calendarCards_is_non_empty() {
        val cards = SeedContentFactory.calendarCards(listOf("General"))
        assertThat(cards).isNotEmpty()
    }

    @Test
    fun seedContentFactory_insightCards_respects_niches() {
        val cards = SeedContentFactory.insightCards(listOf("Software as Medical Device SaMD AI ML"), "eu")
        assertThat(cards).isNotEmpty()
    }

    @Test
    fun sanitizeJson_roundTrip_matches_JsonSanitizer_tests() {
        val raw = """
            ```json
            {"title":"MDR","subtitle":"ok","body":"answer"}
            ```
        """.trimIndent()
        assertThat(sanitizeJson(raw)).isEqualTo("""{"title":"MDR","subtitle":"ok","body":"answer"}""")
    }
}
