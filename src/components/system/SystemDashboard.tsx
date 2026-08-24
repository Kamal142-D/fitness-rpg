import { View } from 'react-native';

import { GateCard } from '@/components/gate/GateCard';
import { AttributeRow } from '@/components/system/AttributeRow';
import { StatChip } from '@/components/system/StatChip';
import { SystemHeader } from '@/components/system/SystemHeader';
import { XPBar } from '@/components/system/XPBar';
import { Card, RankBadge, Text } from '@/components/ui';
import type { SuggestedGate } from '@/constants/gates';
import type { Rank } from '@/constants/ranks';
import { Spacing } from '@/constants/theme';

export interface SystemDashboardData {
  displayName?: string | null;
  level: number;
  currentXp: number;
  hunterRank: Rank;
  hunterScore: number;
  attributes: { strength: number; physique: number; endurance: number; discipline: number };
  streakDays: number;
  longestStreakDays: number;
}

export interface SystemDashboardProps extends SystemDashboardData {
  gate: SuggestedGate;
  onEnterGate: () => void;
  onSettings: () => void;
}

/**
 * The System dashboard, presentational (data as props) so it renders in tests
 * without a backend. Answers the four dashboard questions: who am I, how strong
 * am I, what should I do today, how close is my next milestone.
 */
export function SystemDashboard({
  displayName,
  level,
  currentXp,
  hunterRank,
  hunterScore,
  attributes,
  streakDays,
  longestStreakDays,
  gate,
  onEnterGate,
  onSettings,
}: SystemDashboardProps) {
  return (
    <View style={{ gap: Spacing.lg }}>
      <SystemHeader displayName={displayName} onSettings={onSettings} />

      <Card>
        <View style={{ flexDirection: 'row', gap: Spacing.lg, alignItems: 'center' }}>
          <RankBadge rank={hunterRank} size="lg" />
          <View style={{ flex: 1, gap: Spacing.xs }}>
            <Text variant="caption" color="secondary">
              PLAYER STATUS
            </Text>
            <Text variant="title">Rank {hunterRank}</Text>
            <Text variant="caption" color="tertiary" mono>
              Hunter score {Math.round(hunterScore)}
            </Text>
          </View>
        </View>

        <View style={{ marginTop: Spacing.lg }}>
          <XPBar level={level} currentXp={currentXp} />
        </View>

        <View style={{ marginTop: Spacing.lg, gap: Spacing.md }}>
          <AttributeRow label="Strength" value={attributes.strength} />
          <AttributeRow label="Physique" value={attributes.physique} />
          <AttributeRow label="Endurance" value={attributes.endurance} />
          <AttributeRow label="Discipline" value={attributes.discipline} />
        </View>
      </Card>

      <GateCard gate={gate} onEnter={onEnterGate} />

      <View style={{ flexDirection: 'row', gap: Spacing.sm }}>
        <StatChip label="STREAK" value={`${streakDays} d`} />
        <StatChip label="BEST STREAK" value={`${longestStreakDays} d`} />
      </View>
    </View>
  );
}
