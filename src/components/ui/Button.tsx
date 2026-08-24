import { type ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  type PressableProps,
  type StyleProp,
  StyleSheet,
  View,
  type ViewStyle,
} from 'react-native';

import { Text } from '@/components/ui/Text';
import { Palette, Radius, Spacing } from '@/constants/theme';

type Variant = 'primary' | 'secondary' | 'ghost';

export interface ButtonProps extends Omit<PressableProps, 'style' | 'children'> {
  label: string;
  variant?: Variant;
  disabled?: boolean;
  /** Show a spinner and block presses while an action is in flight. */
  loading?: boolean;
  /** Optional element rendered before the label (e.g. a small custom mark). */
  leading?: ReactNode;
  style?: StyleProp<ViewStyle>;
}

/**
 * Button primitive.
 *
 * Intentionally NOT a glowy gradient pill. It uses a solid or tonal surface with
 * a legible label and reacts to press with a clean tonal shift — no lift, no
 * scale "boop", no glow. Minimum 44pt touch target for accessibility.
 */
export function Button({
  label,
  variant = 'primary',
  disabled = false,
  loading = false,
  leading,
  style,
  ...rest
}: ButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: isDisabled, busy: loading }}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.base,
        variantSurface(variant, pressed),
        isDisabled && styles.disabled,
        style,
      ]}
      {...rest}
    >
      {loading ? (
        <ActivityIndicator color={labelColor(variant)} size="small" />
      ) : (
        <View style={styles.content}>
          {leading}
          <Text
            variant="label"
            color="inherit"
            style={{ color: labelColor(variant), fontWeight: '600' }}
          >
            {label}
          </Text>
        </View>
      )}
    </Pressable>
  );
}

function variantSurface(variant: Variant, pressed: boolean): ViewStyle {
  switch (variant) {
    case 'primary':
      return { backgroundColor: pressed ? '#2E8AE0' : Palette.primary, borderColor: 'transparent' };
    case 'secondary':
      return {
        backgroundColor: pressed ? Palette.surface3 : Palette.surface2,
        borderColor: Palette.hairlineStrong,
      };
    case 'ghost':
      return {
        backgroundColor: pressed ? Palette.surface1 : 'transparent',
        borderColor: 'transparent',
      };
  }
}

function labelColor(variant: Variant): string {
  // Dark ink on the bright primary fill clears it for legibility; light ink on
  // the dark tonal surfaces.
  return variant === 'primary' ? '#07111F' : Palette.textPrimary;
}

const styles = StyleSheet.create({
  base: {
    minHeight: 44,
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.md,
    borderRadius: Radius.md,
    borderWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  disabled: {
    opacity: 0.45,
  },
});
