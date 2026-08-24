import { View } from 'react-native';

import { ProgressBar, Text } from '@/components/ui';
import { getRankColor, scoreToRank } from '@/constants/ranks';
import { Spacing } from '@/constants/theme';

export interface AttributeRowProps {
  label: string;
  /** 0..100 score. */
  value: number;
  /** Show the derived rank letter beside the value. Default true. */
  showRank?: boolean;
}

/**
 * One attribute (STR / PHY / END / DIS): label, value, its derived rank letter,
 * and a tonal progress bar. The letter keeps rank legible without color alone.
 */
export function AttributeRow({ label, value, showRank = true }: AttributeRowProps) {
  const v = Math.round(value);
  const rank = scoreToRank(value);
  return (
    <View style={{ gap: Spacing.xs }}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text variant="label" color="secondary">
          {label}
        </Text>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
          {showRank ? (
            <Text variant="label" style={{ color: getRankColor(rank), fontWeight: '800' }}>
              {rank}
            </Text>
          ) : null}
          <Text variant="label" mono>
            {v}
          </Text>
        </View>
      </View>
      <ProgressBar value={v / 100} />
    </View>
  );
}
