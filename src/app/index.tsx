import { Redirect } from 'expo-router';

import { Splash } from '@/components/Splash';
import { useAuth } from '@/features/auth';
import { useProfile } from '@/features/onboarding';

/**
 * App entry gate. Resolve session, then profile, then route:
 * signed-out -> sign-in; onboarding incomplete -> the Awakening; else the app.
 */
export default function Index() {
  const { session, initializing } = useAuth();
  const { data: profile, isLoading } = useProfile();

  if (initializing) return <Splash />;
  if (!session) return <Redirect href="/login" />;
  if (isLoading) return <Splash />;
  if (profile && !profile.onboarding_completed) return <Redirect href="/awakening" />;
  return <Redirect href="/system" />;
}
