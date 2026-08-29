package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.R
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.LocalBottomBarClearance
import com.fitnessrpg.app.ui.components.appGlass
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    HOME("Home", R.drawable.ic_home),
    RANKING("Ranking", R.drawable.ic_ranking),
    GATES("Gates", R.drawable.ic_gates),
    PROFILE("Profile", R.drawable.ic_profile),
}

@Composable
fun HomeTabs(
    userId: String,
    onOpenGate: (gateId: String?) -> Unit,
    onOpenGates: () -> Unit,
    onWorkoutStarted: () -> Unit,
    onSettings: () -> Unit,
    onAssessment: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenDailyMarch: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlan: () -> Unit,
    onImportPlan: () -> Unit,
    onReevaluate: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val navigationHazeState = rememberHazeState()

    CompositionLocalProvider(LocalBottomBarClearance provides 88.dp) {
        Box(
            modifier = Modifier.fillMaxSize().background(Palette.Background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(navigationHazeState),
            ) {
                when (tab) {
                    Tab.HOME -> SystemScreen(userId, onEnterGate = onOpenGate, onSettings = onSettings, onOpenPlan = onOpenPlan)
                    Tab.RANKING -> RankingScreen(userId, onAssessment)
                    Tab.GATES -> GatesScreen(
                        userId = userId,
                        onOpenGate = { onOpenGate(it) },
                        onNewGate = onOpenGates,
                        onWorkoutStarted = onWorkoutStarted,
                        onImportPlan = onImportPlan,
                    )
                    Tab.PROFILE -> ProfileScreen(
                        userId = userId,
                        email = ServiceLocator.authRepository.currentUserEmail(),
                        onOpenQuests = onOpenQuests,
                        onOpenDailyMarch = onOpenDailyMarch,
                        onOpenHistory = onOpenHistory,
                        onOpenPlan = onOpenPlan,
                        onSettings = onSettings,
                        onReevaluate = onReevaluate,
                    )
                }
            }

            // Compact floating pill: wraps its content (smaller bar), items sit
            // close together, and the selected tab gets one rounded highlight that
            // wraps BOTH its icon and its label.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.sm)
                    .navigationBarsPadding()
                    .appGlass(
                        shape = RoundedCornerShape(Radius.pill),
                        hazeState = navigationHazeState,
                        border = false,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tab.entries.forEach { item ->
                        val isSelected = tab == item
                        Column(
                            // Fixed, equal width per tab so every selected highlight is the
                            // same rounded shape (no odd-looking wider pill on longer labels).
                            modifier = Modifier
                                .width(66.dp)
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(if (isSelected) Palette.PrimaryContainer else Color.Transparent)
                                .clickable { tab = item }
                                .semantics { selected = isSelected }
                                .padding(vertical = Spacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                painterResource(item.icon),
                                contentDescription = item.label,
                                tint = if (isSelected) Palette.Primary else Palette.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                            AppText(
                                item.label,
                                variant = TextVariant.CAPTION,
                                color = if (isSelected) Palette.Primary else Color.Unspecified,
                                tone = if (isSelected) TextTone.INHERIT else TextTone.SECONDARY,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
