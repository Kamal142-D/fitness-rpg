import { fireEvent, render } from '@testing-library/react-native';

import { TextField } from '@/components/ui/TextField';

describe('TextField', () => {
  it('renders its label and shows an error message', () => {
    const { getByText } = render(
      <TextField
        label="Email"
        error="Enter a valid email address"
        value=""
        onChangeText={() => {}}
      />,
    );
    expect(getByText('Email')).toBeTruthy();
    expect(getByText('Enter a valid email address')).toBeTruthy();
  });

  it('calls onChangeText when the user types', () => {
    const onChangeText = jest.fn();
    const { getByLabelText } = render(
      <TextField label="Email" value="" onChangeText={onChangeText} />,
    );
    fireEvent.changeText(getByLabelText('Email'), 'hello@test.dev');
    expect(onChangeText).toHaveBeenCalledWith('hello@test.dev');
  });

  it('toggles password visibility when secureToggle is set', () => {
    const { getByLabelText } = render(
      <TextField label="Password" secureToggle value="secret" onChangeText={() => {}} />,
    );
    // Starts hidden -> offers "Show"; tapping flips to "Hide".
    fireEvent.press(getByLabelText('Show password'));
    expect(getByLabelText('Hide password')).toBeTruthy();
  });
});
