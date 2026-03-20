import SwiftUI
import SwiftData

// MARK: - App Entry Point (v3.0)

/// Configure SwiftData for UserNote persistence and inject into environment.
///
/// Integration checklist:
/// 1. Replace your existing @main App struct with this
/// 2. Set your Grok API key
/// 3. SwiftData handles schema creation automatically
@main
struct RegulationApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    let modelContainer: ModelContainer

    init() {
        // Configure API
        let grokKey = ProcessInfo.processInfo.environment["GROK_API_KEY"] ?? ""
        RegulatoryRepository.shared.configure(apiKey: grokKey)

        // Configure SwiftData for UserNote
        do {
            let schema = Schema([UserNote.self])
            let config = ModelConfiguration(
                "RegulationAssistant",
                schema: schema,
                isStoredInMemoryOnly: false // Persist to disk
            )
            modelContainer = try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("Failed to configure SwiftData: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .modelContainer(modelContainer)
        }
    }
}

// MARK: - ContentView Integration Example

/// Example showing how to wire up CalendarViewModel with SwiftData
struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @StateObject private var viewModel = CalendarViewModel()

    var body: some View {
        TabView(selection: $viewModel.currentTab) {

            // Tab 1: Calendar (events + filters)
            CalendarTab(viewModel: viewModel)
                .tabItem {
                    Label("Calendar", systemImage: "calendar")
                }
                .tag(CalendarViewModel.AppTab.calendar)

            // Tab 2: Standing Requirements (persistent rules)
            StandingRequirementsTab(viewModel: viewModel)
                .tabItem {
                    Label("Requirements", systemImage: "checkmark.shield")
                }
                .tag(CalendarViewModel.AppTab.standingReqs)

            // Tab 3: My Notes (personal annotations)
            NotesTab(viewModel: viewModel)
                .tabItem {
                    Label("Notes", systemImage: "note.text")
                }
                .tag(CalendarViewModel.AppTab.notes)

            // Tab 4: Glossary
            GlossaryTab(viewModel: viewModel)
                .tabItem {
                    Label("Glossary", systemImage: "book")
                }
                .tag(CalendarViewModel.AppTab.glossary)
        }
        .onAppear {
            // Wire up SwiftData context for notes
            viewModel.configureNotes(modelContext: modelContext)

            // Load events if niches are selected
            if !viewModel.selectedNicheIDs.isEmpty {
                viewModel.fetchEvents()
            }
        }
        .sheet(isPresented: $viewModel.isFirstLaunch) {
            OnboardingView {
                viewModel.completeOnboarding()
            }
        }
    }
}

// MARK: - Tab Placeholders

/// These are structural placeholders — replace with your actual UI implementation.
/// The important part is HOW they connect to the ViewModel.

struct CalendarTab: View {
    @ObservedObject var viewModel: CalendarViewModel

