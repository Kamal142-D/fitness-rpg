/**
 * Design tokens — the single source of truth for the app's visual language.
 *
 * Rules (see docs/ARCHITECTURE.md and PLAN.txt §7):
 * - Screens and components must read from these tokens, never hard-code colors,
 *   spacing, or radii inline.
 * - The app is dark-first (an original dark "system" interface). Rank colors are
 *   accents, not full-screen floods.
 * - Depth comes from tonal elevation and self-colored edges, not big drop shadows
 *   or default glows.
 *
 * The palette values are fixed by the product brief (PLAN.txt §7). Typography
 * deliberately uses the platform's neutral system font for now; a signature
 * display face is a later design decision, not a foundation dependency.
 */

import { Platform } from 'react-native';

/** Core surfaces, text, and semantic colors (PLAN.txt §7). */
export const Palette = {
  background: '#070A0F',
  surface1: '#0E141D',
  surface2: '#111923',
  /** A hair above surface2, for pressed/selected states. */
  surface3: '#18202C',

  primary: '#3EA6FF', // electric blue
  accent: '#7B61FF', // blue-violet

  textPrimary: '#F4F7FA',
  textSecondary: '#8B99AA',
  /** Even quieter than secondary, for de-emphasized meta text. */
  textTertiary: '#5B6675',

  success: '#4ADE80',
  danger: '#FF4D5A',

  /** Self-colored hairline: a light stroke at low opacity for tonal edges. */
  hairline: 'rgba(244, 247, 250, 0.08)',
  /** Slightly stronger edge for interactive surfaces. */
  hairlineStrong: 'rgba(244, 247, 250, 0.14)',
} as const;

export type PaletteColor = keyof typeof Palette;

/** Spacing scale (4pt base). Use these, not raw numbers. */
export const Spacing = {
  none: 0,
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
  xxxl: 48,
} as const;

export type SpacingToken = keyof typeof Spacing;

/** Corner radii. */
export const Radius = {
  none: 0,
  sm: 6,
  md: 10,
  lg: 14,
  xl: 20,
  pill: 999,
} as const;

export type RadiusToken = keyof typeof Radius;

/**
 * Typography scale. Sizes + line heights only; the family is the platform's
 * neutral system font (genuinely neutral, not a "trendy" Google pick).
 */
export const Typography = {
  display: { fontSize: 34, lineHeight: 40, fontWeight: '700' },
  title: { fontSize: 24, lineHeight: 30, fontWeight: '700' },
  heading: { fontSize: 18, lineHeight: 24, fontWeight: '600' },
  body: { fontSize: 16, lineHeight: 22, fontWeight: '400' },
  label: { fontSize: 14, lineHeight: 18, fontWeight: '500' },
  caption: { fontSize: 12, lineHeight: 16, fontWeight: '500' },
} as const;

export type TypographyVariant = keyof typeof Typography;

/**
 * Font families. system-ui is deliberately neutral; a monospace is reserved for
 * genuine data (timestamps, codes, numbers), never as the house voice.
 */
export const Fonts = Platform.select({
  ios: { sans: 'system-ui', mono: 'ui-monospace' },
  android: { sans: 'sans-serif', mono: 'monospace' },
  default: { sans: 'system-ui', mono: 'monospace' },
}) as { sans: string; mono: string };

/**
 * Depth tokens. Kept intentionally restrained: a tight, low-offset, tinted
 * shadow — never a fat all-around black bloom. Prefer tonal elevation
 * (surface1 -> surface2) over shadow where possible.
 */
export const Elevation = {
  none: {
    shadowColor: 'transparent',
    shadowOpacity: 0,
    shadowRadius: 0,
    shadowOffset: { width: 0, height: 0 },
    elevation: 0,
  },
  card: {
    shadowColor: '#000000',
    shadowOpacity: 0.28,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 2 },
    elevation: 3,
  },
} as const;

/** The single dark theme, assembled from the palette. */
export const Theme = {
  colors: Palette,
  spacing: Spacing,
  radius: Radius,
  typography: Typography,
  fonts: Fonts,
  elevation: Elevation,
} as const;

export type AppTheme = typeof Theme;

/** Maximum content width for large screens / web. */
export const MaxContentWidth = 640;
