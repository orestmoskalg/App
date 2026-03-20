import Foundation
import os.log

// MARK: - Config Models

/// Decoded representation of RegulationConfig.json
struct RegulationConfig: Codable {
    let version: String
    let eventsPerNicheBatch: Int
    let maxNichesPerRequest: Int
    let apiTimeoutSeconds: Int
    let maxRetries: Int
    let cacheTTLMinutes: Int
    let niches: [NicheConfig]
    let countries: [String]
    let defaultDateRangeMonths: Int
}

struct NicheConfig: Codable, Identifiable {
    let key: String
    let prompt: String
    let display: String
    let category: String

    var id: String { key }
}

// MARK: - ConfigManager

/// Singleton that loads RegulationConfig.json once at app startup.
/// Thread-safe, provides typed access to all config values.
final class ConfigManager {
    static let shared = ConfigManager()

    private let logger = Logger(subsystem: "com.regulation.assistant", category: "Config")
    private var _config: RegulationConfig?
    private let lock = NSLock()

    /// The loaded config. Falls back to hardcoded defaults if JSON fails.
    var config: RegulationConfig {
        lock.lock()
        defer { lock.unlock() }

        if let existing = _config {
            return existing
        }

        let loaded = loadConfig()
        _config = loaded
        return loaded
    }

    private init() {}

    // MARK: - Convenience Accessors

    var niches: [NicheConfig] { config.niches }
    var countries: [String] { config.countries }
    var eventsPerBatch: Int { config.eventsPerNicheBatch }
    var maxNichesPerRequest: Int { config.maxNichesPerRequest }
    var apiTimeout: TimeInterval { TimeInterval(config.apiTimeoutSeconds) }
    var maxRetries: Int { config.maxRetries }
    var cacheTTL: TimeInterval { TimeInterval(config.cacheTTLMinutes * 60) }
    var defaultDateRangeMonths: Int { config.defaultDateRangeMonths }

    /// Grouped niches by category for UI display
    var nichesByCategory: [(category: String, niches: [NicheConfig])] {
        let grouped = Dictionary(grouping: config.niches, by: { $0.category })
        let order = ["MedTech", "FinTech", "Digital", "Legal", "Industrial"]
        return order.compactMap { cat in
            guard let items = grouped[cat], !items.isEmpty else { return nil }
            return (category: cat, niches: items)
        }
    }

    // MARK: - Loading

    private func loadConfig() -> RegulationConfig {
        guard let url = Bundle.main.url(forResource: "RegulationConfig", withExtension: "json") else {
            logger.warning("RegulationConfig.json not found in bundle — using defaults")
            return Self.fallbackConfig
        }

        do {
            let data = try Data(contentsOf: url)
            let decoded = try JSONDecoder().decode(RegulationConfig.self, from: data)
            logger.info("Config loaded: v\(decoded.version), \(decoded.niches.count) niches")
            return decoded
        } catch {
            logger.error("Failed to decode RegulationConfig.json: \(error.localizedDescription)")
            return Self.fallbackConfig
        }
    }

    /// Hardcoded fallback so the app never crashes if JSON is missing/corrupt
    private static let fallbackConfig = RegulationConfig(
        version: "1.2-fallback",
        eventsPerNicheBatch: 40,
        maxNichesPerRequest: 3,
        apiTimeoutSeconds: 60,
        maxRetries: 3,
        cacheTTLMinutes: 120,
        niches: [
            NicheConfig(key: "medtech_mdr_ivdr", prompt: "MedTech (MDR/IVDR)", display: "MedTech (MDR/IVDR)", category: "MedTech"),
            NicheConfig(key: "ai_act_digital", prompt: "EU AI Act & Digital Services Act", display: "AI Act & Digital Services", category: "Digital"),
            NicheConfig(key: "gdpr_ccpa_privacy", prompt: "GDPR/CCPA Data Privacy", display: "GDPR/CCPA Data Privacy", category: "Digital")
        ],
        countries: ["EU", "UK", "USA"],
        defaultDateRangeMonths: 12
    )

    /// Force reload (e.g., after remote config update)
    func reload() {
        lock.lock()
        defer { lock.unlock() }
        _config = loadConfig()
    }
}
