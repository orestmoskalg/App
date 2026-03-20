import Foundation

/// Ніша MDR/IVDR для фільтрації подій
struct Niche: Identifiable, Hashable {
    let id = UUID()
    let promptKey: String
    let displayName: String

    static let all: [Niche] = [
        Niche(promptKey: "Cardiovascular Devices (stents, pacemakers, valves)", displayName: "Cardiovascular Devices"),
        Niche(promptKey: "Orthopedic and Trauma Devices (implants, prosthetics)", displayName: "Orthopedic & Trauma Devices"),
        Niche(promptKey: "In Vitro Diagnostic Devices IVDR", displayName: "In Vitro Diagnostics (IVDR)"),
        Niche(promptKey: "Software as Medical Device SaMD AI ML", displayName: "Software as Medical Device (SaMD)"),
        Niche(promptKey: "Active Implantable Medical Devices AIMD", displayName: "Active Implantable Devices"),
        Niche(promptKey: "Ophthalmic Devices (lenses, implants)", displayName: "Ophthalmic Devices"),
        Niche(promptKey: "Dental and Maxillofacial Devices", displayName: "Dental & Maxillofacial"),
        Niche(promptKey: "General Surgery Instruments", displayName: "General Surgery Instruments"),
        Niche(promptKey: "Wound Care and Dressings", displayName: "Wound Care"),
        Niche(promptKey: "Diagnostic Imaging Equipment", displayName: "Diagnostic Imaging"),
        Niche(promptKey: "Infusion Pumps and Injection Devices", displayName: "Infusion & Injection"),
        Niche(promptKey: "Home Healthcare Devices", displayName: "Home Healthcare"),
        Niche(promptKey: "Custom-Made Devices Class III", displayName: "Custom-Made Class III"),
        Niche(promptKey: "Legacy Devices MDR transition", displayName: "Legacy Devices"),
        Niche(promptKey: "AI ML Medical Devices Regulation", displayName: "AI/ML Medical Devices"),
        Niche(promptKey: "Drug-Device Combination Products", displayName: "Drug-Device Combination"),
        Niche(promptKey: "Patient Monitoring Systems", displayName: "Patient Monitoring"),
        Niche(promptKey: "Rehabilitation and Physiotherapy Devices", displayName: "Rehabilitation & Physiotherapy"),
    ]
}
