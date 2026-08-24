import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import { claimQuest, listUserQuests } from '@/features/quests/api';

export function useQuests() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['quests', user?.id],
    queryFn: listUserQuests,
    enabled: !!user?.id,
    staleTime: 30_000,
  });
}

export function useClaimQuest() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userQuestId: string) => claimQuest(userQuestId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['quests', user?.id] });
      void queryClient.invalidateQueries({ queryKey: ['progression', user?.id] });
    },
  });
}
