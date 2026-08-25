package com.fitnessrpg.app.ui.screens.main

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.fitnessrpg.app.BuildConfig
import com.fitnessrpg.app.data.updates.ApkInstaller
import com.fitnessrpg.app.data.updates.ReleaseApi
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.updates.RELEASES_PAGE_URL
import com.fitnessrpg.app.domain.updates.ReleaseInfo
import com.fitnessrpg.app.domain.updates.isNewerVersion
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, NO_RELEASES, ERROR, INSTALLING }

@Composable
fun UpdateSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(UpdateStatus.IDLE) }
    var latest by remember { mutableStateOf<ReleaseInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppText("APP UPDATES", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("Current version ${BuildConfig.VERSION_NAME}", variant = TextVariant.BODY)

            when (status) {
                UpdateStatus.UP_TO_DATE -> AppText("You're on the latest version.", variant = TextVariant.CAPTION, tone = TextTone.SUCCESS)
                UpdateStatus.NO_RELEASES -> AppText("No releases published yet.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                UpdateStatus.AVAILABLE -> AppText("Update ${latest?.version} is available.", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
                UpdateStatus.ERROR -> AppText(message ?: "Could not check for updates.", variant = TextVariant.CAPTION, tone = TextTone.DANGER)
                UpdateStatus.INSTALLING -> AppText("Downloading update…", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                else -> Unit
            }

            AppButton(
                "Check for updates",
                onClick = {
                    scope.launch {
                        status = UpdateStatus.CHECKING
                        message = null
                        runCatching { ReleaseApi.getLatestRelease() }
                            .onSuccess { release ->
                                latest = release
                                status = when {
                                    release == null -> UpdateStatus.NO_RELEASES
                                    isNewerVersion(release.version, BuildConfig.VERSION_NAME) -> UpdateStatus.AVAILABLE
                                    else -> UpdateStatus.UP_TO_DATE
                                }
                            }
                            .onFailure {
                                message = friendlyDataError(it, "Could not check for updates.")
                                status = UpdateStatus.ERROR
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SECONDARY,
                loading = status == UpdateStatus.CHECKING,
            )

            if (status == UpdateStatus.AVAILABLE) {
                val apkUrl = latest?.apkUrl
                if (apkUrl != null) {
                    AppButton(
                        "Download & install",
                        onClick = {
                            scope.launch {
                                status = UpdateStatus.INSTALLING
                                runCatching { ApkInstaller.downloadAndInstall(context, apkUrl) }
                                    .onFailure {
                                        message = friendlyDataError(it, "Update failed.")
                                        status = UpdateStatus.ERROR
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        loading = status == UpdateStatus.INSTALLING,
                    )
                } else {
                    AppButton(
                        "Open release page",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, RELEASES_PAGE_URL.toUri()))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
