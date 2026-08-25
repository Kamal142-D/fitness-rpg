# Fitness RPG Ranking System V2 — Implementation Report

Release target: `v0.4.0` (`versionCode 4`)

## 1. Why the old system was wrong

The previous implementation mixed long-lived workout statistics with current physical ability. A completed workout could directly recalculate physical attributes from lifetime PRs, while discipline/streak behavior sat beside physical scores. That allowed old evidence, one exceptional movement, or ordinary workout attendance to overstate a current Hunter Rank. It also used broad exercise-name matching and equipment conversion assumptions that made unlike lifts comparable, had incomplete confidence/staleness handling, and blended Gate difficulty with outcome-quality concepts.

V2 makes the boundaries explicit:

- Hunter Rank is based only on current Physique, Strength, and Conditioning evidence.
- Player Level/XP and Discipline remain separate progression systems.
- Exercise Rank is separate from Hunter Rank; unsupported equipment uses Personal Performance Tier.
- Gate Difficulty describes how demanding the completed workout was for that player.
- Gate Clear Grade describes how well the player completed that Gate.

## 2. Ranking formulas removed

- Removed post-workout physical-rank recomputation from lifetime/top estimated 1RM records.
- Removed workout attendance/minutes as a Conditioning Rank input.
- Removed the compatibility Hunter formula as an authoritative path and removed any discipline contribution to physical rank.
- Removed duplicated Hunter and strength constants from `RankingConfig.kt`.
- Removed broad exercise-name-to-movement standards that treated different variations as equivalent.
- Removed machine, cable, Smith, and other equipment conversion assumptions from global Strength Rank.
- Removed implicit dumbbell per-hand interpretation when the load mode is unknown.
- Removed the old conditioning age bonus (`+2` score per decade after age 30) and separate non-normalized Cooper curves.
- Removed the old Gate aggregation (`70%` average exercise, `20%` hardest exercise, `5%` volume factor, `5%` density) and its rep/RPE effort proxy.
- Removed the old Gate volume proxy based on summed 1RM values.
- Removed ambiguous generic muscle-mass use as skeletal muscle evidence.
- Removed the obsolete ranking display implementation in `domain/rankings/Display.kt`.

## 3. New formulas added

### Hunter Rank

1. `base = 0.35 × Physique + 0.40 × Strength + 0.25 × Conditioning`, renormalized only across available pillars.
2. `Hunter Score = 0.80 × base + 0.20 × weakest available pillar`.
3. The final rank must meet both the Hunter-score threshold and every pillar floor for that rank.
4. Missing/stale evidence makes the result provisional and caps it at C. Low confidence caps at C, medium at A, and only high confidence can reach S.

Hunter requirements (Hunter / Physique / Strength / Conditioning): E `0/0/0/0`, D `25/20/15/15`, C `40/35/30/25`, B `55/50/45/40`, A `72/68/68/60`, S `87/82/82/78`.

### Physique

- `Physique = weighted mean(30% body composition, 35% muscularity, 25% waist-to-height, 10% segmental balance)` with weights renormalized when optional measurements are absent.
- Body-composition scoring uses sex-specific healthy body-fat curves and age allowances; lower body fat is not automatically better.
- `FFMI = lean body mass kg / height m²`. Lean mass is measured directly or derived as `bodyweight × (1 − body-fat fraction)`. Skeletal-muscle percentage is an explicit fallback only.
- `waist ratio = waist cm / height cm` and is scored on the configured healthy curve.
- Segmental balance uses mean left/right arm and leg asymmetry and an explicit balance curve.
- Every rank also enforces component floors. Body evidence expires after 90 days.

### Strength

- Epley estimate: `estimated 1RM = ranked load × (1 + reps / 30)`, with ranking repetitions capped at 12.
- Relative strength: `estimated 1RM / bodyweight`.
- Standards are keyed by exact exercise, variation, and equipment. Sex and age scales are applied to the configured ratio anchors.
- `Strength = 0.80 × average movement score + 0.20 × weakest movement score`.
- One movement caps at C; two movement patterns cap at B; A/S require at least three patterns, current evidence, and repeated qualifying sessions. Strength evidence expires after 60 days.

### Conditioning

- Supports Cooper 12-minute distance, 1.5-mile run time, and 3-minute step-test recovery heart rate.
- Inputs are validated, normalized by sex and age, and interpolated against one reference curve per test.
- Conditioning evidence expires after 90 days. Attendance and workout count are not inputs.

### Gate Difficulty

- `Difficulty = 0.45 × relative intensity + 0.25 × hard-set score + 0.20 × personal-volume score + 0.10 × density`.
- Relative intensity is working-set load divided by the player's recent estimated 1RM, targeted at `85%`.
- A hard set is RPE `≥7` or intensity `≥65%`; ten hard sets produce the maximum hard-set component.
- Volume is compared with the player's recent per-exercise baseline. First-history values are neutral and provisional.
- Density targets `0.35` working sets per minute.

### Gate Clear Grade

- `Clear = 0.35 × completion + 0.30 × prescribed-target performance + 0.25 × progress + 0.10 × PR`.
- Prescribed reps/RPE are used first; a personal performance baseline is the fallback.
- Gate Difficulty and Gate Clear Grade are stored and displayed separately.

## 4. Files changed

### Build and documentation

