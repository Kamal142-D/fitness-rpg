# Ranking system

Pure, deterministic ranking math in `src/services/ranking/`. All tunable
constants live in `config.ts` (isolated from logic and UI). `RANKING_VERSION`
is bumped whenever the math or constants change.

> **Provisional data.** The strength standards, physique curves, and endurance
> mapping here are seed estimates, **not** authoritative population standards.
> They are labeled PROVISIONAL in `config.ts`/`attributes.ts` and are meant to be
> calibrated against real reference data later. Physique scoring uses healthy,
> non-punitive ranges and makes no medical claims.

## Distinct concepts (never conflated)

| Concept | When | Meaning | Code |
| --- | --- | --- | --- |
| **Gate Difficulty** | before | intended challenge of a Gate | `workout_templates.difficulty` |
| **Gate Clear Rank** | after | quality of the actual session | `gateClearRank(computeGateScore(...))` |
| **Exercise Rank** | slow | permanent capability on an exercise | `nextExerciseRank(...)` |
| **Performance Grade** | per session | today vs recent baseline | `performanceGrade(performanceScore(...))` |
| **Hunter Rank** | slowest | overall, from attributes | `hunterRank(hunterScore(...))` |
| **Level** | activity | XP/account progression (not a rank) | `features/progression/xp` |

## Rank scale (`constants/ranks.ts`, §6.1)

`E 0–19 · D 20–34 · C 35–49 · B 50–64 · A 65–79 · S 80–100`. `scoreToRank`
clamps out-of-range input; every score in the engine is clamped to 0–100.

## Estimated 1RM (§6.2)

Epley: `weight × (1 + reps/30)` (`features/workouts/epley.ts`). Only reps 1–12
with positive load are used; warm-ups and out-of-range reps return null.

## Exercise score & permanent rank (§6.3)

`best estimated-1RM → bodyweight ratio → provisional standard → 0–100 → rank`.
Standards are per movement (bench/squat/deadlift/ohp/row) with sex scaling
(`config.ts`), interpolated by `interp.ts`. Exercises without a standard return
null (unranked).

**Anti-inflation upgrade guard** (`nextExerciseRank`, §6.6):
- permanent rank is a **high-water mark** (never drops on a worse session);
- a single update may not jump more than **2 bands**;
- reaching **S requires ≥2 qualifying sessions**.

## Validation (§6.6)

`validateWorkingSet` excludes non-completed sets, warm-ups, no-load/no-rep sets,
implausible weight (>600 kg) / reps (>100) / RPE (out of 0–10), and reps >12 for
strength ranking — always with a reason. A qualifying performance needs **≥2**
valid working sets (`meetsQualifyingThreshold`). Raw logs are never modified.

## Performance grade (§6.4)

`performanceScore(todayBest, baseline)` maps today/baseline ratio onto 0–100 via
a provisional curve. **Missing baseline (first time) → neutral (60)**, so new
users aren't punished.

## Gate score → Gate Clear Rank (§6.5)

```
gateScore = performance·0.50 + completion·0.20 + progress·0.15 + pr·0.10 + quality·0.05
```
Components: performance (avg grade), completion (completed/planned working sets),
progress (vs prior volume — **null when no history**), PR bonus, RPE quality.
Missing factors are **renormalized out** (`weightedRenormalized`) rather than
scored zero. All-missing → neutral (60).

## Hunter attributes → Hunter Rank (§6.7–6.9)

```
hunterScore = strength·0.40 + physique·0.30 + endurance·0.15 + discipline·0.15
```
Renormalized over available attributes. `limitingAttribute` names the lowest one
(what's holding back the next rank).

- **Strength**: average of ranked exercise scores.
- **Physique** (§6.8): provisional blend of a healthy-body-fat curve (peaks in a
  healthy band, does **not** reward ever-lower body fat) and skeletal-muscle-mass
  percentage. Null without an assessment.
- **Endurance**: provisional map of weekly training minutes.
- **Discipline** (§6.9): mostly adherence with a **capped** streak bonus, so a
  long streak can't dominate and unsafe "never miss a day" behavior isn't
  incentivized. Scheduled rest days never break a streak (`updateStreak`).

## Calibration TODO

- Replace provisional strength standards with a validated reference table
  (per sex, ideally age-adjusted).
- Add age normalization to exercise scoring.
- Calibrate physique/endurance curves against real assessment distributions.
- Consider segmental/balance inputs for physique when available.

## Tests

`src/services/ranking/__tests__/` covers thresholds, boundaries, invalid input,
missing-history renormalization, the A→S upgrade guard, gate weighting/clamping,
attribute weighting, streak/adherence, and unit conversion. Per PLAN.txt Phase 9,
ranking work is not complete until these pass.
