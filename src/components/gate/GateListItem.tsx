import { Pressable, View } from 'react-native';

import { RankBadge, Text } from '@/components/ui';
import { Palette, Radius, Spacing } from '@/constants/theme';
import {
  intensityForDifficulty,
  muscleGroupsFor,
  templateDifficulty,
} from '@/features/gates/mappers';
import type { GateTemplate } from '@/features/gates/types';

export interface GateListItemProps {
  template: GateTemplate;
  onPress: () => void;
}

/** A tappable Gate row for the Gates list. */
export function GateListItem({ template, onPress }: GateListItemProps) {
  const difficulty = templateDifficulty(template);
  const muscles = muscleGroupsFor(template);
  const duration = template.estimated_duration_minutes;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${template.name} gate, difficulty ${difficulty}`}
      onPress={onPress}
      style={({ pressed }) => ({
        flexDirection: 'row',
        alignItems: 'center',
        gap: Spacing.lg,
        backgroundColor: pressed ? Palette.surface2 : Palette.surface1,
        borderColor: Palette.hairline,
        borderWidth: 1,
        borderRadius: Radius.lg,
        padding: Spacing.lg,
      })}
    >
      <RankBadge rank={difficulty} size="md" />
      <View style={{ flex: 1, gap: 2 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: Spacing.sm }}>
          <Text variant="heading">{template.name}</Text>
          {template.is_system_template ? (
            <Text variant="caption" color="tertiary">
              SYSTEM
            </Text>
          ) : null}
        </View>
        {muscles.length > 0 ? (
          <Text variant="caption" color="secondary">
            {muscles.join(' · ')}
          </Text>
        ) : null}
        <Text variant="caption" color="tertiary">
          {intensityForDifficulty(difficulty)}
          {duration ? ` · ${duration} min` : ''}
        </Text>
      </View>
    </Pressable>
  );
}
