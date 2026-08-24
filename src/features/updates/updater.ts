/**
 * Download a release APK and hand it to the Android package installer. Android
 * only, and only meaningful in a real installed build — Expo Go cannot install
 * APKs. Requires the REQUEST_INSTALL_PACKAGES permission (declared in app.json)
 * and the user granting "install unknown apps" for the app on first use.
 */
import * as FileSystem from 'expo-file-system/legacy';
import * as IntentLauncher from 'expo-intent-launcher';
import { Platform } from 'react-native';

export const APK_INSTALL_SUPPORTED = Platform.OS === 'android';

const APK_MIME = 'application/vnd.android.package-archive';
const FLAG_GRANT_READ_URI_PERMISSION = 1;

/**
 * Download the APK (reporting 0..1 progress), then launch the installer. The
 * install itself is confirmed by the OS UI; this resolves once the installer
 * intent has been launched.
 */
export async function downloadAndInstallApk(
  apkUrl: string,
  onProgress?: (fraction: number) => void,
): Promise<void> {
  if (!APK_INSTALL_SUPPORTED) {
    throw new Error('In-app APK install is only available on Android.');
  }

  const dest = `${FileSystem.cacheDirectory}fitness-rpg-update.apk`;

  const download = FileSystem.createDownloadResumable(apkUrl, dest, {}, (p) => {
    if (p.totalBytesExpectedToWrite > 0) {
      onProgress?.(p.totalBytesWritten / p.totalBytesExpectedToWrite);
    }
  });

  const result = await download.downloadAsync();
  if (!result?.uri) throw new Error('Download failed.');

  // A content:// URI (via expo-file-system's FileProvider) is required so the
  // installer can read the file on modern Android.
  const contentUri = await FileSystem.getContentUriAsync(result.uri);

  await IntentLauncher.startActivityAsync('android.intent.action.VIEW', {
    data: contentUri,
    type: APK_MIME,
    flags: FLAG_GRANT_READ_URI_PERMISSION,
  });
}
