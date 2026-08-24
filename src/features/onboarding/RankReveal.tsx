import { View } from 'react-native';

import { AttributeRow } from '@/components/system/AttributeRow';
import { Button, Card, RankBadge, Text } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';
import type { InitialAssessment } from '@/features/onboarding/initialAssessment';

export interface RankRevealProps {
  assessment: InitialAssessment;
  name: string;
  onContinue: () => void;
  saving?: boolean;
  error?: string | null;
}

/**
 * The Awakening payoff: the assigned starting rank + initial attributes. Copy is
 * explicit that this is a starting point, not a verdict.
 */
export function RankReveal({ assessment, name, onContinue, saving, error }: RankRevealProps) {
  return (
    <View style={{ gap: Spacing.xl }}>
      <View style={{ alignItems: 'center', gap: Spacing.md, marginTop: Spacing.lg }}>
        <Text variant="caption" color="secondary">
          AWAKENING COMPLETE
        </Text>
        <RankBadge rank={assessment.hunterRank} size="lg" />
        <Text variant="display">Rank {assessment.hunterRank}</Text>
        <Text variant="body" color="secondary" style={{ textAlign: 'center' }}>
          Welcome, {name || 'Hunter'}. This is your starting rank. Every workout can raise it.
        </Text>
      </View>

      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.md }}>
          Starting attributes
        </Text>
        <View style={{ gap: Spacing.md }}>
          <AttributeRow label="Strength" value={assessment.strength} />
          <AttributeRow label="Physique" value={assessment.physique} />
          <AttributeRow label="Endurance" value={assessment.endurance} />
          <AttributeRow label="Discipline" value={assessment.discipline} />
        </View>
        <Text variant="caption" color="tertiary" style={{ marginTop: Spacing.md }}>
          Provisional estimate ({assessment.version}). Refined as you train.
        </Text>
      </Card>

      {error ? (
        <Card tone="raised" padding="md" accessibilityLiveRegion="polite">
          <Text variant="label" color="danger">
            {error}
          </Text>
        </Card>
      ) : null}

      <Button label="Enter the System" onPress={onContinue} loading={saving} />

      <Text variant="caption" color="tertiary" style={{ color: Palette.textTertiary }}>
        Estimates only. Not medical or fitness advice.
      </Text>
    </View>
  );
}
