import { View, type ViewProps, type ViewStyle } from 'react-native';

import { Palette, Radius, Spacing, Elevation } from '@/constants/theme';

export interface CardProps extends ViewProps {
  /** Surface tone. `raised` sits a step above the page for tonal elevation. */
  tone?: 'flat' | 'raised';
  /** Inner padding token. Defaults to `lg`. Pass `none` to control it yourself. */
  padding?: keyof typeof Spacing;
  /** Add a restrained, tight shadow. Off by default — prefer tonal elevation. */
  shadow?: boolean;
}

/**
 * Container primitive. Depth comes from a self-colored hairline edge and a tonal
 * surface shift, not a drawn contrasting outline or a fat drop shadow. Shadow is
 * opt-in and intentionally tight when used.
 */
export function Card({
  tone = 'raised',
  padding = 'lg',
  shadow = false,
  style,
  ...rest
}: CardProps) {
  const surface: ViewStyle = {
    backgroundColor: tone === 'raised' ? Palette.surface1 : Palette.background,
    borderRadius: Radius.lg,
    borderWidth: 1,
    borderColor: Palette.hairline,
    padding: Spacing[padding],
  };
  return <View style={[surface, shadow && Elevation.card, style]} {...rest} />;
}
