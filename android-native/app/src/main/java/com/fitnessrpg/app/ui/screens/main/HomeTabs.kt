package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.saveable.rememberSaveable
import com.fitnessrpg.app.ui.theme.Palette

private enum class Tab(val label: String, val icon: ImageVector) {
    SYSTEM("System", Icons.Filled.Home),
    MARCH("March", Icons.AutoMirrored.Filled.DirectionsWalk),
    GATES("Gates", Icons.AutoMirrored.Filled.List),
    PLAYER("Player", Icons.Filled.Person),
    QUESTS("Quests", Icons.Filled.Star),
}

@Composable
fun HomeTabs(
    userId: String,
    onOpenGate: (gateId: String?) -> Unit,
    onOpenGates: () -> Unit,
    onSettings: () -> Unit,
    onAssessment: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.SYSTEM) }

    Scaffold(
        containerColor = Palette.Background,
        bottomBar = {
            NavigationBar(containerColor = Palette.Surface1) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Palette.Primary,
                            selectedTextColor = Palette.Primary,
                            indicatorColor = Palette.Surface3,
                            unselectedIconColor = Palette.TextSecondary,
                            unselectedTextColor = Palette.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.SYSTEM -> SystemScreen(userId, onEnterGate = onOpenGate, onSettings = onSettings)
                Tab.MARCH -> DailyMarchScreen(userId)
                Tab.GATES -> GatesScreen(userId, onOpenGate = { onOpenGate(it) }, onNewGate = onOpenGates)
                Tab.PLAYER -> PlayerScreen(userId, onAssessment)
                Tab.QUESTS -> QuestsScreen()
            }
        }
    }
}
