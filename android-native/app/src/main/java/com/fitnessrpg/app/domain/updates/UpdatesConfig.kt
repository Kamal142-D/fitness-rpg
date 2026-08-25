package com.fitnessrpg.app.domain.updates

/**
 * In-app update source: GitHub Releases of this project's repo. The app compares
 * its own version to the latest release tag and, on Android, downloads +
 * installs the release APK attached to that release. The repo's Releases must be
 * PUBLIC so the app can read them without a token.
 */
const val GITHUB_OWNER = "Kamal142-D"
const val GITHUB_REPO = "fitness-rpg"

const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
const val RELEASES_PAGE_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"

/** Metadata about the latest published release. */
data class ReleaseInfo(
    val version: String,
    /** Direct download URL of the .apk asset, or null if the release has none. */
    val apkUrl: String?,
    val notes: String,
    val htmlUrl: String,
)
