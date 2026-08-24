import { View } from 'react-native';

import { Button, Card, ProgressBar, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import type { UserQuestView } from '@/features/quests';

export interface QuestCardProps {
  quest: UserQuestView;
  onClaim: () => void;
  claiming?: boolean;
}

/** A quest with its progress and (when complete) a claim action. */
export function QuestCard({ quest, onClaim, claiming }: QuestCardProps) {
  const fraction = quest.requirementValue > 0 ? quest.progress / quest.requirementValue : 0;
  const progressLabel = `${trim(quest.progress)} / ${trim(quest.requirementValue)}`;

  return (
    <Card>
      <View
        style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' }}
      >
        <View style={{ flex: 1, gap: 2 }}>
          <Text variant="heading">{quest.name}</Text>
          {quest.description ? (
            <Text variant="caption" color="secondary">
              {quest.description}
            </Text>
          ) : null}
        </View>
        <Text variant="caption" color="tertiary">
          +{quest.xpReward} XP
        </Text>
      </View>

      <View style={{ marginTop: Spacing.md, gap: Spacing.xs }}>
        <ProgressBar value={fraction} />
        <View
          style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}
        >
          <Text variant="caption" color="secondary" mono>
            {progressLabel}
          </Text>
          {quest.claimed ? (
            <Text variant="caption" color="success">
              CLAIMED
            </Text>
          ) : quest.completed ? (
            <Text variant="caption" color="success">
              READY
            </Text>
          ) : null}
        </View>
      </View>

      {quest.completed && !quest.claimed ? (
        <Button
          label={`Claim +${quest.xpReward} XP`}
          onPress={onClaim}
          loading={claiming}
          style={{ marginTop: Spacing.md }}
        />
      ) : null}
    </Card>
  );
}

function trim(n: number): string {
  return Number.isInteger(n) ? String(n) : n.toFixed(0);
}
