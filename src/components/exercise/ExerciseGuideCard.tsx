import type { Exercise } from '@bryllim/workout-guide';
import { Pressable, View } from 'react-native';

import { ExerciseArtwork } from '@/components/exercise/ExerciseArtwork';
import { Text } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';

export function ExerciseGuideCard({
  exercise,
  onPress,
}: {
  exercise: Exercise;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Open ${exercise.name}`}
      onPress={onPress}
      style={({ pressed }) => ({
        minHeight: 82,
        flexDirection: 'row',
        alignItems: 'center',
        gap: Spacing.md,
        padding: Spacing.sm,
        paddingRight: Spacing.lg,
        backgroundColor: pressed ? Palette.surface3 : Palette.surface1,
        borderRadius: Radius.lg,
        borderWidth: 1,
        borderColor: pressed ? Palette.hairlineStrong : Palette.hairline,
      })}
    >
      <View
        style={{
          width: 66,
          height: 66,
          borderRadius: Radius.md,
          backgroundColor: Palette.surface2,
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <ExerciseArtwork slug={exercise.slug} size={58} tintColor={Palette.textSecondary} />
      </View>

      <View style={{ flex: 1, gap: 2 }}>
        <Text variant="label">{exercise.name}</Text>
        <Text variant="caption" color="tertiary">
          {exercise.primaryMuscle} · {exercise.equipment}
        </Text>
      </View>

      <Text variant="heading" color="tertiary">
        ›
      </Text>
    </Pressable>
  );
}
