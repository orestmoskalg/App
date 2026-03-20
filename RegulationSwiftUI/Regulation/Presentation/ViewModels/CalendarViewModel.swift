import Foundation
import SwiftData
import Combine
import os.log

// MARK: - CalendarViewModel (v3.0)

/// Full ViewModel integrating all v3 features:
/// - Standing Requirements (persistent reference cards)
/// - User Notes (personal annotations via SwiftData)
/// - Regional Packs (curated MENA/APAC events)
/// - All v2 features (filters, export, notifications, glossary)
@MainActor
final class CalendarViewModel: ObservableObject {

    // MARK: - Published State — Events

    @Published var events: [RegulatoryEvent] = []
    @Published var status: FetchStatus = .idle
    @Published var selectedNicheIDs: Set<String> = []
    @Published var searchText: String = ""

    // MARK: - Published State — Filters

    @Published var selectedJurisdictions: Set<String> = []
    @Published var selectedEventTypes: Set<RegulatoryEvent.EventType> = []
    @Published var selectedImpacts: Set<RegulatoryEvent.ImpactLevel> = []
    @Published var showOnlyVerified: Bool = false
    @Published var sortBy: SortOption = .dateAscending

    // MARK: - Published State — Standing Requirements (NEW v3)

    @Published var standingRequirements: [StandingRequirement] = []
    @Published var standingReqFilter: StandingReqFilter = .all

    enum StandingReqFilter: String, CaseIterable {
        case all = "All"
        case continuous = "Continuous"
        case triggered = "When triggered"
        case periodic = "Periodic"
    }

    // MARK: - Published State — User Notes (NEW v3)

    @Published var notesForCurrentEvent: [UserNote] = []
    @Published var allUserNotes: [UserNote] = []
    @Published var noteSearchText: String = ""

    // MARK: - Published State — Regional Packs (NEW v3)

    @Published var activeRegionalPacks: [(region: String, name: String, count: Int)] = []

    // MARK: - Published State — UI

    @Published var fromDate: Date
    @Published var toDate: Date
    @Published var isFirstLaunch: Bool = false
    @Published var showDataTransparencyInfo: Bool = false
    @Published var exportMessage: String?
    @Published var currentTab: AppTab = .calendar

    enum AppTab: String, CaseIterable {
        case calendar = "Calendar"
        case standingReqs = "Requirements"
        case notes = "My notes"
        case glossary = "Glossary"
    }

    // MARK: - Sort Options

    enum SortOption: String, CaseIterable {
        case dateAscending = "Date (earliest)"
        case dateDescending = "Date (latest)"
        case impactHighFirst = "Impact (critical first)"
        case nicheGrouped = "Grouped by niche"
        case confidenceFirst = "Verified first"
    }

    // MARK: - Computed: Filtered Events

    var filteredEvents: [RegulatoryEvent] {
        var result = events

        if !searchText.isEmpty {
            let q = searchText.lowercased()
            result = result.filter {
                $0.title.lowercased().contains(q) ||
                $0.description.lowercased().contains(q) ||
                $0.niche.lowercased().contains(q) ||
                $0.jurisdiction.region.lowercased().contains(q) ||
                ($0.jurisdiction.authority?.lowercased().contains(q) ?? false) ||
                ($0.sourceAuthority?.lowercased().contains(q) ?? false)
            }
        }

        if !selectedJurisdictions.isEmpty {
            result = result.filter { selectedJurisdictions.contains($0.jurisdiction.region) }
        }
        if !selectedEventTypes.isEmpty {
            result = result.filter { selectedEventTypes.contains($0.eventType) }
        }
        if !selectedImpacts.isEmpty {
            result = result.filter { selectedImpacts.contains($0.impact) }
        }
        if showOnlyVerified {
            result = result.filter { $0.confidence == .verified || $0.confidence == .high }
        }

        switch sortBy {
        case .dateAscending: result.sort { $0.date < $1.date }
        case .dateDescending: result.sort { $0.date > $1.date }
        case .impactHighFirst: result.sort { $0.impact.sortOrder < $1.impact.sortOrder }
        case .nicheGrouped: result.sort { $0.niche < $1.niche }
        case .confidenceFirst:
            let order: [RegulatoryEvent.ConfidenceLevel] = [.verified, .high, .projected, .estimated]
            result.sort { order.firstIndex(of: $0.confidence)! < order.firstIndex(of: $1.confidence)! }
        }

        return result
    }

