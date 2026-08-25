package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        AppButton("Sign out", onClick = onSignOut, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
    }
}
