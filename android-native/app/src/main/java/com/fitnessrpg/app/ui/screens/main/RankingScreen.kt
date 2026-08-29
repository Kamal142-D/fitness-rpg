package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.ExerciseRankItem
import com.fitnessrpg.app.domain.analytics.completedWorkoutDates
import com.fitnessrpg.app.domain.analytics.computeExerciseRanks
import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.domain.rankings.PhysicalAttribute
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppProgressBar
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.CardTone
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.theme.rankColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/** Ranking tab: current standing, supporting evidence, and per-exercise ranks. */
@Composable
fun RankingScreen(userId: String, onAssessment: () -> Unit) {
    val player = rememberPlayerBundle(userId)

    ScreenScaffold {
        ScreenHeader("Ranking", subtitle = "Your current standing and the evidence behind it.")

        val bundle = player.data
        when {
            bundle == null && player.error != null -> AppCard {
                AppText(friendlyDataError(player.error, "Couldn't load your ranking."), tone = TextTone.DANGER)
            }
            bundle == null -> AppText("Loading ranking…", tone = TextTone.SECONDARY)
            else -> {
                val assessment = bundle.assessment
                val hunter = assessment.hunter
                val ranks = computeExerciseRanks(bundle.data.stats, bundle.data.bodyweightKg, bundle.data.sex)
                val workoutDates = completedWorkoutDates(bundle.data.sessions)

                RankSummary(hunter)

                if (assessment.needsAssessmentUpdate) {
                    AssessmentUpdateCard(onAssessment)
                }

                AttributeBalanceCard(hunter)
                WeeklyEvidenceCard(workoutDates)
                RankInsightCard(hunter)

                SectionHeader("Exercise ranks", if (ranks.isEmpty()) null else "${ranks.size} completed")
                ExerciseRanksCard(ranks)
            }
        }
    }
}

@Composable
private fun RankSummary(hunter: HunterRankResult) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RankMetricTile(
                value = hunter.hunterScore?.roundToInt()?.toString() ?: "—",
                label = "Hunter score",
                meta = "out of 100",
                modifier = Modifier.weight(1f),
            )
            RankMetricTile(
                value = hunter.rank.wire,
                label = "Current rank",
                meta = if (hunter.provisional) "Provisional" else "Validated",
                modifier = Modifier.weight(1f),
                highlight = true,
                accent = rankColor(hunter.rank),
            )
            RankMetricTile(
                value = hunter.rp.toString(),
                label = "Rank points",
                meta = "of 100 RP",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RankMetricTile(
    value: String,
    label: String,
    meta: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    accent: Color = Palette.Primary,
) {
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (highlight) accent.copy(alpha = .16f) else Palette.Surface1)
            .border(BorderStroke(1.dp, if (highlight) accent.copy(alpha = .55f) else Palette.Hairline), shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AppText(value, variant = TextVariant.HEADING, color = if (highlight) accent else Palette.TextPrimary, mono = true)
        AppText(label, variant = TextVariant.CAPTION, maxLines = 1)
        AppText(meta, variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, maxLines = 1)
    }
}

@Composable
private fun AssessmentUpdateCard(onAssessment: () -> Unit) {
    AppCard(tone = CardTone.GLASS) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("Assessment update", variant = TextVariant.HEADING)
            StatusPill("Action needed")
        }
        AppText(
            "Complete missing or stale physical assessments to improve rank accuracy.",
            variant = TextVariant.CAPTION,
            tone = TextTone.SECONDARY,
        )
        AppButton("Complete assessment", onAssessment, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AttributeBalanceCard(hunter: HunterRankResult) {
    val attributes = listOf(
        AttributePoint("Physique", hunter.physiqueScore, hunter.physique?.rp),
        AttributePoint("Strength", hunter.strengthScore, hunter.strength?.rp),
        AttributePoint("Conditioning", hunter.conditioningScore, hunter.conditioning?.rp),
    )
    val hasEvidence = attributes.any { it.score != null }

    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("Attribute balance", variant = TextVariant.HEADING)
            StatusPill("Score")
        }

        if (!hasEvidence) {
            AppText("Complete a physical assessment to reveal your attribute balance.", tone = TextTone.SECONDARY)
        } else {
            AttributeLineChart(
                values = attributes.map { (it.score ?: 0.0).toFloat().coerceIn(0f, 100f) },
                limitingIndex = hunter.limitingAttribute?.ordinal,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                attributes.forEach { attribute ->
                    AttributeLegend(attribute, Modifier.weight(1f))
                }
            }
        }
    }
}

private data class AttributePoint(val label: String, val score: Double?, val rp: Int?)

