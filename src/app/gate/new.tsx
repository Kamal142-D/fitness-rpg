import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, View } from 'react-native';

import { Splash } from '@/components/Splash';
import { Button, Card, ChoiceGroup, Screen, Text, TextField, TextLink } from '@/components/ui';
import { RANKS, type Rank } from '@/constants/ranks';
import { Palette, Radius, Spacing } from '@/constants/theme';
import {
  createGateHasErrors,
  intensityForDifficulty,
  useCreateGate,
  useExercises,
  validateCreateGate,
  type CreateGateErrors,
} from '@/features/gates';

const DIFFICULTY_OPTIONS = RANKS.map((r) => ({
  label: `${r} · ${intensityForDifficulty(r)}`,
  value: r,
}));

export default function NewGateScreen() {
  const router = useRouter();
  const exercisesQ = useExercises();
  const createMut = useCreateGate();

  const [name, setName] = useState('');
  const [difficulty, setDifficulty] = useState<Rank | null>(null);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [errors, setErrors] = useState<CreateGateErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (exercisesQ.isLoading) return <Splash label="Loading exercises" />;

  const exercises = exercisesQ.data ?? [];

  function toggle(id: string) {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  function onCreate() {
    setSubmitError(null);
    const draft = { name, difficulty, exerciseIds: selectedIds };
    const errs = validateCreateGate(draft);
    setErrors(errs);
    if (createGateHasErrors(errs)) return;

    createMut.mutate(
      { name: name.trim(), difficulty: difficulty!, exerciseIds: selectedIds },
      {
        onSuccess: (newId) => {
          router.replace({ pathname: '/gate/[id]', params: { id: newId } });
        },
        onError: () => setSubmitError('Could not create the Gate. Please try again.'),
      },
    );
  }

  return (
    <Screen>
      <View style={{ marginTop: Spacing.sm }}>
        <TextLink label="‹ Gates" onPress={() => router.back()} />
      </View>

      <View style={{ gap: Spacing.xs }}>
        <Text variant="caption" color="secondary">
          NEW GATE
        </Text>
        <Text variant="display">Custom Gate</Text>
      </View>

      <TextField
        label="Name"
        placeholder="e.g. Chest & Arms"
        value={name}
        onChangeText={setName}
        error={errors.name}
        autoCapitalize="words"
      />

      <View style={{ gap: Spacing.sm }}>
        <Text variant="caption" color="secondary" style={{ marginLeft: Spacing.xs }}>
          DIFFICULTY
        </Text>
        <ChoiceGroup options={DIFFICULTY_OPTIONS} value={difficulty} onChange={setDifficulty} />
        {errors.difficulty ? (
          <Text variant="caption" color="danger" style={{ marginLeft: Spacing.xs }}>
            {errors.difficulty}
          </Text>
        ) : null}
      </View>

      <View style={{ gap: Spacing.sm }}>
        <View
          style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' }}
        >
          <Text variant="caption" color="secondary" style={{ marginLeft: Spacing.xs }}>
            EXERCISES
          </Text>
          <Text variant="caption" color="tertiary">
            {selectedIds.length} selected
          </Text>
        </View>
        {errors.exercises ? (
          <Text variant="caption" color="danger" style={{ marginLeft: Spacing.xs }}>
            {errors.exercises}
          </Text>
        ) : null}
        <Card padding="sm">
          {exercises.map((ex) => {
            const selected = selectedIds.includes(ex.id);
            return (
              <Pressable
                key={ex.id}
                accessibilityRole="checkbox"
                accessibilityState={{ checked: selected }}
                accessibilityLabel={ex.name}
                onPress={() => toggle(ex.id)}
                style={{
                  flexDirection: 'row',
                  alignItems: 'center',
                  gap: Spacing.md,
                  paddingVertical: Spacing.md,
                  paddingHorizontal: Spacing.sm,
                }}
              >
                <View
                  style={{
                    width: 20,
                    height: 20,
                    borderRadius: Radius.sm,
                    borderWidth: 1.5,
                    borderColor: selected ? Palette.primary : Palette.hairlineStrong,
                    backgroundColor: selected ? Palette.primary : 'transparent',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {selected ? (
                    <Text variant="caption" style={{ color: '#07111F', fontWeight: '800' }}>
                      ✓
                    </Text>
                  ) : null}
                </View>
                <View style={{ flex: 1 }}>
                  <Text variant="label">{ex.name}</Text>
                  <Text variant="caption" color="tertiary">
                    {ex.category}
                    {ex.primary_muscle_group ? ` · ${ex.primary_muscle_group}` : ''}
                  </Text>
                </View>
              </Pressable>
            );
          })}
        </Card>
      </View>

      {submitError ? (
        <Card tone="raised" padding="md">
          <Text variant="label" color="danger">
            {submitError}
          </Text>
        </Card>
      ) : null}

      <Button label="Create Gate" onPress={onCreate} loading={createMut.isPending} />
    </Screen>
  );
}
