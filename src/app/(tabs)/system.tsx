import { useRouter } from 'expo-router';
import { View } from 'react-native';

import { Splash } from '@/components/Splash';
import { SystemDashboard } from '@/components/system/SystemDashboard';
import { Button, Card, Screen, Text } from '@/components/ui';
import { STARTER_GATE } from '@/constants/gates';
import type { Rank } from '@/constants/ranks';
import { Spacing } from '@/constants/theme';
import { templateToSuggestedGate, useRecommendedGate } from '@/features/gates';
import { useProfile } from '@/features/onboarding';
import { useProgression } from '@/features/progression';
import { isSupabaseConfigured } from '@/services/supabase';

export default function SystemScreen() {
  const router = useRouter();
  const profileQ = useProfile();
  const progQ = useProgression();
  const recommendedQ = useRecommendedGate();

  if (profileQ.isLoading || progQ.isLoading) {
    return <Splash label="Loading System" />;
  }

  const goSettings = () => router.push('/settings');

  if (!isSupabaseConfigured) {
    return (
      <Screen>
        <StateCard
          title="Not connected"
          body="Add your Supabase URL and anon key to .env, then restart, to load your System."
          actionLabel="Account & settings"
          onAction={goSettings}
        />
      </Screen>
    );
  }

  if (profileQ.isError || progQ.isError) {
    return (
      <Screen>
        <StateCard
          title="Couldn't load your System"
          body="Something went wrong reaching the server. Check your connection and try again."
          actionLabel="Retry"
          onAction={() => {
            void profileQ.refetch();
            void progQ.refetch();
          }}
        />
      </Screen>
    );
  }

  const progression = progQ.data;
  if (!progression) {
    return (
      <Screen>
        <StateCard
          title="No data yet"
          body="Your progression hasn't been set up. Complete the Awakening to begin."
          actionLabel="Go to Awakening"
          onAction={() => router.replace('/awakening')}
        />
      </Screen>
    );
  }

  const recommended = recommendedQ.data ?? null;
  const gate = recommended ? templateToSuggestedGate(recommended) : STARTER_GATE;
  const onEnterGate = recommended
    ? () => router.push({ pathname: '/gate/[id]', params: { id: recommended.id } })
    : () => router.push('/gates');

  return (
    <Screen>
      <SystemDashboard
        displayName={profileQ.data?.display_name}
        level={progression.level}
        currentXp={progression.current_xp}
        hunterRank={progression.hunter_rank as Rank}
        hunterScore={progression.hunter_score}
        attributes={{
          strength: progression.strength_score,
          physique: progression.physique_score,
          endurance: progression.endurance_score,
          discipline: progression.discipline_score,
        }}
        streakDays={progression.current_streak_days}
        longestStreakDays={progression.longest_streak_days}
        gate={gate}
        onEnterGate={onEnterGate}
        onSettings={goSettings}
      />
    </Screen>
  );
}

function StateCard({
  title,
  body,
  actionLabel,
  onAction,
}: {
  title: string;
  body: string;
  actionLabel: string;
  onAction: () => void;
}) {
  return (
    <View style={{ gap: Spacing.lg, marginTop: Spacing.xxl }}>
      <View style={{ gap: Spacing.xs }}>
        <Text variant="caption" color="secondary">
          SYSTEM
        </Text>
        <Text variant="display">{title}</Text>
      </View>
      <Card>
        <Text variant="body" color="secondary">
          {body}
        </Text>
      </Card>
      <Button label={actionLabel} variant="secondary" onPress={onAction} />
    </View>
  );
}
