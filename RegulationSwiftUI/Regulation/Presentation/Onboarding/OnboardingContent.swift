import Foundation

// MARK: - OnboardingContent

/// First-launch onboarding steps.
/// Addresses feedback: "No onboarding flow — I had no idea what to do first" (Anastasia)
struct OnboardingStep: Identifiable {
    let id: Int
    let title: String
    let description: String
    let iconName: String
    let actionLabel: String?
}

struct OnboardingContent {

    static let steps: [OnboardingStep] = [
        OnboardingStep(
            id: 1,
            title: "Choose your regulatory world",
            description: "Select the niches you work in — MedTech, FinTech, AI Act, GDPR, ESG, or any combination. We cover 22 regulatory domains across 16 countries.",
            iconName: "square.grid.2x2.fill",
            actionLabel: "Select niches"
        ),
        OnboardingStep(
            id: 2,
            title: "Your regulatory radar",
            description: "Events load automatically — deadlines, law updates, consultations, and enforcement actions. Each event shows impact level, jurisdiction, and confidence rating.",
            iconName: "calendar.badge.clock",
            actionLabel: nil
        ),
        OnboardingStep(
            id: 3,
            title: "Always verify critical dates",
            description: "This is an AI-powered radar, not an official database. Events include source links and verification hints. Always confirm critical compliance dates with the official authority.",
            iconName: "checkmark.shield.fill",
            actionLabel: nil
        ),
        OnboardingStep(
            id: 4,
            title: "Export and share",
            description: "Export events to your calendar (Outlook, Apple Calendar), share with colleagues, or set up push notifications for upcoming deadlines.",
            iconName: "square.and.arrow.up.fill",
            actionLabel: "Get started"
        )
    ]

    /// Quick tips shown as contextual hints in the UI
    static let contextualTips: [String: String] = [
        "niche_selector": "Tip: Select up to 10 niches. Events load in batches — you'll see them appear progressively.",
        "event_confidence": "Events with a shield icon are based on announced regulations. Question marks indicate AI projections — verify before acting.",
        "jurisdiction_filter": "Tap a country flag to filter events by jurisdiction. Useful when you only need EU or US compliance data.",
        "export": "Long-press any event to add it to your calendar, share it, or copy the source link.",
        "glossary": "New to regulatory terms? Tap any highlighted term to see its definition.",
        "data_transparency": "Only your selected niches and date range are sent to the AI. No personal or company data is transmitted."
    ]
}