    /// Events grouped by month
    var eventsByMonth: [(month: String, events: [RegulatoryEvent])] {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        let grouped = Dictionary(grouping: filteredEvents) { formatter.string(from: $0.date) }
        return grouped.sorted { lhs, rhs in
            guard let ld = lhs.value.first?.date, let rd = rhs.value.first?.date else { return false }
            return sortBy == .dateDescending ? ld > rd : ld < rd
        }.map { (month: $0.key, events: $0.value) }
    }

    // MARK: - Computed: Filtered Standing Requirements

    var filteredStandingRequirements: [StandingRequirement] {
        var result = standingRequirements

        // Filter by urgency type
        switch standingReqFilter {
        case .all: break
        case .continuous: result = result.filter { $0.urgency == .continuous }
        case .triggered: result = result.filter { $0.urgency == .triggered }
        case .periodic: result = result.filter { $0.urgency == .periodic }
        }

        // Filter by jurisdiction if active
        if !selectedJurisdictions.isEmpty {
            result = result.filter { req in
                !Set(req.jurisdictions).isDisjoint(with: selectedJurisdictions)
            }
        }

        // Search
        if !searchText.isEmpty {
            let q = searchText.lowercased()
            result = result.filter {
                $0.title.lowercased().contains(q) ||
                $0.obligation.lowercased().contains(q) ||
                $0.legalBasis.lowercased().contains(q)
            }
        }

        return result
    }

    // MARK: - Computed: Stats

    var stats: EventStats {
        let all = filteredEvents
        let curatedCount = all.filter { $0.id.hasPrefix("curated_") }.count
        return EventStats(
            total: all.count,
            critical: all.filter { $0.impact == .critical }.count,
            upcoming30d: all.filter {
                $0.date > Date() && $0.date < Calendar.current.date(byAdding: .day, value: 30, to: Date())!
            }.count,
            verified: all.filter { $0.confidence == .verified || $0.confidence == .high }.count,
            curatedRegional: curatedCount,
            standingReqCount: filteredStandingRequirements.count,
            userNotesCount: allUserNotes.count,
            jurisdictions: Set(all.map { $0.jurisdiction.region }).count,
            niches: Set(all.map { $0.niche }).count
        )
    }

    struct EventStats {
        let total: Int
        let critical: Int
        let upcoming30d: Int
        let verified: Int
        let curatedRegional: Int
        let standingReqCount: Int
        let userNotesCount: Int
        let jurisdictions: Int
        let niches: Int
    }

    var availableJurisdictions: [String] {
        Array(Set(events.map { $0.jurisdiction.region })).sorted()
    }

    var activeFilterCount: Int {
        var c = 0
        if !selectedJurisdictions.isEmpty { c += 1 }
        if !selectedEventTypes.isEmpty { c += 1 }
        if !selectedImpacts.isEmpty { c += 1 }
        if showOnlyVerified { c += 1 }
        return c
    }

    // MARK: - Dependencies

    private let repository = RegulatoryRepository.shared
    private let exportService = EventExportService.shared
    private let notificationService = NotificationService.shared
    private let config = ConfigManager.shared
    private let logger = Logger(subsystem: "com.regulation.assistant", category: "CalendarVM")
    private var noteManager: UserNoteManager?
    private let maxSelectedNiches = 10

    private var debounceTask: Task<Void, Never>?
    private let debounceDelay: UInt64 = 1_500_000_000

    private let nicheSelectionKey = "selectedNicheIDs_v3"
    private let firstLaunchKey = "hasLaunchedBefore_v3"

    // MARK: - Init

    init() {
        let months = config.defaultDateRangeMonths
        let now = Date()
        self.fromDate = now
        self.toDate = Calendar.current.date(byAdding: .month, value: months, to: now) ?? now

        if let saved = UserDefaults.standard.array(forKey: nicheSelectionKey) as? [String] {
            self.selectedNicheIDs = Set(saved)
        }
        if !UserDefaults.standard.bool(forKey: firstLaunchKey) {
            self.isFirstLaunch = true
        }

        activeRegionalPacks = repository.availableRegionalPacks
    }

