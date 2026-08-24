import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import { getProgression } from '@/features/progression/api';

/** Query the signed-in user's progression (level, XP, attributes, rank, streak). */
export function useProgression() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['progression', user?.id],
    queryFn: () => getProgression(user!.id),
    enabled: !!user?.id,
    staleTime: 30_000,
  });
}
