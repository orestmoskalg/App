import Foundation

/// Подія регуляторного календаря MDR/IVDR
struct RegulatoryEvent: Identifiable, Codable, Hashable {
    let id: UUID
    var title: String
    var date: Date
    var description: String
    var priority: EventPriority
    var source: String
    var officialLink: URL?
    var actionChecklist: [String]
    var impact: String
    var regulationReference: String
    var effortEstimate: String
    var affectedClasses: [String]
    var status: EventStatus
    var resources: [URL]
    var niche: String

    init(
        id: UUID = UUID(),
        title: String,
        date: Date,
        description: String,
        priority: EventPriority = .medium,
        source: String = "",
        officialLink: URL? = nil,
        actionChecklist: [String] = [],
        impact: String = "",
        regulationReference: String = "",
        effortEstimate: String = "",
        affectedClasses: [String] = [],
        status: EventStatus = .upcoming,
        resources: [URL] = [],
        niche: String = ""
    ) {
        self.id = id
        self.title = title
        self.date = date
        self.description = description
        self.priority = priority
        self.source = source
        self.officialLink = officialLink
        self.actionChecklist = actionChecklist
        self.impact = impact
        self.regulationReference = regulationReference
        self.effortEstimate = effortEstimate
        self.affectedClasses = affectedClasses
        self.status = status
        self.resources = resources
        self.niche = niche
    }

    /// Днів до події
    var daysLeft: Int {
        Calendar.current.dateComponents([.day], from: Date(), to: date).day ?? 0
    }

    var isUrgent: Bool { daysLeft >= 0 && daysLeft <= 7 }
}

enum EventPriority: String, Codable, CaseIterable {
    case high
    case medium
    case low
}

enum EventStatus: String, Codable, CaseIterable {
    case upcoming
    case urgent
    case completed
}
