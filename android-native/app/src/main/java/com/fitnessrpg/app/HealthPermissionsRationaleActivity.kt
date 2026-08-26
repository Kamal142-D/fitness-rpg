package com.fitnessrpg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.FitnessRpgTheme
import com.fitnessrpg.app.ui.theme.Spacing

/** Privacy explanation opened from Health Connect's permission-management UI. */
class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessRpgTheme {
                ScreenScaffold {
                    AppText("DAILY MARCH", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    AppText("Your steps stay under your control", variant = TextVariant.DISPLAY)
                    AppCard {
                        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            AppText("Fitness RPG requests read-only access to your daily step total so it can fill the Daily March meter, calculate your walking streak, and unlock the daily XP reward.")
                            AppText("The app does not write health records, read routes or location, or use step data to calculate Hunter Rank.", tone = TextTone.SECONDARY)
                            AppText("You can remove access at any time in Health Connect settings.", tone = TextTone.SECONDARY)
                        }
                    }
                    AppButton("Close", onClick = { finish() }, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
