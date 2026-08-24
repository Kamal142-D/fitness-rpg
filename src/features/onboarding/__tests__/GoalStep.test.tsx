import { fireEvent, render } from '@testing-library/react-native';

import { GoalStep } from '@/features/onboarding/steps';
import { useOnboardingStore } from '@/features/onboarding/useOnboardingStore';

describe('GoalStep', () => {
  beforeEach(() => {
    useOnboardingStore.getState().reset();
  });

  it('renders goal options and writes the selection to the draft store', () => {
    const { getByText } = render(<GoalStep errors={{}} />);
    expect(getByText('Build muscle')).toBeTruthy();

    fireEvent.press(getByText('Get stronger'));
    expect(useOnboardingStore.getState().draft.fitness_goal).toBe('get_stronger');
  });

  it('shows a validation error when provided', () => {
    const { getByText } = render(<GoalStep errors={{ fitness_goal: 'Choose a goal' }} />);
    expect(getByText('Choose a goal')).toBeTruthy();
  });
});
