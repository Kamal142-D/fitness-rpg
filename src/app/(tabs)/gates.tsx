import { useRouter } from 'expo-router';
import { View } from 'react-native';

import { Splash } from '@/components/Splash';
import { GateListItem } from '@/components/gate/GateListItem';
import { Button, Card, Screen, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useGates } from '@/features/gates';
import { isSupabaseConfigured } from '@/services/supabase';

export default function GatesScreen() {
  const router = useRouter();
  const gatesQ = useGates();

  if (gatesQ.isLoading) return <Splash label="Loading Gates" />;

  const header = (
    <View style={{ gap: Spacing.xs }}>
      <Text variant="caption" color="secondary">
        GATES
      </Text>
      <Text variant="display">Choose a Gate</Text>
      <Text variant="body" color="secondary">
        Pick a workout to enter. Difficulty is set before you train.
      </Text>
    </View>
  );

  if (!isSupabaseConfigured) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            Connect a server (add your Supabase keys to .env) to load the Gate library.
          </Text>
        </Card>
      </Screen>
    );
  }

  if (gatesQ.isError) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            Couldn&apos;t load Gates. Check your connection and try again.
          </Text>
        </Card>
        <Button label="Retry" variant="secondary" onPress={() => void gatesQ.refetch()} />
      </Screen>
    );
  }

  const gates = gatesQ.data ?? [];

  return (
    <Screen>
      {header}

      {gates.length === 0 ? (
        <Card>
          <Text variant="body" color="secondary">
            No Gates yet. Create your first custom Gate to get started.
          </Text>
        </Card>
      ) : (
        <View style={{ gap: Spacing.sm }}>
          {gates.map((template) => (
            <GateListItem
              key={template.id}
              template={template}
              onPress={() => router.push({ pathname: '/gate/[id]', params: { id: template.id } })}
            />
          ))}
        </View>
      )}

      <Button
        label="Create custom Gate"
        variant="secondary"
        onPress={() => router.push('/gate/new')}
      />
      <Button
        label="Browse movement guide"
        variant="secondary"
        onPress={() => router.push('/exercise')}
      />
    </Screen>
  );
}
