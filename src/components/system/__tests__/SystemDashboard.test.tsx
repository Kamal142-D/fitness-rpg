import { fireEvent, render } from '@testing-library/react-native';

import { SystemDashboard } from '@/components/system/SystemDashboard';
import { STARTER_GATE } from '@/constants/gates';

const data = {
  displayName: 'Kai',
  level: 3,
  currentXp: 120,
  hunterRank: 'B' as const,
  hunterScore: 57,
  attributes: { strength: 60, physique: 48, endurance: 42, discipline: 55 },
  streakDays: 4,
  longestStreakDays: 9,
};

describe('SystemDashboard', () => {
  it('renders identity, rank, level and attributes', () => {
    const { getByText } = render(
      <SystemDashboard
        {...data}
        gate={STARTER_GATE}
        onEnterGate={() => {}}
        onSettings={() => {}}
      />,
    );
    expect(getByText('Kai')).toBeTruthy();
    expect(getByText('Rank B')).toBeTruthy();
    expect(getByText('Level 3')).toBeTruthy();
    expect(getByText('Strength')).toBeTruthy();
    expect(getByText('Discipline')).toBeTruthy();
    expect(getByText('4 d')).toBeTruthy();
  });

  it('fires the gate and settings actions', () => {
    const onEnterGate = jest.fn();
    const onSettings = jest.fn();
    const { getByText } = render(
      <SystemDashboard
        {...data}
        gate={STARTER_GATE}
        onEnterGate={onEnterGate}
        onSettings={onSettings}
      />,
    );
    fireEvent.press(getByText('Enter Gate'));
    fireEvent.press(getByText('Settings'));
    expect(onEnterGate).toHaveBeenCalledTimes(1);
    expect(onSettings).toHaveBeenCalledTimes(1);
  });
});
