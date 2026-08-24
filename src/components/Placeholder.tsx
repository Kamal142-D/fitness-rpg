import { View } from 'react-native';

import { Card, Screen, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';

/**
 * Generic "not built yet" screen used for Phase 1 route placeholders. Names the
 * screen and the phase that will implement it, so the navigation shell is
 * walkable without pretending features exist.
 */
export function Placeholder({ title, phase }: { title: string; phase: string }) {
  return (
    <Screen>
      <View style={{ gap: Spacing.xs }}>
        <Text variant="caption" color="secondary">
          PLACEHOLDER
        </Text>
        <Text variant="display">{title}</Text>
      </View>
      <Card>
        <Text variant="body" color="secondary">
          This screen is part of the navigation shell only. It will be built in {phase}.
        </Text>
      </Card>
    </Screen>
  );
}
