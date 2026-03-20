package com.example.myapplication2.presentation.onboarding

/**
 * Onboarding copy synced with Desktop `files (2)/cursor-package` V2.
 * Contextual tips adjusted for this app (event cards, Settings/Profile/Dashboard glossary entry).
 */
data class OnboardingStep(
    val id: Int,
    val title: String,
    val description: String,
    val iconName: String,
    val actionLabel: String?,
)

object OnboardingContent {

    val steps = listOf(
        OnboardingStep(
            id = 1,
            title = "Choose your regulatory world",
            description = "Select the niches you work in — MedTech, FinTech, AI Act, GDPR, ESG, or any combination. We cover 22 regulatory domains across 16 countries.",
            iconName = "grid_view",
            actionLabel = "Select niches",
        ),
        OnboardingStep(
            id = 2,
            title = "Your regulatory radar",
            description = "Events load automatically — deadlines, law updates, consultations, and enforcement actions. Each event shows impact level, jurisdiction, and confidence rating.",
            iconName = "radar",
            actionLabel = null,
        ),
        OnboardingStep(
            id = 3,
            title = "Always verify critical dates",
            description = "This is an AI-powered radar, not an official database. Events include source links and verification hints. Always confirm critical compliance dates with the official authority.",
            iconName = "verified_user",
            actionLabel = null,
        ),
        OnboardingStep(
            id = 4,
            title = "Export, share, and stay notified",
            description = "Export events to your calendar (Google, Outlook), share with colleagues, attach your own notes, and get push notifications for upcoming deadlines.",
            iconName = "share",
            actionLabel = "Get started",
        ),
    )

    val contextualTips = mapOf(
        "niche_selector" to "Tip: Select up to 10 niches. Events load in batches — you'll see them appear progressively.",
        "event_confidence" to "Events with a shield icon are based on announced regulations. Question marks indicate AI projections — verify before acting.",
        "jurisdiction_filter" to "Tap a country to filter events by jurisdiction. Useful when you only need EU or US compliance data.",
        "export" to "Use Share or Add to calendar on an event card; open event detail for more actions.",
        "glossary" to "Regulatory Glossary: Settings, Profile, or Dashboard quick action. Highlighted terms may link to definitions where implemented.",
        "data_transparency" to "Only your selected niches and date range are sent to the AI. No personal or company data is transmitted.",
        "standing_rules" to "Standing requirements show persistent compliance rules — not dates. They apply continuously.",
        "user_notes" to "Tap the note icon on any event to add your own compliance context or action items.",
    )
}
