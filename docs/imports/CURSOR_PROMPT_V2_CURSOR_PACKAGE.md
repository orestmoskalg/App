# CURSOR: Universal Regulatory Assistant v2.0 — Full Integration Guide

## CONTEXT FOR CURSOR

You are working on an Android app called "Universal Regulatory Assistant" (previously "MDR/IVDR Assistant").
Repository: https://github.com/orestmoskalg/App
Tech stack: Kotlin, Jetpack Compose, Gradle KTS, Material 3.
The app uses the xAI Grok API to generate regulatory compliance events for a calendar view.

## WHAT'S BROKEN IN THE CURRENT BUILD (GitHub main branch)

1. **Parser is empty** — `parseEvents()`, `parseSingleEvent()`, `sanitizeJSON()` are stubs with `/* оригінальний код */`. API returns JSON but it's never parsed. Users see blank screen.
2. **Prompt requests 400-800 events** but `max_tokens=16384` physically holds ~50. API truncates JSON mid-object.
3. **`UUID()` in Niche** regenerates on every `.all` access → infinite recomposition loop, crashes on older devices.
4. **No retry logic** — one network fail = silent blank screen.
5. **No timeout** — request hangs forever.
6. **No caching** — every screen entry = new API call ($0.01-0.05 each time, 30-90s wait).
7. **System prompt is in Ukrainian** — AI returns mixed-language events.
8. **`RegulationConfig.json` exists but is never loaded** — dead file.
9. **No progress indicator, no onboarding, no export, no sharing, no glossary, no notifications.**
10. **`writePref` returns Preferences instead of Unit** — type mismatch cascades.

## WHAT YOU NEED TO DO

### Step 1: Add these files to the project

Copy ALL `.kt` files below into the Android project at these package paths:

```
app/src/main/java/com/regulation/assistant/
├── data/
│   ├── api/
│   │   ├── GrokApiService.kt          ← REPLACE existing
│   │   └── RegulatoryCalendarService.kt  ← REPLACE existing
│   ├── cache/
│   │   └── EventCacheManager.kt       ← NEW
│   ├── config/
│   │   └── ConfigManager.kt           ← NEW
│   ├── export/
│   │   └── EventExportService.kt      ← NEW
│   └── repository/
│       ├── RegulatoryRepository.kt    ← NEW
│       └── UserNoteManager.kt         ← NEW
├── domain/
│   ├── entities/
│   │   ├── RegulatoryEvent.kt         ← REPLACE existing
│   │   └── Niche.kt                   ← REPLACE existing
│   └── glossary/
│       └── RegulatoryGlossary.kt      ← NEW
├── presentation/
│   ├── viewmodels/
│   │   └── CalendarViewModel.kt       ← REPLACE existing
│   └── onboarding/
│       └── OnboardingContent.kt       ← NEW
├── notifications/
│   └── NotificationService.kt         ← NEW
└── standing/
    └── StandingRequirement.kt         ← NEW
```

### Step 2: Place config in assets

```
app/src/main/assets/RegulationConfig.json   ← NEW
```

### Step 3: Update build.gradle.kts (app level)

Add if missing:
```kotlin
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // These should already exist:
    // implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

### Step 4: Update AndroidManifest.xml

Add permissions:
```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

Add receiver inside `<application>`:
```xml
<receiver
    android:name=".notifications.NotificationReceiver"
    android:exported="false" />
```

### Step 5: Initialize in Application/MainActivity

```kotlin
// In onCreate():
RegulatoryRepository.initialize(applicationContext, "YOUR_GROK_API_KEY")
NotificationService.initialize(applicationContext)
```

### Step 6: Update UI Composables

The new CalendarViewModel exposes:

