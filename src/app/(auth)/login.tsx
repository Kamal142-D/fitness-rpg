import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { View } from 'react-native';

import { Button, Card, Screen, Text, TextField, TextLink } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { loginSchema, signIn, type LoginValues } from '@/features/auth';

export default function LoginScreen() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  async function onSubmit(values: LoginValues) {
    setFormError(null);
    const result = await signIn(values.email, values.password);
    if (!result.ok) setFormError(result.message);
    // On success the auth listener updates the session and the guard redirects.
  }

  return (
    <Screen>
      <View style={{ gap: Spacing.xs, marginTop: Spacing.xxl }}>
        <Text variant="caption" color="secondary">
          THE SYSTEM
        </Text>
        <Text variant="display">Sign in</Text>
        <Text variant="body" color="secondary">
          Welcome back, Hunter. Continue your ascent.
        </Text>
      </View>

      {formError ? (
        <Card tone="raised" padding="md" accessibilityLiveRegion="polite">
          <Text variant="label" color="danger">
            {formError}
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
              placeholder="Your password"
              autoCapitalize="none"
              autoComplete="password"
              textContentType="password"
              secureToggle
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              error={errors.password?.message}
              returnKeyType="done"
              onSubmitEditing={handleSubmit(onSubmit)}
            />
          )}
        />

        <View style={{ alignItems: 'flex-end' }}>
          <TextLink label="Forgot password?" onPress={() => router.push('/forgot-password')} />
        </View>

        <Button label="Sign in" onPress={handleSubmit(onSubmit)} loading={isSubmitting} />
      </View>

      <View style={{ flexDirection: 'row', justifyContent: 'center', gap: Spacing.sm }}>
        <Text variant="label" color="secondary">
          New here?
        </Text>
        <TextLink label="Create an account" onPress={() => router.replace('/register')} />
      </View>
    </Screen>
  );
}
