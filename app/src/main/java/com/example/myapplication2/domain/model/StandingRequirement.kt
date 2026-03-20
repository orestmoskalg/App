package com.example.myapplication2.domain.model

/**
 * Постійна регуляторна вимога (не одна дата в календарі) — аналог iOS StandingRequirement.
 */
data class StandingRequirement(
    val id: String,
    val title: String,
    val obligation: String,
    val timeframe: String?,
    val legalBasis: String,
    val nicheID: String,
    val nicheCategory: String,
    val jurisdictions: List<String>,
    val authority: String,
    val practicalSteps: List<String>,
    val sourceURL: String?,
    val urgency: Urgency,
) {
    enum class Urgency { Continuous, Triggered, Periodic }

    val timeframeBadge: String
        get() = timeframe ?: if (urgency == Urgency.Continuous) "Ongoing" else "When triggered"
}
