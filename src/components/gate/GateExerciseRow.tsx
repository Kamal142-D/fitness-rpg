import { View } from 'react-native';

import { Text } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';

export interface GateExerciseRowProps {
  index: number;
  name: string;
  detail?: string | null;
  targets: string;
}

/** One exercise row in the Gate details list: order, name + muscle, targets. */
export function GateExerciseRow({ index, name, detail, targets }: GateExerciseRowProps) {
  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        gap: Spacing.md,
        paddingVertical: Spacing.md,
      }}
    >
      <View
        style={{
          width: 28,
          height: 28,
          borderRadius: Radius.sm,
          backgroundColor: Palette.surface2,
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Text variant="caption" color="secondary" mono>
          {index + 1}
        </Text>
      </View>
      <View style={{ flex: 1 }}>
        <Text variant="label">{name}</Text>
        {detail ? (
          <Text variant="caption" color="tertiary">
            {detail}
          </Text>
        ) : null}
      </View>
      <Text variant="label" mono color="secondary">
        {targets}
      </Text>
    </View>
  );
}
