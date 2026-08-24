import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { AccessibilityInfo, Animated, View } from 'react-native';

import { StatChip } from '@/components/system/StatChip';
import { XPBar } from '@/components/system/XPBar';
import { PRBadge } from '@/components/workout/PRBadge';
import { Button, Card, RankBadge, Screen, Text } from '@/components/ui';
import { getRankColor, type Rank } from '@/constants/ranks';
import { Spacing } from '@/constants/theme';
import type { RecordType } from '@/features/pr/types';
import { applyXp } from '@/features/progression/rewards';
import { useProgression } from '@/features/progression';

interface RevealData {
  name: string;
  difficulty: string | null;
  durationSeconds: number;
  volume: number;
  sets: number;
  exercises: number;
  completion: number;
  clearRank: Rank;
  gateScore: number;
  xp: number;
  perExercise: { name: string; grade: Rank }[];
  prs: { type: RecordType; name: string; value: number }[];
}

function parse(raw: string | undefined): RevealData | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as RevealData;
  } catch {
    return null;
  }
}

/** A subtle, reduced-motion-safe entrance. Content is fully visible throughout. */
function useEntrance() {
  const [scale] = useState(() => new Animated.Value(0.92));
  useEffect(() => {
    let cancelled = false;
    AccessibilityInfo.isReduceMotionEnabled().then((reduced) => {
      if (cancelled) return;
      if (reduced) {
        scale.setValue(1);
        return;
      }
      Animated.spring(scale, { toValue: 1, useNativeDriver: true, friction: 6 }).start();
    });
    return () => {
      cancelled = true;
    };
  }, [scale]);
  return scale;
}

export default function WorkoutCompleteScreen() {
  const router = useRouter();
  const { data: raw } = useLocalSearchParams<{ data?: string }>();
  const data = parse(raw);
  const scale = useEntrance();
  const progQ = useProgression();

  const projection =
    data && progQ.data
      ? applyXp(
          {
            level: progQ.data.level,
            currentXp: progQ.data.current_xp,
            lifetimeXp: progQ.data.lifetime_xp,
          },
          data.xp,
        )
      : null;

  if (!data) {
    return (
      <Screen>
        <Text variant="display" style={{ marginTop: Spacing.xxl }}>
          Workout complete
        </Text>
        <Button label="Back to System" onPress={() => router.replace('/system')} />
      </Screen>
    );
  }

  return (
    <Screen>
      <View style={{ alignItems: 'center', gap: Spacing.md, marginTop: Spacing.xl }}>
        <Text variant="caption" color="secondary">
          GATE CLEARED
        </Text>
        <Animated.View style={{ transform: [{ scale }], alignItems: 'center', gap: Spacing.sm }}>
          <RankBadge rank={data.clearRank} size="lg" />
          <Text variant="title">Clear Rank {data.clearRank}</Text>
        </Animated.View>
        <Text variant="caption" color="tertiary" mono>
          Gate score {data.gateScore}
          {data.difficulty ? ` · difficulty ${data.difficulty}` : ''}
        </Text>
        <Text variant="heading" style={{ marginTop: Spacing.xs }}>
          {data.name}
        </Text>
      </View>

      <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
        <StatChip
          label="DURATION"
          value={`${Math.max(1, Math.round(data.durationSeconds / 60))} min`}
        />
        <StatChip label="VOLUME" value={`${Math.round(data.volume)} kg`} />
      </View>
      <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
        <StatChip label="COMPLETION" value={`${data.completion}%`} />
        <StatChip label="SETS" value={String(data.sets)} />
      </View>

      {/* XP + projected level */}
      <Card>
        <View
          style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' }}
        >
          <Text variant="heading">Rewards</Text>
          <Text variant="title" style={{ color: getRankColor('S') }}>
            +{data.xp} XP
          </Text>
        </View>
        {projection?.leveledUp ? (
          <Text variant="label" color="success" style={{ marginTop: Spacing.sm }}>
            LEVEL UP → {projection.level}
          </Text>
        ) : null}
        {projection ? (
          <View style={{ marginTop: Spacing.md }}>
            <XPBar level={projection.level} currentXp={projection.currentXp} />
          </View>
        ) : null}
      </Card>

      {data.prs.length > 0 ? (
        <Card>
          <Text variant="heading" style={{ marginBottom: Spacing.md }}>
            {data.prs.length} personal record{data.prs.length === 1 ? '' : 's'}
          </Text>
          <View style={{ gap: Spacing.sm }}>
            {data.prs.map((pr, i) => (
              <PRBadge
                key={`${pr.type}-${i}`}
                recordType={pr.type}
                newValue={pr.value}
                name={pr.name}
              />
            ))}
          </View>
        </Card>
      ) : null}

      {data.perExercise.length > 0 ? (
        <Card>
          <Text variant="heading" style={{ marginBottom: Spacing.md }}>
            Performance
          </Text>
          <View style={{ gap: Spacing.sm }}>
            {data.perExercise.map((pe, i) => (
              <View
                key={`${pe.name}-${i}`}
                style={{
                  flexDirection: 'row',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <Text variant="body" color="secondary">
                  {pe.name}
                </Text>
                <Text variant="label" style={{ color: getRankColor(pe.grade), fontWeight: '800' }}>
                  {pe.grade}
                </Text>
              </View>
            ))}
          </View>
          <Text variant="caption" color="tertiary" style={{ marginTop: Spacing.md }}>
            Grade is today&apos;s performance vs your recent baseline, distinct from your permanent
            Exercise Rank.
          </Text>
        </Card>
      ) : null}

      <Button label="Back to System" onPress={() => router.replace('/system')} />
    </Screen>
  );
}
