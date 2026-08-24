import { forwardRef, useState } from 'react';
import {
  Pressable,
  StyleSheet,
  TextInput,
  type TextInputProps,
  View,
  type ViewStyle,
} from 'react-native';

import { Text } from '@/components/ui/Text';
import { Fonts, Palette, Radius, Spacing, Typography } from '@/constants/theme';

export interface TextFieldProps extends TextInputProps {
  label?: string;
  /** Validation error to show under the field. Also colors the border. */
  error?: string;
  /** Render a Show/Hide toggle for password fields. */
  secureToggle?: boolean;
  containerStyle?: ViewStyle;
}

/**
 * Labeled text input primitive. The border is a self-colored hairline that
 * shifts to the primary accent on focus and to danger on error — tonal, no glow.
 */
export const TextField = forwardRef<TextInput, TextFieldProps>(function TextField(
  { label, error, secureToggle = false, containerStyle, secureTextEntry, style, ...rest },
  ref,
) {
  const [focused, setFocused] = useState(false);
  const [hidden, setHidden] = useState(secureTextEntry ?? secureToggle);

  const borderColor = error ? Palette.danger : focused ? Palette.primary : Palette.hairlineStrong;

  return (
    <View style={[styles.container, containerStyle]}>
      {label ? (
        <Text variant="caption" color="secondary" style={styles.label}>
          {label}
        </Text>
      ) : null}
      <View style={[styles.inputWrap, { borderColor }]}>
        <TextInput
          ref={ref}
          accessibilityLabel={label}
          placeholderTextColor={Palette.textTertiary}
          secureTextEntry={secureToggle ? hidden : secureTextEntry}
          onFocus={(e) => {
            setFocused(true);
            rest.onFocus?.(e);
          }}
          onBlur={(e) => {
            setFocused(false);
            rest.onBlur?.(e);
          }}
          style={[styles.input, style]}
          {...rest}
        />
        {secureToggle ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={hidden ? 'Show password' : 'Hide password'}
            hitSlop={8}
            onPress={() => setHidden((h) => !h)}
          >
            <Text variant="caption" style={{ color: Palette.primary }}>
              {hidden ? 'Show' : 'Hide'}
            </Text>
          </Pressable>
        ) : null}
      </View>
      {error ? (
        <Text variant="caption" color="danger" style={styles.error}>
          {error}
        </Text>
      ) : null}
    </View>
  );
});

const styles = StyleSheet.create({
  container: {
    gap: Spacing.xs,
  },
  label: {
    marginLeft: Spacing.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    backgroundColor: Palette.surface2,
    borderRadius: Radius.md,
    borderWidth: 1,
    paddingHorizontal: Spacing.lg,
    minHeight: 48,
  },
  input: {
    flex: 1,
    color: Palette.textPrimary,
    fontFamily: Fonts.sans,
    fontSize: Typography.body.fontSize,
    paddingVertical: Spacing.md,
  },
  error: {
    marginLeft: Spacing.xs,
  },
});
