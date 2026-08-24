import {
  forgotPasswordSchema,
  loginSchema,
  registerSchema,
  MIN_PASSWORD_LENGTH,
} from '@/features/auth/schemas';

describe('loginSchema', () => {
  it('accepts a valid email + non-empty password', () => {
    expect(loginSchema.safeParse({ email: 'a@b.com', password: 'x' }).success).toBe(true);
  });

  it('rejects an invalid email', () => {
    const r = loginSchema.safeParse({ email: 'nope', password: 'x' });
    expect(r.success).toBe(false);
  });

  it('rejects an empty password', () => {
    expect(loginSchema.safeParse({ email: 'a@b.com', password: '' }).success).toBe(false);
  });

  it('trims surrounding whitespace on email', () => {
    const r = loginSchema.safeParse({ email: '  a@b.com  ', password: 'x' });
    expect(r.success && r.data.email).toBe('a@b.com');
  });
});

describe('registerSchema', () => {
  it('accepts matching passwords of sufficient length', () => {
    const r = registerSchema.safeParse({
      email: 'a@b.com',
      password: 'abcd1234',
      confirmPassword: 'abcd1234',
    });
    expect(r.success).toBe(true);
  });

  it(`rejects passwords shorter than ${MIN_PASSWORD_LENGTH}`, () => {
    const r = registerSchema.safeParse({
      email: 'a@b.com',
      password: 'short',
      confirmPassword: 'short',
    });
    expect(r.success).toBe(false);
  });

  it('rejects mismatched passwords with a path on confirmPassword', () => {
    const r = registerSchema.safeParse({
      email: 'a@b.com',
      password: 'abcd1234',
      confirmPassword: 'abcd9999',
    });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues.some((i) => i.path.includes('confirmPassword'))).toBe(true);
    }
  });
});

describe('forgotPasswordSchema', () => {
  it('accepts a valid email', () => {
    expect(forgotPasswordSchema.safeParse({ email: 'a@b.com' }).success).toBe(true);
  });
  it('rejects a missing email', () => {
    expect(forgotPasswordSchema.safeParse({ email: '' }).success).toBe(false);
  });
});
