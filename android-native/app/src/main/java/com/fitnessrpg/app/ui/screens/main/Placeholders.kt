package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant

/** Temporary placeholder used while the remaining tab screens are being ported. */
@Composable
fun PlaceholderScreen(title: String, note: String) {
    ScreenScaffold {
        Column {
            AppText(title.uppercase(), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText(title, variant = TextVariant.DISPLAY)
        }
        AppCard { AppText(note, tone = TextTone.SECONDARY) }
    }
}
