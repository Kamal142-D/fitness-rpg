import { friendlyAuthError } from '@/features/auth/errors';

describe('friendlyAuthError', () => {
  it('returns a generic message for empty input', () => {
    expect(friendlyAuthError()).toMatch(/something went wrong/i);
    expect(friendlyAuthError(null)).toMatch(/something went wrong/i);
  });

  it('maps invalid credentials', () => {
    expect(friendlyAuthError('Invalid login credentials')).toMatch(/incorrect/i);
  });

  it('maps unconfirmed email', () => {
    expect(friendlyAuthError('Email not confirmed')).toMatch(/confirm/i);
  });

  it('maps an already-registered email', () => {
    expect(friendlyAuthError('User already registered')).toMatch(/already exists/i);
  });

  it('maps rate limiting', () => {
    expect(friendlyAuthError('Too many requests')).toMatch(/too many/i);
  });

  it('maps network failures', () => {
    expect(friendlyAuthError('Network request failed')).toMatch(/network/i);
  });

  it('falls back to the raw message when unmatched', () => {
    expect(friendlyAuthError('Some novel server error')).toBe('Some novel server error');
  });
});
