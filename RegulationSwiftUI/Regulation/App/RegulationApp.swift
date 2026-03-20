import SwiftUI

@main
struct RegulationApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            CalendarView(viewModel: appDelegate.calendarViewModel)
        }
    }
}
