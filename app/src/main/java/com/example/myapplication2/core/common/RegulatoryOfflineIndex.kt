package com.example.myapplication2.core.common

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bundled **curated** regulatory entry points (titles + HTTPS URLs) — not a full law database.
 * Layers: [CountryRegulatoryContext] country quick links → sector defaults → country/sector overrides
 * → optional niche extras (prompt keys from [NicheCatalog], same strings as stored user niche tags).
 *
 * See [docs/REGULATORY_OFFLINE_DATA.md].
 */
object RegulatoryOfflineIndex {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var root: RegulatoryOfflineIndexRoot? = null

    @Serializable
    data class RegulatoryOfflineIndexRoot(
        val version: Int = 1,
        @SerialName("sectorDefaults") val sectorDefaults: Map<String, SectorOfflineBundles> = emptyMap(),
        @SerialName("byCountry") val byCountry: Map<String, CountryOfflineBundles> = emptyMap(),
        /** Extra links keyed by [Niche.promptKey] (same string as stored in [UserProfile.niches]). */
        @SerialName("nicheExtras") val nicheExtras: Map<String, List<NamedUrl>> = emptyMap(),
    )

    @Serializable
    data class CountryOfflineBundles(
        val sectors: Map<String, SectorOfflineBundles> = emptyMap(),
    )

    @Serializable
    data class SectorOfflineBundles(
        val links: List<NamedUrl> = emptyList(),
        val niches: Map<String, List<NamedUrl>> = emptyMap(),
    )

    @Serializable
    data class NamedUrl(
        val title: String,
        val url: String,
    )

    fun mergedOfficialLinks(
        context: Context,
        country: String,
        sectorKey: String,
        nichePromptKeys: List<String>,
    ): List<Pair<String, String>> {
        ensureLoaded(context)
        val ctx = CountryRegulatoryContext.forCountry(country)
        val r = root ?: return ctx.quickLinks
        val bucket = CountryRegulatoryContext.regulatoryCountryBucket(country)
        val sk = sectorKey.ifBlank { SectorCatalog.DEFAULT_KEY }
        val sectorDef = r.sectorDefaults[sk] ?: SectorOfflineBundles()
        val countrySec = r.byCountry[bucket]?.sectors?.get(sk) ?: SectorOfflineBundles()

        val out = LinkedHashMap<String, Pair<String, String>>()

        fun add(pairs: List<Pair<String, String>>) {
            for ((t, u) in pairs) {
                val url = u.trim()
                if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) continue
                val key = normUrl(url)
                if (key !in out) out[key] = t.trim() to url
            }
        }

        add(ctx.quickLinks)
        add(sectorDef.links.map { it.title to it.url })
        add(countrySec.links.map { it.title to it.url })

        val nicheKeys = nichePromptKeys.distinct().take(12)
        for (pk in nicheKeys) {
            val block = r.nicheExtras[pk].orEmpty() +
                sectorDef.niches[pk].orEmpty() +
                countrySec.niches[pk].orEmpty()
            add(block.map { it.title to it.url })
        }

        return out.values.toList()
    }

    private fun normUrl(u: String): String =
        u.trim().lowercase().trimEnd('/')

    private fun ensureLoaded(context: Context) {
        if (root != null) return
        synchronized(this) {
            if (root != null) return
            root = runCatching {
                context.assets.open("regulatory_offline_index.json").use { stream ->
                    json.decodeFromString<RegulatoryOfflineIndexRoot>(stream.bufferedReader().readText())
                }
            }.getOrElse { RegulatoryOfflineIndexRoot() }
        }
    }
}
