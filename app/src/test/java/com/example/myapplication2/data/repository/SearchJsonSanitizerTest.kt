package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.sanitizeJson
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchJsonSanitizerTest {

    @Test
    fun sanitizeJson_removes_markdown_fences() {
        val raw = """
            ```json
            {"title":"MDR","subtitle":"ok","body":"answer"}
            ```
        """.trimIndent()

        val sanitized = sanitizeJson(raw)

        assertThat(sanitized).isEqualTo("""{"title":"MDR","subtitle":"ok","body":"answer"}""")
    }

    @Test
    fun sanitizeJson_extracts_object_from_prefixed_text() {
        val raw = """
            Here is the JSON you asked for:
            {"title":"MDR","subtitle":"ok","body":"answer"}
            Thanks.
        """.trimIndent()

        val sanitized = sanitizeJson(raw)

        assertThat(sanitized).isEqualTo("""{"title":"MDR","subtitle":"ok","body":"answer"}""")
    }

    @Test
    fun sanitizeJson_keeps_json_arrays_for_calendar_style_payloads() {
        val raw = """
            ```json
            [{"title":"One"},{"title":"Two"}]
            ```
        """.trimIndent()

        val sanitized = sanitizeJson(raw)

        assertThat(sanitized).isEqualTo("""[{"title":"One"},{"title":"Two"}]""")
    }
}
