import { fireEvent, render } from '@testing-library/react-native';

import { WorkoutSetRow } from '@/components/workout/WorkoutSetRow';
import type { ActiveSet } from '@/features/workouts/types';

function makeSet(overrides: Partial<ActiveSet> = {}): ActiveSet {
  return {
    id: 's1',
    setNumber: 1,
    weightKg: null,
    reps: 5,
    rpe: null,
    isWarmup: false,
    isCompleted: false,
    completedAt: null,
    ...overrides,
  };
}

describe('WorkoutSetRow', () => {
  it('reports numeric weight changes', () => {
    const onChangeWeight = jest.fn();
    const { getByLabelText } = render(
      <WorkoutSetRow
        set={makeSet()}
        onChangeWeight={onChangeWeight}
        onChangeReps={() => {}}
        onToggleComplete={() => {}}
        onToggleWarmup={() => {}}
      />,
    );
    fireEvent.changeText(getByLabelText('kg'), '100');
    expect(onChangeWeight).toHaveBeenCalledWith(100);
  });

  it('reports empty weight as null', () => {
    const onChangeWeight = jest.fn();
    const { getByLabelText } = render(
      <WorkoutSetRow
        set={makeSet({ weightKg: 100 })}
        onChangeWeight={onChangeWeight}
        onChangeReps={() => {}}
        onToggleComplete={() => {}}
        onToggleWarmup={() => {}}
      />,
    );
    fireEvent.changeText(getByLabelText('kg'), '');
    expect(onChangeWeight).toHaveBeenCalledWith(null);
  });

  it('fires complete and warm-up toggles', () => {
    const onToggleComplete = jest.fn();
    const onToggleWarmup = jest.fn();
    const { getByLabelText } = render(
      <WorkoutSetRow
        set={makeSet()}
        onChangeWeight={() => {}}
        onChangeReps={() => {}}
        onToggleComplete={onToggleComplete}
        onToggleWarmup={onToggleWarmup}
      />,
    );
    fireEvent.press(getByLabelText('Complete set 1'));
    fireEvent.press(getByLabelText('Working set 1, tap to make warm-up'));
    expect(onToggleComplete).toHaveBeenCalledTimes(1);
    expect(onToggleWarmup).toHaveBeenCalledTimes(1);
  });
});
