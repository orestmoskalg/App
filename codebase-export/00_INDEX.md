# MyApplication2 — Експорт кодової бази для AI

**Проєкт:** Android-додаток на Kotlin + Jetpack Compose  
**Тематика:** EU MDR/IVDR регуляторний асистент (календар подій, пошук, інсайти, стратегія, навчання)

---

## Зміст експорту

| Файл / папка | Опис |
|--------------|------|
| **source/** | Всі 48 Kotlin-файлів з повною структурою пакетів |
| **00_INDEX.md** | Цей файл — огляд проєкту |
| **01_core.md** | Модуль core — моделі, NicheCatalog, утиліти |
| **02_domain.md** | Модуль domain — репозиторії, use cases |
| **app-build.gradle.kts** | Залежності app-модуля |
| **root-build.gradle.kts** | Кореневий build |
| **settings.gradle.kts** | Налаштування Gradle |
| **libs.versions.toml** | Версії залежностей |
| **AndroidManifest.xml** | Манифест додатку |
| **res_values_*.xml** | strings, themes |

---

## Структура source/

```
source/com/example/myapplication2/
├── MainActivity.kt
├── RegulationApplication.kt
├── core/model/CardModels.kt
├── core/common/NicheCatalog.kt, JsonProvider.kt, AppDispatchers.kt
├── core/util/ErrorMessageHelper.kt
├── domain/model/DomainModels.kt
├── domain/repository/Repositories.kt
├── domain/usecase/*.kt
├── data/remote/GrokApi.kt
├── data/repository/RemoteRepositories.kt, LocalRepositories.kt, SeedContentFactory.kt
├── data/local/AppDatabase.kt, dao/, entity/
├── data/mapper/CardMappers.kt
├── di/AppContainer.kt
├── worker/CalendarRefreshWorker.kt
├── ui/theme/Color.kt, Theme.kt, Type.kt, Gradients.kt
└── presentation/
    ├── calendar/CalendarScreen.kt, CalendarGrid.kt, EventRowCard.kt, PeriodSettingsSheet.kt
    ├── carddetail/CardDetailScreen.kt
    ├── dashboard/DashboardScreen.kt
    ├── search/SearchScreen.kt
    ├── insights/InsightsScreen.kt
    ├── strategy/StrategyScreen.kt
    ├── learning/LearningScreen.kt
    ├── onboarding/OnboardingScreen.kt
    ├── components/DashboardCardItem.kt, NicheSelectorView.kt, CardShare.kt, AppDesignComponents.kt
    ├── navigation/RegulationNavHost.kt, ViewModelFactory.kt, PendingCardHolder.kt
    └── root/AppRootViewModel.kt
```

---

## Інструкція для AI

1. **Для аналізу** — використовуй `source/` та конфігураційні файли.
2. **Для відновлення проєкту** — скопіюй `source/` у `app/src/main/java/` і конфіги у відповідні місця.
3. **GROK_API_KEY** — задається в `secrets.properties` або змінній оточення; у `app/build.gradle.kts` є логіка підключення.
4. **Залежності** — див. `app-build.gradle.kts` та `libs.versions.toml`.
