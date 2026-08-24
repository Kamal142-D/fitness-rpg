import { Linking, View } from 'react-native';

import { Button, Card, ProgressBar, Text, TextLink } from '@/components/ui';
import { Spacing } from '@/constants/theme';
import { APK_INSTALL_SUPPORTED, RELEASES_PAGE_URL, useUpdateCheck } from '@/features/updates';

/**
 * "App updates" settings section. Checks GitHub Releases for a newer version and,
 * on Android release builds, downloads + installs the release APK. In Expo Go or
 * on iOS it falls back to opening the releases page.
 */
export function UpdateSection() {
  const u = useUpdateCheck();

  const openReleases = () => void Linking.openURL(RELEASES_PAGE_URL);

  return (
    <Card>
      <View
        style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline' }}
      >
        <Text variant="heading">App updates</Text>
        <Text variant="caption" color="tertiary" mono>
          v{u.currentVersion}
        </Text>
      </View>

      {u.status === 'up-to-date' ? (
        <Text variant="caption" color="success" style={{ marginTop: Spacing.sm }}>
          You&apos;re on the latest version.
        </Text>
      ) : null}
      {u.status === 'no-releases' ? (
        <Text variant="caption" color="secondary" style={{ marginTop: Spacing.sm }}>
          No releases have been published yet.
        </Text>
      ) : null}
      {u.status === 'error' && u.error ? (
        <Text variant="caption" color="danger" style={{ marginTop: Spacing.sm }}>
          {u.error}
        </Text>
      ) : null}

      {u.status === 'available' && u.latest ? (
        <View style={{ marginTop: Spacing.md, gap: Spacing.xs }}>
          <Text variant="label" color="accent">
            Update available: {u.latest.version}
          </Text>
          {u.latest.notes ? (
            <Text variant="caption" color="secondary" numberOfLines={4}>
              {u.latest.notes}
            </Text>
          ) : null}
        </View>
      ) : null}

      {u.status === 'downloading' ? (
        <View style={{ marginTop: Spacing.md, gap: Spacing.xs }}>
          <ProgressBar value={u.progress} />
          <Text variant="caption" color="secondary" mono>
            Downloading {Math.round(u.progress * 100)}%
          </Text>
        </View>
      ) : null}
      {u.status === 'installing' ? (
        <Text variant="caption" color="secondary" style={{ marginTop: Spacing.sm }}>
          Follow the Android prompt to finish installing.
        </Text>
      ) : null}

      <View style={{ marginTop: Spacing.md, gap: Spacing.sm }}>
        {u.status === 'available' && u.canInstall ? (
          <Button label="Download & install" onPress={() => void u.install()} />
        ) : null}

        {u.status === 'available' && !u.canInstall ? (
          <>
            <Button label="Open release page" variant="secondary" onPress={openReleases} />
            <Text variant="caption" color="tertiary">
              {APK_INSTALL_SUPPORTED
                ? 'This release has no APK attached.'
                : 'Install the update from the release page (Android release builds can install in-app).'}
            </Text>
          </>
        ) : null}

        {u.status !== 'available' && u.status !== 'downloading' && u.status !== 'installing' ? (
          <Button
            label={u.status === 'checking' ? 'Checking…' : 'Check for updates'}
            variant="secondary"
            onPress={() => void u.check()}
            loading={u.status === 'checking'}
          />
        ) : null}

        <View style={{ alignItems: 'center' }}>
          <TextLink label="View releases on GitHub" onPress={openReleases} />
        </View>
      </View>
    </Card>
  );
}
