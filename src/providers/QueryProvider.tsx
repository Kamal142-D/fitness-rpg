import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { type ReactNode, useState } from 'react';

/**
 * Provides the TanStack Query client to the app. TanStack Query owns SERVER
 * state (remote data + caching); Zustand is only for small, local client state
 * and must not duplicate this cache (PLAN.txt §4).
 *
 * The client is created once per app instance via lazy `useState` so it is not
 * re-instantiated on re-render.
 */
export function QueryProvider({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            retry: 2,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
