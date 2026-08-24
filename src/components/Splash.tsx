import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { Text } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';

/**
 * Full-screen loading state shown while the session is being resolved. Kept
 * minimal and on-brand (dark background, quiet accent spinner).
 */
export function Splash({ label = 'Initializing' }: { label?: string }) {
  return (
    <View style={styles.root}>
      <View style={styles.center}>
        <ActivityIndicator color={Palette.primary} />
        <Text variant="caption" color="secondary" style={styles.label}>
          {label.toUpperCase()}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Palette.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  center: {
    alignItems: 'center',
    gap: Spacing.md,
  },
  label: {
    letterSpacing: 1,
  },
});
