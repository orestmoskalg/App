import Foundation

/// Діапазон дат для календаря (дефолт: -1 рік до +3 років)
struct CalendarDateRange: Codable {
    var fromDate: Date
    var toDate: Date

    static let `default`: CalendarDateRange = {
        let cal = Calendar.current
        let today = Date()
        guard let from = cal.date(byAdding: .year, value: -1, to: today),
              let to = cal.date(byAdding: .year, value: 3, to: today) else {
            return CalendarDateRange(fromDate: today, toDate: today)
        }
        return CalendarDateRange(fromDate: from, toDate: to)
    }()

    var fromDateString: String {
        dateFormatter.string(from: fromDate)
    }

    var toDateString: String {
        dateFormatter.string(from: toDate)
    }

    private var dateFormatter: DateFormatter {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }
}
