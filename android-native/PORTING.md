# Fitness RPG — Kotlin / Jetpack Compose rewrite

A from-scratch native Android rewrite of the React Native (Expo) app, one layer
at a time. The **Supabase backend is unchanged** — same project, schema, RLS,
and RPCs; only the client is rewritten.

## Target stack

- **Language:** Kotlin 2.2, JVM 17
- **UI:** Jetpack Compose (Material 3), dark-first, tokens ported from the RN theme
- **Architecture:** Clean-ish layering by package (single `:app` module for now)
  - `domain/` — pure Kotlin: ranking engine, models, use cases (no Android deps)
  - `data/` — Supabase data sources, repositories, DTO↔domain mappers
  - `ui/` — Compose screens, ViewModels, navigation, design system
- **Backend client:** supabase-kt (Postgrest + Auth + Storage) over Ktor 3
- **Async:** Coroutines + Flow; **DI:** manual ServiceLocator (small app)
- **Local state:** DataStore (preferences, active-workout draft)
- **Self-update:** GitHub Releases API + PackageInstaller (Android-native)

## Package: `com.fitnessrpg.app`

## Phase plan (mirrors the original 13-phase build)

| # | Phase | Status |
|---|-------|--------|
| 1 | Foundation: Gradle, theme, app shell | ✅ done, building |
| 2 | Domain: ranking + all pure logic (tested) | ✅ done — 123 JUnit tests pass |
| 3 | Data: Supabase client, auth, all repositories | ✅ done, compiling |
| 4 | Auth screens (login/register/forgot) + session gate | ✅ done |
| 5 | Onboarding (Awakening) + initial assessment + reveal | ✅ done |
| 6 | System dashboard | ✅ done |
| 7 | Gates (list + detail) | ✅ create/edit/duplicate/archive + full exercise search |
| 8 | Workout logger + active-workout store | ✅ done (in-memory store) |
| 9 | PR engine + finish-workout flow | ✅ done (FinishWorkoutUseCase) |
| 10 | Progression (XP/level), quests | ✅ done |
| 11 | Player + analytics | ✅ ranks + monthly; SVG charts ⬜ |
| 12 | Settings + in-app updater | ✅ done (GitHub release + APK install) |
| 13 | Hardening: error states, polish, release signing | 🚧 signing done; polish ongoing (v0.3.0) |

### Gate/routine upgrade (v0.3.0)

- Gate Difficulty is no longer selected on a template. It is calculated after
  completion from personalized relative intensity, working volume, set effort,
  progression, and bodyweight-aware effective load. Gate Difficulty and Clear
  Rank are stored and displayed independently.
- New routines show **Not Assessed** until their first completion; templates
  retain last/average difficulty and completion history.
- The native Gate builder searches and filters the complete imported exercise
  catalog; workout exercises can also be added or replaced through the picker.
- User routines use soft deletion, preserving every historical session, PR,
  progression update, and analytics record. System routines are hidden per-user.

### UI layer built (Jetpack Compose)

Design system: `AppText`, `AppButton`, `AppCard`, `AppTextField`, `ChoiceGroup`,
`RankBadge`, `AppProgressBar`, `ScreenScaffold`, `GateCard`, `XpBar`,
`AttributeRow`, `StatChip`. Navigation: `AppRoot` (session-gated) → `AuthFlow` /
`OnboardingFlow` / `MainNavHost` (tabs: System, Gates, Player, Quests; + Settings,
Gate detail, Workout, Complete). DI: `ServiceLocator`. Session: `SessionViewModel`.

### Hunter Ranking redesign (2026-08-25)

Replaced the weighted-average Hunter Rank with a three-pillar, gated system in
`domain/rankings/`:
- Pillars: **Physique + Strength + Conditioning** (Discipline no longer affects
  Hunter Rank — only XP/streaks/quests).
- `hunterScore = base·0.70 + weakestPillar·0.30`, base = P·0.4+S·0.4+C·0.2
  (renormalized over assessed pillars).
- Per-rank **minimum requirements** (`HUNTER_RANK_REQUIREMENTS`) gate the rank, so
  a weak pillar caps it. **Provisional** cap at C until conditioning is assessed
  (only HIGH confidence → S).
- **muscleMassKg / skeletalMuscleMassKg / leanBodyMassKg kept separate**; physique
  uses body-fat + FFMI and never treats total muscle mass as SMM.
- Per-exercise strength standards (bench ≠ squat), est-1RM (reps clamped to 12),
  bodyweight-relative, imbalance-penalized (never `max`).
- Wired into onboarding (new strength assessment UI + skip → provisional) and the
  post-workout finalize; Player/System re-derive the rank client-side so existing
  users are re-gated to provisional. 25 new tests + updated finalize test.
- Regression: the 71.5 kg test user → **D provisional** (was A).

### Remaining / follow-ups

- SVG line/bar charts on the Player screen (volume/frequency/bodyweight).
- DataStore persistence of the active-workout draft across process death.
- Compose UI tests to replace the RN component tests.

### Phase 2 domain modules ported (all tested, 116 tests green)

`domain/rank` · `domain/ranking` (interp, config, validation, exerciseRank,
performanceGrade, gateScore, attributes, streak) · `domain/workouts` (epley,
restTimer, logic, completionPayload, gateResult) · `domain/pr` (detect) ·
`domain/progression` (xp, rewards, finalize) · `domain/analytics` (transforms) ·
`domain/gates` (mappers, validation) · `domain/onboarding` (initialAssessment,
validation) · `util` (units, ids, time).

## Ported so far (Phase 2)

- `domain/rank/Rank.kt` — E..S ladder, thresholds, `clampScore`, `scoreToRank`
- `domain/ranking/` — `Interp`, `RankingConfig`, `Validation`, `ExerciseRank`,
  `PerformanceGrade`, `GateScore`, `Attributes`, `Streak`
- `ui/theme/` — `Palette`, `Spacing`/`Radius`, `AppTypography`, `FitnessRpgTheme`,
  `rankColor`
- Unit tests mirroring the RN Jest suites for every ranking module.

## Notes / decisions

- **In-place upgrade:** `applicationId = com.anonymous.fitnessrpg` (matches the
  RN app) and the release APK is signed with the **same** keystore
  (`release.keystore`, alias `fitnessrpg`). `versionCode = 3` (> RN's 1) and
  `versionName = 0.3.0`. The
  code package / `namespace` stays `com.fitnessrpg.app` (internal only).
  - Caveat: Android only upgrades in place when the installed app was signed with
    this same key. If the device currently has a **debug/Expo dev build**, its
    signature differs, so the first Kotlin release install needs a one-time
    uninstall; every Kotlin release after that upgrades in place.
  - `keystore.properties` (git-ignored) holds the signing creds; the keystore is
    also git-ignored (`*.keystore`).
- Scores are modelled as `Double` throughout (RN used JS `number`).
- Rank→color mapping lives in `ui/` (not `domain/`) to keep the domain pure.
