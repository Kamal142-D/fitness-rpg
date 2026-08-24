import { View } from 'react-native';

import { Splash } from '@/components/Splash';
import { QuestCard } from '@/components/quests/QuestCard';
import { Button, Card, Screen, Text } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { useClaimQuest, useQuests, type UserQuestView } from '@/features/quests';
import { isSupabaseConfigured } from '@/services/supabase';

export default function QuestsScreen() {
  const questsQ = useQuests();
  const claimMut = useClaimQuest();

  if (questsQ.isLoading) return <Splash label="Loading Quests" />;

  const header = (
    <View style={{ gap: Spacing.xs }}>
      <Text variant="caption" color="secondary">
        QUESTS
      </Text>
      <Text variant="display">Quests</Text>
      <Text variant="body" color="secondary">
        Complete objectives from your training to earn XP.
      </Text>
    </View>
  );

  if (!isSupabaseConfigured) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            Connect a server (add your Supabase keys to .env) to receive daily and weekly quests.
          </Text>
        </Card>
      </Screen>
    );
  }

  if (questsQ.isError) {
    return (
      <Screen>
        {header}
        <Card>
          <Text variant="body" color="secondary">
            Couldn&apos;t load quests. Check your connection and try again.
          </Text>
        </Card>
        <Button label="Retry" variant="secondary" onPress={() => void questsQ.refetch()} />
      </Screen>
    );
  }

  const quests = questsQ.data ?? [];
  const daily = quests.filter((q) => q.type === 'daily');
  const weekly = quests.filter((q) => q.type === 'weekly');

  const claimingId = claimMut.isPending ? claimMut.variables : undefined;
  const renderQuest = (q: UserQuestView) => (
    <QuestCard
      key={q.id}
      quest={q}
      onClaim={() => claimMut.mutate(q.id)}
      claiming={claimingId === q.id}
    />
  );

  return (
    <Screen>
      {header}

      {quests.length === 0 ? (
        <Card>
          <Text variant="body" color="secondary">
            No active quests right now. Check back after your next session.
          </Text>
        </Card>
      ) : (
        <>
          {daily.length > 0 ? (
            <View style={{ gap: Spacing.sm }}>
              <Text variant="caption" color="secondary">
                DAILY
              </Text>
              {daily.map(renderQuest)}
            </View>
          ) : null}
          {weekly.length > 0 ? (
            <View style={{ gap: Spacing.sm }}>
              <Text variant="caption" color="secondary">
                WEEKLY
              </Text>
              {weekly.map(renderQuest)}
            </View>
          ) : null}
        </>
      )}
    </Screen>
  );
}
