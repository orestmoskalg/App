import Foundation
import UserNotifications

/// Push-сповіщення про нові події
final class NotificationService {
    func requestPermission() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
        } catch {
            return false
        }
    }

    func scheduleNewEventsNotification(count: Int) async {
        let content = UNMutableNotificationContent()
        content.title = "Регуляторний календар"
        content.body = "Знайдено \(count) нових подій у вашому діапазоні"
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: "new_events_\(UUID().uuidString)", content: content, trigger: trigger)
        try? await UNUserNotificationCenter.current().add(request)
    }

    func scheduleEventNotification(niche: String, title: String) async {
        let content = UNMutableNotificationContent()
        content.title = "Нова подія в ніші \(niche)"
        content.body = title
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: "event_\(UUID().uuidString)", content: content, trigger: trigger)
        try? await UNUserNotificationCenter.current().add(request)
    }

    func scheduleDailyBackgroundRefresh() {
        // Використовується з BGTaskScheduler в AppDelegate
    }
}
