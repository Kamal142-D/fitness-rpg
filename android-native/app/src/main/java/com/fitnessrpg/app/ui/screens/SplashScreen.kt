package com.fitnessrpg.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing

@Composable
fun SplashScreen(label: String = "Loading") {
    Column(
        modifier = Modifier.fillMaxSize().background(Palette.Background),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText("THE SYSTEM", variant = TextVariant.TITLE)
        CircularProgressIndicator(color = Palette.Primary, strokeWidth = 3.dp)
        AppText(label, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
    }
}
