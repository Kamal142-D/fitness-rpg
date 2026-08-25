package com.fitnessrpg.app.data.updates

import com.fitnessrpg.app.domain.updates.LATEST_RELEASE_URL
import com.fitnessrpg.app.domain.updates.ReleaseInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GithubAssetDto(
    val name: String,
    @SerialName("browser_download_url") val url: String,
)

@Serializable
private data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GithubAssetDto> = emptyList(),
)

/** Reads the latest published GitHub release for the in-app updater. */
object ReleaseApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLatestRelease(): ReleaseInfo? {
        val client = HttpClient(Android)
        try {
            val response = client.get(LATEST_RELEASE_URL) {
                header("Accept", "application/vnd.github+json")
            }
            val code = response.status.value
            if (code == 404) return null // no releases published yet
            if (code >= 400) throw RuntimeException("GitHub returned $code")

            val dto = json.decodeFromString<GithubReleaseDto>(response.bodyAsText())
            val apk = dto.assets.firstOrNull { it.name.lowercase().endsWith(".apk") }
            return ReleaseInfo(
                version = dto.tagName,
                apkUrl = apk?.url,
                notes = dto.body ?: "",
                htmlUrl = dto.htmlUrl,
            )
        } finally {
            client.close()
        }
    }
}
