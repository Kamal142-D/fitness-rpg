package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.domain.rank.scoreToRank
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.domain.rankings.PhysicalAttribute
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** Explains a Hunter Rank: badge + provisional tag + pillars + limiting attribute
 *  + next-rank requirements, so the user sees exactly why they are ranked here. */
@Composable
fun HunterRankPanel(result: HunterRankResult, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(result.rank, size = RankBadgeSize.LG)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("HUNTER RANK", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    AppText("Rank ${result.rank.name}", variant = TextVariant.TITLE)
                    if (result.provisional) ProvisionalTag()
                }
                AppText("Calculated Hunter Score ${result.hunterScore?.roundToInt()?.toString() ?: "—"} / 100", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
                result.rankCap?.let { AppText("CURRENT RANK CAP  ${it.name}", variant = TextVariant.CAPTION, tone = TextTone.ACCENT, mono = true) }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppText("PHYSICAL ATTRIBUTES", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            PillarRow("Physique", result.physiqueScore, result.physique?.rank)
            PillarRow("Strength", result.strengthScore, result.strength?.rank)
            PillarRow("Conditioning", result.conditioningScore, result.conditioning?.rank)
        }

        result.limitingAttribute?.let { limiting ->
            Column(modifier = Modifier.padding(top = Spacing.md)) {
                AppText("LIMITING ATTRIBUTE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText(limiting.label(), variant = TextVariant.HEADING, tone = TextTone.DANGER)
                AppText("${limiting.label()} is limiting your rank.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            }
        }

        Column(modifier = Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppText("ASSESSMENT CONFIDENCE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText(result.confidence.name, variant = TextVariant.HEADING, mono = true)
            result.reasons.forEach { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
        }

        val next = result.nextRank
        if (next != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("NEXT RANK: ${next.rank.name}", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                RequirementRow("Physique", result.physiqueScore, next.physique)
                RequirementRow("Strength", result.strengthScore, next.strength)
                RequirementRow("Conditioning", result.conditioningScore, next.conditioning)
                RequirementRow("Hunter Score", result.hunterScore, next.hunterScore)
            }
        }

        if (result.provisional) {
            AppText(
                "Complete missing or stale physical assessments to unlock your full Hunter Rank.",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Composable
private fun ProvisionalTag() {
    val shape = RoundedCornerShape(Radius.sm)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(shape)
            .background(Palette.Accent.copy(alpha = 0.15f))
            .border(androidx.compose.foundation.BorderStroke(1.dp, Palette.Accent.copy(alpha = 0.6f)), shape)
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    ) {
        AppText("PROVISIONAL", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
    }
}

@Composable
private fun PillarRow(label: String, score: Double?, rank: com.fitnessrpg.app.domain.rank.Rank?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AppText(label, variant = TextVariant.LABEL, tone = TextTone.SECONDARY)
        if (score == null) {
            AppText("Not assessed", variant = TextVariant.LABEL, tone = TextTone.TERTIARY)
        } else {
            AppText("${(rank ?: scoreToRank(score)).name} — ${score.roundToInt()}", variant = TextVariant.LABEL, mono = true)
        }
    }
}

@Composable
private fun RequirementRow(label: String, current: Double?, required: Int) {
    val met = current != null && current >= required
    val currentText = current?.roundToInt()?.toString() ?: "—"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AppText(label, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppText(
            "$currentText / $required ${if (met) "✓" else "✕"}",
            variant = TextVariant.CAPTION,
            tone = if (met) TextTone.SUCCESS else TextTone.DANGER,
            mono = true,
        )
    }
}

private fun PhysicalAttribute.label(): String = when (this) {
    PhysicalAttribute.PHYSIQUE -> "Physique"
    PhysicalAttribute.STRENGTH -> "Strength"
    PhysicalAttribute.CONDITIONING -> "Conditioning"
}
