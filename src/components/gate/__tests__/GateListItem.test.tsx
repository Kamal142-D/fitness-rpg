import { fireEvent, render } from '@testing-library/react-native';

import { GateListItem } from '@/components/gate/GateListItem';
import type { GateTemplate } from '@/features/gates/types';

const template: GateTemplate = {
  id: 't1',
  user_id: null,
  name: 'Push',
  description: 'Chest, shoulders, triceps',
  estimated_duration_minutes: 50,
  difficulty: 'C',
  is_system_template: true,
  created_at: '2026-01-01T00:00:00Z',
  updated_at: '2026-01-01T00:00:00Z',
};

describe('GateListItem', () => {
  it('renders the gate name and muscle groups', () => {
    const { getByText } = render(<GateListItem template={template} onPress={() => {}} />);
    expect(getByText('Push')).toBeTruthy();
    expect(getByText('Chest · shoulders · triceps')).toBeTruthy();
    expect(getByText('SYSTEM')).toBeTruthy();
  });

  it('fires onPress when tapped', () => {
    const onPress = jest.fn();
    const { getByLabelText } = render(<GateListItem template={template} onPress={onPress} />);
    fireEvent.press(getByLabelText('Push gate, difficulty C'));
    expect(onPress).toHaveBeenCalledTimes(1);
  });
});
