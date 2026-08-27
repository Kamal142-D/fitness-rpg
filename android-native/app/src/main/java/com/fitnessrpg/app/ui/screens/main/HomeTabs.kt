package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("System", Icons.Filled.Home),
    RANKING("Ranking", Icons.Filled.Leaderboard),
    GATES("Gates", Icons.AutoMirrored.Filled.List),
    PROFILE("Profile", Icons.Filled.Person),
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

    Scaffold(
        containerColor = Palette.Background,
        bottomBar = {
            Surface(
                color = Palette.Surface1,
                border = BorderStroke(1.dp, Palette.HairlineStrong),
                shadowElevation = 0.dp,
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    Tab.entries.forEach { item ->
                        val selected = tab == item
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = {
                                AppText(
                                    item.label,
                                    variant = TextVariant.CAPTION,
                                    tone = if (selected) TextTone.ACCENT else TextTone.SECONDARY,
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
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
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
    }
}
