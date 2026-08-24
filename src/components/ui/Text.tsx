import { Text as RNText, type TextProps as RNTextProps, type TextStyle } from 'react-native';

import { Palette, Typography, Fonts, type TypographyVariant } from '@/constants/theme';

type TextColor = 'primary' | 'secondary' | 'tertiary' | 'accent' | 'success' | 'danger' | 'inherit';

const COLOR_MAP: Record<Exclude<TextColor, 'inherit'>, string> = {
  primary: Palette.textPrimary,
  secondary: Palette.textSecondary,
  tertiary: Palette.textTertiary,
  accent: Palette.primary,
  success: Palette.success,
  danger: Palette.danger,
};

export interface TextProps extends RNTextProps {
  /** Typographic role from the scale. Defaults to `body`. */
  variant?: TypographyVariant;
  /** Semantic text color token. Defaults to `primary`. */
  color?: TextColor;
  /** Use the monospace family (reserve for genuine data: codes, timestamps). */
  mono?: boolean;
}

/**
 * Themed text primitive. All app text should route through this so type sizes,
 * weights, and colors come from tokens rather than inline styles.
 */
export function Text({
  variant = 'body',
  color = 'primary',
  mono = false,
  style,
  ...rest
}: TextProps) {
  const base = Typography[variant];
  const resolved: TextStyle = {
    fontSize: base.fontSize,
    lineHeight: base.lineHeight,
    fontWeight: base.fontWeight,
    fontFamily: mono ? Fonts.mono : Fonts.sans,
    ...(color === 'inherit' ? null : { color: COLOR_MAP[color] }),
  };
  return <RNText style={[resolved, style]} {...rest} />;
}
