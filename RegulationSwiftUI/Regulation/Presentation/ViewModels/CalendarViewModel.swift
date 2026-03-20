import Foundation
import Combine

@MainActor
final class CalendarViewModel: ObservableObject {
    // MARK: - State
    @Published var events: [RegulatoryEvent] = []
    @Published var selectedNicheKeys: Set<String> = []
    @Published var dateRange: CalendarDateRange = .default
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var lastRefreshDate: Date?
    @Published var newEventsCount: Int = 0

    // MARK: - Dependencies
    private let calendarService: RegulatoryCalendarService
    private let notificationService: NotificationService
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Persistence Keys
    private let nichesKey = "regulation_calendar_niches"
    private let dateRangeFromKey = "regulation_calendar_from"
    private let dateRangeToKey = "regulation_calendar_to"
    private let lastRefreshKey = "regulation_calendar_last_refresh"
    private let cachedEventsKey = "regulation_calendar_cached_events"

    init(calendarService: RegulatoryCalendarService, notificationService: NotificationService) {
        self.calendarService = calendarService
        self.notificationService = notificationService
        loadPersistedState()
    }

    var sortedEvents: [RegulatoryEvent] {
        events.sorted { $0.date < $1.date }
    }

    var selectedNiches: [Niche] {
        Niche.all.filter { selectedNicheKeys.contains($0.promptKey) }
    }

    var canGenerate: Bool {
        !selectedNicheKeys.isEmpty && !isLoading
    }

    // MARK: - Actions
    func toggleNiche(_ niche: Niche) {
        if selectedNicheKeys.contains(niche.promptKey) {
            selectedNicheKeys.remove(niche.promptKey)
        } else if selectedNicheKeys.count < 5 {
            selectedNicheKeys.insert(niche.promptKey)
        }
        persistNiches()
    }

    func updateDateRange(from: Date, to: Date) {
        dateRange = CalendarDateRange(fromDate: from, toDate: to)
        UserDefaults.standard.set(from.timeIntervalSince1970, forKey: dateRangeFromKey)
        UserDefaults.standard.set(to.timeIntervalSince1970, forKey: dateRangeToKey)
    }

    func refreshCalendar() async {
        guard !selectedNicheKeys.isEmpty else {
            errorMessage = "Оберіть хоча б одну нішу"
            return
        }

        isLoading = true
        errorMessage = nil
        let previousCount = events.count
        let niches = Array(selectedNicheKeys)

        do {
            let newEvents = try await calendarService.fetchEvents(
                niches: niches,
                fromDate: dateRange.fromDate,
                toDate: dateRange.toDate
            )
            events = newEvents
            lastRefreshDate = Date()
            newEventsCount = max(0, newEvents.count - previousCount)

            if newEventsCount > 0 {
                await notificationService.scheduleNewEventsNotification(count: newEventsCount)
            }

            persistEvents()
            persistLastRefresh()
        } catch {
            errorMessage = error.localizedDescription
        }

        isLoading = false
    }

    func scheduleDailyRefresh() {
        notificationService.scheduleDailyBackgroundRefresh()
    }

    // MARK: - Persistence
    private func loadPersistedState() {
        if let data = UserDefaults.standard.data(forKey: nichesKey),
           let arr = try? JSONDecoder().decode([String].self, from: data) {
            selectedNicheKeys = Set(arr)
        }

        let fromTS = UserDefaults.standard.double(forKey: dateRangeFromKey)
        let toTS = UserDefaults.standard.double(forKey: dateRangeToKey)
        if fromTS > 0, toTS > 0 {
            dateRange = CalendarDateRange(
                fromDate: Date(timeIntervalSince1970: fromTS),
                toDate: Date(timeIntervalSince1970: toTS)
            )
        }

        lastRefreshDate = UserDefaults.standard.object(forKey: lastRefreshKey) as? Date

        if let data = UserDefaults.standard.data(forKey: cachedEventsKey),
           let decoded = try? JSONDecoder().decode([RegulatoryEvent].self, from: data) {
            events = decoded
        }
    }

    private func persistNiches() {
        if let data = try? JSONEncoder().encode(Array(selectedNicheKeys)) {
            UserDefaults.standard.set(data, forKey: nichesKey)
        }
    }

    private func persistEvents() {
        if let data = try? JSONEncoder().encode(events) {
            UserDefaults.standard.set(data, forKey: cachedEventsKey)
        }
    }

    private func persistLastRefresh() {
        UserDefaults.standard.set(lastRefreshDate, forKey: lastRefreshKey)
    }
}
