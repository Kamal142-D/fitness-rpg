/**
 * Map raw Supabase auth error messages to friendly, non-technical copy.
 * Falls back to the raw message (or a generic line) when nothing matches.
 */
export function friendlyAuthError(message?: string | null): string {
  if (!message) return 'Something went wrong. Please try again.';
  const m = message.toLowerCase();

  if (m.includes('invalid login credentials')) {
    return 'The email or password is incorrect.';
  }
  if (m.includes('email not confirmed')) {
    return 'Please confirm your email first. Check your inbox for the link.';
  }
  if (m.includes('already registered') || m.includes('already been registered')) {
    return 'An account with this email already exists. Try signing in instead.';
  }
  if (m.includes('password should be at least') || m.includes('password is too short')) {
    return 'That password is too short.';
  }
  if (m.includes('unable to validate email address') || m.includes('invalid email')) {
    return "That email address doesn't look valid.";
  }
  if (
    m.includes('rate limit') ||
    m.includes('too many requests') ||
    m.includes('for security purposes')
  ) {
    return 'Too many attempts. Please wait a moment and try again.';
  }
  if (m.includes('network') || m.includes('failed to fetch') || m.includes('fetch failed')) {
    return 'Network error. Check your connection and try again.';
  }
  return message;
}
