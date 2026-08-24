import { View } from 'react-native';

import { BarChart } from '@/components/charts/BarChart';
import { LineChart } from '@/components/charts/LineChart';
import { Splash } from '@/components/Splash';
import { AttributeRow } from '@/components/system/AttributeRow';
import { StatChip } from '@/components/system/StatChip';
import { XPBar } from '@/components/system/XPBar';
import { Button, Card, RankBadge, Screen, Text } from '@/components/ui';
import { getRankColor, type Rank } from '@/constants/ranks';
import { Spacing } from '@/constants/theme';
import {
  computeExerciseRanks,
  frequencyByWeek,
  monthlyComparison,
  usePlayerData,
  volumeByWeek,
} from '@/features/analytics';
import { useProgression } from '@/features/progression';
import { limitingAttribute } from '@/services/ranking';
import { isSupabaseConfigured } from '@/services/supabase';

function shortDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

const ATTR_LABEL: Record<string, string> = {
  strength: 'Strength',
  physique: 'Physique',
  endurance: 'Endurance',
  discipline: 'Discipline',
};

export default function PlayerScreen() {
  const progQ = useProgression();
  const dataQ = usePlayerData();

  if (progQ.isLoading || dataQ.isLoading) return <Splash label="Loading Player" />;

  const header = (
    <View style={{ gap: Spacing.xs }}>
      <Text variant="caption" color="secondary">
        PLAYER
      </Text>
      <Text variant="display">Player</Text>
    </View>
  );

  if (!isSupabaseConfigured) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            Connect a server (add your Supabase keys to .env) to see your progression and analytics.
          </Text>
        </Card>
      </Screen>
    );
  }

  if (progQ.isError || dataQ.isError) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            Couldn&apos;t load your Player data. Check your connection and try again.
          </Text>
        </Card>
        <Button
          label="Retry"
          variant="secondary"
          onPress={() => {
            void progQ.refetch();
            void dataQ.refetch();
          }}
        />
      </Screen>
    );
  }

  const prog = progQ.data;
  const pd = dataQ.data;
  if (!prog || !pd) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            No data yet. Complete the Awakening and your first Gate to populate your Player.
          </Text>
        </Card>
      </Screen>
    );
  }

  const volume = volumeByWeek(pd.sessions);
  const freq = frequencyByWeek(pd.sessions);
  const monthly = monthlyComparison(pd.sessions);
  const ranks = computeExerciseRanks(pd.stats, pd.bodyweightKg, pd.sex);
  const weightSeries = pd.weights.map((w) => ({
    label: shortDate(w.date),
    value: Math.round(w.weightKg),
  }));

  const meaningful = (v: number) => (v > 0 ? v : null);
  const limiting = limitingAttribute({
    strength: meaningful(prog.strength_score),
    physique: meaningful(prog.physique_score),
    endurance: meaningful(prog.endurance_score),
    discipline: meaningful(prog.discipline_score),
  });

  return (
    <Screen>
      {header}

      {/* Overview */}
      <Card>
        <View style={{ flexDirection: 'row', gap: Spacing.lg, alignItems: 'center' }}>
          <RankBadge rank={prog.hunter_rank as Rank} size="lg" />
          <View style={{ flex: 1, gap: Spacing.xs }}>
            <Text variant="caption" color="secondary">
              HUNTER RANK
            </Text>
            <Text variant="title">
              {prog.hunter_rank} · Level {prog.level}
            </Text>
            <Text variant="caption" color="tertiary" mono>
              Hunter score {Math.round(prog.hunter_score)}
            </Text>
          </View>
        </View>
        <View style={{ marginTop: Spacing.lg }}>
          <XPBar level={prog.level} currentXp={prog.current_xp} />
        </View>
        <View style={{ marginTop: Spacing.lg, gap: Spacing.md }}>
          <AttributeRow label="Strength" value={prog.strength_score} />
          <AttributeRow label="Physique" value={prog.physique_score} />
          <AttributeRow label="Endurance" value={prog.endurance_score} />
          <AttributeRow label="Discipline" value={prog.discipline_score} />
        </View>
        {limiting ? (
          <Text variant="caption" color="tertiary" style={{ marginTop: Spacing.md }}>
            {ATTR_LABEL[limiting]} is limiting your next Hunter Rank.
          </Text>
        ) : null}
      </Card>

      {/* Rank explanation */}
      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.xs }}>
          How ranks work
        </Text>
        <Text variant="caption" color="secondary">
          Level tracks activity. Hunter Rank reflects your attributes overall. Exercise Rank is your
          permanent capability on a lift. Performance Grade is one session vs your baseline. Gate
          Difficulty is chosen before a workout; Gate Clear Rank is scored after.
        </Text>
      </Card>

      {/* Monthly comparison */}
      <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
        <StatChip
          label="WORKOUTS (MO)"
          value={`${monthly.thisMonth.workouts} vs ${monthly.lastMonth.workouts}`}
        />
        <StatChip
          label="VOLUME (MO)"
          value={`${Math.round(monthly.thisMonth.volumeKg / 1000)}t vs ${Math.round(monthly.lastMonth.volumeKg / 1000)}t`}
        />
      </View>

      {/* Charts */}
      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.md }}>
          Volume per week
        </Text>
        <BarChart data={volume} title="Weekly volume" />
      </Card>
      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.md }}>
          Workouts per week
        </Text>
        <BarChart data={freq} color={getRankColor('D')} title="Weekly workouts" />
      </Card>
      {weightSeries.length >= 2 ? (
        <Card>
          <Text variant="heading" style={{ marginBottom: Spacing.md }}>
            Bodyweight
          </Text>
          <LineChart data={weightSeries} title="Bodyweight" unit="kg" />
        </Card>
      ) : null}

      {/* Exercise ranks */}
      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.md }}>
          Exercise ranks
        </Text>
        {ranks.length === 0 ? (
          <Text variant="caption" color="secondary">
            Log the main barbell lifts to earn Exercise Ranks.
          </Text>
        ) : (
          <View style={{ gap: Spacing.sm }}>
            {ranks.map((r) => (
              <View
                key={r.exerciseId}
                style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}
              >
                <RankBadge rank={r.rank} size="sm" />
                <Text variant="label" style={{ flex: 1 }}>
                  {r.name}
                </Text>
                {r.best1RMkg != null ? (
                  <Text variant="caption" color="secondary" mono>
                    {Math.round(r.best1RMkg)} kg 1RM
                  </Text>
                ) : null}
              </View>
            ))}
          </View>
        )}
      </Card>

      {/* PR history */}
      {pd.prs.length > 0 ? (
        <Card>
          <Text variant="heading" style={{ marginBottom: Spacing.md }}>
            Recent records
          </Text>
          <View style={{ gap: Spacing.sm }}>
            {pd.prs.slice(0, 8).map((pr) => (
              <View
                key={pr.id}
                style={{
                  flexDirection: 'row',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <Text variant="label" style={{ flex: 1 }}>
                  {pr.exerciseName}
                </Text>
                <Text variant="caption" color="secondary" mono>
                  {Math.round(pr.newValue)} · {pr.recordType.replace('_', ' ')}
                </Text>
                <Text variant="caption" color="tertiary" style={{ marginLeft: Spacing.sm }}>
                  {shortDate(pr.achievedAt)}
                </Text>
              </View>
            ))}
          </View>
        </Card>
      ) : null}

      {/* Workout history */}
      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.md }}>
          Workout history
        </Text>
        {pd.sessions.length === 0 ? (
          <Text variant="caption" color="secondary">
            No workouts yet.
          </Text>
        ) : (
          <View style={{ gap: Spacing.sm }}>
            {pd.sessions.slice(0, 10).map((s) => (
              <View
                key={s.id}
                style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}
              >
                {s.gateClearRank ? (
                  <RankBadge rank={s.gateClearRank as Rank} size="sm" />
                ) : (
                  <View style={{ width: 28 }} />
                )}
                <Text variant="label" style={{ flex: 1 }}>
                  {s.name ?? 'Workout'}
                </Text>
                <Text variant="caption" color="secondary" mono>
                  {Math.round(s.totalVolumeKg ?? 0)} kg
                </Text>
                <Text variant="caption" color="tertiary" style={{ marginLeft: Spacing.sm }}>
                  {s.completedAt ? shortDate(s.completedAt) : ''}
                </Text>
              </View>
            ))}
          </View>
        )}
      </Card>
    </Screen>
  );
}
