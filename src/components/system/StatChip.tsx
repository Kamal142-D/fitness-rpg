import { View, type ViewStyle } from 'react-native';

import { Text } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';

export interface StatChipProps {
  label: string;
  value: string;
  style?: ViewStyle;
}

/**
 * Compact stat: a quiet tonal container (self-colored edge, no glow) with a
 * caption label over a value. Used sparingly for streak / quest summaries.
 */
export function StatChip({ label, value, style }: StatChipProps) {
  return (
    <View
      style={[
        {
          flex: 1,
          gap: 2,
          backgroundColor: Palette.surface1,
          borderColor: Palette.hairline,
          borderWidth: 1,
          borderRadius: Radius.md,
          paddingVertical: Spacing.md,
          paddingHorizontal: Spacing.lg,
        },
        style,
      ]}
    >
      <Text variant="caption" color="secondary">
        {label}
      </Text>
      <Text variant="heading">{value}</Text>
    </View>
  );
}
