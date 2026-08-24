import Constants from 'expo-constants';
import { useCallback, useState } from 'react';

import { getLatestRelease, type ReleaseInfo } from '@/features/updates/api';
import { APK_INSTALL_SUPPORTED, downloadAndInstallApk } from '@/features/updates/updater';
import { isNewerVersion } from '@/features/updates/version';

/** The running app's version (from app.json, embedded at build time). */
export const CURRENT_VERSION = Constants.expoConfig?.version ?? '0.0.0';

export type UpdateStatus =
  | 'idle'
  | 'checking'
  | 'up-to-date'
  | 'available'
  | 'no-releases'
  | 'error'
  | 'downloading'
  | 'installing';

export interface UpdateState {
  status: UpdateStatus;
  currentVersion: string;
  latest: ReleaseInfo | null;
  progress: number;
  error: string | null;
  canInstall: boolean;
  check: () => Promise<void>;
  install: () => Promise<void>;
}

export function useUpdateCheck(): UpdateState {
  const [status, setStatus] = useState<UpdateStatus>('idle');
  const [latest, setLatest] = useState<ReleaseInfo | null>(null);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const check = useCallback(async () => {
    setStatus('checking');
    setError(null);
    try {
      const release = await getLatestRelease();
      if (!release) {
        setLatest(null);
        setStatus('no-releases');
        return;
      }
      setLatest(release);
      setStatus(isNewerVersion(release.version, CURRENT_VERSION) ? 'available' : 'up-to-date');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not check for updates.');
      setStatus('error');
    }
  }, []);

  const install = useCallback(async () => {
    if (!latest?.apkUrl) return;
    setStatus('downloading');
    setProgress(0);
    setError(null);
    try {
      await downloadAndInstallApk(latest.apkUrl, setProgress);
      setStatus('installing');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Update failed.');
      setStatus('error');
    }
  }, [latest]);

  const canInstall = APK_INSTALL_SUPPORTED && !!latest?.apkUrl;

  return {
    status,
    currentVersion: CURRENT_VERSION,
    latest,
    progress,
    error,
    canInstall,
    check,
    install,
  };
}
