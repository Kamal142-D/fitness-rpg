# Architecture

Original fitness RPG (React Native + Expo + TypeScript). Fitness logging is the
priority; RPG elements motivate without slowing training down.

## Layers

```
src/
  app/            Expo Router routes (thin screens; compose features + components)
  components/     Reusable UI — ui/ primitives, system/, gate/, workout/, quests/, charts/
  features/       Domain logic per area (auth, onboarding, gates, workouts, pr,
                  progression, quests, analytics) — api + hooks + pure logic
  services/       Cross-cutting services — supabase/ (typed client), ranking/ (pure engine)
  stores/         Small Zustand stores (client-only state, e.g. active workout)
  providers/      App-wide providers (Query, Auth, ErrorBoundary)
  constants/      Theme tokens, rank thresholds, gate constants
  utils/          Pure helpers (id, uuid, color, units)
  types/          Generated Supabase types (database.ts)
```

### Boundaries (enforced by convention)

- **Screens are thin.** Ranking, progression, validation and data access live in
  `features/*` and `services/*`, never inline in a route.
- **Pure logic is separated from I/O.** The ranking engine, PR detection, workout
  reducers, XP curve, and analytics transforms are pure and unit-tested; hooks/api
  wrap them. UI components import pure helpers from their file, not feature
  barrels (barrels pull the Supabase client and break tests).
- **TanStack Query owns server state**; Zustand holds only local client state
  (the persisted active-workout store). They don't duplicate each other.
- **Theme via tokens** (`constants/theme.ts`) — no scattered hard-coded colors.
- **Weights stored in kilograms**; `utils/units.ts` supports future imperial.

## Data flow: a workout

1. Enter a Gate → `useActiveWorkoutStore` (Zustand + AsyncStorage) holds the
   session; every edit autosaves and survives restarts (resume / safe abandon).
2. Finish → `buildCompletionPayload` (pure) → `useFinishWorkout` orchestrates:
   prior stats → **detect PRs** (pure) → **compute Gate result** (pure ranking) →
   `complete_workout` RPC (atomic, idempotent) → apply PRs/stats → quest progress
   → durable progression — each server step idempotent and best-effort.
3. Gate Cleared reveal shows clear rank, grades, PRs, XP + projected level.

## Key concepts kept distinct

Gate Difficulty (pre) ≠ Gate Clear Rank (post) · Exercise Rank (permanent) ≠
Performance Grade (per session) · Hunter Rank (attributes) ≠ Level (activity).
See `docs/RANKING_SYSTEM.md`.

## Server

Supabase Postgres with RLS on every table. Trusted, atomic, idempotent mutations
go through SECURITY DEFINER RPCs pinned to `auth.uid()` (`complete_workout`,
`apply_workout_results`, `apply_session_progression`, quest RPCs). See
`docs/DATABASE.md`.

## Testing without Docker

No local Docker here, so schema + RLS + RPC behavior is verified with **PGlite**
(in-process WASM Postgres) harnesses in the scratchpad, and all domain logic has
Jest unit tests. See `docs/decisions/`.
