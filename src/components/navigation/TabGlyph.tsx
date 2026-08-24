import { View } from 'react-native';

import { hexToRgba } from '@/utils/color';

/**
 * A tiny bespoke tab mark — a rotated square "rune" that fills when the tab is
 * focused and sits as a thin outline when it is not. Deliberately hand-built
 * from a View instead of pulling an icon pack, so the tab bar's marks belong to
 * this app rather than reading as the default Lucide/Material set.
 *
 * The label beside it carries the actual meaning, so the tab is never
 * identified by color or glyph alone.
 */
export function TabGlyph({ color, focused }: { color: string; focused: boolean }) {
  const size = 14;
  return (
    <View style={{ width: 20, height: 20, alignItems: 'center', justifyContent: 'center' }}>
      <View
        style={{
          width: size,
          height: size,
          transform: [{ rotate: '45deg' }],
          borderRadius: 3,
          borderWidth: 1.5,
          borderColor: color,
          backgroundColor: focused ? hexToRgba(color, 0.9) : 'transparent',
        }}
      />
    </View>
  );
}
