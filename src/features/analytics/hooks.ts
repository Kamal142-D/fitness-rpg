import { useQuery } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import { getPlayerData } from '@/features/analytics/api';

export function usePlayerData() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['player-data', user?.id],
    queryFn: () => getPlayerData(user!.id),
    enabled: !!user?.id,
    staleTime: 60_000,
  });
}
