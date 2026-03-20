# Regulation Calendar — SwiftUI Module (MDR/IVDR)

Повний модуль календаря для додатку Regulation у SwiftUI.

## Структура

```
Regulation/
├── App/
│   ├── RegulationApp.swift      # @main entry
│   └── AppDelegate.swift        # BGTaskScheduler, щоденний апдейт
├── Domain/
│   └── Entities/
│       ├── RegulatoryEvent.swift
│       ├── Niche.swift
│       └── CalendarDateRange.swift
├── Data/
│   └── Services/
│       ├── GrokApiService.swift
│       ├── RegulatoryCalendarService.swift
│       └── NotificationService.swift
├── Presentation/
│   ├── ViewModels/
│   │   └── CalendarViewModel.swift
│   └── Views/
│       ├── CalendarView.swift
│       ├── NicheSelectorSheet.swift
│       ├── DateRangeSettingsView.swift
│       └── EventDetailView.swift
└── Info.plist
```

## Інтеграція в Xcode

1. Створи новий iOS-проєкт або відкрий існуючий.
2. Скопіюй папку `Regulation` в проєкт.
3. Додай до target: усі `.swift` файли.
4. У Info.plist додай `BGTaskSchedulerPermittedIdentifiers` і `UIBackgroundModes`.
5. Установи змінну середовища `GROK_API_KEY` (або через Build Settings / xcconfig).

## Можливості

- **Вибір ніш**: до 5 ніш зі списку, sheet з чекбоксами.
- **Період**: from_date / to_date, дефолт -1 рік … +3 роки.
- **Grok API**: max_tokens=8192, temperature=0.0.
- **Події**: 100–200+ подій, фільтрація за діапазоном.
- **EventDetailView**: title, date, description, priority, source, officialLink, actionChecklist (відмітки), impact, regulationReference, effortEstimate, affectedClasses, status, resources.
- **Кнопка «Додати в Google Calendar»**: відкриває calendar.google.com з заповненим event.
- **Щоденний апдейт**: BGTaskScheduler, push при нових подіях.
