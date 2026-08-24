# Fitness RPG (working title)

A mobile fitness RPG: a serious workout tracker underneath, an original
progression-fantasy "system" on top. Your real training drives player levels,
attributes, exercise ranks, and Hunter Rank.

> **Core rule:** fitness functionality comes first. RPG elements exist to
> motivate and must never slow down logging a workout.

This is an **original** product inspired by general progression-fantasy
mechanics. It does not use protected names, characters, artwork, or UI from any
existing property.

The full product/engineering brief lives in [`PLAN.txt`](./PLAN.txt), and the
design law the UI must follow is in [`slop.md`](./slop.md).

---

## Status

**All 13 phases complete** (`PLAN.txt` §8): Foundation, Database, Authentication,
Awakening onboarding, System screen, Gate system, Workout logger, PR engine,
Ranking engine, Gate Cleared, Quests & progression, Player & analytics, and
Hardening & release prep.

The full MVP loop is built: register → Awakening → System dashboard → choose a
Gate → log a workout (autosave / resume / safe abandon) → Gate Cleared reveal
(PRs, Gate Clear Rank, XP once) → durable progression + quests → Player analytics.

- Domain logic (ranking, PR detection, XP/progression, workout reducers,
  analytics) is pure and unit-tested (**164 tests**).
- Schema, RLS (all 13 tables), and idempotent RPCs are verified with PGlite
  harnesses (incl. a 23-check security audit). See
  [`docs/DATABASE.md`](./docs/DATABASE.md), [`docs/RANKING_SYSTEM.md`](./docs/RANKING_SYSTEM.md),
  [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md), [`docs/RELEASE.md`](./docs/RELEASE.md).

**To run against real data:** provision a Supabase project, apply the migrations,
and set `EXPO_PUBLIC_SUPABASE_URL` / `EXPO_PUBLIC_SUPABASE_ANON_KEY` — see the
"Applying to your Supabase project" section of `docs/DATABASE.md`. Without keys
the app runs and shows friendly "not connected" states.

## Tech stack

- **React Native** + **Expo** (SDK 57), **TypeScript** (strict)
- **Expo Router** (file-based routing, typed routes)
- **Supabase** (Postgres, Auth, Storage) — client wired up; schema is Phase 2
- **Zustand** for small local client state
- **TanStack Query** for server state + caching
- **React Hook Form** + **Zod** for validated forms (used from Phase 3 on)
- **Jest** (`jest-expo`) + **React Native Testing Library** for tests
- **ESLint** + **Prettier** + **tsc** for quality

## Prerequisites

- **Node.js** 20+ (developed on 26.x)
- **npm** 10+
- The **Expo Go** app on a physical device, or an Android/iOS emulator, for
  running the app. (iOS simulators require macOS.)

## Setup

```bash
npm install
```

Then create your local environment file:

```bash
cp .env.example .env
```

Fill in your Supabase project values in `.env`:

```
EXPO_PUBLIC_SUPABASE_URL=https://your-project-ref.supabase.co
EXPO_PUBLIC_SUPABASE_ANON_KEY=your-anon-public-key
```

Only **public** (anon) values belong in the app. **Never** put a Supabase
service-role key or any secret in the client or in git. The app still boots
without these set (with a dev warning); network calls simply fail until they
are provided.

## Running

```bash
npm run start      # start the Expo dev server (press a / i / w)
npm run android    # open on Android
npm run ios        # open on iOS (macOS only)
npm run web        # open in the browser
```

## Quality checks

```bash
npm run typecheck    # tsc --noEmit
npm run lint         # eslint (expo config)
npm run format       # prettier --write
npm run format:check # prettier --check
npm test             # jest
```

All of the above pass on a clean checkout.

## Project architecture

Routes live under `src/app` (Expo Router's `src` convention); all other domain
code lives beside it under `src/`. Screens stay thin — business rules, scoring,
and data access live in testable modules, never inside screen components.

```
src/
├── app/                      # Expo Router routes (file-based)
│   ├── _layout.tsx           # root: providers + headerless Stack
│   ├── index.tsx             # entry redirect -> /system
│   ├── (auth)/               # sign in / register / forgot password (Phase 3)
│   ├── (onboarding)/         # Awakening onboarding (Phase 4)
│   ├── (tabs)/               # main shell: System · Gates · Start · Quests · Player
│   ├── workout/              # active-workout flow (Phase 7)
│   ├── exercise/             # exercise detail (later)
│   └── settings/             # settings (later)
├── components/
│   ├── ui/                   # Screen, Text, Button, Card, RankBadge (+ barrel)
│   ├── navigation/           # bespoke tab glyph
│   └── Placeholder.tsx       # shared "not built yet" screen
├── constants/
│   ├── theme.ts              # design tokens (color, spacing, type, radius, elevation)
│   └── ranks.ts              # rank thresholds + pure scoreToRank / getRankColor
├── features/                 # feature modules (auth, workouts, rankings, …) — Phase 2+
├── services/
│   └── supabase/             # typed client (env-based, no service-role key)
├── providers/                # AppProviders, QueryProvider (TanStack Query)
├── stores/                   # small focused Zustand stores (example: preferences)
├── hooks/                    # shared hooks (useTheme)
├── types/                    # shared types (database.ts placeholder until Phase 2)
└── utils/                    # small pure helpers (color)
```

### Key conventions

- **Tokens, not hard-coded values.** Colors, spacing, radii, and type come from
  `src/constants/theme.ts`. Components read them (often via `useTheme`).
- **Metric-first.** Canonical weights are stored in **kilograms**; imperial
  display is a later conversion layer.
- **Server vs client state.** TanStack Query owns remote data + cache. Zustand
  holds only small local UI state and never mirrors the remote cache.
- **Pure domain logic.** Ranking/progression math is pure and unit-tested
  (see `src/constants/ranks.ts` and its tests). The full ranking engine lands
  in Phase 9.
- **Security.** Row-Level Security guards every user-owned table (Phase 2). The
  client only ever holds the public anon key.

See [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) for boundaries and the
decisions behind this layout.

## License / attribution

Original work. Inspired by progression-fantasy mechanics in general; not
affiliated with or derived from any specific property.
