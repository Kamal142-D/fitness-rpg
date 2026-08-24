import type { Session, User } from '@supabase/supabase-js';
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { supabase } from '@/services/supabase';

interface AuthState {
  session: Session | null;
  user: User | null;
  /** True until the initial session has been resolved from storage. */
  initializing: boolean;
}

const AuthContext = createContext<AuthState>({
  session: null,
  user: null,
  initializing: true,
});

/**
 * Tracks the Supabase auth session. Reads the persisted session on mount (kept
 * in AsyncStorage by the client) and subscribes to future auth changes, so the
 * whole app reacts to sign-in / sign-out without prop drilling.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    let mounted = true;

    supabase.auth.getSession().then(({ data }) => {
      if (!mounted) return;
      setSession(data.session);
      setInitializing(false);
    });

    const { data: sub } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession);
    });

    return () => {
      mounted = false;
      sub.subscription.unsubscribe();
    };
  }, []);

  const value = useMemo<AuthState>(
    () => ({ session, user: session?.user ?? null, initializing }),
    [session, initializing],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** Read the current auth state. */
export function useAuth(): AuthState {
  return useContext(AuthContext);
}
