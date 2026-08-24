import { View, type ViewStyle } from 'react-native';

import { Text } from '@/components/ui';
import { RANK_COLORS } from '@/constants/ranks';
import { Radius, Spacing } from '@/constants/theme';
import type { RecordType } from '@/features/pr/types';
import { hexToRgba } from '@/utils/color';

const GOLD = RANK_COLORS.S;

const TYPE_LABEL: Record<RecordType, string> = {
  estimated_1rm: 'est 1RM',
  weight: 'weight',
  reps: 'reps',
  volume: 'volume',
};

function formatValue(type: RecordType, value: number): string {
  switch (type) {
    case 'reps':
      return `${Math.round(value)} reps`;
    case 'estimated_1rm':
      return `${value} kg 1RM`;
    case 'volume':
      return `${Math.round(value)} kg vol`;
    case 'weight':
      return `${value} kg`;
  }
}

export interface PRBadgeProps {
  recordType: RecordType;
  newValue: number;
  name?: string;
  style?: ViewStyle;
}

/**
 * A personal-record badge. Tonal gold (self-colored edge + faint tint, no glow)
 * with an explicit "PR" tag and the value — never color alone.
 */
export function PRBadge({ recordType, newValue, name, style }: PRBadgeProps) {
  return (
    <View
      style={[
        {
          flexDirection: 'row',
          alignItems: 'center',
          gap: Spacing.sm,
          alignSelf: 'flex-start',
          backgroundColor: hexToRgba(GOLD, 0.12),
          borderColor: hexToRgba(GOLD, 0.6),
          borderWidth: 1,
          borderRadius: Radius.md,
          paddingVertical: Spacing.xs,
          paddingHorizontal: Spacing.md,
        },
        style,
      ]}
    >
      <Text variant="caption" style={{ color: GOLD, fontWeight: '800' }}>
        PR
      </Text>
      <Text variant="label">
        {name ? `${name} · ` : ''}
        {formatValue(recordType, newValue)}
      </Text>
      <Text variant="caption" color="tertiary">
        {TYPE_LABEL[recordType]}
      </Text>
    </View>
  );
}
