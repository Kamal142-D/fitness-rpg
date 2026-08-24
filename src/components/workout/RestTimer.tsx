import { Pressable, View } from 'react-native';

import { Text } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';
import { formatClock } from '@/features/workouts/restTimer';
import { hexToRgba } from '@/utils/color';

export interface RestTimerProps {
  remainingSeconds: number;
  onAdd: (seconds: number) => void;
  onSkip: () => void;
}

/**
 * Rest countdown bar. Presentational — the parent ticks `remainingSeconds` from
 * the persisted absolute end time, so it stays correct across app restarts.
 */
export function RestTimer({ remainingSeconds, onAdd, onSkip }: RestTimerProps) {
  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        gap: Spacing.md,
        backgroundColor: hexToRgba(Palette.primary, 0.1),
        borderColor: hexToRgba(Palette.primary, 0.4),
        borderWidth: 1,
        borderRadius: Radius.md,
        paddingVertical: Spacing.sm,
        paddingHorizontal: Spacing.lg,
      }}
    >
      <Text variant="caption" color="secondary">
        REST
      </Text>
      <Text variant="heading" mono style={{ color: Palette.primary }}>
        {formatClock(remainingSeconds)}
      </Text>
      <View style={{ flex: 1 }} />
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Add 30 seconds"
        hitSlop={8}
        onPress={() => onAdd(30)}
      >
        <Text variant="label" style={{ color: Palette.primary }}>
          +30s
        </Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Skip rest"
        hitSlop={8}
        onPress={onSkip}
      >
        <Text variant="label" color="secondary">
          Skip
        </Text>
      </Pressable>
    </View>
  );
}
