import Foundation

/// Сервіс генерації регуляторних подій через Grok API
final class RegulatoryCalendarService {
    private let grokApi: GrokApiService
    private let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    init(apiKey: String) {
        self.grokApi = GrokApiService(apiKey: apiKey)
    }

    /// Генерує 100-200+ подій для ніш в заданому діапазоні
    func fetchEvents(niches: [String], fromDate: Date, toDate: Date) async throws -> [RegulatoryEvent] {
        let fromStr = dateFormatter.string(from: fromDate)
        let toStr = dateFormatter.string(from: toDate)
        let nichesStr = niches.joined(separator: ", ")

        let systemPrompt = """
        Ти — експерт EU MDR/IVDR на 10 березня 2026. Шукай ВСІ події, дедлайни, зміни, конференції, оновлення в нішах: \(nichesStr).
        Включи ключові: EUDAMED mandatory 28 May 2026, Custom-made Class III 26 May 2026, Legacy transition 2027/2028, IVDR Class D Dec 2027, Class C Dec 2028 тощо.
        Обмеж діапазоном: from \(fromStr) to \(toStr).
        Поверни ТІЛЬКИ великий JSON масив (200+ подій якщо є).
        Будь точним, використовуй офіційні джерела.
        """

        let userPrompt = """
        Ніші: \(nichesStr)
        Діапазон: \(fromStr) — \(toStr)
        Поверни JSON масив подій.
        """

        let messages: [[String: String]] = [
            ["role": "system", "content": systemPrompt],
            ["role": "user", "content": userPrompt]
        ]

        let content = try await grokApi.createCompletion(messages: messages, maxTokens: 8192, temperature: 0.0)

        return parseEvents(from: content, fromDate: fromDate, toDate: toDate)
    }

    private func parseEvents(from jsonString: String, fromDate: Date, toDate: Date) -> [RegulatoryEvent] {
        let sanitized = sanitizeJSON(jsonString)
        guard let data = sanitized.data(using: .utf8),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }

        return raw.compactMap { dict -> RegulatoryEvent? in
            parseSingleEvent(dict, fromDate: fromDate, toDate: toDate)
        }.filter { event in
            event.date >= fromDate && event.date <= toDate
        }.sorted { $0.date < $1.date }
    }

    private func parseSingleEvent(_ dict: [String: Any], fromDate: Date, toDate: Date) -> RegulatoryEvent? {
        guard let title = dict["title"] as? String, !title.isEmpty else { return nil }

        let dateStr = dict["date"] as? String ?? ""
        let date = dateFormatter.date(from: dateStr) ?? Date()

        let desc = dict["description"] as? String ?? ""
        let priorityStr = (dict["priority"] as? String ?? "medium").lowercased()
        let priority: EventPriority = priorityStr == "high" ? .high : (priorityStr == "low" ? .low : .medium)
        let source = dict["source"] as? String ?? ""
        let impact = dict["impact"] as? String ?? ""
        let regulationRef = dict["regulationReference"] as? String ?? ""
        let effort = dict["effortEstimate"] as? String ?? ""
        let niche = dict["niche"] as? String ?? ""

        var officialLink: URL?
        if let linkStr = dict["officialLink"] as? String, let url = URL(string: linkStr) {
            officialLink = url
        }

        var actionChecklist: [String] = []
        if let list = dict["actionChecklist"] as? [String] {
            actionChecklist = list
        }

        var affectedClasses: [String] = []
        if let arr = dict["affectedClasses"] as? [String] {
            affectedClasses = arr
        }

        var resources: [URL] = []
        if let res = dict["resources"] as? [String] {
            resources = res.compactMap { URL(string: $0) }
        }

        let statusStr = (dict["status"] as? String ?? "upcoming").lowercased()
        let status: EventStatus = statusStr == "urgent" ? .urgent : (statusStr == "completed" ? .completed : .upcoming)

        return RegulatoryEvent(
            title: title,
            date: date,
            description: desc,
            priority: priority,
            source: source,
            officialLink: officialLink,
            actionChecklist: actionChecklist,
            impact: impact,
            regulationReference: regulationRef,
            effortEstimate: effort,
            affectedClasses: affectedClasses,
            status: status,
            resources: resources,
            niche: niche
        )
    }

    private func sanitizeJSON(_ s: String) -> String {
        var result = s
        if let start = s.firstIndex(of: "["), let end = s.lastIndex(of: "]") {
            result = String(s[start...end])
        }
        return result
    }
}
