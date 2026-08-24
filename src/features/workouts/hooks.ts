import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import { completeWorkout } from '@/features/workouts/api';
import type { CompletionPayload } from '@/features/workouts/payload';

/**
 * Persist a completed workout. On success, invalidate the queries whose data a
 * finished workout affects (progression, history) so they refetch.
 */
export function useCompleteWorkout() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CompletionPayload) => completeWorkout(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['progression', user?.id] });
      void queryClient.invalidateQueries({ queryKey: ['workout-history', user?.id] });
    },
  });
}
