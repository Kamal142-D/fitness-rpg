-- Daily March: durable daily step totals and an idempotent XP claim.
-- Step data affects Player Level only. It never changes physical/Hunter Rank.

create table if not exists public.daily_step_progress (
  user_id        uuid not null references auth.users(id) on delete cascade,
  step_date      date not null,
  steps          integer not null default 0 check (steps between 0 and 100000),
  goal           integer not null default 8000 check (goal between 1000 and 50000),
  reward_claimed boolean not null default false,
  xp_awarded     integer not null default 0 check (xp_awarded between 0 and 1000),
  claimed_at     timestamptz,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  primary key (user_id, step_date)
);

create index if not exists daily_step_progress_user_date_idx
  on public.daily_step_progress (user_id, step_date desc);

alter table public.daily_step_progress enable row level security;

do $$
begin
  if not exists (
    select 1 from pg_policies
    where schemaname = 'public' and tablename = 'daily_step_progress'
      and policyname = 'daily_step_progress_select_own'
  ) then
    create policy "daily_step_progress_select_own" on public.daily_step_progress
      for select using (auth.uid() = user_id);
  end if;
  if not exists (
    select 1 from pg_policies
    where schemaname = 'public' and tablename = 'daily_step_progress'
      and policyname = 'daily_step_progress_insert_own'
  ) then
    create policy "daily_step_progress_insert_own" on public.daily_step_progress
      for insert with check (auth.uid() = user_id);
  end if;
  if not exists (
    select 1 from pg_policies
    where schemaname = 'public' and tablename = 'daily_step_progress'
      and policyname = 'daily_step_progress_update_own'
  ) then
    create policy "daily_step_progress_update_own" on public.daily_step_progress
      for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
  end if;
end;
$$;

create or replace function public.sync_daily_steps(
  p_step_date date,
  p_steps integer,
  p_goal integer
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_row public.daily_step_progress;
begin
  if v_user is null then raise exception 'not authenticated'; end if;
  if p_step_date < current_date - 1 or p_step_date > current_date + 1 then
    raise exception 'invalid step date';
  end if;
  if p_steps not between 0 and 100000 then raise exception 'invalid step count'; end if;
  if p_goal not between 1000 and 50000 then raise exception 'invalid step goal'; end if;

  insert into public.daily_step_progress (user_id, step_date, steps, goal)
  values (v_user, p_step_date, p_steps, p_goal)
  on conflict (user_id, step_date) do update set
    steps = greatest(public.daily_step_progress.steps, excluded.steps),
    goal = case
      when public.daily_step_progress.reward_claimed then public.daily_step_progress.goal
      else excluded.goal
    end,
    updated_at = now()
  returning * into v_row;

  return to_jsonb(v_row);
end;
$$;

create or replace function public.claim_daily_step_reward(p_step_date date)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_row public.daily_step_progress;
  v_reward constant integer := 100;
begin
  if v_user is null then raise exception 'not authenticated'; end if;

  select * into v_row
  from public.daily_step_progress
  where user_id = v_user and step_date = p_step_date
  for update;

  if v_row.user_id is null then raise exception 'daily march not found'; end if;
  if v_row.steps < v_row.goal then raise exception 'daily march incomplete'; end if;
  if not v_row.reward_claimed then
    update public.daily_step_progress
      set reward_claimed = true,
          xp_awarded = v_reward,
          claimed_at = now(),
          updated_at = now()
      where user_id = v_user and step_date = p_step_date
      returning * into v_row;
    perform public.award_xp(v_user, v_reward);
  end if;

  return to_jsonb(v_row);
end;
$$;

grant select on public.daily_step_progress to authenticated;
grant execute on function public.sync_daily_steps(date, integer, integer) to authenticated;
grant execute on function public.claim_daily_step_reward(date) to authenticated;
