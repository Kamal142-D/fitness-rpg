import { Pressable, View, type ViewStyle } from 'react-native';

import { Text } from '@/components/ui/Text';
import { Palette, Radius, Spacing } from '@/constants/theme';
import { hexToRgba } from '@/utils/color';

export interface ChoiceOption<T extends string> {
  label: string;
  value: T;
  description?: string;
}

export interface ChoiceGroupProps<T extends string> {
  options: readonly ChoiceOption<T>[];
  value: T | null;
  onChange: (value: T) => void;
  style?: ViewStyle;
}

/**
 * Single-select list rendered as tappable rows. The selected row is marked with
 * a tonal accent border + faint tint and an explicit indicator dot — never color
 * alone (the label always carries the meaning).
 */
export function ChoiceGroup<T extends string>({
  options,
  value,
  onChange,
  style,
}: ChoiceGroupProps<T>) {
  return (
    <View style={[{ gap: Spacing.sm }, style]}>
      {options.map((opt) => {
        const selected = opt.value === value;
        return (
          <Pressable
            key={opt.value}
            accessibilityRole="radio"
            accessibilityState={{ selected }}
            accessibilityLabel={opt.label}
            onPress={() => onChange(opt.value)}
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              gap: Spacing.md,
              borderRadius: Radius.md,
              borderWidth: 1,
              borderColor: selected ? hexToRgba(Palette.primary, 0.7) : Palette.hairline,
              backgroundColor: selected ? hexToRgba(Palette.primary, 0.1) : Palette.surface1,
              paddingVertical: Spacing.md,
              paddingHorizontal: Spacing.lg,
              minHeight: 52,
            }}
          >
            <View
              style={{
                width: 18,
                height: 18,
                borderRadius: Radius.pill,
                borderWidth: 2,
                borderColor: selected ? Palette.primary : Palette.hairlineStrong,
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {selected ? (
                <View
                  style={{
                    width: 8,
                    height: 8,
                    borderRadius: Radius.pill,
                    backgroundColor: Palette.primary,
                  }}
                />
              ) : null}
            </View>
            <View style={{ flex: 1 }}>
              <Text variant="label" color={selected ? 'primary' : 'primary'}>
                {opt.label}
              </Text>
              {opt.description ? (
                <Text variant="caption" color="secondary" style={{ marginTop: 2 }}>
                  {opt.description}
                </Text>
              ) : null}
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}
