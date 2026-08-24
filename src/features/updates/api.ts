import { LATEST_RELEASE_URL } from '@/features/updates/config';

export interface ReleaseInfo {
  version: string;
  /** Direct download URL of the .apk asset, or null if the release has none. */
  apkUrl: string | null;
  notes: string;
  htmlUrl: string;
}

interface GithubAsset {
  name: string;
  browser_download_url: string;
}
interface GithubRelease {
  tag_name: string;
  name: string | null;
  body: string | null;
  html_url: string;
  assets: GithubAsset[];
}

/**
 * Fetch the latest published GitHub release. Returns null when the repo has no
 * releases yet (404). Requires the repo's releases to be public.
 */
export async function getLatestRelease(): Promise<ReleaseInfo | null> {
  const res = await fetch(LATEST_RELEASE_URL, {
    headers: { Accept: 'application/vnd.github+json' },
  });
  if (res.status === 404) return null; // no releases published yet
  if (!res.ok) throw new Error(`GitHub returned ${res.status}`);

  const data = (await res.json()) as GithubRelease;
  const apk = (data.assets ?? []).find((a) => a.name.toLowerCase().endsWith('.apk'));
  return {
    version: data.tag_name,
    apkUrl: apk?.browser_download_url ?? null,
    notes: data.body ?? '',
    htmlUrl: data.html_url,
  };
}
