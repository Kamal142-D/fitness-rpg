import { useRouter, useSegments } from 'expo-router';
import { useEffect } from 'react';

import { useAuth } from '@/features/auth/AuthProvider';
import { useProfile } from '@/features/onboarding/useProfile';

/**
 * Route guard. Once session + profile are resolved:
 *   - signed-out anywhere outside (auth) -> sign-in;
 *   - signed-in but onboarding not completed -> the Awakening;
 *   - signed-in + onboarded but still in (auth)/(onboarding) -> into the app.
 *
 * If the profile can't be loaded (e.g. no backend configured yet), the
 * onboarding redirect is skipped rather than risking a redirect loop.
 */
export function useProtectedRoute(): void {
  const { session, initializing } = useAuth();
  const { data: profile, isLoading, isError } = useProfile();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (initializing) return;

    const inAuthGroup = segments[0] === '(auth)';
    const inOnboarding = segments[0] === '(onboarding)';

    if (!session) {
      if (!inAuthGroup) router.replace('/login');
      return;
    }

    // Signed in: wait for the profile query to settle before onboarding routing.
    if (isLoading) return;

    const needsOnboarding = !isError && profile != null && !profile.onboarding_completed;

    if (needsOnboarding) {
      if (!inOnboarding) router.replace('/awakening');
      return;
    }

    if (inAuthGroup || inOnboarding) {
      router.replace('/system');
    }
  }, [session, initializing, profile, isLoading, isError, segments, router]);
}
