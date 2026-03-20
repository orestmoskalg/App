import Foundation
import os.log

// MARK: - RegulatoryCalendarService (v2.0)

/// Generates regulatory events via Grok API with niche-aware deep prompts.
///
/// **v2.0 fixes from UX feedback:**
/// - Each niche gets its own `promptContext` → deep, specific events (not generic)
/// - AI returns `confidence` and `source_url` per event
/// - `verificationHint` tells users how to verify each event independently
/// - Batch processing with progressive callbacks for UI
/// - Full JSON parser with 3-level fallback + sanitization
final class RegulatoryCalendarService {
    private let grokApi: GrokApiService
    private let config = ConfigManager.shared
    private let logger = Logger(subsystem: "com.regulation.assistant", category: "CalendarService")

    private let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        return f
    }()

    init(apiKey: String) {
        self.grokApi = GrokApiService(apiKey: apiKey)
    }

    // MARK: - Public API

    /// Fetch events with batch processing and progressive callbacks.
    func fetchEvents(
        nichePrompts: [String],
        niches: [Niche],
        fromDate: Date,
        toDate: Date,
        onBatchComplete: ((_ batchEvents: [RegulatoryEvent], _ progress: Double) -> Void)? = nil
    ) async throws -> [RegulatoryEvent] {

        let batchSize = config.maxNichesPerRequest
        let batches = niches.chunked(into: batchSize)
        var allEvents: [RegulatoryEvent] = []
        var allErrors: [Error] = []

        logger.info("Fetching events: \(niches.count) niches in \(batches.count) batches")

        for (index, batch) in batches.enumerated() {
            try Task.checkCancellation()

            do {
                let events = try await fetchBatch(
                    niches: batch,
                    fromDate: fromDate,
                    toDate: toDate
                )
                allEvents.append(contentsOf: events)

                let progress = Double(index + 1) / Double(batches.count)
                onBatchComplete?(events, progress)

                logger.info("Batch \(index + 1)/\(batches.count): \(events.count) events")
            } catch is CancellationError {
                throw GrokApiError.cancelled
            } catch {
                allErrors.append(error)
                logger.error("Batch \(index + 1) failed: \(error.localizedDescription)")
            }

            if index < batches.count - 1 {
                try await Task.sleep(nanoseconds: 500_000_000)
            }
        }

        if allEvents.isEmpty && !allErrors.isEmpty {
            throw allErrors.first!
        }

        let deduplicated = deduplicateEvents(allEvents)
        let sorted = deduplicated.sorted { $0.date < $1.date }

        logger.info("Total: \(sorted.count) unique events")
        return sorted
    }

    // MARK: - Batch with Deep Niche Context

    private func fetchBatch(
        niches: [Niche],
        fromDate: Date,
        toDate: Date
    ) async throws -> [RegulatoryEvent] {

        let fromStr = dateFormatter.string(from: fromDate)
        let toStr = dateFormatter.string(from: toDate)
        let eventsTarget = config.eventsPerBatch

        // Build niche-specific context block
        let nicheContextBlock = niches.map { niche in
            """
            ### \(niche.promptKey)
            \(niche.promptContext)
            Official sources to reference: \(niche.officialSources.joined(separator: ", "))
            Key authorities: \(niche.keyAuthorities.joined(separator: ", "))
            """
        }.joined(separator: "\n\n")

        let systemPrompt = """
        You are a senior regulatory affairs analyst with 20 years of experience across \
        all regulatory domains globally. Today is March 20, 2026.

        Generate exactly \(eventsTarget) regulatory events for the niches below.
        Date range: \(fromStr) to \(toStr).

        NICHE-SPECIFIC REQUIREMENTS:
        \(nicheContextBlock)

        CRITICAL RULES:
        1. Be SPECIFIC — not "GDPR deadline" but "EDPB Guidelines 01/2026 on AI consent models — adoption deadline"
        2. Include JURISDICTION — which country/authority, not just "EU" for everything
        3. For each event, assess your CONFIDENCE honestly:
           - "verified" = you are certain this is a real, announced event with known dates
           - "high" = based on official announcements but date may shift
           - "projected" = based on regulatory patterns and published timelines
           - "estimated" = your best projection, user should verify
        4. Provide source_url where possible (EUR-Lex, Federal Register, official authority websites)
        5. Provide verification_hint = a 1-sentence instruction on how the user can verify this event
        6. Include the niche_category ("MedTech", "FinTech", "Digital", "Legal", "Industrial")

        Return ONLY a valid JSON array. No markdown, no code fences, no explanation.
        Each event object:
        {
          "title": "Specific descriptive title",
          "date": "YYYY-MM-DD",
          "end_date": "YYYY-MM-DD or null",
          "event_type": "deadline|conference|law_update|enforcement|consultation|guidance|transition_period|reporting_due|audit_window|standard_update|market_surveillance|other",
          "niche": "Exact niche name",
          "niche_category": "MedTech|FinTech|Digital|Legal|Industrial",
          "jurisdiction": {
            "region": "EU|USA|UK|Canada|Australia|Japan|China|Brazil|India|South Korea|UAE|Singapore|Switzerland|Mexico|International",
            "sub_region": "Specific state/country or null",
            "authority": "FDA|EMA|FCA|EDPB|etc. or null"
          },
          "description": "2-3 sentences explaining the event and its regulatory significance",
          "impact": "critical|high|medium|low",
          "confidence": "verified|high|projected|estimated",
          "source_url": "URL to official source or null",
          "source_authority": "Name of the issuing authority",
          "verification_hint": "How to verify: check [specific database/website] for [specific reference]",
          "checklist": ["Specific action item 1", "Action item 2"],
          "risks": ["Risk of non-compliance 1"],
          "cross_niche_impacts": ["Impact on other niche"],
          "related_terms": ["Glossary term 1", "Term 2"],
          "recurring": { "frequency": "quarterly|annually|monthly", "next_occurrence": "YYYY-MM-DD" } or null
        }
        """

        let userPrompt = "Generate the \(eventsTarget) regulatory events now as a JSON array."

        let messages: [[String: String]] = [
            ["role": "system", "content": systemPrompt],
            ["role": "user", "content": userPrompt]
        ]

        let content = try await grokApi.createCompletion(
            messages: messages,
            maxTokens: 16384,
            temperature: 0.0
        )

        return parseEvents(from: content, fromDate: fromDate, toDate: toDate)
    }

    // MARK: - JSON Parsing (complete implementation)

    func parseEvents(from rawJSON: String, fromDate: Date, toDate: Date) -> [RegulatoryEvent] {
        let sanitized = sanitizeJSON(rawJSON)

        // Attempt 1: direct array
        if let data = sanitized.data(using: .utf8),
           let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
            let events = array.compactMap { parseSingleEvent($0, fromDate: fromDate, toDate: toDate) }
            logger.debug("Parsed \(events.count) from \(array.count) JSON objects")
            return events
        }

        // Attempt 2: wrapper object — find first array value
        if let data = sanitized.data(using: .utf8),
           let wrapper = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            for (_, value) in wrapper {
                if let array = value as? [[String: Any]] {
                    let events = array.compactMap { parseSingleEvent($0, fromDate: fromDate, toDate: toDate) }
                    logger.debug("Parsed \(events.count) from wrapper")
                    return events
                }
            }
        }

        // Attempt 3: extract JSON array via bracket matching
        if let extracted = extractJSONArray(from: sanitized),
           let data = extracted.data(using: .utf8),
           let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
            let events = array.compactMap { parseSingleEvent($0, fromDate: fromDate, toDate: toDate) }
            logger.debug("Parsed \(events.count) from extracted array")
            return events
        }

        logger.error("All parsing failed. Raw length: \(rawJSON.count)")
        return []
    }

    private func parseSingleEvent(_ dict: [String: Any], fromDate: Date, toDate: Date) -> RegulatoryEvent? {
        guard let title = dict["title"] as? String, !title.isEmpty,
              let dateStr = dict["date"] as? String,
              let date = dateFormatter.date(from: dateStr) else {
            return nil
        }

        // Date validation with tolerance
        let dayBefore = Calendar.current.date(byAdding: .day, value: -1, to: fromDate)!
        let dayAfter = Calendar.current.date(byAdding: .day, value: 1, to: toDate)!
        guard date >= dayBefore && date <= dayAfter else { return nil }

        let endDate = (dict["end_date"] as? String).flatMap { dateFormatter.date(from: $0) }

        let eventType = (dict["event_type"] as? String)
            .flatMap { RegulatoryEvent.EventType(rawValue: $0) } ?? .other

        let impact = (dict["impact"] as? String)
            .flatMap { RegulatoryEvent.ImpactLevel(rawValue: $0) } ?? .medium

        let confidence = (dict["confidence"] as? String)
            .flatMap { RegulatoryEvent.ConfidenceLevel(rawValue: $0) } ?? .estimated

        // Parse jurisdiction
        let jurisdictionDict = dict["jurisdiction"] as? [String: Any]
        let jurisdiction = RegulatoryEvent.Jurisdiction(
            region: jurisdictionDict?["region"] as? String ?? "International",
            subRegion: jurisdictionDict?["sub_region"] as? String,
            authority: jurisdictionDict?["authority"] as? String
        )

        let niche = dict["niche"] as? String ?? "General"
        let nicheCategory = dict["niche_category"] as? String ?? "Other"
        let description = dict["description"] as? String ?? ""
        let sourceURL = dict["source_url"] as? String
        let sourceAuthority = dict["source_authority"] as? String
        let verificationHint = dict["verification_hint"] as? String
        let checklist = dict["checklist"] as? [String] ?? []
        let risks = dict["risks"] as? [String] ?? []
        let crossNiche = dict["cross_niche_impacts"] as? [String] ?? []
        let relatedTerms = dict["related_terms"] as? [String] ?? []

        // Recurring info
        var recurringInfo: RegulatoryEvent.RecurringInfo? = nil
        if let recDict = dict["recurring"] as? [String: Any],
           let freq = recDict["frequency"] as? String {
            let nextOcc = (recDict["next_occurrence"] as? String).flatMap { dateFormatter.date(from: $0) }
            recurringInfo = RegulatoryEvent.RecurringInfo(frequency: freq, nextOccurrence: nextOcc)
        }

        // Stable ID
        let idSource = "\(title.lowercased().trimmingCharacters(in: .whitespaces))_\(dateStr)"
        let stableID: String = {
            var hash: UInt64 = 5381
            for byte in idSource.utf8 { hash = ((hash &<< 5) &+ hash) &+ UInt64(byte) }
            return String(hash, radix: 16)
        }()

        return RegulatoryEvent(
            id: stableID,
            title: title,
            date: date,
            endDate: endDate,
            eventType: eventType,
            niche: niche,
            nicheCategory: nicheCategory,
            jurisdiction: jurisdiction,
            description: description,
            impact: impact,
            confidence: confidence,
            sourceURL: sourceURL,
            sourceAuthority: sourceAuthority,
            verificationHint: verificationHint,
            checklist: checklist,
            risks: risks,
            crossNicheImpacts: crossNiche,
            relatedTerms: relatedTerms,
            recurringInfo: recurringInfo
        )
    }

    // MARK: - Sanitization

    func sanitizeJSON(_ raw: String) -> String {
        var s = raw.trimmingCharacters(in: .whitespacesAndNewlines)

        if s.hasPrefix("```") {
            if let firstNewline = s.firstIndex(of: "\n") {
                s = String(s[s.index(after: firstNewline)...])
            }
            if s.hasSuffix("```") {
                s = String(s.dropLast(3))
            }
            s = s.trimmingCharacters(in: .whitespacesAndNewlines)
        }

        if let firstBracket = s.firstIndex(where: { $0 == "[" || $0 == "{" }) {
            s = String(s[firstBracket...])
        }

        if let lastBracket = s.lastIndex(where: { $0 == "]" || $0 == "}" }) {
            s = String(s[...lastBracket])
        }

        s = s.replacingOccurrences(of: #",\s*([}\]])"#, with: "$1", options: .regularExpression)

        return s
    }

    private func extractJSONArray(from text: String) -> String? {
        guard let startIdx = text.firstIndex(of: "[") else { return nil }
        var depth = 0
        var endIdx: String.Index?

        for idx in text.indices[startIdx...] {
            switch text[idx] {
            case "[": depth += 1
            case "]":
                depth -= 1
                if depth == 0 { endIdx = idx; break }
            default: break
            }
            if endIdx != nil { break }
        }

        guard let end = endIdx else { return nil }
        return String(text[startIdx...end])
    }

    private func deduplicateEvents(_ events: [RegulatoryEvent]) -> [RegulatoryEvent] {
        var seen = Set<String>()
        return events.filter { event in
            let key = "\(event.title.lowercased())_\(dateFormatter.string(from: event.date))"
            if seen.contains(key) { return false }
            seen.insert(key)
            return true
        }
    }
}

// MARK: - Array Chunking

extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [self] }
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}
