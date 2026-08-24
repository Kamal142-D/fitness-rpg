import { Component, type ReactNode } from 'react';
import { View } from 'react-native';

import { Button, Text } from '@/components/ui';
import { Palette, Spacing } from '@/constants/theme';

interface Props {
  children: ReactNode;
}
interface State {
  error: Error | null;
}

/**
 * App-wide error boundary. Catches render/runtime errors below it and shows a
 * recoverable fallback instead of a white screen. In dev the crash still logs to
 * the console; a production build would forward `error` to a crash reporter here
 * (see docs/RELEASE.md — crash reporting is opt-in and privacy-scoped).
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error) {
    // Hook point for a crash reporter (Sentry/Bugsnag) once configured.
    if (__DEV__) console.error('[ErrorBoundary]', error);
  }

  reset = () => this.setState({ error: null });

  render() {
    if (this.state.error) {
      return (
        <View
          style={{
            flex: 1,
            backgroundColor: Palette.background,
            justifyContent: 'center',
            padding: Spacing.xl,
            gap: Spacing.lg,
          }}
        >
          <View style={{ gap: Spacing.xs }}>
            <Text variant="caption" color="secondary">
              SYSTEM
            </Text>
            <Text variant="display">Something went wrong</Text>
            <Text variant="body" color="secondary">
              The app hit an unexpected error. You can try again; your data is safe.
            </Text>
          </View>
          <Button label="Try again" onPress={this.reset} />
        </View>
      );
    }
    return this.props.children;
  }
}
