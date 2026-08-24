import { Stack } from 'expo-router';

import { Palette } from '@/constants/theme';

/** Settings navigator (built in later phases). */
export default function SettingsLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: Palette.background },
      }}
    />
  );
}
