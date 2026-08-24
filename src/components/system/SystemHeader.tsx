import { View } from 'react-native';

import { Text, TextLink } from '@/components/ui';
import { Spacing } from '@/constants/theme';

export interface SystemHeaderProps {
  displayName?: string | null;
  onSettings: () => void;
}

/** Dashboard header: the SYSTEM eyebrow, a greeting, and a settings action. */
export function SystemHeader({ displayName, onSettings }: SystemHeaderProps) {
  return (
    <View
      style={{ flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between' }}
    >
      <View style={{ gap: Spacing.xs, flex: 1 }}>
        <Text variant="caption" color="secondary">
          SYSTEM
        </Text>
        <Text variant="display">{displayName ? displayName : 'Hunter'}</Text>
      </View>
      <TextLink label="Settings" onPress={onSettings} />
    </View>
  );
}
