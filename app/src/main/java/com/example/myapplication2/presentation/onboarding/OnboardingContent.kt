package com.example.myapplication2.presentation.onboarding

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
            description = "Select the niches you work in — MedTech, FinTech, AI Act, GDPR, ESG, or any combination. The app maps them to deep regulatory context for AI prompts.",
            iconName = "grid_view",
            actionLabel = "Select niches",
        ),
        OnboardingStep(
            id = 2,
            title = "Your regulatory radar",
            description = "Events load from the calendar sync — deadlines, guidance updates, and milestones. Each event shows priority, jurisdiction, and prep links where available.",
            iconName = "radar",
            actionLabel = null,
        ),
        OnboardingStep(
            id = 3,
            title = "Always verify critical dates",
            description = "This is an AI-powered radar, not an official database. Use source links and your own compliance process. Confirm critical dates with the competent authority.",
            iconName = "verified_user",
            actionLabel = null,
        ),
        OnboardingStep(
            id = 4,
            title = "Export, share, and stay notified",
            description = "Add events to your device calendar, share text with colleagues, keep notes in-app, and use notifications for upcoming deadlines (where enabled).",
            iconName = "share",
            actionLabel = "Get started",
        ),
    )

    val contextualTips = mapOf(
        "niche_selector" to "Tip: Narrow events with niche chips on the calendar. Profile niches define the default scope.",
        "event_confidence" to "Use confidence labels and links to judge how much to rely on a given date.",
        "export" to "Use Share or Add to calendar on an event card for handoff to your team.",
        "glossary" to "New to a term? Search the Glossary tab for short definitions.",
        "data_transparency" to "Only your selected niches and jurisdiction context are sent to the AI for generation.",
        "standing_rules" to "Standing requirements are ongoing obligations — not tied to a single calendar date.",
        "user_notes" to "Notes are stored on-device (Room) and tied to events.",
    )
}
