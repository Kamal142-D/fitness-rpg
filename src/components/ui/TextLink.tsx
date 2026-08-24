import { Pressable, type ViewStyle } from 'react-native';

import { Text } from '@/components/ui/Text';
import { Palette } from '@/constants/theme';

export interface TextLinkProps {
  label: string;
  onPress: () => void;
  color?: string;
  disabled?: boolean;
  style?: ViewStyle;
}

/**
 * A tappable text link (no underline animation, no glow) for secondary actions
 * like "Forgot password?" or "Create account".
 */
export function TextLink({
  label,
  onPress,
  color = Palette.primary,
  disabled,
  style,
}: TextLinkProps) {
  return (
    <Pressable
      accessibilityRole="link"
      accessibilityState={{ disabled: !!disabled }}
      disabled={disabled}
      hitSlop={8}
      onPress={onPress}
      style={style}
    >
      <Text variant="label" color="inherit" style={{ color, fontWeight: '600' }}>
        {label}
      </Text>
    </Pressable>
  );
}
