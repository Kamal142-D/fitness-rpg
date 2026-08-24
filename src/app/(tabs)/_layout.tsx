import { Tabs } from 'expo-router';

import { TabGlyph } from '@/components/navigation/TabGlyph';
import { Fonts, Palette } from '@/constants/theme';

/**
 * Primary tab shell (PLAN.txt §7): SYSTEM · GATES · START · QUESTS · PLAYER.
 * Screens are placeholders in Phase 1 — real feature content lands in later
 * phases. `START` is the center action that will launch a workout.
 */
export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: Palette.primary,
        tabBarInactiveTintColor: Palette.textSecondary,
        tabBarStyle: {
          backgroundColor: Palette.surface1,
          borderTopColor: Palette.hairline,
          borderTopWidth: 1,
        },
        tabBarLabelStyle: {
          fontFamily: Fonts.sans,
          fontSize: 11,
          fontWeight: '600',
        },
        tabBarIcon: ({ color, focused }) => <TabGlyph color={String(color)} focused={focused} />,
      }}
    >
      <Tabs.Screen name="system" options={{ title: 'System' }} />
      <Tabs.Screen name="gates" options={{ title: 'Gates' }} />
      <Tabs.Screen name="start" options={{ title: 'Start' }} />
      <Tabs.Screen name="quests" options={{ title: 'Quests' }} />
      <Tabs.Screen name="player" options={{ title: 'Player' }} />
    </Tabs>
  );
}
