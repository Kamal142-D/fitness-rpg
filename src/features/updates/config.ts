/**
 * In-app update source: GitHub Releases of this project's repo. The app compares
 * its own version to the latest release tag and, on Android, downloads +
 * installs the release APK attached to that release.
 *
 * The repo (or at least its Releases) must be PUBLIC so the app can read them
 * without embedding a token.
 */
export const GITHUB_OWNER = 'Kamal142-D';
export const GITHUB_REPO = 'fitness-rpg';

export const LATEST_RELEASE_URL = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`;
export const RELEASES_PAGE_URL = `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases`;
