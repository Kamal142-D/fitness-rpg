import { View, type ViewStyle } from 'react-native';

import { Palette, Radius } from '@/constants/theme';

export interface ProgressBarProps {
  /** Progress from 0 to 1. */
  value: number;
  style?: ViewStyle;
}

/**
 * Determinate progress track. Tonal and quiet — a filled bar over a faint track,
 * no glow or gimmick. Used for the onboarding step indicator.
 */
export function ProgressBar({ value, style }: ProgressBarProps) {
  const pct = Math.max(0, Math.min(1, value));
  return (
    <View
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 0, max: 100, now: Math.round(pct * 100) }}
      style={[
        {
          height: 6,
          borderRadius: Radius.pill,
          backgroundColor: Palette.surface2,
          overflow: 'hidden',
        },
        style,
      ]}
    >
      <View
        style={{
          width: `${pct * 100}%`,
          height: '100%',
          borderRadius: Radius.pill,
          backgroundColor: Palette.primary,
        }}
      />
    </View>
  );
}