    func completeOnboarding() {
        UserDefaults.standard.set(true, forKey: firstLaunchKey)
        isFirstLaunch = false
    }

    /// Must be called after init with SwiftData modelContext
    func configureNotes(modelContext: ModelContext) {
        self.noteManager = UserNoteManager(modelContext: modelContext)
        refreshAllNotes()
    }

    // MARK: - Niche Selection

    func toggleNiche(_ niche: Niche) {
        if selectedNicheIDs.contains(niche.id) {
            selectedNicheIDs.remove(niche.id)
        } else if selectedNicheIDs.count < maxSelectedNiches {
            selectedNicheIDs.insert(niche.id)
        } else { return }
        persistNiches()
        updateStandingRequirements()
        debouncedFetch()
    }

    func toggleCategory(_ category: String) {
        let niches = Niche.all.filter { $0.category == category }
        let allSelected = niches.allSatisfy { selectedNicheIDs.contains($0.id) }
        if allSelected {
            niches.forEach { selectedNicheIDs.remove($0.id) }
        } else {
            for n in niches { guard selectedNicheIDs.count < maxSelectedNiches else { break }; selectedNicheIDs.insert(n.id) }
        }
        persistNiches()
        updateStandingRequirements()
        debouncedFetch()
    }

    func isNicheSelected(_ niche: Niche) -> Bool { selectedNicheIDs.contains(niche.id) }

    // MARK: - Fetching

    func fetchEvents(forceRefresh: Bool = false) {
        guard !selectedNicheIDs.isEmpty else {
            events = []
            status = .idle
            return
        }
        Task {
            status = .loading(progress: 0, message: "Connecting...")
            do {
                let result = try await repository.fetchEvents(
                    nicheIDs: Array(selectedNicheIDs),
                    fromDate: fromDate, toDate: toDate,
                    forceRefresh: forceRefresh,
                    onProgress: { [weak self] partial, progress, message in
                        Task { @MainActor in
                            self?.events = partial
                            self?.status = .loading(progress: progress, message: message)
                        }
                    }
                )
                events = result
                let verified = result.filter { $0.confidence == .verified || $0.confidence == .high }.count
                let curated = result.filter { $0.id.hasPrefix("curated_") }.count
                status = result.isEmpty ? .idle : .loaded(eventCount: result.count, verifiedCount: verified, curatedCount: curated)

                let important = result.filter { $0.impact == .critical || $0.impact == .high }
                await notificationService.scheduleAll(events: important)
            } catch is CancellationError {
                logger.info("Cancelled")
            } catch {
                status = .error(message: error.localizedDescription)
            }
        }
    }

    func refresh() { fetchEvents(forceRefresh: true) }
    func onDisappear() { repository.cancelFetch(); debounceTask?.cancel() }

    // MARK: - Standing Requirements

    private func updateStandingRequirements() {
        standingRequirements = repository.standingRequirements(
            for: Array(selectedNicheIDs),
            jurisdictions: selectedJurisdictions
        )
    }

    // MARK: - User Notes (NEW v3)

    func addNote(to event: RegulatoryEvent, text: String, tags: [String] = []) {
        guard let nm = noteManager else { return }
        _ = nm.addNote(eventID: event.id, eventTitle: event.title, text: text, tags: tags)
        refreshNotesForEvent(event.id)
        refreshAllNotes()
    }

    func updateNote(_ note: UserNote, text: String? = nil, status: UserNote.NoteStatus? = nil, tags: [String]? = nil, isPinned: Bool? = nil) {
        guard let nm = noteManager else { return }
        nm.updateNote(note, text: text, tags: tags, status: status, isPinned: isPinned)
        refreshAllNotes()
    }

    func deleteNote(_ note: UserNote) {
        guard let nm = noteManager else { return }
        nm.deleteNote(note)
        refreshAllNotes()
    }

    func loadNotes(for eventID: String) {
        notesForCurrentEvent = noteManager?.notes(for: eventID) ?? []
    }

    func hasNotes(for eventID: String) -> Bool {
        noteManager?.hasNotes(for: eventID) ?? false
    }

    func noteCount(for eventID: String) -> Int {
        noteManager?.noteCount(for: eventID) ?? 0
    }

