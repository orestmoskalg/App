import SwiftUI

struct NicheSelectorSheet: View {
    @ObservedObject var viewModel: CalendarViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    disclaimer
                    nicheList
                }
                .padding()
            }
            .navigationTitle("Обрати нішу")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") { dismiss() }
                }
            }
        }
    }

    private var disclaimer: some View {
        Text("Ми покажемо тільки події та зміни в регуляціях, пов'язані з вашою нішею (дедлайни, оновлення MDCG, конференції). Оберіть до 5 ніш.")
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .padding()
            .background(Color(.secondarySystemBackground))
            .cornerRadius(12)
    }

    private var nicheList: some View {
        VStack(spacing: 0) {
            ForEach(Niche.all) { niche in
                NicheRow(
                    niche: niche,
                    isSelected: viewModel.selectedNicheKeys.contains(niche.promptKey),
                    onToggle: { viewModel.toggleNiche(niche) }
                )
            }
        }
    }
}

struct NicheRow: View {
    let niche: Niche
    let isSelected: Bool
    let onToggle: () -> Void

    var body: some View {
        Button {
            onToggle()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .accentColor : .gray)
                Text(niche.displayName)
                    .font(.body)
                    .foregroundColor(.primary)
                Spacer()
            }
            .padding()
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
