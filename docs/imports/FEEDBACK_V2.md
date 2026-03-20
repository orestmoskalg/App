# Інтеграція пакета feedback v2 (Desktop `files (1)`)

Пакет `com.regulation.assistant.*` **не копіювався** у проєкт як окреме дерево — щоб уникнути дублювання ViewModel, Room-нотаток і двох каталогів ніш.

## Що перенесено

| Джерело v2 | Реалізація в застосунку |
|------------|------------------------|
| `Niche.kt` (22 deep prompts) | `core/common/NichePromptEnrichmentCatalog.kt` — мапінг на `NicheCatalog` + тексти для промпту |
| `StandingRequirement.kt` | Розширено `StandingRequirementsCatalog` (DPIA, NIS2 management body, санкції, HACCP CCP); CSRD — згадка double materiality |
| `UserNoteManager.kt` (SharedPreferences) | **Не** додано — нотатки до подій уже в Room (`EventNotesRepository`) |

## Де дивитися код

- Промпт: `RemoteSearchRepository.buildMainPrompt` — блок `NICHE-SPECIFIC AI INSTRUCTIONS`.
- Постійні правила: `data/calendar/StandingRequirementsCatalog.kt`.

Оригінальні файли залишайте на робочому столі як довідник; при зміні текстів можна оновлювати лише `NichePromptEnrichmentCatalog` / standing-каталог.
