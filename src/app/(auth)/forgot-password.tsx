import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { View } from 'react-native';

import { Button, Card, Screen, Text, TextLink, TextField } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import {
  forgotPasswordSchema,
  sendPasswordReset,
  type ForgotPasswordValues,
} from '@/features/auth';

export default function ForgotPasswordScreen() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  async function onSubmit(values: ForgotPasswordValues) {
    setFormError(null);
    const result = await sendPasswordReset(values.email);
    if (!result.ok) {
      setFormError(result.message);
      return;
    }
    setSent(true);
  }

  return (
    <Screen>
      <View style={{ gap: Spacing.xs, marginTop: Spacing.xxl }}>
        <Text variant="caption" color="secondary">
          RECOVERY
        </Text>
        <Text variant="display">Reset password</Text>
        <Text variant="body" color="secondary">
          Enter your email and we will send a link to reset your password.
        </Text>
      </View>

      {formError ? (
        <Card tone="raised" padding="md" accessibilityLiveRegion="polite">
          <Text variant="label" color="danger">
            {formError}
          </Text>
        </Card>
      ) : null}

      {sent ? (
        <Card accessibilityLiveRegion="polite">
          <Text variant="heading" color="success" style={{ marginBottom: Spacing.xs }}>
            Check your inbox
          </Text>
          <Text variant="body" color="secondary">
            If an account exists for that email, a reset link is on its way.
          </Text>
        </Card>
      ) : (
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
                returnKeyType="done"
                onSubmitEditing={handleSubmit(onSubmit)}
              />
            )}
          />
          <Button label="Send reset link" onPress={handleSubmit(onSubmit)} loading={isSubmitting} />
        </View>
      )}

      <View style={{ flexDirection: 'row', justifyContent: 'center' }}>
        <TextLink label="Back to sign in" onPress={() => router.replace('/login')} />
      </View>
    </Screen>
  );
}
