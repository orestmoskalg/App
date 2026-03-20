import Foundation
import UserNotifications
import os.log

// MARK: - NotificationService

/// Schedules local push notifications for regulatory deadlines.
///
/// Addresses feedback:
/// - Chiara: "no way to set recurring reminders — I need 30 days before each quarter end"
/// - Anastasia: "Push notifications for upcoming deadlines"
///
/// Notification tiers:
/// - Critical events: 90d + 30d + 7d + 1d before
/// - High events: 30d + 7d + 1d before
/// - Medium events: 7d + 1d before
/// - Low events: 1d before
final class NotificationService {

    static let shared = NotificationService()

    private let center = UNUserNotificationCenter.current()
    private let logger = Logger(subsystem: "com.regulation.assistant", category: "Notifications")

    private init() {}

    // MARK: - Permission

    func requestPermission() async -> Bool {
        do {
            let granted = try await center.requestAuthorization(options: [.alert, .badge, .sound])
            logger.info("Notification permission: \(granted)")
            return granted
        } catch {
            logger.error("Notification permission error: \(error.localizedDescription)")
            return false
        }
    }

    // MARK: - Schedule for Event

    /// Schedule all notification tiers for a regulatory event
    func scheduleNotifications(for event: RegulatoryEvent) async {
        guard await requestPermission() else { return }

        let leadDays = leadDaysFor(impact: event.impact)

        for days in leadDays {
            guard let fireDate = Calendar.current.date(byAdding: .day, value: -days, to: event.date),
                  fireDate > Date() else {
                continue // Skip past dates
            }

            let content = UNMutableNotificationContent()
            content.title = daysLabel(days)
            content.subtitle = event.title
            content.body = "\(event.niche) — \(event.jurisdiction.displayName)\n\(event.impact.displayName) impact"
            content.sound = event.impact == .critical ? .defaultCritical : .default
            content.categoryIdentifier = "REGULATORY_EVENT"
            content.userInfo = [
                "eventId": event.id,
                "eventTitle": event.title,
                "niche": event.niche,
                "impact": event.impact.rawValue
            ]

            // Badge number = days until
            content.badge = NSNumber(value: days)

            let components = Calendar.current.dateComponents([.year, .month, .day, .hour], from: fireDate)
            var triggerComponents = components
            triggerComponents.hour = 9 // Always fire at 9 AM local time

            let trigger = UNCalendarNotificationTrigger(dateMatching: triggerComponents, repeats: false)
            let identifier = "\(event.id)_\(days)d"

            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

            do {
                try await center.add(request)
                logger.debug("Scheduled: \(event.title) — \(days)d before")
            } catch {
                logger.error("Failed to schedule: \(error.localizedDescription)")
            }
        }
    }

    /// Schedule notifications for multiple events at once
    func scheduleAll(events: [RegulatoryEvent]) async {
        // Only schedule for future events
        let futureEvents = events.filter { $0.date > Date() }

        // Remove existing notifications first
        await removeAll()

        for event in futureEvents.prefix(64) { // iOS limit ~64 pending notifications
            await scheduleNotifications(for: event)
        }

        logger.info("Scheduled notifications for \(min(futureEvents.count, 64)) events")
    }

    /// Remove all scheduled notifications
    func removeAll() async {
        center.removeAllPendingNotificationRequests()
        logger.info("Cleared all pending notifications")
    }

    /// Remove notifications for a specific event
    func removeNotifications(for eventId: String) {
        let pending = ["\(eventId)_90d", "\(eventId)_30d", "\(eventId)_7d", "\(eventId)_1d"]
        center.removePendingNotificationRequests(withIdentifiers: pending)
    }

    // MARK: - Helpers

    private func leadDaysFor(impact: RegulatoryEvent.ImpactLevel) -> [Int] {
        switch impact {
        case .critical: return [90, 30, 7, 1]
        case .high: return [30, 7, 1]
        case .medium: return [7, 1]
        case .low: return [1]
        }
    }

    private func daysLabel(_ days: Int) -> String {
        switch days {
        case 1: return "Tomorrow"
        case 7: return "1 week away"
        case 30: return "30 days away"
        case 90: return "3 months away"
        default: return "\(days) days away"
        }
    }
}