```kotlin
// States (StateFlow)
viewModel.events              // List<RegulatoryEvent>
viewModel.status              // FetchStatus (Idle/Loading/Loaded/Error)
viewModel.selectedNicheIDs    // Set<String>
viewModel.searchText          // String
viewModel.selectedJurisdictions // Set<String>
viewModel.selectedEventTypes  // Set<EventType>
viewModel.selectedImpacts     // Set<ImpactLevel>
viewModel.showOnlyVerified    // Boolean
viewModel.sortBy              // SortOption
viewModel.isFirstLaunch       // Boolean
viewModel.exportMessage       // String?

// Computed (call these methods)
viewModel.getFilteredEvents()     // filtered + sorted events
viewModel.getEventsByMonth()      // grouped by month for sections
viewModel.getAvailableJurisdictions()  // for filter chips
viewModel.getStats()              // total, critical, upcoming, verified counts
viewModel.activeFilterCount       // for filter badge
viewModel.getRelevantGlossaryTerms()  // for glossary screen
viewModel.getStandingRequirements()   // persistent rules
viewModel.dataTransparencyInfo    // String for info dialog

// Actions
viewModel.toggleNiche(niche)
viewModel.toggleCategory("MedTech")
viewModel.fetchEvents()
viewModel.refresh()               // force refresh (pull-to-refresh)
viewModel.onDisappear()           // cancel in-flight requests
viewModel.toggleJurisdiction("EU")
viewModel.toggleImpact(ImpactLevel.CRITICAL)
viewModel.setShowOnlyVerified(true)
viewModel.clearAllFilters()
viewModel.addToCalendar(event)    // opens system calendar
viewModel.shareEvent(event)       // opens share sheet
viewModel.saveNote(eventId, text) // user notes
viewModel.getNote(eventId)
viewModel.dismissOnboarding()
```

### Step 7: Keep existing UI/design unchanged

DO NOT change:
- Color scheme, typography, theme
- Screen layouts and navigation structure
- App icon, branding assets

DO update:
- Wire new ViewModel states to existing composables
- Add progress bar to loading state
- Add filter chips bar above event list
- Add confidence badge to event rows
- Add onboarding sheet on first launch
- Add "How is data generated?" button somewhere accessible

## ARCHITECTURE AFTER INTEGRATION

```
Composable UI (keep existing design)
    ↓
CalendarViewModel (AndroidViewModel, StateFlow)
    ↓
RegulatoryRepository (cache-first singleton)
    ├── EventCacheManager (memory + disk, 2hr TTL)
    ├── RegulatoryCalendarService (batch by 3 niches)
    │       └── GrokApiService (3x retry, 60s timeout, backoff)
    ├── EventExportService (ICS + calendar intent + share)
    ├── NotificationService (tiered: 90/30/7/1 days)
    ├── UserNoteManager (SharedPreferences CRUD)
    └── StandingRequirements (persistent compliance rules)

ConfigManager ← assets/RegulationConfig.json
RegulatoryGlossary (35+ terms across all niches)
OnboardingContent (4-step flow + contextual tips)
```

## KEY FIXES SUMMARY

| Problem | File | Fix |
|---------|------|-----|
| Parser empty | RegulatoryCalendarService.kt | Full 3-level fallback JSON parser |
| 400-800 events impossible | RegulatoryCalendarService.kt | 40 events per batch, realistic |
| UUID() causes crashes | Niche.kt | Stable string IDs from config key |
| No retry | GrokApiService.kt | 3x retry with exponential backoff |
| No timeout | GrokApiService.kt | 60s timeout on HttpURLConnection |
| No cache | EventCacheManager.kt | Memory + disk, 2hr TTL |
| Ukrainian prompt | RegulatoryCalendarService.kt | English-only prompts |
| Config not loaded | ConfigManager.kt | Reads from assets, singleton |
| Generic events | Niche.kt | Deep promptContext per niche (5-10 lines each) |
| No source links | RegulatoryEvent.kt | sourceURL, confidence, verificationHint |
| No filters | CalendarViewModel.kt | Jurisdiction, type, impact, verified-only |
| No export | EventExportService.kt | ICS file + calendar intent + share text |
| No notifications | NotificationService.kt | Tiered by impact level |
| No glossary | RegulatoryGlossary.kt | 35+ terms, searchable |
| No onboarding | OnboardingContent.kt | 4-step + contextual tips |
| No user notes | UserNoteManager.kt | CRUD by eventId |
| No standing rules | StandingRequirement.kt | 10 persistent compliance rules |
| writePref type error | CalendarViewModel.kt | Uses SharedPreferences directly |
