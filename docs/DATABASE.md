# Database

Postgres schema for the fitness RPG, managed as Supabase migrations in
`supabase/migrations/`. Migrations are immutable once shared — add a new one for
later changes rather than editing an applied file (`PLAN.txt` §10).

## Migrations

| File | Purpose |
| --- | --- |
| `20260822090100_initial_schema.sql` | All tables, constraints, FKs, indexes, `updated_at` triggers, new-user provisioning trigger |
| `20260822090200_row_level_security.sql` | Enables RLS on every table + all ownership/reference policies |
| `20260822090300_storage.sql` | Private `assessments` storage bucket + per-user-folder policies |
| `20260822090400_seed_exercises.sql` | 32 system exercises (chest/back/legs/shoulders/arms/core) |
| `20260823090500_seed_system_gates.sql` | 6 system Gates (Push/Pull/Legs/Upper/Lower/Full Body) + 32 ordered template exercises |
| `20260823090600_complete_workout_rpc.sql` | `complete_workout(payload jsonb)` — atomic, idempotent session+exercises+sets insert (SECURITY DEFINER, owner-pinned to `auth.uid()`) |
| `20260823090700_workout_results_rpc.sql` | `results_applied` flag + `apply_workout_results(session, prs, stats)` — idempotently writes `personal_records` (resolving `workout_set_id`) and upserts `exercise_user_stats` |
| `20260823090800_complete_workout_gate_fields.sql` | `create or replace complete_workout` to also store gate scoring fields on the session (completion/progress/quality/gate score, gate_clear_rank, xp_earned) + per-exercise grade/score |
| `20260823090900_quests_progression.sql` | Quest seed + `progression_applied`/`quests_applied` flags; RPCs: `ensure_active_quests`, `record_workout_for_quests`, `apply_session_progression`, `claim_quest`, `award_xp`, `xp_required_for_level` (all idempotent) |

## Tables

| Table | Owner | Purpose |
| --- | --- | --- |
| `profiles` | user (`id` = `auth.users.id`) | Profile + onboarding answers |
| `body_assessments` | user (`user_id`) | Manual / InBody body-composition snapshots |
| `exercises` | system (global) | Exercise catalog — reference data |
| `workout_templates` | user or system | "Gates" — system (`user_id` null) or user-created |
| `workout_template_exercises` | via template | Ordered exercises + targets in a template |
| `workout_sessions` | user (`user_id`) | A single training session (a Gate attempt) |
| `workout_exercises` | via session | An exercise performed in a session |
| `workout_sets` | via exercise → session | Individual sets (raw logs preserved) |
| `exercise_user_stats` | user (`user_id`) | Rolled-up per-exercise bests + exercise rank |
| `player_progression` | user (`user_id`) | Level, XP, attributes, Hunter rank |
| `quests` | system (global) | Quest definitions — reference data |
| `user_quests` | user (`user_id`) | Assigned quests + progress / claim state |
| `personal_records` | user (`user_id`) | Detected PRs (weight/reps/1RM/volume) |

### Conventions

- **UUID** primary keys (`gen_random_uuid()`), **`timestamptz`** timestamps.
- Canonical weights are stored in **kilograms**.
- Rank letters (`E,D,C,B,A,S`) and other small domains are validated with
  `CHECK` constraints; scores are constrained to `0..100`.
- `updated_at` is kept fresh by the `set_updated_at()` trigger on tables that
  carry it.
- On new `auth.users` insert, `handle_new_user()` (SECURITY DEFINER) provisions
  a `profiles` row and a `player_progression` row.

### Key relationships & delete behavior

- Everything user-owned cascades from `auth.users` (`on delete cascade`), so
  deleting an account removes its data.
- `workout_sessions → workout_exercises → workout_sets` cascade on delete.
- References to `exercises` use `on delete restrict` (can't delete a catalog
  entry that history points at); `workout_sessions.template_id` is
  `on delete set null`; `personal_records.workout_set_id` is `on delete set null`.

## Row-Level Security

RLS is **enabled on every table**. Ownership is enforced by the database, never
trusted from the client. The mobile client uses the public anon key; the
`service_role` key (which bypasses RLS) must never ship in the app.

| Table(s) | Policy summary |
| --- | --- |
| `profiles` | select/insert/update where `id = auth.uid()` |
| `body_assessments`, `workout_sessions`, `exercise_user_stats`, `player_progression`, `user_quests`, `personal_records` | full CRUD where `user_id = auth.uid()` |
| `workout_templates` | read own **or** system templates; insert/update/delete only own non-system rows |
| `workout_template_exercises` | gated by parent template (own for writes; own-or-system for reads) |
| `workout_exercises` | gated by parent `workout_sessions` ownership |
| `workout_sets` | gated by parent `workout_exercises → workout_sessions` ownership |
| `exercises` | SELECT to authenticated; **no** write policy (writes only via service/admin) |
| `quests` | SELECT active quests, or any the user has been assigned; no write policy |
| `storage.objects` (`assessments` bucket) | read/write only under `assessments/{auth.uid()}/…` |

### Verification

The schema + RLS were verified with a two-user isolation test (see
`PLAN.txt` §2 "Test ownership policies with at least two users"). It confirms:
new-user provisioning fires, the seed loads, a user can CRUD their own rows and
read system templates + the catalog, and a second user **cannot** select,
update, delete, or spoof-insert the first user's rows, nor write to reference
tables. All 19 assertions passed.

## Applying to your Supabase project

With Docker + the Supabase CLI (local stack):

```bash
npx supabase start          # boots local Postgres/Auth/Storage
npx supabase db reset       # applies all migrations + seed
npx supabase gen types typescript --local > src/types/database.ts
```

Against a hosted project:

```bash
npx supabase login
npx supabase link --project-ref <your-project-ref>
npx supabase db push        # applies migrations
npx supabase gen types typescript --project-id <your-project-ref> > src/types/database.ts
```

> `src/types/database.ts` is currently generated directly from these migrations
> and matches the CLI output shape, so regenerating with the CLI is a drop-in
> replacement.
