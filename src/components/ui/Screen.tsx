import { type ReactNode } from 'react';
import { ScrollView, StyleSheet, View, type ViewStyle } from 'react-native';
import { SafeAreaView, type Edge } from 'react-native-safe-area-context';

import { MaxContentWidth, Palette, Spacing } from '@/constants/theme';

export interface ScreenProps {
  children: ReactNode;
  /** Scroll the content vertically. Defaults to true. */
  scroll?: boolean;
  /** Horizontal + vertical content padding token. Defaults to `xl`. */
  padding?: keyof typeof Spacing;
  /** Which safe-area edges to inset. Defaults to top + bottom. */
  edges?: readonly Edge[];
  contentStyle?: ViewStyle;
}

/**
 * Page-level wrapper: paints the app background, applies safe-area insets, caps
 * content width on large screens, and optionally scrolls. Screens compose this
 * rather than re-implementing safe-area/background each time.
 */
export function Screen({
  children,
  scroll = true,
  padding = 'xl',
  edges = ['top', 'bottom'],
  contentStyle,
}: ScreenProps) {
  const inner: ViewStyle = {
    width: '100%',
    maxWidth: MaxContentWidth,
    alignSelf: 'center',
    padding: Spacing[padding],
    gap: Spacing.lg,
  };

  return (
    <SafeAreaView style={styles.root} edges={edges}>
      {scroll ? (
        <ScrollView
          contentContainerStyle={[inner, contentStyle]}
          showsVerticalScrollIndicator={false}
        >
          {children}
        </ScrollView>
      ) : (
        <View style={[styles.flex, inner, contentStyle]}>{children}</View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Palette.background,
  },
  flex: {
    flex: 1,
  },
});
