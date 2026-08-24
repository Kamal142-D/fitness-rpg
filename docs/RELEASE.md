# Release & hardening (Phase 13)

Status of the hardening checklist and the plan for shipping an internal beta.

## Audits (done)

- **Security / RLS** ✅ — `scratchpad/dbtest/rls_audit.mjs` (23/23): RLS enabled +
  policies on all 13 tables; reference tables read-only; cross-user read/update/
  delete/insert denied; RPC ownership enforced. Client uses the anon key only;
  never the service-role key.
- **Accessibility** ✅ (self-audit) —
  - Rank is never communicated by color alone (the letter always shows;
    `RankBadge` has an accessibility label).
  - Buttons/links/inputs have roles + labels; touch targets ≥ 44pt; password
    show/hide labeled.
  - `ProgressBar` exposes `progressbar` role + value; charts expose image roles
    with text summaries.
  - The Gate Cleared entrance respects **reduce-motion**; content is visible by
    default (never gated on an animation).
  - Contrast: dark surfaces with legible text tokens; primary buttons use dark
    ink on the bright fill for legibility.
- **Resilience** ✅ — app-wide `ErrorBoundary` with a recoverable fallback; every
  data screen has explicit loading / not-connected / error+retry / empty states;
  TanStack Query retries (2) with `refetchOnWindowFocus` off; the active workout
  autosaves locally and survives restarts; completion + rewards are idempotent.

## To do before store submission

- **Crash reporting** (opt-in): add `sentry-expo` (or similar). Wire the
  `ErrorBoundary.componentDidCatch` hook + a global handler. Scrub PII/tokens.
- **Product analytics** (opt-in): minimal event set (workout completed, gate
  cleared) with a settings toggle; no training specifics by default.
- **Performance pass**: convert long history/quest lists to `FlatList` if they
  grow; profile the active-workout screen under a full session; memoize hot rows.
- **User controls**: export data + delete account + analytics toggle in Settings.
- **App icons & splash**: replace the Expo placeholder art in `assets/` with
  final brand art (keep `#070A0F` background).
- **Store metadata**: name, subtitle, description, keywords, screenshots (System,
  Gate, active workout, Gate Cleared, Player), age rating, category (Health &
  Fitness). Original identity only — no Solo Leveling names/art.
- **Legal**: finalize `docs/PRIVACY.md` + Terms with counsel; link in-app.

## In-app updates (GitHub Releases)

The app checks **GitHub Releases** of `Kamal142-D/fitness-rpg` for a newer
version (Settings → App updates). On an **Android release build** it downloads
and installs the release APK in-app; in Expo Go or on iOS it opens the releases
page. Requires the repo's Releases to be **public** and the
`REQUEST_INSTALL_PACKAGES` permission (declared in `app.json`); the user grants
"install unknown apps" on first install.

### Publishing an update

1. Bump `expo.version` in `app.json` (e.g. `0.1.0` → `0.1.1`).
2. Build a **release** APK:
   ```bash
   npx expo prebuild -p android
   cd android && ./gradlew assembleRelease   # app-release.apk (signed)
   ```
   (or `eas build -p android --profile production` and download the artifact).
3. Create a **GitHub Release** tagged with the new version (e.g. `v0.1.1`) and
   attach the `.apk` as a release asset. Write release notes in the body.
4. Existing installs will see "Update available" on the next check and can
   download + install it. Never attach a **debug** APK — only signed release
   builds. Keep the signing key stable so updates install over the prior version.

## Internal beta

1. Provision a Supabase project; apply migrations (`docs/DATABASE.md`); set
   `EXPO_PUBLIC_SUPABASE_URL` / `_ANON_KEY`.
2. `eas build` (dev/preview) for Android (internal testing track) and iOS
   (TestFlight). Configure `eas.json` + app signing.
3. Smoke test the critical path on device: Register → Awakening → Gate → log a
   workout (interrupt + resume) → Finish → Gate Cleared (PRs/XP once) → System +
   Player reflect progression → claim a quest.
4. Collect feedback (in-app link or TestFlight feedback); triage; iterate.

## Open follow-ups (documented in code/memory)

- Calendar-accurate streaks (rest-safe `updateStreak` exists + tested; wire true
  per-day evaluation).
- Gate `progress_score` vs recent sessions (currently renormalized out).
- Persist `exercise_user_stats.exercise_rank` (currently computed on read).
- Endurance attribute source (currently carried/neutral).
- Calibrate the PROVISIONAL strength/physique standards (`docs/RANKING_SYSTEM.md`).
