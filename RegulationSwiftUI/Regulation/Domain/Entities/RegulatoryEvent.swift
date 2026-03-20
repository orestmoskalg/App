import Foundation

// MARK: - RegulatoryEvent (v2.0)

/// A single regulatory event with verification metadata.
///
/// **v2.0 changes (from UX feedback):**
/// - `sourceURL`: official link for verification (Feedback: Henrik, Markus, Luisa — 9/10 users)
/// - `confidence`: AI self-assessed confidence level (addresses hallucination fear)
/// - `verificationHint`: tells user HOW to verify this event independently
/// - `jurisdiction`: specific country/region for filtering (Feedback: James, Rachel, Chiara)
/// - `recurringInfo`: supports recurring events like quarterly reports (Feedback: Chiara)
/// - `relatedTerms`: links to glossary for juniors (Feedback: David)
struct RegulatoryEvent: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let date: Date
    let endDate: Date?
    let eventType: EventType
    let niche: String
    let nicheCategory: String
    let jurisdiction: Jurisdiction
    let description: String
    let impact: ImpactLevel
    let confidence: ConfidenceLevel
    let sourceURL: String?
    let sourceAuthority: String?
    let verificationHint: String?
    let checklist: [String]
    let risks: [String]
    let crossNicheImpacts: [String]
    let relatedTerms: [String]
    let recurringInfo: RecurringInfo?

    // MARK: - Event Type (universal across all niches)

    enum EventType: String, Codable, CaseIterable {
        case deadline = "deadline"
        case conference = "conference"
        case lawUpdate = "law_update"
        case enforcement = "enforcement"
        case consultation = "consultation"
        case guidance = "guidance"
        case transitionPeriod = "transition_period"
        case reportingDue = "reporting_due"
        case auditWindow = "audit_window"
        case standardUpdate = "standard_update"
        case marketSurveillance = "market_surveillance"
        case other = "other"

        var displayName: String {
            switch self {
            case .deadline: return "Compliance deadline"
            case .conference: return "Conference"
            case .lawUpdate: return "Law/regulation update"
            case .enforcement: return "Enforcement action"
            case .consultation: return "Public consultation"
            case .guidance: return "Guidance published"
            case .transitionPeriod: return "Transition period"
            case .reportingDue: return "Reporting due"
            case .auditWindow: return "Audit window"
            case .standardUpdate: return "Standard update"
            case .marketSurveillance: return "Market surveillance"
            case .other: return "Other"
            }
        }

        var iconName: String {
            switch self {
            case .deadline: return "exclamationmark.triangle.fill"
            case .conference: return "person.3.fill"
            case .lawUpdate: return "doc.text.fill"
            case .enforcement: return "gavel.fill"
            case .consultation: return "bubble.left.and.bubble.right.fill"
            case .guidance: return "book.fill"
            case .transitionPeriod: return "clock.fill"
            case .reportingDue: return "doc.badge.clock.fill"
            case .auditWindow: return "checkmark.shield.fill"
            case .standardUpdate: return "arrow.triangle.2.circlepath"
            case .marketSurveillance: return "magnifyingglass"
            case .other: return "calendar"
            }
        }
    }

    // MARK: - Impact Level

    enum ImpactLevel: String, Codable, CaseIterable {
        case critical = "critical"
        case high = "high"
        case medium = "medium"
        case low = "low"

        var displayName: String { rawValue.capitalized }

        var colorName: String {
            switch self {
            case .critical: return "red"
            case .high: return "orange"
            case .medium: return "yellow"
            case .low: return "green"
            }
        }

        var sortOrder: Int {
            switch self {
            case .critical: return 0
            case .high: return 1
            case .medium: return 2
            case .low: return 3
            }
        }
    }

    // MARK: - Confidence Level (AI self-assessment)

    /// How confident the AI is about this event's accuracy.
    /// Displayed as a badge so users know what to verify.
    enum ConfidenceLevel: String, Codable, CaseIterable {
        case verified = "verified"       // Cross-referenced with known regulation
        case high = "high"               // Based on official announcement
        case projected = "projected"     // Based on regulatory patterns
        case estimated = "estimated"     // AI's best guess

        var displayName: String {
            switch self {
            case .verified: return "Verified"
            case .high: return "High confidence"
            case .projected: return "Projected"
            case .estimated: return "Estimated"
            }
        }

        var iconName: String {
            switch self {
            case .verified: return "checkmark.seal.fill"
            case .high: return "checkmark.circle.fill"
            case .projected: return "arrow.right.circle.fill"
            case .estimated: return "questionmark.circle.fill"
            }
        }
    }

    // MARK: - Jurisdiction

    struct Jurisdiction: Codable, Hashable {
        let region: String      // "EU", "USA", "UK", etc.
        let subRegion: String?  // "California", "Bavaria", specific member state
        let authority: String?  // "FDA", "EMA", "FCA", "BaFin", "EDPB"

        var displayName: String {
            if let sub = subRegion {
                return "\(region) — \(sub)"
            }
            return region
        }

        static let eu = Jurisdiction(region: "EU", subRegion: nil, authority: nil)
        static let usa = Jurisdiction(region: "USA", subRegion: nil, authority: "FDA")
        static let uk = Jurisdiction(region: "UK", subRegion: nil, authority: nil)
    }

    // MARK: - Recurring Info

    struct RecurringInfo: Codable, Hashable {
        let frequency: String   // "quarterly", "annually", "monthly"
        let nextOccurrence: Date?
    }

    // MARK: - Computed Properties

    /// Days until this event (negative = past)
    var daysUntil: Int {
        Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: Date()), to: Calendar.current.startOfDay(for: date)).day ?? 0
    }

    /// Human-readable urgency label
    var urgencyLabel: String? {
        let days = daysUntil
        if days < 0 { return "Past" }
        if days == 0 { return "Today" }
        if days <= 7 { return "This week" }
        if days <= 30 { return "This month" }
        if days <= 90 { return "Next 3 months" }
        return nil
    }

    /// Whether user should be extra cautious about this event's data
    var needsVerification: Bool {
        confidence == .estimated || confidence == .projected
    }
}
