import { useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';

import { Text } from '@/components/ui';
import { Fonts, Palette, Radius, Spacing } from '@/constants/theme';
import type { ActiveSet } from '@/features/workouts/types';
import { hexToRgba } from '@/utils/color';

export interface WorkoutSetRowProps {
  set: ActiveSet;
  previous?: string | null;
  onChangeWeight: (v: number | null) => void;
  onChangeReps: (v: number | null) => void;
  onToggleComplete: () => void;
  onToggleWarmup: () => void;
}

/**
 * One editable set row: a warm-up/working badge, weight + reps cells, and a
 * one-tap complete toggle. Optimized for speed — cells stay editable after
 * completion; completing a set is a single tap.
 */
export function WorkoutSetRow({
  set,
  previous,
  onChangeWeight,
  onChangeReps,
  onToggleComplete,
  onToggleWarmup,
}: WorkoutSetRowProps) {
  return (
    <View style={[styles.row, set.isCompleted && styles.rowDone]}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={
          set.isWarmup
            ? 'Warm-up set, tap to make working set'
            : `Working set ${set.setNumber}, tap to make warm-up`
        }
        onPress={onToggleWarmup}
        style={[styles.badge, set.isWarmup && styles.badgeWarmup]}
      >
        <Text
          variant="caption"
          mono
          style={{ color: set.isWarmup ? Palette.accent : Palette.textSecondary }}
        >
          {set.isWarmup ? 'W' : set.setNumber}
        </Text>
      </Pressable>

      <NumericCell
        label="kg"
        value={set.weightKg}
        onChange={onChangeWeight}
        placeholder={previous ?? '—'}
      />
      <NumericCell
        label="reps"
        value={set.reps}
        onChange={onChangeReps}
        placeholder="—"
        decimal={false}
      />

      <Pressable
        accessibilityRole="checkbox"
        accessibilityState={{ checked: set.isCompleted }}
        accessibilityLabel={`Complete set ${set.setNumber}`}
        onPress={onToggleComplete}
        style={[styles.check, set.isCompleted && styles.checkDone]}
      >
        <Text
          variant="label"
          style={{ color: set.isCompleted ? '#07130C' : Palette.textTertiary, fontWeight: '800' }}
        >
          ✓
        </Text>
      </Pressable>
    </View>
  );
}

function NumericCell({
  label,
  value,
  onChange,
  placeholder,
  decimal = true,
}: {
  label: string;
  value: number | null;
  onChange: (v: number | null) => void;
  placeholder?: string;
  decimal?: boolean;
}) {
  const [text, setText] = useState(value == null ? '' : String(value));
  return (
    <View style={styles.cell}>
      <TextInput
        accessibilityLabel={label}
        value={text}
        keyboardType={decimal ? 'decimal-pad' : 'number-pad'}
        placeholder={placeholder}
        placeholderTextColor={Palette.textTertiary}
        selectTextOnFocus
        onChangeText={(t) => {
          setText(t);
          const trimmed = t.trim();
          if (trimmed === '') return onChange(null);
          const n = Number(trimmed);
          onChange(Number.isFinite(n) ? n : null);
        }}
        style={styles.input}
      />
      <Text variant="caption" color="tertiary">
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    paddingVertical: Spacing.sm,
  },
  rowDone: {
    backgroundColor: hexToRgba(Palette.success, 0.08),
    borderRadius: Radius.md,
    paddingHorizontal: Spacing.xs,
  },
  badge: {
    width: 34,
    height: 34,
    borderRadius: Radius.sm,
    backgroundColor: Palette.surface2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeWarmup: {
    backgroundColor: hexToRgba(Palette.accent, 0.14),
  },
  cell: {
    flex: 1,
    alignItems: 'center',
  },
  input: {
    width: '100%',
    textAlign: 'center',
    color: Palette.textPrimary,
    fontFamily: Fonts.mono,
    fontSize: 18,
    backgroundColor: Palette.surface2,
    borderRadius: Radius.sm,
    paddingVertical: Spacing.sm,
  },
  check: {
    width: 40,
    height: 40,
    borderRadius: Radius.sm,
    borderWidth: 1,
    borderColor: Palette.hairlineStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkDone: {
    backgroundColor: Palette.success,
    borderColor: 'transparent',
  },
});
