export { AuthProvider, useAuth } from '@/features/auth/AuthProvider';
export { useProtectedRoute } from '@/features/auth/useProtectedRoute';
export { signIn, signUp, signOut, sendPasswordReset } from '@/features/auth/api';
export type { AuthResult, SignUpResult } from '@/features/auth/api';
export {
  loginSchema,
  registerSchema,
  forgotPasswordSchema,
  emailSchema,
  MIN_PASSWORD_LENGTH,
} from '@/features/auth/schemas';
export type { LoginValues, RegisterValues, ForgotPasswordValues } from '@/features/auth/schemas';
export { friendlyAuthError } from '@/features/auth/errors';
