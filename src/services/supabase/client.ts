/**
 * Typed Supabase client for the mobile app.
 *
 * Configuration comes from PUBLIC env vars only — never a service-role key in the
 * client (PLAN.txt §10, master prompt rule 9). Session persistence uses
 * AsyncStorage so auth survives app restarts (wired up for Phase 3).
 *
 * The client is created even when env vars are absent (they will be, until the
 * project is provisioned) so the app still boots during Phase 1. Callers can
 * check `isSupabaseConfigured` before performing real network work.
 */
import 'react-native-url-polyfill/auto';

import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient } from '@supabase/supabase-js';

import type { Database } from '@/types/database';

const url = process.env.EXPO_PUBLIC_SUPABASE_URL ?? '';
const anonKey = process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? '';

/** True only when both public env vars are present. */
export const isSupabaseConfigured = url.length > 0 && anonKey.length > 0;

if (!isSupabaseConfigured && __DEV__) {
  console.warn(
    '[supabase] EXPO_PUBLIC_SUPABASE_URL / EXPO_PUBLIC_SUPABASE_ANON_KEY are not set. ' +
      'Copy .env.example to .env and fill them in. Network calls will fail until then.',
  );
}

// Fall back to a harmless local placeholder so createClient does not throw at
// import time when the project has not been provisioned yet.
export const supabase = createClient<Database>(
  isSupabaseConfigured ? url : 'http://localhost:54321',
  isSupabaseConfigured ? anonKey : 'public-anon-key-placeholder',
  {
    auth: {
      storage: AsyncStorage,
      autoRefreshToken: true,
      persistSession: true,
      // Deep-link session detection is handled explicitly in the auth flow, not
      // by URL sniffing on a native client.
      detectSessionInUrl: false,
    },
  },
);
