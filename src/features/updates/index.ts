export { parseVersion, compareVersions, isNewerVersion } from '@/features/updates/version';
export { getLatestRelease } from '@/features/updates/api';
export type { ReleaseInfo } from '@/features/updates/api';
export { downloadAndInstallApk, APK_INSTALL_SUPPORTED } from '@/features/updates/updater';
export { useUpdateCheck, CURRENT_VERSION } from '@/features/updates/useUpdateCheck';
export type { UpdateState, UpdateStatus } from '@/features/updates/useUpdateCheck';
export { RELEASES_PAGE_URL, GITHUB_OWNER, GITHUB_REPO } from '@/features/updates/config';
