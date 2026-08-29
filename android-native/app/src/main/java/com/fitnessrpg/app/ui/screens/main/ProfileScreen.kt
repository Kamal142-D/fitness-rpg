package com.fitnessrpg.app.ui.screens.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.R
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.completedWorkoutDates
import com.fitnessrpg.app.domain.analytics.monthlyComparison
import com.fitnessrpg.app.domain.analytics.normalizedWeeklyGoal
import com.fitnessrpg.app.domain.analytics.workoutsThisWeek
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.domain.onboarding.ageFromDob
import com.fitnessrpg.app.domain.progression.xpProgress
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppIconButton
import com.fitnessrpg.app.ui.components.AppProgressBar
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AttributeRow
import com.fitnessrpg.app.ui.components.CardTone
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.theme.rankColor
import kotlin.math.roundToInt

/** Profile tab: identity, progress, activity, profile details, and Hunter tools. */
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
        val bundle = player.data
        when {
            bundle == null && player.error != null -> AppCard {
                AppText(friendlyDataError(player.error, "Couldn't load your profile."), tone = TextTone.DANGER)
            }
            bundle == null -> AppText("Loading profile…", tone = TextTone.SECONDARY)
            else -> {
                val profile = bundle.profile
                val progression = bundle.progression
                val hunter = bundle.assessment.hunter
                val sessions = bundle.data.sessions
                val workoutDates = completedWorkoutDates(sessions)
                val weeklyGoal = normalizedWeeklyGoal(profile?.trainingDaysPerWeek)
                val weeklyWorkouts = workoutsThisWeek(workoutDates)
                val monthly = monthlyComparison(sessions)
                val totalVolume = sessions.sumOf { it.totalVolumeKg ?: 0.0 }

                ProfileHero(
                    displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: bundle.displayName ?: "Hunter",
                    email = email,
                    profile = profile,
                    hunter = hunter,
                    onSettings = onSettings,
                )

                ProfileSummaryCard(
                    workouts = sessions.size,
                    streakDays = progression?.currentStreakDays ?: 0,
                    volumeKg = totalVolume,
                    thisMonthWorkouts = monthly.thisMonth.workouts,
                    thisMonthVolumeKg = monthly.thisMonth.volumeKg,
                )

                if (progression != null) {
                    HunterProgressCard(
                        progression = progression,
                        hunter = hunter,
                        weeklyWorkouts = weeklyWorkouts,
                        weeklyGoal = weeklyGoal,
                    )
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

                PersonalInfoCard(profile)
            }
        }
    }
}

@Composable
private fun ProfileHero(
    displayName: String,
    email: String?,
    profile: Profile?,
    hunter: HunterRankResult,
    onSettings: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            HunterAvatar(hunter.rank.wire)
            AppText(displayName, variant = TextVariant.TITLE)
            email?.takeIf { it.isNotBlank() }?.let {
                AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                profile?.experienceLevel?.let { StatusPill(it.displayLabel()) }
                StatusPill("Rank ${hunter.rank.wire}", color = rankColor(hunter.rank))
            }
        }
        AppIconButton(
            iconRes = R.drawable.ic_settings,
            contentDescription = "Settings",
            onClick = onSettings,
            modifier = Modifier.align(Alignment.TopEnd),
            contained = true,
        )
    }
}

@Composable
private fun HunterAvatar(rank: String) {
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Palette.PrimaryContainer)
                .border(BorderStroke(2.dp, Palette.Primary), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_awesome),
                contentDescription = null,
                tint = Palette.Primary,
                modifier = Modifier.size(44.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(Palette.Primary)
                .border(BorderStroke(3.dp, Palette.Background), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppText(rank, variant = TextVariant.CAPTION, color = Palette.Background, mono = true)
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    workouts: Int,
    streakDays: Int,
    volumeKg: Double,
    thisMonthWorkouts: Int,
    thisMonthVolumeKg: Int,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS, padding = Spacing.lg) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ProfileMetric(workouts.toString(), "Workouts", Modifier.weight(1f))
            MetricDivider()
            ProfileMetric(streakDays.toString(), "Day streak", Modifier.weight(1f), valueColor = Palette.Primary)
            MetricDivider()
            ProfileMetric(formatVolume(volumeKg), "Volume", Modifier.weight(1f))
        }
        AppText(
            "This month  ·  $thisMonthWorkouts workouts  ·  ${formatVolume(thisMonthVolumeKg.toDouble())} lifted",
            variant = TextVariant.CAPTION,
            tone = TextTone.TERTIARY,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = Palette.TextPrimary) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppText(value, variant = TextVariant.HEADING, color = valueColor, mono = true, maxLines = 1)
        AppText(label, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, maxLines = 1)
    }
}

