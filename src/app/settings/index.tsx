import { useState } from 'react';
import { View } from 'react-native';

import { UpdateSection } from '@/components/settings/UpdateSection';
import { Button, Card, Screen, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { signOut, useAuth } from '@/features/auth';

export default function SettingsScreen() {
  const { user } = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  async function onSignOut() {
    setSigningOut(true);
    const result = await signOut();
    // On success the auth listener clears the session and the guard redirects.
    if (!result.ok) setSigningOut(false);
  }

  return (
    <Screen>
      <View style={{ gap: Spacing.xs }}>
        <Text variant="caption" color="secondary">
          ACCOUNT
        </Text>
        <Text variant="display">Settings</Text>
      </View>

      <Card>
        <Text variant="caption" color="secondary" style={{ marginBottom: Spacing.xs }}>
          SIGNED IN AS
        </Text>
        <Text variant="body">{user?.email ?? 'Unknown'}</Text>
      </Card>

      <UpdateSection />

      <Button label="Sign out" variant="secondary" onPress={onSignOut} loading={signingOut} />
    </Screen>
  );
}
