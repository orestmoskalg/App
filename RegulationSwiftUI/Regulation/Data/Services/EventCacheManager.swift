import Foundation
import os.log

// MARK: - EventCacheManager

/// Two-tier cache for regulatory events: in-memory + disk (FileManager).
///
/// **Why cache?**
/// - Each API call costs money and takes 5-15 seconds
/// - Regulatory events don't change every minute
/// - TTL of 2 hours means users get instant loads on app re-entry
///
/// **Cache key format:** `events_{nicheHash}_{fromDate}_{toDate}`
final class EventCacheManager {
    static let shared = EventCacheManager()

    private let logger = Logger(subsystem: "com.regulation.assistant", category: "Cache")
    private let config = ConfigManager.shared
    private let fileManager = FileManager.default

    // In-memory cache
    private var memoryCache: [String: CacheEntry] = [:]
    private let lock = NSLock()

    // Disk cache directory
    private lazy var cacheDirectory: URL = {
        let dir = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
            .appendingPathComponent("RegulatoryEvents", isDirectory: true)
        try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }()

    private let encoder: JSONEncoder = {
        let e = JSONEncoder()
        e.dateEncodingStrategy = .iso8601
        return e
    }()

    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }()

    private init() {}

    // MARK: - Cache Entry

    private struct CacheEntry: Codable {
        let events: [RegulatoryEvent]
        let timestamp: Date
        let nicheKeys: [String]
    }

    // MARK: - Public API

    /// Retrieve cached events if they exist and haven't expired.
    func get(nicheKeys: [String], fromDate: Date, toDate: Date) -> [RegulatoryEvent]? {
        let key = cacheKey(nicheKeys: nicheKeys, fromDate: fromDate, toDate: toDate)

        // Check memory first
        lock.lock()
        if let entry = memoryCache[key], !isExpired(entry) {
            lock.unlock()
            logger.debug("Memory cache hit: \(key)")
            return entry.events
        }
        lock.unlock()

        // Check disk
        if let entry = loadFromDisk(key: key), !isExpired(entry) {
            // Promote to memory cache
            lock.lock()
            memoryCache[key] = entry
            lock.unlock()
            logger.debug("Disk cache hit: \(key)")
            return entry.events
        }

        logger.debug("Cache miss: \(key)")
        return nil
    }

    /// Store events in both memory and disk cache.
    func set(events: [RegulatoryEvent], nicheKeys: [String], fromDate: Date, toDate: Date) {
        let key = cacheKey(nicheKeys: nicheKeys, fromDate: fromDate, toDate: toDate)
        let entry = CacheEntry(events: events, timestamp: Date(), nicheKeys: nicheKeys)

        // Write memory
        lock.lock()
        memoryCache[key] = entry
        lock.unlock()

        // Write disk (async, non-blocking)
        Task.detached(priority: .utility) { [self] in
            self.saveToDisk(entry: entry, key: key)
        }

        logger.info("Cached \(events.count) events for key: \(key)")
    }

    /// Invalidate all caches (e.g., when user changes niche selection).
    func invalidateAll() {
        lock.lock()
        memoryCache.removeAll()
        lock.unlock()

        // Clear disk cache
        if let files = try? fileManager.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: nil) {
            for file in files {
                try? fileManager.removeItem(at: file)
            }
        }

        logger.info("All caches invalidated")
    }

    /// Invalidate cache for specific niches
    func invalidate(nicheKeys: [String]) {
        lock.lock()
        let keysToRemove = memoryCache.keys.filter { key in
            nicheKeys.contains(where: { key.contains($0) })
        }
        keysToRemove.forEach { memoryCache.removeValue(forKey: $0) }
        lock.unlock()

        logger.info("Invalidated cache for \(nicheKeys.count) niches")
    }

    // MARK: - Private Helpers

    private func cacheKey(nicheKeys: [String], fromDate: Date, toDate: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd"
        let sorted = nicheKeys.sorted().joined(separator: "_")
        let hash = sorted.hash
        return "events_\(hash)_\(formatter.string(from: fromDate))_\(formatter.string(from: toDate))"
    }

    private func isExpired(_ entry: CacheEntry) -> Bool {
        Date().timeIntervalSince(entry.timestamp) > config.cacheTTL
    }

    private func diskURL(for key: String) -> URL {
        cacheDirectory.appendingPathComponent("\(key).json")
    }

    private func saveToDisk(entry: CacheEntry, key: String) {
        do {
            let data = try encoder.encode(entry)
            try data.write(to: diskURL(for: key), options: .atomic)
        } catch {
            logger.error("Failed to write cache to disk: \(error.localizedDescription)")
        }
    }

    private func loadFromDisk(key: String) -> CacheEntry? {
        let url = diskURL(for: key)
        guard fileManager.fileExists(atPath: url.path) else { return nil }

        do {
            let data = try Data(contentsOf: url)
            return try decoder.decode(CacheEntry.self, from: data)
        } catch {
            logger.error("Failed to read cache from disk: \(error.localizedDescription)")
            try? fileManager.removeItem(at: url) // Clean up corrupt file
            return nil
        }
    }
}
