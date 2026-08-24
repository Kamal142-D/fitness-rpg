import { useLocalSearchParams, useRouter } from 'expo-router';
import { Alert, View } from 'react-native';

import { Splash } from '@/components/Splash';
import { GateExerciseRow } from '@/components/gate/GateExerciseRow';
import { Button, Card, RankBadge, Screen, Text, TextLink } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { formatTargets, muscleGroupsFor, templateDifficulty, useGate } from '@/features/gates';
import type { GateDetail } from '@/features/gates/types';
import { useActiveWorkoutStore } from '@/features/workouts';

export default function GateDetailsScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const gateQ = useGate(id);

  if (gateQ.isLoading) return <Splash label="Loading Gate" />;

  if (gateQ.isError || !gateQ.data) {
    return (
      <Screen>
        <Text variant="display" style={{ marginTop: Spacing.xl }}>
          Gate not found
        </Text>
        <Card>
          <Text variant="body" color="secondary">
            This Gate couldn&apos;t be loaded.
          </Text>
        </Card>
        <TextLink label="Back to Gates" onPress={() => router.replace('/gates')} />
      </Screen>
    );
  }

  const detail = gateQ.data;
  const { template, exercises } = detail;
  const difficulty = templateDifficulty(template);
  const muscles = muscleGroupsFor(template);

  function enterGate(d: GateDetail) {
    const active = useActiveWorkoutStore.getState().workout;
    const start = useActiveWorkoutStore.getState().start;
    if (active && active.templateId === d.template.id) {
      router.push('/workout'); // resume the same Gate
      return;
    }
    if (active) {
      Alert.alert('Workout in progress', 'Starting this Gate will discard your current workout.', [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Discard & start',
          style: 'destructive',
          onPress: () => {
            start(d);
            router.push('/workout');
          },
        },
      ]);
      return;
    }
    start(d);
    router.push('/workout');
  }

  return (
    <Screen>
      <View style={{ marginTop: Spacing.sm }}>
        <TextLink label="‹ Gates" onPress={() => router.back()} />
      </View>

      <Card>
        <View style={{ flexDirection: 'row', gap: Spacing.lg, alignItems: 'center' }}>
          <RankBadge rank={difficulty} size="lg" />
          <View style={{ flex: 1, gap: Spacing.xs }}>
            <Text variant="caption" color="secondary">
              GATE · DIFFICULTY {difficulty}
            </Text>
            <Text variant="title">{template.name}</Text>
            {muscles.length > 0 ? (
              <Text variant="caption" color="secondary">
                {muscles.join(' · ')}
              </Text>
            ) : null}
          </View>
        </View>
        {template.estimated_duration_minutes ? (
          <Text variant="caption" color="tertiary" style={{ marginTop: Spacing.md }}>
            Estimated {template.estimated_duration_minutes} min · {exercises.length} exercises
          </Text>
        ) : null}
      </Card>

      <Card>
        <Text variant="heading" style={{ marginBottom: Spacing.xs }}>
          Exercises
        </Text>
        {exercises.map((te, i) => (
          <GateExerciseRow
            key={te.id}
            index={i}
            name={te.exercise.name}
            detail={te.exercise.primary_muscle_group}
            targets={formatTargets(te)}
          />
        ))}
      </Card>

      <Button label="Enter Gate" onPress={() => enterGate(detail)} />
    </Screen>
  );
}
