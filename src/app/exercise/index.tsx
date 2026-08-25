import { exercises, searchExercises, type Exercise } from '@bryllim/workout-guide';
import { useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import { FlatList, Pressable, ScrollView, View } from 'react-native';

import { ExerciseGuideCard } from '@/components/exercise/ExerciseGuideCard';
import { Screen, Text, TextField, TextLink } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';

type CatalogFilter = 'all' | 'strength' | 'bodyweight' | 'mobility';

const FILTERS: { label: string; value: CatalogFilter }[] = [
  { label: 'All', value: 'all' },
  { label: 'Strength', value: 'strength' },
  { label: 'Bodyweight', value: 'bodyweight' },
  { label: 'Mobility', value: 'mobility' },
];

function matchesFilter(exercise: Exercise, filter: CatalogFilter) {
  if (filter === 'all') return true;
  if (filter === 'strength') return exercise.exerciseType === 'weight_reps';
  if (filter === 'bodyweight') return exercise.equipment === 'Bodyweight' && !exercise.isStretch;
  return exercise.isStretch || exercise.primaryMuscle === 'Mobility';
}

export default function ExerciseIndex() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<CatalogFilter>('all');

  const results = useMemo(
    () =>
      (query.trim() ? searchExercises(query) : exercises).filter((item) =>
        matchesFilter(item, filter),
      ),
    [filter, query],
  );

  return (
    <Screen scroll={false} padding="lg">
      <View style={{ gap: Spacing.xs, marginTop: Spacing.xs }}>
        <TextLink label="‹ Back" onPress={() => router.back()} />
        <Text variant="caption" color="secondary" style={{ marginTop: Spacing.sm }}>
          EXERCISE ARCHIVE · {exercises.length} MOVEMENTS
        </Text>
        <Text variant="display">Movement Guide</Text>
        <Text variant="body" color="secondary">
          Search by exercise, muscle, or equipment.
        </Text>
      </View>

      <TextField
        accessibilityLabel="Search exercises"
        placeholder="Search exercises"
        value={query}
        onChangeText={setQuery}
        autoCapitalize="none"
        autoCorrect={false}
      />

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={{ flexGrow: 0 }}
        contentContainerStyle={{ gap: Spacing.sm }}
      >
        {FILTERS.map((item) => {
          const selected = item.value === filter;
          return (
            <Pressable
              key={item.value}
              accessibilityRole="button"
              accessibilityState={{ selected }}
              onPress={() => setFilter(item.value)}
              style={{
                paddingHorizontal: Spacing.lg,
                paddingVertical: Spacing.sm,
                borderRadius: Radius.pill,
                borderWidth: 1,
                borderColor: selected ? Palette.primary : Palette.hairlineStrong,
                backgroundColor: selected ? Palette.surface3 : Palette.surface1,
              }}
            >
              <Text variant="caption" color={selected ? 'accent' : 'secondary'}>
                {item.label}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      <View style={{ flex: 1, minHeight: 0 }}>
        <FlatList
          data={results}
          keyExtractor={(item) => item.id}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ gap: Spacing.sm, paddingBottom: Spacing.xl }}
          renderItem={({ item }) => (
            <ExerciseGuideCard
              exercise={item}
              onPress={() =>
                router.push({ pathname: '/exercise/[slug]', params: { slug: item.slug } } as never)
              }
            />
          )}
          ListEmptyComponent={
            <View style={{ paddingVertical: Spacing.xxxl, alignItems: 'center' }}>
              <Text variant="heading">No matching movements</Text>
              <Text variant="body" color="secondary">
                Try another exercise or muscle.
              </Text>
            </View>
          }
        />
      </View>
    </Screen>
  );
}