    var body: some View {
        NavigationStack {
            List {
                // Stats header
                Section {
                    HStack {
                        StatBadge(label: "Events", value: "\(viewModel.stats.total)")
                        StatBadge(label: "Critical", value: "\(viewModel.stats.critical)")
                        StatBadge(label: "Verified", value: "\(viewModel.stats.verified)")
                        if viewModel.stats.curatedRegional > 0 {
                            StatBadge(label: "Regional", value: "\(viewModel.stats.curatedRegional)")
                        }
                    }
                }

                // Progress bar when loading
                if case .loading(let progress, let message) = viewModel.status {
                    Section {
                        VStack(alignment: .leading, spacing: 4) {
                            ProgressView(value: progress)
                            Text(message)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                // Events grouped by month
                ForEach(viewModel.eventsByMonth, id: \.month) { section in
                    Section(header: Text(section.month)) {
                        ForEach(section.events) { event in
                            EventRow(event: event, hasNotes: viewModel.hasNotes(for: event.id))
                        }
                    }
                }
            }
            .searchable(text: $viewModel.searchText)
            .refreshable { viewModel.refresh() }
            .navigationTitle("Regulatory radar")
            .onDisappear { viewModel.onDisappear() }
        }
    }
}

struct StandingRequirementsTab: View {
    @ObservedObject var viewModel: CalendarViewModel

    var body: some View {
        NavigationStack {
            List {
                Picker("Filter", selection: $viewModel.standingReqFilter) {
                    ForEach(CalendarViewModel.StandingReqFilter.allCases, id: \.self) {
                        Text($0.rawValue)
                    }
                }
                .pickerStyle(.segmented)

                ForEach(viewModel.filteredStandingRequirements) { req in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(req.title).font(.headline)
                            Spacer()
                            Text(req.timeframeBadge)
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(.orange.opacity(0.15))
                                .clipShape(Capsule())
                        }
                        Text(req.obligation)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(req.legalBasis)
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Standing requirements")
        }
    }
}

struct NotesTab: View {
    @ObservedObject var viewModel: CalendarViewModel

    var body: some View {
        NavigationStack {
            List {
                if !viewModel.pinnedNotes.isEmpty {
                    Section("Pinned") {
                        ForEach(viewModel.pinnedNotes) { note in
                            NoteRow(note: note)
                        }
                    }
                }
                Section("All notes (\(viewModel.allUserNotes.count))") {
                    ForEach(viewModel.allUserNotes) { note in
                        NoteRow(note: note)
                    }
                    .onDelete { indices in
                        for i in indices {
                            viewModel.deleteNote(viewModel.allUserNotes[i])
                        }
                    }
                }
            }
            .searchable(text: $viewModel.noteSearchText)
            .onChange(of: viewModel.noteSearchText) { _, _ in viewModel.searchNotes() }
            .navigationTitle("My notes")
        }
    }
}

struct GlossaryTab: View {
    @ObservedObject var viewModel: CalendarViewModel

    var body: some View {
        NavigationStack {
            List(viewModel.relevantGlossaryTerms) { term in
                VStack(alignment: .leading, spacing: 4) {
                    Text(term.term).font(.headline)
                    Text(term.definition).font(.subheadline).foregroundStyle(.secondary)
                    HStack {
                        ForEach(term.niches, id: \.self) { niche in
                            Text(niche).font(.caption2).padding(.horizontal, 6).padding(.vertical, 2)
                                .background(.blue.opacity(0.1)).clipShape(Capsule())
                        }
                    }
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Glossary")
        }
    }
}

// MARK: - Reusable Components

struct StatBadge: View {
    let label: String
    let value: String
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.title3).fontWeight(.medium)
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct EventRow: View {
    let event: RegulatoryEvent
    let hasNotes: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: event.eventType.iconName).font(.caption)
                Text(event.title).font(.subheadline).fontWeight(.medium)
                if hasNotes { Image(systemName: "note.text").font(.caption2).foregroundStyle(.orange) }
            }
            HStack(spacing: 8) {
                Label(event.jurisdiction.displayName, systemImage: "globe").font(.caption)
                Label(event.confidence.displayName, systemImage: event.confidence.iconName).font(.caption)
                    .foregroundStyle(event.needsVerification ? .orange : .green)
                if event.id.hasPrefix("curated_") {
                    Text("Regional").font(.caption2).padding(.horizontal, 4).padding(.vertical, 1)
                        .background(.blue.opacity(0.1)).clipShape(Capsule())
                }
            }
            .foregroundStyle(.secondary)
        }
    }
}

struct NoteRow: View {
    let note: UserNote
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: note.status.iconName).font(.caption)
                Text(note.eventTitle).font(.subheadline).fontWeight(.medium)
                if note.isPinned { Image(systemName: "pin.fill").font(.caption2).foregroundStyle(.orange) }
            }
            Text(note.text).font(.caption).foregroundStyle(.secondary).lineLimit(2)
            if !note.tags.isEmpty {
                HStack {
                    ForEach(note.tags, id: \.self) { tag in
                        Text(tag).font(.caption2).padding(.horizontal, 6).padding(.vertical, 1)
                            .background(.purple.opacity(0.1)).clipShape(Capsule())
                    }
                }
            }
        }
    }
}

struct OnboardingView: View {
    let onComplete: () -> Void
    var body: some View {
        VStack(spacing: 20) {
            ForEach(OnboardingContent.steps) { step in
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: step.iconName).font(.title2).frame(width: 40)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(step.title).font(.headline)
                        Text(step.description).font(.subheadline).foregroundStyle(.secondary)
                    }
                }
            }
            Button("Get started", action: onComplete).buttonStyle(.borderedProminent).padding(.top)
        }
        .padding()
    }
}
