package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing

@Composable
fun SettingsScreen(email: String?, onBack: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    ScreenScaffold {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                AppText("ACCOUNT", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText("Settings", variant = TextVariant.DISPLAY)
            }
            AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST)
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("SIGNED IN AS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText(email ?: "Unknown", variant = TextVariant.BODY, mono = true)
            }
        }

        UpdateSection(modifier = Modifier.fillMaxWidth())

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppText("HEALTH & ACTIVITY DATA", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText("Daily March requests read-only step totals. It does not request routes, GPS location, or permission to write health records.", variant = TextVariant.CAPTION)
                AppButton(
                    "Manage Health Connect access",
                    onClick = { runCatching { context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context)) } },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("EXERCISE DATA & ATTRIBUTION", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText("1,318 unique exercises imported from hasaneyldrm/exercises-dataset (MIT data/code).", variant = TextVariant.CAPTION)
                AppText("Exercise media © Gym Visual. OpenGym uses the same underlying catalog, so it is not imported a second time.", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
            }
        }

        AppButton("Sign out", onClick = onSignOut, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
    }
}
