# Пакет Desktop `files (2)` — як інтегровано в проєкт

Оригінал: `CURSOR_PROMPT.md` + 16 файлів у пакеті `com.regulation.assistant.*`.

У цьому репозиторії **не** додано паралельний пакет `com.regulation.assistant`: логіка перенесена в `com.example.myapplication2`, щоб не дублювати `CalendarViewModel`, Room-нотатки та `NicheCatalog`.

## Що зроблено

| Ідея з пакета | Реалізація |
|---------------|------------|
| Retry + timeout + backoff (GrokApiService) | `GrokApi` — таймаути з `regulation_config.json`, повтори для 429 / 502–504 / мережі |
| Батчі ніш + реалістична кількість подій | `RemoteCalendarRepository` — `chunked(maxNichesPerRequest)`, 8–15 подій на батч, пауза 500 ms між батчами |
| Парсер JSON (3 рівні) | `sanitizeJsonArrayDeep`, `extractTopJsonArray`, `parseJsonArrayLenient` |
| RegulationConfig з assets | Уже було: `assets/regulation_config.json` + `RegulationConfigLoader` |
| Глосарій | `core/common/RegulatoryGlossary.kt` (клас лишається в проєкті для повторного використання; вкладка в календарі прибрана) |
| Експорт / календар / share | `data/calendar/RegulatoryEventExport.kt` — кнопки на картці події |
| Сповіщення | `notifications/RegulatoryNotificationService.kt` + `RegulatoryNotificationReceiver`, ініціалізація в `RegulationApplication` |
| Onboarding copy | `presentation/onboarding/OnboardingContent.kt` (тексти для майбутнього UI) |
| UserNoteManager (SharedPreferences) | **Не** додано — нотатки в Room, як і раніше |

## Залежності

Gson з пакета **не** додавався: кеш і конфіг уже на kotlinx.serialization / JSON.

## Де лежить оригінал

`C:\Users\Orest\Desktop\files (2)\` — можна зберігати як довідник.
