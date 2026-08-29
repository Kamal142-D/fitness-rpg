package com.fitnessrpg.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.data.repo.AssessmentArea
import com.fitnessrpg.app.data.repo.AssessmentChange
import com.fitnessrpg.app.data.repo.AssessmentImprovementReport
import com.fitnessrpg.app.domain.rankings.PhysicalAttribute
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.CardTone
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.RevealContent
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun AssessmentImprovementScreen(
    report: AssessmentImprovementReport,
    onDone: () -> Unit,
) {
    val previous = report.previousHunter
    val current = report.currentHunter
    val rankImproved = current.rank.ordinal > previous.rank.ordinal

    ScreenScaffold {
        RevealContent {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                StatusPill("Assessment saved", color = Palette.Success)
                AppText("Your progress", variant = TextVariant.DISPLAY)
                AppText(
                    "Here is exactly what changed since your previous assessment.",
                    variant = TextVariant.CAPTION,
                    tone = TextTone.SECONDARY,
                )

                AppCard(tone = CardTone.GLASS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RankBadge(current.rank, size = RankBadgeSize.LG)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            AppText(if (rankImproved) "RANK INCREASED" else "CURRENT HUNTER RANK", variant = TextVariant.CAPTION, tone = if (rankImproved) TextTone.SUCCESS else TextTone.SECONDARY)
                            AppText(
                                if (rankImproved) "${previous.rank.wire} → ${current.rank.wire}" else "Rank ${current.rank.wire}",
                                variant = TextVariant.TITLE,
                                mono = true,
                            )
                            AppText(
                                "Hunter score ${previous.hunterScore.scoreText()} → ${current.hunterScore.scoreText()}",
                                variant = TextVariant.CAPTION,
                                tone = TextTone.SECONDARY,
                                mono = true,
                            )
                        }
                    }
                    current.limitingAttribute?.let {
                        AppText(
                            "Next focus: ${it.displayName()}",
                            variant = TextVariant.LABEL,
                            tone = TextTone.ACCENT,
                        )
                    }
                }

                SectionHeader("Where you improved", report.improvements.size.toString())
                if (report.improvements.isEmpty()) {
                    AppCard(tone = CardTone.FLAT) {
                        AppText("No ranked metric increased yet", variant = TextVariant.HEADING)
                        AppText(
                            "Your assessment was saved. Keep training and compare again after your next measurable change.",
                            variant = TextVariant.CAPTION,
                            tone = TextTone.SECONDARY,
                        )
                    }
                } else {
                    AssessmentGroups(report.improvements, baseline = false)
                }

                if (report.newBaselines.isNotEmpty()) {
                    SectionHeader("New evidence recorded", report.newBaselines.size.toString())
                    AppText(
                        "These values were not available before, so they establish a baseline instead of claiming an improvement.",
                        variant = TextVariant.CAPTION,
                        tone = TextTone.SECONDARY,
                    )
                    AssessmentGroups(report.newBaselines, baseline = true)
                }

                AppButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AssessmentGroups(changes: List<AssessmentChange>, baseline: Boolean) {
    AssessmentArea.entries.forEach { area ->
        val areaChanges = changes.filter { it.area == area }
        if (areaChanges.isNotEmpty()) {
            AppCard(tone = CardTone.RAISED) {
                AppText(area.displayName(), variant = TextVariant.HEADING)
                areaChanges.forEachIndexed { index, change ->
                    if (index > 0) HorizontalDivider(color = Palette.Hairline)
                    AssessmentChangeRow(change, baseline)
                }
            }
        }
    }
}

@Composable
private fun AssessmentChangeRow(change: AssessmentChange, baseline: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(change.metric, variant = TextVariant.LABEL, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("${change.previous} → ${change.current}", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
                AppText(change.delta, variant = TextVariant.LABEL, tone = if (baseline) TextTone.ACCENT else TextTone.SUCCESS, mono = true)
            }
        }
        change.detail?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true) }
    }
}

private fun AssessmentArea.displayName(): String = when (this) {
    AssessmentArea.OVERALL -> "Overall"
    AssessmentArea.PHYSIQUE -> "Physique"
    AssessmentArea.STRENGTH -> "Strength"
    AssessmentArea.CONDITIONING -> "Conditioning"
}

private fun PhysicalAttribute.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun Double?.scoreText(): String = this?.roundToInt()?.toString() ?: "—"
