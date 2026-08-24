import { Stack } from 'expo-router';

import { Palette } from '@/constants/theme';

export const unstable_settings = {
  initialRouteName: 'login',
};

/** Auth flow navigator: login, register, forgot-password. */
export default function AuthLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: Palette.background },
      }}
    >
      <Stack.Screen name="login" />
      <Stack.Screen name="register" />
      <Stack.Screen name="forgot-password" />
    </Stack>
  );
}
