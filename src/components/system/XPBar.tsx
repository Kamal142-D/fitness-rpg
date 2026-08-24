import { View } from 'react-native';

import { ProgressBar, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { xpProgress } from '@/features/progression/xp';

export interface XPBarProps {
  level: number;
  currentXp: number;
}

/** Level label + XP progress toward the next level. */
export function XPBar({ level, currentXp }: XPBarProps) {
  const { current, required, fraction } = xpProgress(currentXp, level);
  return (
    <View style={{ gap: Spacing.sm }}>
      <View
        style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' }}
      >
        <Text variant="label">Level {level}</Text>
        <Text variant="caption" color="secondary" mono>
          {current} / {required} XP
        </Text>
      </View>
      <ProgressBar value={fraction} />
    </View>
  );
}
