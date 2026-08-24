import { Stack } from 'expo-router';

import { Palette } from '@/constants/theme';

export const unstable_settings = {
  initialRouteName: 'awakening',
};

/** Awakening onboarding navigator. */
export default function OnboardingLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: Palette.background },
      }}
    >
      <Stack.Screen name="awakening" />
    </Stack>
  );
}
