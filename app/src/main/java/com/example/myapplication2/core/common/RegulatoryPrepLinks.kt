package com.example.myapplication2.core.common

import com.example.myapplication2.core.model.CardLink
import com.example.myapplication2.core.model.DashboardCard
import kotlin.random.Random

/**
 * Official URLs for preparation (technical documentation, vigilance, conformity).
 * Used to enrich calendar events so each item has actionable links.
 */
object RegulatoryPrepLinks {

    val euCore: List<CardLink> = listOf(
        CardLink("EUR-Lex — MDR 2017/745", "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745", "EU", true),
        CardLink("EUR-Lex — IVDR 2017/746", "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0746", "EU", true),
        CardLink("MDCG endorsed documents", "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en", "EC", true),
        CardLink("EUDAMED", "https://ec.europa.eu/tools/eudamed", "EC", true),
        CardLink("NANDO — Notified Bodies", "https://ec.europa.eu/growth/tools-databases/nando/", "EC", true),
    )

    private fun String.normUrl(): String = trim().lowercase().trimEnd('/')

    /**
     * Merges model-generated links with jurisdiction official resources; caps total count.
     */
    fun mergeIntoCard(card: DashboardCard, ctx: CountryRegulatoryContext.Context): DashboardCard {
        val fromModel = card.links.filter { it.url.trim().startsWith("http", ignoreCase = true) }
        val fromCtx = ctx.quickLinks.map { (title, url) ->
            CardLink(title = title, url = url, sourceLabel = ctx.jurisdictionName, isVerified = true)
        }
        val merged = (fromModel + fromCtx)
            .distinctBy { it.url.normUrl() }
            .take(6)
        return card.copy(links = merged, jurisdictionKey = ctx.jurisdictionKey)
    }

    /**
     * Static / offline seeds: pick a stable subset so lists are not identical on every row.
     */
    fun diversifyForSeed(title: String, niche: String, extra: List<CardLink> = emptyList()): List<CardLink> {
        val r = Random(title.hashCode() + 31 * niche.hashCode())
        val shuffled = euCore.shuffled(r)
        return (extra + shuffled).distinctBy { it.url.normUrl() }.take(5)
    }
}
