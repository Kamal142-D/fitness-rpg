package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.R
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.monthlyComparison
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.CardTone
import com.fitnessrpg.app.ui.components.PlayerStatusCard
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.components.SystemMark
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
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
    onReevaluate: () -> Unit,
) {
    val player = rememberPlayerBundle(userId)

    ScreenScaffold {
        ScreenHeader("Profile", subtitle = "Your identity, activity, and tools.")

        val t = player.data
        when {
            t == null && player.error != null -> AppCard { AppText(friendlyDataError(player.error, "Couldn't load your profile."), tone = TextTone.DANGER) }
            t == null -> AppText("Loading profile…", tone = TextTone.SECONDARY)
            else -> {
                AppCard(tone = CardTone.GLASS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        SystemMark()
                        Column(Modifier.weight(1f)) {
                            AppText(t.displayName?.takeIf { it.isNotBlank() } ?: "Hunter", variant = TextVariant.TITLE)
                            email?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
                        }
                        StatusPill("Active")
                    }
                }

                t.progression?.let { prog -> PlayerStatusCard(prog, t.assessment.hunter) }

                val monthly = monthlyComparison(t.data.sessions)
                SectionHeader("This month", "Compared with last month")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatChip("WORKOUTS", "${monthly.thisMonth.workouts}  /  ${monthly.lastMonth.workouts}", modifier = Modifier.weight(1f))
                    StatChip("VOLUME", "${(monthly.thisMonth.volumeKg / 1000.0).roundToInt()}t  /  ${(monthly.lastMonth.volumeKg / 1000.0).roundToInt()}t", modifier = Modifier.weight(1f))
                }

                SectionHeader("Hunter tools")
                HunterToolsCard(
                    onOpenPlan = onOpenPlan,
                    onOpenHistory = onOpenHistory,
                    onOpenQuests = onOpenQuests,
                    onOpenDailyMarch = onOpenDailyMarch,
                    onSettings = onSettings,
                    onReevaluate = onReevaluate,
                )
            }
        }
    }
}

@Composable
private fun HunterToolsCard(
    onOpenPlan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenDailyMarch: () -> Unit,
    onSettings: () -> Unit,
    onReevaluate: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        tone = CardTone.GLASS,
        padding = Spacing.md,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            HunterTool("Re-evaluation", R.drawable.ic_reeval, onReevaluate, Modifier.weight(1f))
            HunterTool("Training Plan", R.drawable.ic_plan, onOpenPlan, Modifier.weight(1f))
            HunterTool("History", R.drawable.ic_history, onOpenHistory, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            HunterTool("Quests", R.drawable.ic_quests, onOpenQuests, Modifier.weight(1f))
            HunterTool("Daily March", R.drawable.ic_march, onOpenDailyMarch, Modifier.weight(1f))
            HunterTool("Settings", R.drawable.ic_settings, onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HunterTool(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Palette.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Palette.Primary,
                modifier = Modifier.size(26.dp),
            )
        }
        AppText(
            text = label,
            variant = TextVariant.CAPTION,
            tone = TextTone.PRIMARY,
            maxLines = 1,
        )
    }
}
