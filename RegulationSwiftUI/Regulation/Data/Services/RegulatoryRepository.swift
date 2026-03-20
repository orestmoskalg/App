import Foundation
import os.log

// MARK: - FetchStatus

enum FetchStatus: Equatable {
    case idle
    case loading(progress: Double, message: String)
    case loaded(eventCount: Int, verifiedCount: Int, curatedCount: Int)
    case error(message: String)
    case partiallyLoaded(eventCount: Int, failedBatches: Int)

    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    static func == (lhs: FetchStatus, rhs: FetchStatus) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle): return true
        case (.loading(let lp, _), .loading(let rp, _)): return lp == rp
        case (.loaded(let lc, _, _), .loaded(let rc, _, _)): return lc == rc
        case (.error(let lm), .error(let rm)): return lm == rm
        case (.partiallyLoaded(let lc, let lf), .partiallyLoaded(let rc, let rf)): return lc == rc && lf == rf
        default: return false
        }
    }
}

// MARK: - RegulatoryRepository (v3.0)

/// Unified data access combining AI events + curated regional packs.
///
/// v3 changes:
/// - Merges AI-generated events with curated regional pack events
/// - Curated events always have `confidence: .verified`
/// - Standing requirements accessible alongside events
final class RegulatoryRepository {
    static let shared = RegulatoryRepository()

    private let cache = EventCacheManager.shared
    private let config = ConfigManager.shared
    private let regionalPacks = RegionalPackManager.shared
    private let logger = Logger(subsystem: "com.regulation.assistant", category: "Repository")

    private var calendarService: RegulatoryCalendarService?
    private var currentTask: Task<[RegulatoryEvent], Error>?

    private init() {}

    func configure(apiKey: String) {
        self.calendarService = RegulatoryCalendarService(apiKey: apiKey)
    }

    // MARK: - Events (AI + Curated merged)

    func fetchEvents(
        nicheIDs: [String],
        fromDate: Date,
        toDate: Date,
        forceRefresh: Bool = false,
        onProgress: ((_ events: [RegulatoryEvent], _ progress: Double, _ message: String) -> Void)? = nil
    ) async throws -> [RegulatoryEvent] {

        currentTask?.cancel()

        let task = Task<[RegulatoryEvent], Error> {
            // Step 1: Check cache
            if !forceRefresh {
                if let cached = cache.get(nicheKeys: nicheIDs, fromDate: fromDate, toDate: toDate) {
                    logger.info("Cache hit: \(cached.count) events")
                    onProgress?(cached, 1.0, "Loaded from cache")
                    return cached
                }
            }

            // Step 2: Fetch AI events
            let niches = nicheIDs.compactMap { Niche.byID[$0] }
            let prompts = niches.map { $0.promptKey }
            guard !niches.isEmpty, let service = calendarService else { return [] }

            var accumulated: [RegulatoryEvent] = []
            let batchSize = config.maxNichesPerRequest
            let totalBatches = (niches.count + batchSize - 1) / batchSize

            let aiEvents = try await service.fetchEvents(
                nichePrompts: prompts,
                niches: niches,
                fromDate: fromDate,
                toDate: toDate,
                onBatchComplete: { batchEvents, progress in
                    accumulated.append(contentsOf: batchEvents)
                    let batchNum = Int(progress * Double(totalBatches))
                    onProgress?(accumulated, progress * 0.9, "AI batch \(batchNum)/\(totalBatches)...")
                }
            )

            // Step 3: Get curated events from regional packs
            onProgress?(aiEvents, 0.95, "Adding verified regional events...")

            let selectedRegions = Set(niches.flatMap { niche -> [String] in
                ConfigManager.shared.countries
            })

            let mergedEvents = regionalPacks.mergeWithAIEvents(
                aiEvents: aiEvents,
                selectedRegions: selectedRegions
            )

            // Filter by date range
            let filtered = mergedEvents.filter { event in
                event.date >= fromDate && event.date <= toDate
            }

            // Step 4: Cache the merged result
            cache.set(events: filtered, nicheKeys: nicheIDs, fromDate: fromDate, toDate: toDate)

            onProgress?(filtered, 1.0, "Complete")
            return filtered
        }

        self.currentTask = task
        return try await task.value
    }

    // MARK: - Standing Requirements

    /// Get standing requirements relevant to selected niches
    func standingRequirements(for nicheIDs: [String]) -> [StandingRequirement] {
        nicheIDs.flatMap { StandingRequirementsCatalog.requirements(for: $0) }
    }

    /// Get standing requirements filtered by jurisdiction
    func standingRequirements(for nicheIDs: [String], jurisdictions: Set<String>) -> [StandingRequirement] {
        let byNiche = standingRequirements(for: nicheIDs)
        if jurisdictions.isEmpty { return byNiche }
        return byNiche.filter { req in
            !Set(req.jurisdictions).isDisjoint(with: jurisdictions)
        }
    }

    // MARK: - Regional Packs Info

    var availableRegionalPacks: [(region: String, name: String, count: Int)] {
        regionalPacks.availableRegions.map { (region: $0.region, name: $0.packName, count: $0.eventCount) }
    }

    // MARK: - Control

    func cancelFetch() {
        currentTask?.cancel()
        currentTask = nil
    }

    func clearCache() {
        cache.invalidateAll()
    }
}
