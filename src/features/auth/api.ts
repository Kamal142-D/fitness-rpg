import * as Linking from 'expo-linking';

import { isSupabaseConfigured, supabase } from '@/services/supabase';
import { friendlyAuthError } from '@/features/auth/errors';

export type AuthResult = { ok: true } | { ok: false; message: string };

export type SignUpResult =
  { ok: true; needsEmailConfirmation: boolean } | { ok: false; message: string };

const NOT_CONFIGURED =
  'The app is not connected to a server yet. Add your Supabase keys to .env, then restart.';

function configError(): string | null {
  return isSupabaseConfigured ? null : NOT_CONFIGURED;
}

/** Sign in an existing user with email + password. */
export async function signIn(email: string, password: string): Promise<AuthResult> {
  const cfg = configError();
  if (cfg) return { ok: false, message: cfg };

  const { error } = await supabase.auth.signInWithPassword({ email, password });
  return error ? { ok: false, message: friendlyAuthError(error.message) } : { ok: true };
}

/**
 * Register a new user. When the project requires email confirmation, Supabase
 * returns no session; the caller should tell the user to check their inbox.
 */
export async function signUp(email: string, password: string): Promise<SignUpResult> {
  const cfg = configError();
  if (cfg) return { ok: false, message: cfg };

  const { data, error } = await supabase.auth.signUp({ email, password });
  if (error) return { ok: false, message: friendlyAuthError(error.message) };
  return { ok: true, needsEmailConfirmation: data.session === null };
}

/** Send a password-reset email that deep-links back into the app. */
export async function sendPasswordReset(email: string): Promise<AuthResult> {
  const cfg = configError();
  if (cfg) return { ok: false, message: cfg };

  const redirectTo = Linking.createURL('/reset-password');
  const { error } = await supabase.auth.resetPasswordForEmail(email, { redirectTo });
  return error ? { ok: false, message: friendlyAuthError(error.message) } : { ok: true };
}

/** Sign the current user out. */
export async function signOut(): Promise<AuthResult> {
  const { error } = await supabase.auth.signOut();
  return error ? { ok: false, message: friendlyAuthError(error.message) } : { ok: true };
}
