import Foundation
import EventKit
import os.log

// MARK: - EventExportService

/// Handles exporting regulatory events to:
/// - System calendar (iOS Calendar app / Outlook)
/// - ICS file for sharing
/// - Share sheet for sending events to colleagues
///
/// Addresses feedback from Sarah (QA head): "no way to export events to Outlook"
/// Addresses feedback from David (junior RA): "can't share an event with my manager"
final class EventExportService {

    static let shared = EventExportService()

    private let eventStore = EKEventStore()
    private let logger = Logger(subsystem: "com.regulation.assistant", category: "Export")

    private init() {}

    // MARK: - Calendar Permission

    /// Request access to the system calendar
    func requestCalendarAccess() async -> Bool {
        do {
            if #available(iOS 17.0, *) {
                return try await eventStore.requestFullAccessToEvents()
            } else {
                return try await eventStore.requestAccess(to: .event)
            }
        } catch {
            logger.error("Calendar access denied: \(error.localizedDescription)")
            return false
        }
    }

    // MARK: - Add to System Calendar

    /// Add a single event to the user's default calendar
    func addToCalendar(_ event: RegulatoryEvent) async throws {
        guard await requestCalendarAccess() else {
            throw ExportError.calendarAccessDenied
        }

        let calEvent = EKEvent(eventStore: eventStore)
        calEvent.title = "[\(event.niche)] \(event.title)"
        calEvent.startDate = event.date
        calEvent.endDate = event.endDate ?? Calendar.current.date(byAdding: .hour, value: 1, to: event.date)
        calEvent.isAllDay = event.endDate == nil
        calEvent.notes = buildEventNotes(event)
        calEvent.calendar = eventStore.defaultCalendarForNewEvents

        if let url = event.sourceURL, let eventURL = URL(string: url) {
            calEvent.url = eventURL
        }

        // Add alert 7 days before for critical/high, 3 days for others
        let alertDays = (event.impact == .critical || event.impact == .high) ? -7 : -3
        calEvent.addAlarm(EKAlarm(relativeOffset: TimeInterval(alertDays * 86400)))

        // Add 30-day advance alert for critical events
        if event.impact == .critical {
            calEvent.addAlarm(EKAlarm(relativeOffset: TimeInterval(-30 * 86400)))
        }

        try eventStore.save(calEvent, span: .thisEvent)
        logger.info("Added to calendar: \(event.title)")
    }

    /// Add multiple events to calendar at once
    func addMultipleToCalendar(_ events: [RegulatoryEvent]) async throws -> (added: Int, failed: Int) {
        guard await requestCalendarAccess() else {
            throw ExportError.calendarAccessDenied
        }

        var added = 0
        var failed = 0

        for event in events {
            do {
                try await addToCalendar(event)
                added += 1
            } catch {
                failed += 1
                logger.error("Failed to add: \(event.title) — \(error.localizedDescription)")
            }
        }

        return (added, failed)
    }

    // MARK: - Generate ICS File

    /// Generate an ICS (iCalendar) file for one or more events
    func generateICS(events: [RegulatoryEvent]) -> String {
        var ics = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Regulatory Assistant//EN
        CALSCALE:GREGORIAN
        METHOD:PUBLISH
        X-WR-CALNAME:Regulatory Events
        X-WR-TIMEZONE:UTC\n
        """

        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withYear, .withMonth, .withDay, .withTime, .withTimeZone]

        for event in events {
            let dtStart = formatICSDate(event.date)
            let dtEnd = formatICSDate(event.endDate ?? Calendar.current.date(byAdding: .hour, value: 1, to: event.date)!)
            let description = buildEventNotes(event)
                .replacingOccurrences(of: "\n", with: "\\n")
                .replacingOccurrences(of: ",", with: "\\,")

            ics += """
            BEGIN:VEVENT
            UID:\(event.id)@regulatory-assistant
            DTSTART:\(dtStart)
            DTEND:\(dtEnd)
            SUMMARY:[\(event.niche)] \(escapeICS(event.title))
            DESCRIPTION:\(escapeICS(description))
            CATEGORIES:\(event.niche),\(event.nicheCategory)
            PRIORITY:\(event.impact == .critical ? 1 : event.impact == .high ? 3 : 5)
            STATUS:CONFIRMED\n
            """

            if let url = event.sourceURL {
                ics += "URL:\(url)\n"
            }

            ics += "END:VEVENT\n"
        }

        ics += "END:VCALENDAR"
        return ics
    }

    /// Save ICS to a temporary file and return the URL for sharing
    func saveICSFile(events: [RegulatoryEvent], filename: String = "regulatory_events") -> URL? {
        let ics = generateICS(events: events)
        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent("\(filename).ics")

        do {
            try ics.write(to: fileURL, atomically: true, encoding: .utf8)
            logger.info("ICS file saved: \(fileURL.path)")
            return fileURL
        } catch {
            logger.error("Failed to save ICS: \(error.localizedDescription)")
            return nil
        }
    }

    // MARK: - Share Text

    /// Generate shareable text summary of an event
    func shareText(for event: RegulatoryEvent) -> String {
        var text = """
        \(event.title)
        
        Date: \(formattedDate(event.date))
        Niche: \(event.niche)
        Jurisdiction: \(event.jurisdiction.displayName)
        Impact: \(event.impact.displayName)
        Confidence: \(event.confidence.displayName)
        
        \(event.description)
        """

        if !event.checklist.isEmpty {
            text += "\n\nChecklist:\n"
            text += event.checklist.enumerated().map { "  \($0.offset + 1). \($0.element)" }.joined(separator: "\n")
        }

        if let url = event.sourceURL {
            text += "\n\nSource: \(url)"
        }

        if let hint = event.verificationHint {
            text += "\nVerification: \(hint)"
        }

        text += "\n\n— Shared from Regulatory Assistant"

        return text
    }

    // MARK: - Helpers

    private func buildEventNotes(_ event: RegulatoryEvent) -> String {
        var notes = """
        Niche: \(event.niche)
        Jurisdiction: \(event.jurisdiction.displayName)
        Impact: \(event.impact.displayName)
        Confidence: \(event.confidence.displayName)
        
        \(event.description)
        """

        if !event.checklist.isEmpty {
            notes += "\n\nAction items:\n"
            notes += event.checklist.map { "- \($0)" }.joined(separator: "\n")
        }

        if !event.risks.isEmpty {
            notes += "\n\nRisks:\n"
            notes += event.risks.map { "- \($0)" }.joined(separator: "\n")
        }

        if let hint = event.verificationHint {
            notes += "\n\nHow to verify: \(hint)"
        }

        return notes
    }

    private func formatICSDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd'T'HHmmss'Z'"
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter.string(from: date)
    }

    private func escapeICS(_ text: String) -> String {
        text.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: ";", with: "\\;")
            .replacingOccurrences(of: ",", with: "\\,")
    }

    private func formattedDate(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateStyle = .long
        f.timeStyle = .none
        return f.string(from: date)
    }
}

// MARK: - Export Errors

enum ExportError: LocalizedError {
    case calendarAccessDenied
    case fileCreationFailed

    var errorDescription: String? {
        switch self {
        case .calendarAccessDenied:
            return "Calendar access was denied. Please enable it in Settings > Privacy > Calendars."
        case .fileCreationFailed:
            return "Failed to create export file."
        }
    }
}
