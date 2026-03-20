# Regulation Assistant — SwiftUI (v3 / MDRAssistant v2.2)

Модуль iOS: календар подій, standing requirements, нотатки (SwiftData), глосарій, регіональні пакети, експорт у календар / ICS.

## Структура

```
Regulation/
├── App/
│   ├── RegulationApp.swift       # @main, SwiftData, TabView (Calendar / Requirements / Notes / Glossary)
│   └── AppDelegate.swift         # мінімальний (можна розширити BG tasks)
├── Core/
│   └── ConfigManager.swift       # RegulationConfig.json
├── Resources/
│   └── RegulationConfig.json     # Додай у Copy Bundle Resources у Xcode
├── Domain/
│   ├── Entities/
│   │   ├── Niche.swift
│   │   ├── RegulatoryEvent.swift
│   │   └── StandingRequirement.swift
│   └── Glossary/
│       └── RegulatoryGlossary.swift
├── Data/
│   ├── Models/
│   │   └── UserNote.swift        # SwiftData @Model
│   └── Services/
│       ├── GrokApiService.swift
│       ├── RegulatoryCalendarService.swift
│       ├── RegulatoryRepository.swift
│       ├── NotificationService.swift
│       ├── EventCacheManager.swift
│       ├── EventExportService.swift
│       └── RegionalPackManager.swift
└── Presentation/
    ├── ViewModels/
    │   └── CalendarViewModel.swift
    └── Onboarding/
        └── OnboardingContent.swift
```

## Вимоги

- **iOS 17+** (SwiftData, `requestFullAccessToEvents` у EventExportService)
- **Xcode 15+**

## Інтеграція в Xcode

1. Додай у target усі нові `.swift` з підпапок (`Core`, `Domain/Glossary`, `Data/Models`, тощо).
2. **`RegulationConfig.json`** → у target **Copy Bundle Resources** (інакше `ConfigManager` використає fallback).
3. **Ключ Grok (xAI):** змінна середовища `GROK_API_KEY` у схемі Run або **Info.plist** / xcconfig (не коміть ключ у репозиторій).
4. У **Info.plist** за потреби: дозволи на сповіщення, календар (`NSCalendarsUsageDescription`), фонові задачі — за потреби.

## Що змінилось відносно старого модуля

- Окремі екрани `CalendarView` / `NicheSelectorSheet` / `DateRangeSettingsView` / `EventDetailView` **прибрані** — UI зібраний у `RegulationApp.swift` (вкладки).
- `CalendarDateRange.swift` видалено — діапазон дат у `CalendarViewModel` (`fromDate` / `toDate`).
