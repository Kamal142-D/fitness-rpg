import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import { getProfile } from '@/features/onboarding/api';

/**
 * Fetches the signed-in user's profile row. Used by the router guard to decide
 * whether onboarding is complete. Disabled when there is no session.
 */
export function useProfile() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['profile', user?.id],
    queryFn: () => getProfile(user!.id),
    enabled: !!user?.id,
    staleTime: 60_000,
  });
}
