package com.example.myapplication2.core.config

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RegulationConfig(
    val version: String = "1.0",
    @SerialName("eventsPerNicheBatch") val eventsPerNicheBatch: Int = 40,
    @SerialName("maxNichesPerRequest") val maxNichesPerRequest: Int = 3,
    @SerialName("apiTimeoutSeconds") val apiTimeoutSeconds: Int = 60,
    @SerialName("maxRetries") val maxRetries: Int = 3,
    @SerialName("cacheTTLMinutes") val cacheTTLMinutes: Int = 120,
    val countries: List<String> = emptyList(),
    @SerialName("defaultDateRangeMonths") val defaultDateRangeMonths: Int = 12,
    val positioningStatement: String = "",
    val dataTransparencyVersion: String = "",
)

object RegulationConfigLoader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun load(context: Context): RegulationConfig {
        return runCatching {
            context.assets.open("regulation_config.json").use { stream ->
                json.decodeFromString<RegulationConfig>(stream.bufferedReader().readText())
            }
        }.getOrElse { RegulationConfig() }
    }
}
