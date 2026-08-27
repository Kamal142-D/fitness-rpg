package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.monthlyComparison
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.XpBar
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.components.SystemMark
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Profile tab: who you are, your level and monthly activity, and a hub of icons
 * that open the Quests and Daily March pages plus Settings.
 */
@Composable
fun ProfileScreen(
    userId: String,
    email: String?,
    onOpenQuests: () -> Unit,
    onOpenDailyMarch: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlan: () -> Unit,
    onSettings: () -> Unit,
) {
    val player = rememberPlayerBundle(userId)

    ScreenScaffold {
        ScreenHeader("Hunter file", "Profile", subtitle = "Your identity, activity, and tools.")

        val t = player.data
        when {
            t == null && player.error != null -> AppCard { AppText(friendlyDataError(player.error, "Couldn't load your profile."), tone = TextTone.DANGER) }
            t == null -> AppText("Loading profile…", tone = TextTone.SECONDARY)
            else -> {
                AppCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        SystemMark()
                        Column(Modifier.weight(1f)) {
                            AppText(t.displayName?.takeIf { it.isNotBlank() } ?: "Hunter", variant = TextVariant.TITLE)
                            email?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
                        }
                        StatusPill("Active")
                    }
                }

                t.progression?.let { prog ->
                    AppCard {
                        SectionHeader("Progression", "Level ${prog.level}")
                        XpBar(prog.level, prog.currentXp, modifier = Modifier.padding(top = Spacing.md).fillMaxWidth())
                    }
                }

                val monthly = monthlyComparison(t.data.sessions)
                SectionHeader("This month", "Compared with last month")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatChip("WORKOUTS", "${monthly.thisMonth.workouts}  /  ${monthly.lastMonth.workouts}", modifier = Modifier.weight(1f))
                    StatChip("VOLUME", "${(monthly.thisMonth.volumeKg / 1000.0).roundToInt()}t  /  ${(monthly.lastMonth.volumeKg / 1000.0).roundToInt()}t", modifier = Modifier.weight(1f))
                }

                SectionHeader("Hunter tools")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    HubTile("Training Plan", Icons.Filled.CalendarMonth, onOpenPlan, Modifier.weight(1f))
                    HubTile("History", Icons.Filled.History, onOpenHistory, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    HubTile("Quests", Icons.Filled.Star, onOpenQuests, Modifier.weight(1f))
                    HubTile("Daily March", Icons.AutoMirrored.Filled.DirectionsWalk, onOpenDailyMarch, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    HubTile("Settings", Icons.Filled.Settings, onSettings, Modifier.weight(1f))
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HubTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Palette.Primary)
            AppText(label, variant = TextVariant.LABEL, tone = TextTone.SECONDARY)
        }
    }
}
