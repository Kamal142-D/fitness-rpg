package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    HOME("System", R.drawable.ic_home),
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
) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val navigationHazeState = rememberHazeState()

    CompositionLocalProvider(LocalBottomBarClearance provides 104.dp) {
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
                    )
                    Tab.PROFILE -> ProfileScreen(
                        userId = userId,
                        email = ServiceLocator.authRepository.currentUserEmail(),
                        onOpenQuests = onOpenQuests,
                        onOpenDailyMarch = onOpenDailyMarch,
                        onOpenHistory = onOpenHistory,
                        onOpenPlan = onOpenPlan,
                        onSettings = onSettings,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    .navigationBarsPadding()
                    .appGlass(
                        shape = RoundedCornerShape(Radius.pill),
                        hazeState = navigationHazeState,
                    ),
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    Tab.entries.forEach { item ->
                        val selected = tab == item
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = item },
                            icon = { Icon(painterResource(item.icon), contentDescription = null) },
                            label = {
                                AppText(
                                    item.label,
                                    variant = TextVariant.CAPTION,
                                    // Selected label shares the icon's highlight colour so
                                    // icon + name read as one highlighted control.
                                    color = if (selected) Palette.Primary else Color.Unspecified,
                                    tone = if (selected) TextTone.INHERIT else TextTone.SECONDARY,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.Primary,
                                selectedTextColor = Palette.Primary,
                                indicatorColor = Palette.PrimaryContainer,
                                unselectedIconColor = Palette.TextSecondary,
                                unselectedTextColor = Palette.TextSecondary,
                            ),
                        )
                    }
                }
            }
        }
    }
}
