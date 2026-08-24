import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, View } from 'react-native';

import { RestTimer } from '@/components/workout/RestTimer';
import { WorkoutSetRow } from '@/components/workout/WorkoutSetRow';
import { Button, Card, RankBadge, Screen, Text, TextLink } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';
import { useFinishWorkout } from '@/features/pr';
import {
  buildCompletionPayload,
  restRemainingSeconds,
  useActiveWorkoutStore,
} from '@/features/workouts';

export default function ActiveWorkoutScreen() {
  const router = useRouter();
  const workout = useActiveWorkoutStore((s) => s.workout);
  const store = useActiveWorkoutStore();
  const finishMut = useFinishWorkout();

  // Tick for the rest timer (derived from the persisted absolute end time).
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  if (!workout) {
    return (
      <Screen>
        <View style={{ gap: Spacing.xs, marginTop: Spacing.xxl }}>
          <Text variant="caption" color="secondary">
            WORKOUT
          </Text>
          <Text variant="display">No active workout</Text>
        </View>
        <Card>
          <Text variant="body" color="secondary">
            Enter a Gate to start a workout.
          </Text>
        </Card>
        <Button label="Go to Gates" variant="secondary" onPress={() => router.replace('/gates')} />
      </Screen>
    );
  }

  const exIdx = workout.currentExerciseIndex;
  const ex = workout.exercises[exIdx];
  const restRemaining = restRemainingSeconds(workout.restEndsAt, now);
  const totalCompleted = workout.exercises.reduce(
    (acc, e) => acc + e.sets.filter((s) => s.isCompleted).length,
    0,
  );

  function confirmAbandon() {
    Alert.alert('Abandon workout?', 'Your logged sets will be discarded.', [
      { text: 'Keep training', style: 'cancel' },
      {
        text: 'Abandon',
        style: 'destructive',
        onPress: () => {
          store.abandon();
          router.replace('/system');
        },
      },
    ]);
  }

  function onFinish() {
    if (!workout) return;
    if (totalCompleted === 0) {
      Alert.alert('Nothing logged', 'Complete at least one set before finishing.');
      return;
    }
    const current = workout;
    const { payload, aggregates } = buildCompletionPayload(current);
    finishMut.mutate(
      { payload, aggregates },
      {
        onSuccess: (result) => {
          const nameById = new Map(current.exercises.map((e) => [e.exerciseId, e.name]));
          const data = {
            name: aggregates.name,
            difficulty: aggregates.gateDifficulty,
            durationSeconds: aggregates.durationSeconds,
            volume: aggregates.totalVolumeKg,
            sets: aggregates.completedSets,
            exercises: aggregates.exerciseCount,
            completion: Math.round(result.gate.completionScore),
            clearRank: result.gate.gateClearRank,
            gateScore: Math.round(result.gate.gateScore),
            xp: result.gate.xpEarned,
            perExercise: result.gate.perExercise.map((pe) => ({
              name: nameById.get(pe.exerciseId) ?? '',
              grade: pe.performanceGrade,
            })),
            prs: result.prs.map((pr) => ({
              type: pr.recordType,
              name: nameById.get(pr.exerciseId) ?? '',
              value: pr.newValue,
            })),
          };
          store.abandon();
          router.replace({
            pathname: '/workout/complete',
            params: { data: JSON.stringify(data) },
          });
        },
        onError: () =>
          Alert.alert(
            'Could not save',
            'Your workout is still here. Check your connection and try Finish again.',
          ),
      },
    );
  }

  return (
    <Screen>
      {/* Header */}
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.md }}>
        {workout.gateDifficulty ? <RankBadge rank={workout.gateDifficulty} size="sm" /> : null}
        <View style={{ flex: 1 }}>
          <Text variant="caption" color="secondary">
            ACTIVE GATE
          </Text>
          <Text variant="heading">{workout.name}</Text>
        </View>
        <TextLink label="Abandon" color={Palette.danger} onPress={confirmAbandon} />
      </View>

      {/* Exercise selector */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ gap: Spacing.sm }}
      >
        {workout.exercises.map((e, i) => {
          const active = i === exIdx;
          const done = e.sets.every((s) => s.isCompleted);
          return (
            <Pressable
              key={e.id}
              accessibilityRole="button"
              accessibilityLabel={`${e.name}${active ? ', current' : ''}`}
              onPress={() => store.goToExercise(i)}
              style={{
                paddingVertical: Spacing.sm,
                paddingHorizontal: Spacing.md,
                borderRadius: Radius.pill,
                borderWidth: 1,
                borderColor: active ? Palette.primary : Palette.hairline,
                backgroundColor: active ? Palette.surface2 : 'transparent',
              }}
            >
              <Text
                variant="caption"
                style={{ color: active ? Palette.primary : Palette.textSecondary }}
              >
                {done ? '✓ ' : ''}
                {i + 1}. {e.name}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      {restRemaining > 0 ? (
        <RestTimer
          remainingSeconds={restRemaining}
          onAdd={(s) => store.addRest(s)}
          onSkip={() => store.clearRest()}
        />
      ) : null}

      {/* Current exercise */}
      <Card>
        <View style={{ marginBottom: Spacing.sm }}>
          <Text variant="title">{ex.name}</Text>
          <Text variant="caption" color="secondary">
            {ex.primaryMuscle ?? ''}
            {ex.targetRepsMin != null
              ? `  ·  target ${ex.targetSets ?? ''} × ${ex.targetRepsMin}-${ex.targetRepsMax ?? ex.targetRepsMin}`
              : ''}
          </Text>
        </View>

        <View style={{ gap: Spacing.xs }}>
          {ex.sets.map((set, i) => (
            <WorkoutSetRow
              key={set.id}
              set={set}
              onChangeWeight={(v) => store.updateSet(exIdx, i, { weightKg: v })}
              onChangeReps={(v) => store.updateSet(exIdx, i, { reps: v })}
              onToggleComplete={() =>
                set.isCompleted ? store.uncompleteSet(exIdx, i) : store.completeSet(exIdx, i)
              }
              onToggleWarmup={() => store.toggleWarmup(exIdx, i)}
            />
          ))}
        </View>

        <View style={{ flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.md }}>
          <Button
            label="Add set"
            variant="ghost"
            onPress={() => store.addSet(exIdx)}
            style={{ flex: 1 }}
          />
          {ex.sets.length > 1 ? (
            <Button
              label="Remove last"
              variant="ghost"
              onPress={() => store.removeSet(exIdx, ex.sets.length - 1)}
              style={{ flex: 1 }}
            />
          ) : null}
        </View>
      </Card>

      {/* Nav between exercises */}
      <View style={{ flexDirection: 'row', gap: Spacing.md }}>
        <Button
          label="Previous"
          variant="secondary"
          disabled={exIdx === 0}
          onPress={() => store.goToExercise(exIdx - 1)}
          style={{ flex: 1 }}
        />
        <Button
          label="Next"
          variant="secondary"
          disabled={exIdx === workout.exercises.length - 1}
          onPress={() => store.goToExercise(exIdx + 1)}
          style={{ flex: 1 }}
        />
      </View>

      <Button label="Finish workout" onPress={onFinish} loading={finishMut.isPending} />
      <Text variant="caption" color="tertiary">
        {totalCompleted} set{totalCompleted === 1 ? '' : 's'} logged. Autosaved. You can leave and
        resume anytime.
      </Text>
    </Screen>
  );
}
