package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** The complete rank, XP, and attribute summary shared by the Profile surface. */
@Composable
fun PlayerStatusCard(
    progression: PlayerProgression,
    hunter: HunterRankResult,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, tone = CardTone.GLASS) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SystemMark()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("PLAYER STATUS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RankBadge(hunter.rank, size = RankBadgeSize.MD)
                    AppText("Hunter rank ${hunter.rank.wire}", variant = TextVariant.TITLE)
                }
                AppText(
                    "Hunter score ${hunter.hunterScore?.roundToInt()?.toString() ?: "—"} / 100  ·  ${hunter.rp} RP",
                    variant = TextVariant.CAPTION,
                    tone = TextTone.TERTIARY,
                    mono = true,
                )
            }
        }
        if (hunter.provisional) StatusPill("Provisional")
        XpBar(
            progression.level,
            progression.currentXp,
            modifier = Modifier.padding(top = Spacing.lg).fillMaxWidth(),
        )
        Column(
            modifier = Modifier.padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            hunter.physiqueScore?.let {
                AttributeRow("Physique", it, hunter.physique?.rank, hunter.physique?.rp)
            }
            hunter.strengthScore?.let {
                AttributeRow("Strength", it, hunter.strength?.rank, hunter.strength?.rp)
            }
            hunter.conditioningScore?.let {
                AttributeRow("Conditioning", it, hunter.conditioning?.rank, hunter.conditioning?.rp)
            }
        }
        hunter.limitingAttribute?.let { limiting ->
            AppText(
                "${limiting.name.lowercase().replaceFirstChar { it.uppercase() }} is limiting your rank.",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}
