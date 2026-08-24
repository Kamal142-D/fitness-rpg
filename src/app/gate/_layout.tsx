import { Stack } from 'expo-router';

import { Palette } from '@/constants/theme';

/** Gate details / creation navigator. */
export default function GateLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: Palette.background },
      }}
    >
      <Stack.Screen name="[id]" />
      <Stack.Screen name="new" />
    </Stack>
  );
}
