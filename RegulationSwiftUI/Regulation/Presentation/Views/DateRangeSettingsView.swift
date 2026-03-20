import SwiftUI

struct DateRangeSettingsView: View {
    @ObservedObject var viewModel: CalendarViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var fromDate: Date
    @State private var toDate: Date

    init(viewModel: CalendarViewModel) {
        self.viewModel = viewModel
        _fromDate = State(initialValue: viewModel.dateRange.fromDate)
        _toDate = State(initialValue: viewModel.dateRange.toDate)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker("Від (минуле для історії змін)", selection: $fromDate, displayedComponents: .date)
                    DatePicker("До (майбутнє для прогнозів)", selection: $toDate, displayedComponents: .date)
                } header: {
                    Text("Період")
                } footer: {
                    Text("Дефолт: -1 рік до +3 років від сьогодні. Події поза діапазоном не показуються.")
                }

                Section {
                    Button("Скинути до дефолту") {
                        fromDate = CalendarDateRange.default.fromDate
                        toDate = CalendarDateRange.default.toDate
                    }
                }
            }
            .navigationTitle("Налаштувати період")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Зберегти") {
                        viewModel.updateDateRange(from: fromDate, to: toDate)
                        dismiss()
                    }
                }
            }
        }
    }
}
