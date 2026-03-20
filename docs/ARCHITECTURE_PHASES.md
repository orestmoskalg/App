# Regulatory app architecture — phased plan

## Backup

A full project copy was created next to this repo (e.g. `MyApplication2_backup_20260319/`, excluding `build/`, `.gradle/`). Re-copy before large refactors if needed.

## Phase 1 — **Done (current)**

- **Sector (sphere)** domain: `SectorCatalog` — medical, pharma, food, cosmetics, chemicals, consumer, agri, environment, digital, workplace, construction, automotive, tobacco/alcohol, other.
- **Niches** are tagged with `sectorKey` in `NicheCatalog`; `forSector()` filters onboarding and profile editing.
- **`UserProfile.sector`** + stricter `isComplete` (country + sector + niches + role).
- **Prompt stack** `SectorRegulatoryContext`: combines sector expert line + jurisdiction narrative (non–medical-device sectors no longer use MDR-only country text).
- **Repositories**: `RemoteRepositories`, `RemoteSearchRepository` — sector-aware system prompts, cache key includes sector, calendar prompts mention sector.
- **Onboarding order**: Country → Sector → Niches → Role.
- **Legacy profiles**: `UserProfileRepositoryImpl` infers `sector` from the first niche when missing.
- **Search suggestions**: `NicheQueryCatalog` routes general query groups by `sectorKey` + country.

## Phase 2 — **Done (current)**

- **Glossary** (`ToolsScreens`): `getGlossaryForSectorAndCountry` — food, pharma, chemicals, cosmetics, digital, consumer, and general cross-sector term lists; medical devices still use EU MDR / FDA / Ukraine lists by country.
- **Compliance checklist**: `complianceChecklistForSector` — separate section sets for food, pharma, chemicals, cosmetics, digital, consumer, and general; MDR checklist unchanged for `medical_devices`. Device-class filter (I / IIa / …) only for medical sector.
- **NicheQueryCatalog**: `nicheSpecific` entries for non-medical `promptKey`s (e.g. `food_haccp_food_safety`, `pharma_gmp_gdp_manufacturing`, `chem_reach_registration`, `cosmetic_pif_safety`, `dp_gdpr_processing`, `consumer_gpsr_safety`).
- Profile / Settings: tool rows no longer show fixed “55 / 43” counts.

## Phase 3 — **Done (current)**

- **Knowledge Base filtering**: `CountryRegulatoryContext.knowledgeSectorMatches` — infers sector from `DashboardCard.niche` via `NicheCatalog` (legacy unmapped niches treated as medical-device content; empty / `"General"` niches treated as medical seed). `KnowledgeViewModel.filterKnowledge` applies jurisdiction + **sector** + niche chip rules.
- **Seed**: `SeedContentFactory.knowledgeSeedCards(profile, jurisdictionKey)` — full MDR-centric insight/strategy/learning seed for `medical_devices`; sector-agnostic starter modules + padding to `MIN_KNOWLEDGE_ITEMS` for other sectors (niche label from first `promptKey`). `AppRootViewModel.seedKnowledgeIfNeeded` uses this API.
- **KB seed version**: `SeedContentFactory.KNOWLEDGE_SEED_VERSION` + DataStore `knowledge_seed_version`. On app start, if the stored version is lower, all `INSIGHT` / `STRATEGY` / `LEARNING_MODULE` cards are removed and the default seed is inserted again (also refreshes users who only had the legacy “calendar refresh ⇒ seeded” flag). Bump the constant when changing seed text or structure.
- **NicheQueryCatalog**: `nicheSpecific` extended to all non-medical `promptKey`s in `NicheCatalog`, plus two medical niches that lacked groups (sterile single-use; radiation-emitting devices).

## Phase 4 — **Suggested next**

- **Localized** sector/labels (`strings.xml`) while keeping **English** for LLM JSON fields.
- **Analytics** / telemetry on sector distribution (optional).
- **Matrix tests** for critical `sector × jurisdiction` prompt snippets (optional).
- Optional: `sectorKey` on stored cards (Room migration) if inference from niche is insufficient.
