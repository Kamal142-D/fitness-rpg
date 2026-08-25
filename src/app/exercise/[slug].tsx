import { getExercise } from '@bryllim/workout-guide';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { View } from 'react-native';

import { ExerciseArtwork } from '@/components/exercise/ExerciseArtwork';
import { MuscleMap } from '@/components/exercise/MuscleMap';
import { Card, Screen, Text, TextLink } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';
import { workoutGuideCredit } from '@/features/exercises';

export default function ExerciseGuideDetail() {
  const router = useRouter();
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const exercise = getExercise(slug);

  if (!exercise) {
    return (
      <Screen>
        <TextLink label="‹ Movement Guide" onPress={() => router.back()} />
        <Text variant="display">Movement not found</Text>
      </Screen>
    );
  }

  return (
    <Screen>
      <TextLink label="‹ Movement Guide" onPress={() => router.back()} />

      <View style={{ gap: Spacing.xs }}>
        <Text variant="caption" color="secondary">
          {exercise.primaryMuscle.toUpperCase()} · {exercise.equipment.toUpperCase()}
        </Text>
        <Text variant="display">{exercise.name}</Text>
      </View>

      <Card
        padding="none"
        style={{
          minHeight: 330,
          alignItems: 'center',
          justifyContent: 'center',
          overflow: 'hidden',
          backgroundColor: Palette.surface1,
        }}
      >
        <View
          style={{
            position: 'absolute',
            width: 220,
            height: 220,
            borderRadius: 110,
            borderWidth: 1,
            borderColor: Palette.hairlineStrong,
          }}
        />
        <ExerciseArtwork slug={exercise.slug} size={310} animated tintColor={Palette.primary} />
        <View
          style={{
            position: 'absolute',
            bottom: Spacing.md,
            paddingHorizontal: Spacing.md,
            paddingVertical: Spacing.xs,
            borderRadius: Radius.pill,
            backgroundColor: Palette.surface2,
          }}
        >
          <Text variant="caption" color="secondary">
            MOVEMENT LOOP · 3 PHASES
          </Text>
        </View>
      </Card>

      <Card>
        <Text variant="caption" color="secondary" style={{ marginBottom: Spacing.sm }}>
          BODY MAP
        </Text>
        <MuscleMap primary={exercise.primaryMuscle} secondary={exercise.secondaryMuscles} />
      </Card>

      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.sm }}>
        {[exercise.exerciseType.replaceAll('_', ' '), exercise.equipment].map((label) => (
          <View
            key={label}
            style={{
              paddingHorizontal: Spacing.md,
              paddingVertical: Spacing.sm,
              borderRadius: Radius.pill,
              borderWidth: 1,
              borderColor: Palette.hairlineStrong,
            }}
          >
            <Text variant="caption" color="secondary">
              {label.toUpperCase()}
            </Text>
          </View>
        ))}
      </View>

      <Text variant="caption" color="tertiary" style={{ textAlign: 'center' }}>
        {workoutGuideCredit}
      </Text>
    </Screen>
  );
}
