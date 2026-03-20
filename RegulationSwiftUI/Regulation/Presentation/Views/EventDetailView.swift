import SwiftUI
#if os(iOS)
import UIKit
#elseif os(macOS)
import AppKit
#endif

struct EventDetailView: View {
    let event: RegulatoryEvent
    @Environment(\.dismiss) private var dismiss
    @State private var checkedItems: Set<Int> = []

    private var dateFormatter: DateFormatter {
        let f = DateFormatter()
        f.dateStyle = .long
        f.locale = Locale(identifier: "uk")
        return f
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    headerSection
                    if !event.description.isEmpty { section("Опис", event.description) }
                    if !event.impact.isEmpty { section("Вплив на бізнес", event.impact) }
                    if !event.regulationReference.isEmpty { section("Посилання на статтю", event.regulationReference) }
                    if !event.effortEstimate.isEmpty { section("Оцінка часу + грошей", event.effortEstimate) }
                    if !event.affectedClasses.isEmpty { affectedClassesSection }
                    if !event.actionChecklist.isEmpty { checklistSection }
                    if let url = event.officialLink { officialLinkButton(url) }
                    if !event.resources.isEmpty { resourcesSection }
                }
                .padding()
            }
            .navigationTitle(event.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрити") { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button("Додати в Calendar") {
                        addToCalendar()
                    }
                }
            }
        }
    }

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(dateFormatter.string(from: event.date))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
                priorityBadge
                if event.daysLeft >= 0 && event.daysLeft <= 7 {
                    Text("\(event.daysLeft) днів")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.red)
                        .cornerRadius(6)
                }
            }
            if !event.source.isEmpty {
                Text("Джерело: \(event.source)")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }

    private var priorityBadge: some View {
        Text(event.priority.rawValue)
            .font(.caption)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(priorityColor.opacity(0.3))
            .cornerRadius(6)
    }

    private var priorityColor: Color {
        switch event.priority {
        case .high: return .orange
        case .medium: return .blue
        case .low: return .gray
        }
    }

    private func section(_ title: String, _ content: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
            Text(content)
                .font(.body)
        }
    }

    private var affectedClassesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Класи пристроїв")
                .font(.headline)
            FlowLayout(spacing: 8) {
                ForEach(event.affectedClasses, id: \.self) { cls in
                    Text(cls)
                        .font(.caption)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color.accentColor.opacity(0.2))
                        .cornerRadius(8)
                }
            }
        }
    }

    private var checklistSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Чек-ліст")
                .font(.headline)
            ForEach(Array(event.actionChecklist.enumerated()), id: \.offset) { index, item in
                HStack(alignment: .top, spacing: 12) {
                    Button {
                        if checkedItems.contains(index) {
                            checkedItems.remove(index)
                        } else {
                            checkedItems.insert(index)
                        }
                    } label: {
                        Image(systemName: checkedItems.contains(index) ? "checkmark.square.fill" : "square")
                            .foregroundColor(checkedItems.contains(index) ? .accentColor : .gray)
                    }
                    Text(item)
                        .font(.body)
                        .strikethrough(checkedItems.contains(index))
                }
            }
        }
    }

    private func officialLinkButton(_ url: URL) -> some View {
        Link(destination: url) {
            HStack {
                Image(systemName: "doc.text")
                Text("Відкрити офіційний документ")
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(12)
        }
    }

    private var resourcesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Ресурси")
                .font(.headline)
            ForEach(event.resources, id: \.self) { url in
                Link(destination: url) {
                    HStack {
                        Image(systemName: "link")
                        Text(url.absoluteString)
                            .lineLimit(1)
                            .truncationMode(.middle)
                    }
                    .font(.caption)
                }
            }
        }
    }

    private func addToCalendar() {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate, .withDashSeparatorInDate]
        let dateStr = formatter.string(from: event.date).prefix(10)
        var components = URLComponents(string: "https://calendar.google.com/calendar/render")!
        components.queryItems = [
            URLQueryItem(name: "action", value: "TEMPLATE"),
            URLQueryItem(name: "text", value: event.title),
            URLQueryItem(name: "dates", value: "\(dateStr)/\(dateStr)"),
            URLQueryItem(name: "details", value: event.description)
        ]
        if let url = components.url {
            #if os(iOS)
            UIApplication.shared.open(url)
            #elseif os(macOS)
            NSWorkspace.shared.open(url)
            #endif
        }
    }
}

/// Простий FlowLayout для chips
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, frame) in result.frames.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY), proposal: .unspecified)
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, frames: [CGRect]) {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var frames: [CGRect] = []

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            frames.append(CGRect(x: x, y: y, width: size.width, height: size.height))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }

        return (CGSize(width: maxWidth, height: y + rowHeight), frames)
    }
}
