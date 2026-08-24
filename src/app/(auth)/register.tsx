import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { View } from 'react-native';

import { Button, Card, Screen, Text, TextField, TextLink } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { registerSchema, signUp, type RegisterValues } from '@/features/auth';

export default function RegisterScreen() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '', confirmPassword: '' },
  });

  async function onSubmit(values: RegisterValues) {
    setFormError(null);
    setNotice(null);
    const result = await signUp(values.email, values.password);
    if (!result.ok) {
      setFormError(result.message);
      return;
    }
    if (result.needsEmailConfirmation) {
      setNotice('Account created. Check your inbox to confirm your email, then sign in.');
    }
    // If no confirmation is required, the auth listener signs the user in and the
    // guard redirects automatically.
  }

  return (
    <Screen>
      <View style={{ gap: Spacing.xs, marginTop: Spacing.xxl }}>
        <Text variant="caption" color="secondary">
          AWAKENING
        </Text>
        <Text variant="display">Create account</Text>
        <Text variant="body" color="secondary">
          Register to begin your Awakening.
        </Text>
      </View>

      {formError ? (
        <Card tone="raised" padding="md" accessibilityLiveRegion="polite">
          <Text variant="label" color="danger">
            {formError}
          </Text>
        </Card>
      ) : null}
      {notice ? (
        <Card tone="raised" padding="md" accessibilityLiveRegion="polite">
          <Text variant="label" color="success">
            {notice}
          </Text>
        </Card>
      ) : null}

      <View style={{ gap: Spacing.lg }}>
        <Controller
          control={control}
          name="email"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextField
              label="Email"
              placeholder="you@example.com"
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              textContentType="emailAddress"
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              error={errors.email?.message}
              returnKeyType="next"
            />
          )}
        />
        <Controller
          control={control}
          name="password"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextField
              label="Password"
              placeholder="At least 8 characters"
              autoCapitalize="none"
              autoComplete="password-new"
              textContentType="newPassword"
              secureToggle
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              error={errors.password?.message}
              returnKeyType="next"
            />
          )}
        />
        <Controller
          control={control}
          name="confirmPassword"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextField
              label="Confirm password"
              placeholder="Re-enter your password"
              autoCapitalize="none"
              autoComplete="password-new"
              textContentType="newPassword"
              secureToggle
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              error={errors.confirmPassword?.message}
              returnKeyType="done"
              onSubmitEditing={handleSubmit(onSubmit)}
            />
          )}
        />

        <Button label="Create account" onPress={handleSubmit(onSubmit)} loading={isSubmitting} />
      </View>

      <View style={{ flexDirection: 'row', justifyContent: 'center', gap: Spacing.sm }}>
        <Text variant="label" color="secondary">
          Already have an account?
        </Text>
        <TextLink label="Sign in" onPress={() => router.replace('/login')} />
      </View>
    </Screen>
  );
}
