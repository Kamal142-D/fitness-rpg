import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';

import { Palette } from '@/constants/theme';
import { useAuth, useProtectedRoute } from '@/features/auth';
import { AppProviders } from '@/providers/AppProviders';

// Keep the native splash up until the session has been resolved.
void SplashScreen.preventAutoHideAsync();

/**
 * Navigator + route guard. Lives inside AppProviders so it can read auth state
 * and drive redirects. The Stack is always mounted (so the router context is
 * available); the native splash simply hides once the session is known.
 */
function RootNavigator() {
  const { initializing } = useAuth();
  useProtectedRoute();

  useEffect(() => {
    if (!initializing) void SplashScreen.hideAsync();
  }, [initializing]);

  return (
    <>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: Palette.background },
        }}
      >
        <Stack.Screen name="index" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="(auth)" />
        <Stack.Screen name="(onboarding)" />
        <Stack.Screen name="gate" />
        <Stack.Screen name="workout" />
        <Stack.Screen name="exercise" />
        <Stack.Screen name="settings" />
      </Stack>
    </>
  );
}

export default function RootLayout() {
  return (
    <AppProviders>
      <RootNavigator />
    </AppProviders>
  );
}
