import { Theme, type AppTheme } from '@/constants/theme';

/**
 * Returns the active theme tokens. The app is single-theme (dark) for now; this
 * hook is the seam where light/dynamic themes would plug in later, so components
 * read tokens through it rather than importing `Theme` directly.
 */
export function useTheme(): AppTheme {
  return Theme;
}
