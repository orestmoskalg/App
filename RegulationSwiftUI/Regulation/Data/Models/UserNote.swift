import Foundation
import SwiftData

// MARK: - UserNote (SwiftData Model)

/// Personal annotation attached to a regulatory event.
///
/// Addresses Viktor's feedback: "Can't attach personal notes to events —
/// I need to annotate with my own compliance context."
///
/// Design decisions:
/// - SwiftData for persistence (modern, Apple-native, no CoreData boilerplate)
/// - Linked by `eventID` (String) — not a relationship, since events come from API
/// - Supports tags for categorization (e.g., "urgent", "client-X", "audit-prep")
/// - Supports status tracking (not-started → in-progress → done)
@Model
final class UserNote {
    var id: String
    var eventID: String
    var eventTitle: String
    var text: String
    var tags: [String]
    var status: NoteStatus
    var createdAt: Date
    var updatedAt: Date
    var isPinned: Bool
    var reminderDate: Date?

    enum NoteStatus: String, Codable, CaseIterable {
        case notStarted = "not_started"
        case inProgress = "in_progress"
        case done = "done"
        case blocked = "blocked"

        var displayName: String {
            switch self {
            case .notStarted: return "Not started"
            case .inProgress: return "In progress"
            case .done: return "Done"
            case .blocked: return "Blocked"
            }
        }

        var iconName: String {
            switch self {
            case .notStarted: return "circle"
            case .inProgress: return "circle.lefthalf.filled"
            case .done: return "checkmark.circle.fill"
            case .blocked: return "xmark.circle.fill"
            }
        }
    }

    init(
        eventID: String,
        eventTitle: String,
        text: String = "",
        tags: [String] = [],
        status: NoteStatus = .notStarted,
        isPinned: Bool = false,
        reminderDate: Date? = nil
    ) {
        self.id = UUID().uuidString
        self.eventID = eventID
        self.eventTitle = eventTitle
        self.text = text
        self.tags = tags
        self.status = status
        self.createdAt = Date()
        self.updatedAt = Date()
        self.isPinned = isPinned
        self.reminderDate = reminderDate
    }
}

// MARK: - UserNoteManager

/// CRUD operations for UserNote via SwiftData.
/// Injected via SwiftUI environment or passed to ViewModel.
@MainActor
final class UserNoteManager: ObservableObject {

    private let modelContext: ModelContext

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - CRUD

    /// Create a new note for an event
    func addNote(
        eventID: String,
        eventTitle: String,
        text: String,
        tags: [String] = [],
        status: UserNote.NoteStatus = .notStarted
    ) -> UserNote {
        let note = UserNote(
            eventID: eventID,
            eventTitle: eventTitle,
            text: text,
            tags: tags,
            status: status
        )
        modelContext.insert(note)
        try? modelContext.save()
        return note
    }

    /// Update an existing note
    func updateNote(_ note: UserNote, text: String? = nil, tags: [String]? = nil, status: UserNote.NoteStatus? = nil, isPinned: Bool? = nil, reminderDate: Date?? = nil) {
        if let text = text { note.text = text }
        if let tags = tags { note.tags = tags }
        if let status = status { note.status = status }
        if let isPinned = isPinned { note.isPinned = isPinned }
        if let reminderDate = reminderDate { note.reminderDate = reminderDate }
        note.updatedAt = Date()
        try? modelContext.save()
    }

    /// Delete a note
    func deleteNote(_ note: UserNote) {
        modelContext.delete(note)
        try? modelContext.save()
    }

    // MARK: - Queries

    /// Get all notes for a specific event
    func notes(for eventID: String) -> [UserNote] {
        let descriptor = FetchDescriptor<UserNote>(
            predicate: #Predicate { $0.eventID == eventID },
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    /// Get all pinned notes
    func pinnedNotes() -> [UserNote] {
        let descriptor = FetchDescriptor<UserNote>(
            predicate: #Predicate { $0.isPinned == true },
            sortBy: [SortDescriptor(\.updatedAt, order: .reverse)]
        )
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    /// Get notes by status
    func notes(withStatus status: UserNote.NoteStatus) -> [UserNote] {
        let rawStatus = status.rawValue
        let descriptor = FetchDescriptor<UserNote>(
            predicate: #Predicate { $0.status.rawValue == rawStatus },
            sortBy: [SortDescriptor(\.updatedAt, order: .reverse)]
        )
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    /// Get all notes across all events
    func allNotes() -> [UserNote] {
        let descriptor = FetchDescriptor<UserNote>(
            sortBy: [
                SortDescriptor(\.isPinned, order: .reverse),
                SortDescriptor(\.updatedAt, order: .reverse)
            ]
        )
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    /// Search notes by text content
    func searchNotes(query: String) -> [UserNote] {
        guard !query.isEmpty else { return allNotes() }
        let q = query.lowercased()
        let descriptor = FetchDescriptor<UserNote>(
            sortBy: [SortDescriptor(\.updatedAt, order: .reverse)]
        )
        let all = (try? modelContext.fetch(descriptor)) ?? []
        return all.filter {
            $0.text.lowercased().contains(q) ||
            $0.eventTitle.lowercased().contains(q) ||
            $0.tags.contains(where: { $0.lowercased().contains(q) })
        }
    }

    /// Count notes for an event (for badge display)
    func noteCount(for eventID: String) -> Int {
        notes(for: eventID).count
    }

    /// Check if event has any notes
    func hasNotes(for eventID: String) -> Bool {
        noteCount(for: eventID) > 0
    }

    /// Get all unique tags across all notes
    func allTags() -> [String] {
        let all = allNotes()
        return Array(Set(all.flatMap { $0.tags })).sorted()
    }

    /// Notes with upcoming reminders
    func upcomingReminders() -> [UserNote] {
        let now = Date()
        let descriptor = FetchDescriptor<UserNote>(
            sortBy: [SortDescriptor(\.reminderDate)]
        )
        let all = (try? modelContext.fetch(descriptor)) ?? []
        return all.filter { note in
            guard let reminder = note.reminderDate else { return false }
            return reminder > now
        }
    }
}
