import { Stack } from 'expo-router';

import { Palette } from '@/constants/theme';

/** Active-workout flow navigator. */
export default function WorkoutLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: Palette.background },
      }}
    >
      <Stack.Screen name="index" />
      <Stack.Screen name="complete" />
    </Stack>
  );
}
