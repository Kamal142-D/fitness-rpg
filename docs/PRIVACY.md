# Privacy & data handling (draft)

> Draft for the team and app-store review. Not legal advice — have counsel review
> before publishing a live privacy policy.

## What the app stores

- **Account**: email + auth (Supabase Auth).
- **Profile & onboarding**: display name, date of birth / age, sex (optional,
  with prefer-not-to-say), height, weight, experience, goal, schedule, location.
- **Training data**: workouts, exercises, sets (weight/reps/RPE), personal
  records, exercise stats, progression (level/XP/attributes), quests, streaks.
- **Body composition** (optional): manual entries or InBody uploads (weight,
  body-fat %, muscle mass, etc.), stored in a private Storage bucket scoped to
  the user.

## Where it lives & who can see it

- All user data is in Supabase Postgres with **Row-Level Security**: a user can
  read/write only their own rows (`auth.uid()`), verified by audit
  (`scratchpad/dbtest/rls_audit.mjs`, 23/23). Reference data (exercise catalog,
  quest definitions) is read-only to users.
- Assessment files live in a **private** Storage bucket under
  `assessments/{user_id}/…` with per-user storage policies.
- The mobile client uses only the **public anon key**; the service-role key is
  never shipped.

## Sensitive-data posture

- Body composition is shown as **estimates with non-punitive ranges**; no medical
  claims. Health/wellness copy accompanies these features.
- No personal data is placed in URLs/query strings.
- Sex is optional; age is used only for (future) evidence-based rank
  normalization.

## Analytics & crash reporting (planned, opt-in)

- **Not yet integrated.** When added, crash reporting (e.g. Sentry) and product
  analytics must be **opt-in**, exclude PII/training specifics by default, and be
  disableable in settings. Crash payloads scrub emails and tokens.
- See `docs/RELEASE.md` for the integration plan.

## User controls (planned)

- Export my data, delete my account (cascades via `on delete cascade` from
  `auth.users`), and toggle analytics/crash reporting.

## Retention

- Data persists while the account exists; deleting the account removes it. Raw
  training logs are preserved (never silently deleted) unless the user deletes
  them.