@Composable
private fun MetricDivider() {
    Box(Modifier.width(1.dp).height(48.dp).background(Palette.HairlineStrong))
}

@Composable
private fun HunterProgressCard(
    progression: PlayerProgression,
    hunter: HunterRankResult,
    weeklyWorkouts: Int,
    weeklyGoal: Int,
) {
    val xp = xpProgress(progression.currentXp, progression.level)
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS, padding = Spacing.lg) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("Hunter progress", variant = TextVariant.HEADING)
            if (hunter.provisional) StatusPill("Provisional")
        }

        GoalProgressRow(
            icon = R.drawable.ic_awesome,
            title = "Level ${progression.level}",
            detail = "${xp.current} / ${xp.required} XP",
            value = xp.fraction.toFloat(),
        )
        GoalProgressRow(
            icon = R.drawable.ic_ranking,
            title = "Hunter rank ${hunter.rank.wire}",
            detail = "${hunter.rp} / 100 RP",
            value = hunter.rp / 100f,
        )
        GoalProgressRow(
            icon = R.drawable.ic_streak,
            title = "Weekly training goal",
            detail = "$weeklyWorkouts / $weeklyGoal workouts",
            value = weeklyWorkouts.toFloat() / weeklyGoal.toFloat(),
        )

        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
        AppText("Physical attributes", variant = TextVariant.LABEL)
        hunter.physiqueScore?.let { AttributeRow("Physique", it, hunter.physique?.rank, hunter.physique?.rp) }
        hunter.strengthScore?.let { AttributeRow("Strength", it, hunter.strength?.rank, hunter.strength?.rp) }
        hunter.conditioningScore?.let { AttributeRow("Conditioning", it, hunter.conditioning?.rank, hunter.conditioning?.rp) }
        hunter.limitingAttribute?.let {
            AppText(
                "${it.name.displayLabel()} is currently limiting your rank.",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
            )
        }
    }
}

@Composable
private fun GoalProgressRow(
    @DrawableRes icon: Int,
    title: String,
    detail: String,
    value: Float,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Palette.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText(title, variant = TextVariant.LABEL)
                AppText("${(value.coerceIn(0f, 1f) * 100).roundToInt()}%", variant = TextVariant.CAPTION, tone = TextTone.ACCENT, mono = true)
            }
            AppText(detail, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
            AppProgressBar(value)
        }
    }
}

@Composable
private fun PersonalInfoCard(profile: Profile?) {
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS) {
        AppText("Personal information", variant = TextVariant.HEADING)
        InfoRow("Age", profile?.dateOfBirth?.let { runCatching { "${ageFromDob(it)} years" }.getOrNull() })
        InfoRow("Experience", profile?.experienceLevel?.displayLabel())
        InfoRow("Primary goal", profile?.fitnessGoal?.displayLabel())
        InfoRow("Training schedule", profile?.trainingDaysPerWeek?.let { "$it days / week" })
        InfoRow("Training location", profile?.trainingLocation?.displayLabel())
        InfoRow("Session length", profile?.preferredWorkoutMinutes?.let { "$it minutes" })
        InfoRow("Height", profile?.heightCm?.let { "${it.roundToInt()} cm" })
        InfoRow("Weight", profile?.currentWeightKg?.let { "${formatDecimal(it)} kg" })
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(label, tone = TextTone.SECONDARY)
        AppText(value ?: "Not set", variant = TextVariant.LABEL, tone = if (value == null) TextTone.TERTIARY else TextTone.PRIMARY)
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
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS, padding = Spacing.md) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            HunterTool("Re-evaluate", R.drawable.ic_reeval, onReevaluate, Modifier.weight(1f))
            HunterTool("Plan", R.drawable.ic_plan, onOpenPlan, Modifier.weight(1f))
            HunterTool("History", R.drawable.ic_history, onOpenHistory, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            HunterTool("Quests", R.drawable.ic_quests, onOpenQuests, Modifier.weight(1f))
            HunterTool("March", R.drawable.ic_march, onOpenDailyMarch, Modifier.weight(1f))
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
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Palette.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(24.dp))
        }
        AppText(label, variant = TextVariant.CAPTION, maxLines = 1)
    }
}

private fun String.displayLabel(): String =
    lowercase().replace('_', ' ').split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

private fun formatVolume(volumeKg: Double): String = when {
    volumeKg >= 1_000_000 -> "${formatDecimal(volumeKg / 1_000_000)}M kg"
    volumeKg >= 1_000 -> "${formatDecimal(volumeKg / 1_000)}t"
    else -> "${volumeKg.roundToInt()} kg"
}

private fun formatDecimal(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.roundToInt().toString() else String.format(java.util.Locale.US, "%.1f", rounded)
}
