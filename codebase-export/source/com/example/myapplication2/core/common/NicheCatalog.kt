package com.example.myapplication2.core.common

/**
 * Ніша з назвою для UI та точним promptKey для Grok.
 */
data class Niche(
    val name: String,
    val nameEn: String,
    val promptKey: String,
) {
    override fun equals(other: Any?) = other is Niche && promptKey == other.promptKey
    override fun hashCode() = promptKey.hashCode()
}

object NicheCatalog {
    val all: List<Niche> = listOf(
        Niche("Кардіоваскулярні пристрої", "Cardiovascular Devices", "Cardiovascular Devices (stents, pacemakers, valves)"),
        Niche("Ортопедичні імпланти", "Orthopedic & Trauma Devices", "Orthopedic and Trauma Devices (implants, prosthetics)"),
        Niche("Ін-вітро діагностика (IVDR)", "In Vitro Diagnostics (IVDR)", "In Vitro Diagnostic Devices IVDR"),
        Niche("Програмне забезпечення як медпристрій (SaMD)", "Software as Medical Device (SaMD)", "Software as Medical Device SaMD AI ML"),
        Niche("Активні імплантовані пристрої", "Active Implantable Devices", "Active Implantable Medical Devices AIMD"),
        Niche("Офтальмологічні пристрої", "Ophthalmic Devices", "Ophthalmic Devices (lenses, implants)"),
        Niche("Стоматологічні пристрої", "Dental & Maxillofacial", "Dental and Maxillofacial Devices"),
        Niche("Загальна хірургія та інструменти", "General Surgery Instruments", "General Surgery Instruments"),
        Niche("Догляд за ранами", "Wound Care", "Wound Care and Dressings"),
        Niche("Діагностичне обладнання", "Diagnostic Imaging", "Diagnostic Imaging Equipment"),
        Niche("Інфузійні та ін'єкційні пристрої", "Infusion & Injection", "Infusion Pumps and Injection Devices"),
        Niche("Прилади для домашнього використання", "Home Healthcare", "Home Healthcare Devices"),
        Niche("Custom-made пристрої", "Custom-Made Class III", "Custom-Made Devices Class III"),
        Niche("Legacy пристрої (до MDR)", "Legacy Devices", "Legacy Devices MDR transition"),
        Niche("AI/ML в медичних пристроях", "AI/ML Medical Devices", "AI ML Medical Devices Regulation"),
        Niche("Комбіновані продукти (drug-device)", "Drug-Device Combination", "Drug-Device Combination Products"),
        Niche("Моніторинг пацієнтів", "Patient Monitoring", "Patient Monitoring Systems"),
        Niche("Реабілітаційне обладнання", "Rehabilitation & Physiotherapy", "Rehabilitation and Physiotherapy Devices"),
    )

    fun findByPromptKey(key: String): Niche? = all.firstOrNull { it.promptKey == key }
    fun findByKeyOrName(value: String): Niche? = all.firstOrNull { it.promptKey == value || it.name == value }
}
