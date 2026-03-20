import SwiftUI

struct CalendarView: View {
    @StateObject var viewModel: CalendarViewModel
    @State private var showNicheSheet = false
    @State private var showDateRangeSheet = false
    @State private var selectedEvent: RegulatoryEvent?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Кнопки дій
                    actionButtons

                    if viewModel.isLoading {
                        ProgressView("Завантаження подій...")
                            .padding()
                    } else if let err = viewModel.errorMessage {
                        errorBanner(err)
                    } else if viewModel.events.isEmpty && !viewModel.isLoading {
                        emptyState
                    } else {
                        eventsList
                    }
                }
                .padding()
            }
            .navigationTitle("Календар")
            .sheet(isPresented: $showNicheSheet) {
                NicheSelectorSheet(viewModel: viewModel)
            }
            .sheet(isPresented: $showDateRangeSheet) {
                DateRangeSettingsView(viewModel: viewModel)
            }
            .sheet(item: $selectedEvent) { event in
                EventDetailView(event: event)
            }
        }
        .onAppear {
            viewModel.scheduleDailyRefresh()
        }
    }

    private var actionButtons: some View {
        VStack(spacing: 12) {
            Button {
                showNicheSheet = true
            } label: {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                    Text("Обрати нішу (\(viewModel.selectedNicheKeys.count)/5)")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.accentColor.opacity(0.15))
                .cornerRadius(12)
            }
            .buttonStyle(.plain)

            Button {
                showDateRangeSheet = true
            } label: {
                HStack {
                    Image(systemName: "calendar")
                    Text("Налаштувати період")
                    Spacer()
                    Text("\(viewModel.dateRange.fromDateString) — \(viewModel.dateRange.toDateString)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.gray.opacity(0.1))
                .cornerRadius(12)
            }
            .buttonStyle(.plain)

            Button {
                Task { await viewModel.refreshCalendar() }
            } label: {
                HStack {
                    if viewModel.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    } else {
                        Image(systemName: "arrow.clockwise")
                    }
                    Text("Оновити зараз")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(viewModel.canGenerate ? Color.accentColor : Color.gray.opacity(0.3))
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .disabled(!viewModel.canGenerate)
            .buttonStyle(.plain)
        }
    }

    private func errorBanner(_ message: String) -> some View {
        Text(message)
            .foregroundColor(.red)
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.red.opacity(0.1))
            .cornerRadius(12)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar.badge.clock")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("Ще немає подій")
                .font(.headline)
            Text("Оберіть ніші та натисніть «Оновити зараз»")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(40)
    }

    private var eventsList: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            Text("Події (\(viewModel.events.count))")
                .font(.headline)

            ForEach(viewModel.sortedEvents) { event in
                EventRowView(event: event)
                    .onTapGesture {
                        selectedEvent = event
                    }
            }
        }
    }
}

struct EventRowView: View {
    let event: RegulatoryEvent

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            dateBadge
            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)
                if !event.description.isEmpty {
                    Text(event.description)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                HStack(spacing: 8) {
                    priorityBadge
                    if event.isUrgent {
                        Text("\(event.daysLeft) днів")
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }

    private var dateBadge: some View {
        VStack(spacing: 0) {
            Text(dayString)
                .font(.caption2)
                .fontWeight(.bold)
            Text(monthString)
                .font(.caption2)
        }
        .frame(width: 44, height: 44)
        .background(event.isUrgent ? Color.red.opacity(0.2) : Color.accentColor.opacity(0.2))
        .cornerRadius(8)
    }

    private var dayString: String {
        let f = DateFormatter()
        f.dateFormat = "d"
        return f.string(from: event.date)
    }

    private var monthString: String {
        let f = DateFormatter()
        f.dateFormat = "MMM"
        f.locale = Locale(identifier: "uk")
        return f.string(from: event.date)
    }

    private var priorityBadge: some View {
        Text(event.priority.rawValue)
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(priorityColor.opacity(0.3))
            .cornerRadius(4)
    }

    private var priorityColor: Color {
        switch event.priority {
        case .high: return .orange
        case .medium: return .blue
        case .low: return .gray
        }
    }
}
