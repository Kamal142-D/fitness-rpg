import { View } from 'react-native';

import { Button, Card, RankBadge, Text } from '@/components/ui';
import { StatChip } from '@/components/system/StatChip';
import type { SuggestedGate } from '@/constants/gates';
import { Spacing } from '@/constants/theme';

export interface GateCardProps {
  gate: SuggestedGate;
  onEnter: () => void;
  /** Eyebrow above the name. Default "TODAY'S GATE". */
  eyebrow?: string;
}

/**
 * A Gate summary card. Shows the Gate Difficulty (chosen before training, NOT a
 * Gate Clear Rank), target muscle groups, duration, and intensity, with the
 * primary ENTER GATE action.
 */
export function GateCard({ gate, onEnter, eyebrow = "TODAY'S GATE" }: GateCardProps) {
  return (
    <Card>
      <View style={{ flexDirection: 'row', gap: Spacing.lg, alignItems: 'center' }}>
        <RankBadge rank={gate.difficulty} size="lg" />
        <View style={{ flex: 1, gap: Spacing.xs }}>
          <Text variant="caption" color="secondary">
            {eyebrow}
          </Text>
          <Text variant="title">{gate.name}</Text>
          <Text variant="caption" color="tertiary">
            Difficulty {gate.difficulty}
          </Text>
        </View>
      </View>

      <Text variant="body" color="secondary" style={{ marginTop: Spacing.md }}>
        {gate.muscleGroups.join(' · ')}
      </Text>

      <View style={{ flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.md }}>
        <StatChip label="DURATION" value={`${gate.durationMinutes} min`} />
        <StatChip label="INTENSITY" value={gate.intensity} />
      </View>

      <Button label="Enter Gate" onPress={onEnter} style={{ marginTop: Spacing.lg }} />
    </Card>
  );
}
