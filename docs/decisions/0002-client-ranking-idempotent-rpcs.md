# ADR 0002 — Client-computed ranking, persisted via idempotent RPCs

**Status:** Accepted

**Context.** The ranking/PR/progression math is substantial and must be
unit-tested; porting it to plpgsql would duplicate it and risk divergence.
Persistence must be atomic and safe against retries/double-submits.

**Decision.** Keep the engines **pure and client-side** (`services/ranking`,
`features/pr/detect`, `features/progression/*`), fully Jest-tested. Persist
results through **SECURITY DEFINER RPCs pinned to `auth.uid()`** that are atomic
and **idempotent** via per-session flags (`results_applied`, `progression_applied`,
`quests_applied`) and `user_quests.claimed`. The SQL level curve
(`xp_required_for_level`) mirrors the TS curve and is cross-checked in the PGlite
harness.

**Consequences.** One tested implementation of each rule; strong idempotency
(verified in PGlite). Trade-off: a user could in principle influence their own
derived numbers — acceptable for a solo app (own data, RLS-enforced, no
cross-user leaderboard in MVP). Anti-inflation validation lives in the ranking
engine; server-authoritative recomputation can be added if V3 leaderboards ship.
