import { Stack } from 'expo-router';

import { Palette } from '@/constants/theme';

/** Exercise detail navigator (built in later phases). */
export default function ExerciseLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: Palette.background },
      }}
    />
  );
}
