import SwiftUI
import BackgroundTasks

class AppDelegate: NSObject, UIApplicationDelegate {
    let calendarViewModel: CalendarViewModel

    override init() {
        let apiKey = ProcessInfo.processInfo.environment["GROK_API_KEY"] ?? ""
        let calendarService = RegulatoryCalendarService(apiKey: apiKey)
        let notificationService = NotificationService()
        self.calendarViewModel = CalendarViewModel(
            calendarService: calendarService,
            notificationService: notificationService
        )
        super.init()
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        registerBackgroundTasks()
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        scheduleNextRefresh()
    }

    private func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.regulation.dailyrefresh",
            using: nil
        ) { task in
            self.handleBackgroundRefresh(task: task as! BGAppRefreshTask)
        }
    }

    private func handleBackgroundRefresh(task: BGAppRefreshTask) {
        scheduleNextRefresh()

        Task { @MainActor in
            await calendarViewModel.refreshCalendar()
            task.setTaskCompleted(success: true)
        }
    }

    func scheduleNextRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "com.regulation.dailyrefresh")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 24 * 60 * 60) // 24 години

        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("BGTaskScheduler error: \(error)")
        }
    }
}
