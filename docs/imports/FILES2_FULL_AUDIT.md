# Повний обхід `C:\Users\Orest\Desktop\files (2)` (березень 2026)

У каталозі **32 файли**: 15 у корені (дублікати для зручності) + 17 у `cursor-package\` (повне дерево `com.regulation.assistant.*`).

## Корінь = копія `cursor-package`

Файли в корені папки збігаються з відповідниками всередині `cursor-package` (перевірено `fc /b` для всіх пар):

| Корінь `files (2)` | У `cursor-package` |
|--------------------|---------------------|
| `CURSOR_PROMPT.md` | `cursor-package\CURSOR_PROMPT.md` |
| `RegulationConfig.json` | `app\src\main\assets\RegulationConfig.json` |
| `StandingRequirement.kt` | `...\standing\StandingRequirement.kt` |
| `OnboardingContent.kt` | `...\onboarding\OnboardingContent.kt` |
| `RegulatoryGlossary.kt` | `...\glossary\RegulatoryGlossary.kt` |
| `GrokApiService.kt` | `...\api\GrokApiService.kt` |
| `RegulatoryCalendarService.kt` | `...\api\RegulatoryCalendarService.kt` |
| `ConfigManager.kt` | `...\config\ConfigManager.kt` |
| `EventCacheManager.kt` | `...\cache\EventCacheManager.kt` |
| `EventExportService.kt` | `...\export\EventExportService.kt` |
| `RegulatoryRepository.kt` | `...\repository\RegulatoryRepository.kt` |
| `UserNoteManager.kt` | `...\repository\UserNoteManager.kt` |
| `RegulatoryEvent.kt` | `...\entities\RegulatoryEvent.kt` |
| `Niche.kt` | `...\entities\Niche.kt` |
| `CalendarViewModel.kt` | `...\viewmodels\CalendarViewModel.kt` |
| `NotificationService.kt` | `...\notifications\NotificationService.kt` |

## Збіг з цим репозиторієм

| Референс | Статус у `MyApplication2` |
|----------|-------------------------|
| `RegulationConfig.json` | Ідентично → `app/src/main/assets/regulation_config.json` |
| `CURSOR_PROMPT.md` | Ідентично → `docs/imports/CURSOR_PROMPT_V2_CURSOR_PACKAGE.md` |
| `RegulatoryGlossary.kt` | Той самий список термінів → `core/common/RegulatoryGlossary.kt` |
| `OnboardingContent.kt` | Кроки як у референсі; `contextualTips` частково адаптовані під поточний UI → `presentation/onboarding/OnboardingContent.kt` |
| `StandingRequirement.kt` (список правил V2) | Логіка перенесена в `StandingRequirementsCatalog.kt` + розширення (REACH, CSRD quarterly тощо); модель полів інша (`domain/model/StandingRequirement.kt`) |
| Решта (`GrokApiService`, `CalendarViewModel`, …) | Ідеї інтегровані в `GrokApi`, `RemoteCalendarRepository`, `AppRootViewModel` / календар — див. `FILES2_PACKAGE.md` |

## Висновок

Дублікати в корені й `cursor-package` **однакові**; для оновлення референсу достатньо оновити один набір (зазвичай дерево в `cursor-package\`) і повторити порівняння з `docs/imports` та `assets`.