    private func refreshNotesForEvent(_ eventID: String) {
        notesForCurrentEvent = noteManager?.notes(for: eventID) ?? []
    }

    private func refreshAllNotes() {
        if noteSearchText.isEmpty {
            allUserNotes = noteManager?.allNotes() ?? []
        } else {
            allUserNotes = noteManager?.searchNotes(query: noteSearchText) ?? []
        }
    }

    func searchNotes() { refreshAllNotes() }

    var allNoteTags: [String] { noteManager?.allTags() ?? [] }

    var pinnedNotes: [UserNote] { noteManager?.pinnedNotes() ?? [] }

    // MARK: - Filters

    func toggleJurisdiction(_ j: String) {
        if selectedJurisdictions.contains(j) { selectedJurisdictions.remove(j) } else { selectedJurisdictions.insert(j) }
        updateStandingRequirements()
    }
    func toggleEventType(_ t: RegulatoryEvent.EventType) {
        if selectedEventTypes.contains(t) { selectedEventTypes.remove(t) } else { selectedEventTypes.insert(t) }
    }
    func toggleImpact(_ i: RegulatoryEvent.ImpactLevel) {
        if selectedImpacts.contains(i) { selectedImpacts.remove(i) } else { selectedImpacts.insert(i) }
    }
    func clearAllFilters() {
        searchText = ""; selectedJurisdictions.removeAll(); selectedEventTypes.removeAll()
        selectedImpacts.removeAll(); showOnlyVerified = false; sortBy = .dateAscending
        updateStandingRequirements()
    }

    // MARK: - Export

    func exportToCalendar() {
        Task {
            let future = filteredEvents.filter { $0.date > Date() }
            guard !future.isEmpty else { exportMessage = "No future events to export."; return }
            do {
                let r = try await exportService.addMultipleToCalendar(future)
                exportMessage = "Added \(r.added) events" + (r.failed > 0 ? " (\(r.failed) failed)" : "")
            } catch { exportMessage = error.localizedDescription }
        }
    }
    func generateICSFile() -> URL? { exportService.saveICSFile(events: filteredEvents.filter { $0.date > Date() }) }
    func shareText(for event: RegulatoryEvent) -> String { exportService.shareText(for: event) }

    // MARK: - Glossary

    func glossaryDefinition(for term: String) -> GlossaryTerm? { RegulatoryGlossary.definition(for: term) }
    var relevantGlossaryTerms: [GlossaryTerm] {
        let cats = Set(selectedNicheIDs.compactMap { Niche.byID[$0]?.category })
        return cats.flatMap { RegulatoryGlossary.terms(for: $0) }.uniqued(by: \.term).sorted { $0.term < $1.term }
    }

    // MARK: - Data Transparency

    var dataTransparencyInfo: String {
        """
        This app uses a hybrid data model:
        
        1. AI-generated events (Grok API) — marked with confidence badges. \
        Only your selected niches and date range are sent. No personal data is transmitted.
        
        2. Curated regional events (MENA, South Asia, East Asia) — hand-verified by the Regulatory \
        Assistant team. Always marked as "Verified."
        
        3. Standing requirements — manually curated from official legal texts. Not AI-generated.
        
        4. Your personal notes — stored locally on your device via SwiftData. Never sent anywhere.
        
        Always verify critical compliance dates using the official source links provided with each event.
        """
    }

    // MARK: - Date Range

    func updateDateRange(from: Date, to: Date) {
        guard from != fromDate || to != toDate else { return }
        fromDate = from; toDate = to; debouncedFetch()
    }

    // MARK: - Private

    private func debouncedFetch() {
        debounceTask?.cancel()
        debounceTask = Task { [weak self] in
            do { try await Task.sleep(nanoseconds: self?.debounceDelay ?? 1_500_000_000); self?.fetchEvents() } catch {}
        }
    }
    private func persistNiches() { UserDefaults.standard.set(Array(selectedNicheIDs), forKey: nicheSelectionKey) }
}

// MARK: - Array Unique

extension Sequence {
    func uniqued<T: Hashable>(by keyPath: KeyPath<Element, T>) -> [Element] {
        var seen = Set<T>()
        return filter { seen.insert($0[keyPath: keyPath]).inserted }
    }
}
