import { View, type ViewStyle } from 'react-native';

import { Text } from '@/components/ui/Text';
import { getRankColor, type Rank } from '@/constants/ranks';
import { Radius } from '@/constants/theme';
import { hexToRgba } from '@/utils/color';

type Size = 'sm' | 'md' | 'lg';

const DIMENSIONS: Record<Size, { box: number; fontSize: number; radius: number }> = {
  sm: { box: 28, fontSize: 15, radius: Radius.sm },
  md: { box: 40, fontSize: 22, radius: Radius.md },
  lg: { box: 64, fontSize: 36, radius: Radius.lg },
};

export interface RankBadgeProps {
  rank: Rank;
  size?: Size;
  style?: ViewStyle;
}

/**
 * The rank letter in a compact, rank-tinted badge.
 *
 * Accessibility: rank is NEVER communicated by color alone — the letter is
 * always shown, and an accessibilityLabel spells it out. The color is a tonal
 * accent (a faint tinted fill + a self-colored edge in the rank hue), not a
 * saturated flood.
 */
export function RankBadge({ rank, size = 'md', style }: RankBadgeProps) {
  const { box, fontSize, radius } = DIMENSIONS[size];
  const color = getRankColor(rank);

  return (
    <View
      accessibilityLabel={`Rank ${rank}`}
      accessibilityRole="image"
      style={[
        {
          width: box,
          height: box,
          borderRadius: radius,
          borderWidth: 1.5,
          borderColor: hexToRgba(color, 0.65),
          backgroundColor: hexToRgba(color, 0.12),
          alignItems: 'center',
          justifyContent: 'center',
        },
        style,
      ]}
    >
      <Text
        variant="title"
        color="inherit"
        style={{ color, fontSize, lineHeight: fontSize, fontWeight: '800' }}
      >
        {rank}
      </Text>
    </View>
  );
}
