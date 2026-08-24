import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/features/auth/AuthProvider';
import {
  createGate,
  getGate,
  getRecommendedGate,
  listExercises,
  listGates,
} from '@/features/gates/api';
import type { CreateGateInput } from '@/features/gates/types';

export function useGates() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['gates', user?.id],
    queryFn: listGates,
    enabled: !!user?.id,
    staleTime: 60_000,
  });
}

export function useGate(templateId: string | undefined) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['gate', templateId],
    queryFn: () => getGate(templateId!),
    enabled: !!user?.id && !!templateId,
    staleTime: 60_000,
  });
}

export function useRecommendedGate() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['gate-recommended', user?.id],
    queryFn: getRecommendedGate,
    enabled: !!user?.id,
    staleTime: 60_000,
  });
}

export function useExercises() {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['exercises'],
    queryFn: listExercises,
    enabled: !!user?.id,
    staleTime: 5 * 60_000,
  });
}

export function useCreateGate() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateGateInput) => createGate(user!.id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['gates', user?.id] });
    },
  });
}