- `android-native/app/build.gradle.kts`
- `android-native/gradle.properties`
- `RANKING_V2_IMPLEMENTATION_REPORT.md`
- `release-artifacts/fitness-rpg-0.4.0.apk`

### Data and repositories

- `android-native/app/src/main/java/com/fitnessrpg/app/data/dto/Dtos.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/dto/InsertDtos.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/remote/DataErrors.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/repo/AssessmentRepository.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/repo/ProfileRepository.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/repo/ProgressionRepository.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/repo/WorkoutRepository.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/data/workout/FinishWorkoutUseCase.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/di/ServiceLocator.kt`

### Domain logic

- `android-native/app/src/main/java/com/fitnessrpg/app/domain/gates/GateDifficulty.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/model/PlayerProgression.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/progression/Finalize.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/ranking/Attributes.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/ranking/ExerciseRank.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/ranking/RankingConfig.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/ConditioningScore.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/Display.kt` (removed)
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/HunterRank.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/OnboardingAssessment.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/PhysiqueScore.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/RankRequirements.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/RankingV2Config.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/StrengthScore.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/rankings/Types.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/workouts/CompletionPayload.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/domain/workouts/GateResult.kt`

### Android UI

- `android-native/app/src/main/java/com/fitnessrpg/app/ui/components/HunterRankPanel.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/nav/MainNavHost.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/GatesScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/HomeTabs.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/PlayerScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/QuestsScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/SettingsScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/SystemScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/main/UpdateSection.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/onboarding/AssessmentUpdateScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/onboarding/OnboardingFlow.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/workout/GateBuilderScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/workout/GateDetailScreen.kt`
- `android-native/app/src/main/java/com/fitnessrpg/app/ui/screens/workout/WorkoutScreen.kt`

### Tests

- `android-native/app/src/test/java/com/fitnessrpg/app/data/remote/DataErrorsTest.kt`
- `android-native/app/src/test/java/com/fitnessrpg/app/domain/gates/GateDifficultyV2Test.kt`
- `android-native/app/src/test/java/com/fitnessrpg/app/domain/progression/FinalizeTest.kt`
- `android-native/app/src/test/java/com/fitnessrpg/app/domain/ranking/ExerciseRankTest.kt`
- `android-native/app/src/test/java/com/fitnessrpg/app/domain/rankings/HunterRankTest.kt`
- `android-native/app/src/test/java/com/fitnessrpg/app/domain/rankings/RankingV2AcceptanceTest.kt`

## 5. Database migrations

- `supabase/migrations/20260825070100_gate_difficulty_exercise_catalog_soft_delete.sql`: idempotent hidden-template policy, soft-delete/catalog support, and Gate difficulty storage support.
- `supabase/migrations/20260825070200_import_exercise_catalog.sql`: deployed the source exercise catalog to the live project. The live database contains exactly 1,318 unique imported exercises after removing six exact duplicates from the 1,324-row source.
- `supabase/migrations/20260825080000_ranking_system_v2.sql`: segmental body fields, conditioning score, strength/conditioning assessment tables, indexes, row-level security policies, current-evidence views, personal Gate baselines, and a V2 derived-rank reset that preserves workout history and XP.

All three migrations were applied to Supabase project `jdqqmcuwwxogqszkzlyt`. The Gates table/cache error shown in the supplied screenshot is resolved.

## 6. Regression result

- Kotlin: 168 tests across 33 suites; 0 failures and 0 errors.
- TypeScript typecheck: passed.
- ESLint: passed.
- Jest: 31 suites and 168 tests passed.
- Release build: passed.
- APK manifest: package `com.anonymous.fitnessrpg`, `versionCode 4`, `versionName 0.4.0`, minimum SDK 26, target SDK 36.
- APK signing: verified with APK Signature Scheme v2 and the Fitness RPG release certificate.
- APK SHA-256: `6AD468933168D715A99D2D14FE9407F2AC7399CCA961BA4B858D17FF277A8B79`.

## 7. Rank confidence result

Confidence is now calculated from the actual evidence behind every pillar:

- Low: a pillar is missing, stale, invalid, too narrow, or based on high-repetition strength evidence. Hunter Rank is capped at C.
- Medium: evidence is usable but does not satisfy all high-confidence requirements. Hunter Rank is capped at A.
- High: all physical pillars have current validated evidence; high Hunter ranks, including S, can be unlocked if all score and floor requirements pass.
- Any missing or stale pillar makes Hunter Rank provisional and caps it at C.

The UI exposes confidence, provisional status, limiting pillar, reasons, current component scores, and the requirements for the next rank.

## 8. Remaining data limitations

- Global Strength Rank is deliberately limited to standardized exercise/variation/equipment combinations. Machines, cables, Smith-machine lifts, and unsupported variations remain personal tiers because their loads are not reliably comparable.
- Body-composition and segmental-balance accuracy depends on the user's measurement method; the app reports confidence and expiry but cannot validate a consumer scale's calibration.
- Conditioning scores depend on correctly performed standardized tests and accurate distance, time, or recovery-heart-rate entry.
- The exercise catalog uses the `hasaneyldrm/exercises-dataset` source. The duplicate `openGym` copy was not imported again. Gymbros was treated only as product inspiration. `bryllim/workout-guide` was not merged because it is a separate CC BY-SA SVG catalog. Source and media attribution are shown in Settings.
- The release APK was verified statically; no Android device was available through the local device bridge for an automated install-and-launch smoke test.