@Composable
private fun AttributeLineChart(values: List<Float>, limitingIndex: Int?) {
    Canvas(Modifier.fillMaxWidth().height(184.dp)) {
        val horizontalPadding = 18.dp.toPx()
        val topPadding = 18.dp.toPx()
        val bottomPadding = 14.dp.toPx()
        val chartHeight = size.height - topPadding - bottomPadding
        val chartWidth = size.width - horizontalPadding * 2
        val points = values.mapIndexed { index, value ->
            Offset(
                x = horizontalPadding + chartWidth * index / (values.size - 1).coerceAtLeast(1),
                y = topPadding + chartHeight * (1f - value / 100f),
            )
        }

        repeat(5) { index ->
            val y = topPadding + chartHeight * index / 4f
            drawLine(Palette.Hairline, Offset(horizontalPadding, y), Offset(size.width - horizontalPadding, y), 1.dp.toPx())
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, size.height - bottomPadding)
            lineTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height - bottomPadding)
            close()
        }
        drawPath(fillPath, Palette.Primary.copy(alpha = .08f))

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, Palette.Primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        points.forEachIndexed { index, point ->
            val isLimiting = limitingIndex == index
            drawCircle(if (isLimiting) Palette.Danger else Palette.Primary, radius = 7.dp.toPx(), center = point)
            drawCircle(Palette.Surface1, radius = 3.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun AttributeLegend(attribute: AttributePoint, modifier: Modifier = Modifier) {
    val score = attribute.score
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppText(attribute.label, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, maxLines = 1)
        if (score == null) {
            AppText("—", variant = TextVariant.LABEL, tone = TextTone.TERTIARY, mono = true)
        } else {
            val rank = scoreToRank(score)
            AppText("${rank.wire}  ${attribute.rp ?: 0} RP", variant = TextVariant.LABEL, color = rankColor(rank), mono = true, maxLines = 1)
        }
    }
}

@Composable
private fun WeeklyEvidenceCard(workoutDates: Set<LocalDate>, today: LocalDate = LocalDate.now()) {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..6L).map { weekStart.plusDays(it) }
    val completed = days.count { it in workoutDates }
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("Weekly activity", variant = TextVariant.HEADING)
            AppText("$completed / 7 days", variant = TextVariant.LABEL, tone = TextTone.ACCENT, mono = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { date ->
                val active = date in workoutDates
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (active) 68.dp else 24.dp)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(if (active) Palette.Primary else Palette.Surface3),
                    )
                    AppText(
                        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        variant = TextVariant.CAPTION,
                        tone = if (date == today) TextTone.ACCENT else TextTone.TERTIARY,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankInsightCard(hunter: HunterRankResult) {
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("Rank analysis", variant = TextVariant.HEADING)
            StatusPill(hunter.confidence.name)
        }

        hunter.limitingAttribute?.let {
            InsightRow("Limiting attribute", it.label(), valueColor = Palette.Danger)
        }
        hunter.rankCap?.let { InsightRow("Current rank cap", "Rank ${it.wire}") }

        val next = hunter.nextRank
        if (next == null) {
            AppText("No higher rank requirement is currently available.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        } else {
            AppText("Next rank ${next.rank.wire}", variant = TextVariant.LABEL)
            RequirementProgress("Physique", hunter.physiqueScore, next.physique)
            RequirementProgress("Strength", hunter.strengthScore, next.strength)
            RequirementProgress("Conditioning", hunter.conditioningScore, next.conditioning)
            RequirementProgress("Hunter score", hunter.hunterScore, next.hunterScore)
        }

        if (hunter.reasons.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
            hunter.reasons.forEach { reason -> ReasonRow(reason) }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String, valueColor: Color = Palette.TextPrimary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        AppText(label, tone = TextTone.SECONDARY)
        AppText(value, variant = TextVariant.LABEL, color = valueColor)
    }
}

@Composable
private fun RequirementProgress(label: String, current: Double?, required: Int) {
    val value = current?.toFloat() ?: 0f
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppText(label, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("${current?.roundToInt()?.toString() ?: "—"} / $required", variant = TextVariant.CAPTION, mono = true)
        }
        AppProgressBar(if (required > 0) value / required else 0f)
    }
}

@Composable
private fun ReasonRow(reason: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = Spacing.sm).size(5.dp).clip(CircleShape).background(Palette.TextTertiary))
        AppText(reason, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ExerciseRanksCard(ranks: List<ExerciseRankItem>) {
    AppCard(modifier = Modifier.fillMaxWidth(), tone = CardTone.GLASS) {
        if (ranks.isEmpty()) {
            AppText("No Exercise Ranks yet", variant = TextVariant.HEADING)
            AppText(
                "Log the main barbell lifts during a workout to earn permanent Exercise Ranks.",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
            )
        } else {
            ranks.forEachIndexed { index, rank ->
                ExerciseRankRow(rank)
                if (index < ranks.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
            }
        }
    }
}

@Composable
private fun ExerciseRankRow(rank: ExerciseRankItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        RankBadge(rank.rank, size = RankBadgeSize.MD)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppText(rank.name, variant = TextVariant.LABEL, maxLines = 1)
            AppText("${rank.rp} / 100 RP", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
            AppProgressBar(rank.rp / 100f)
        }
        rank.best1RMkg?.let {
            Column(modifier = Modifier.width(104.dp), horizontalAlignment = Alignment.End) {
                AppText("${it.roundToInt()} kg", variant = TextVariant.LABEL, mono = true)
                AppText("Estimated 1RM", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, maxLines = 1)
            }
        }
    }
}

private fun PhysicalAttribute.label(): String = when (this) {
    PhysicalAttribute.PHYSIQUE -> "Physique"
    PhysicalAttribute.STRENGTH -> "Strength"
    PhysicalAttribute.CONDITIONING -> "Conditioning"
}
