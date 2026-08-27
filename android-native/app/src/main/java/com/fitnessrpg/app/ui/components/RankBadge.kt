package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.rankColor

enum class RankBadgeSize { SM, MD, LG }

/**
 * The rank letter in a compact, rank-tinted badge. Rank is NEVER communicated by
 * color alone — the letter is always shown. The color is a tonal accent (faint
 * tinted fill + self-colored edge), not a saturated flood.
 */
@Composable
fun RankBadge(rank: Rank, modifier: Modifier = Modifier, size: RankBadgeSize = RankBadgeSize.MD) {
    val box = when (size) { RankBadgeSize.SM -> 28.dp; RankBadgeSize.MD -> 40.dp; RankBadgeSize.LG -> 64.dp }
    val baseFont = when (size) { RankBadgeSize.SM -> 15f; RankBadgeSize.MD -> 22f; RankBadgeSize.LG -> 36f }
    val font = (baseFont * when (rank.wire.length) { 1 -> 1f; 2 -> .82f; else -> .66f }).sp
    val radius = when (size) { RankBadgeSize.SM -> Radius.sm; RankBadgeSize.MD -> Radius.md; RankBadgeSize.LG -> Radius.lg }
    val color = rankColor(rank)
    val shape = RoundedCornerShape(radius)

    Box(
        modifier = modifier
            .size(box)
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.5.dp, color.copy(alpha = 0.65f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = rank.wire, color = color, fontWeight = FontWeight.ExtraBold, fontSize = font)
    }
}

/** Placeholder badge for a Gate whose rank isn't known yet (never cleared). */
@Composable
fun UnknownRankBadge(modifier: Modifier = Modifier, size: RankBadgeSize = RankBadgeSize.MD) {
    val box = when (size) { RankBadgeSize.SM -> 28.dp; RankBadgeSize.MD -> 40.dp; RankBadgeSize.LG -> 64.dp }
    val font = when (size) { RankBadgeSize.SM -> 15f; RankBadgeSize.MD -> 22f; RankBadgeSize.LG -> 36f }.sp
    val radius = when (size) { RankBadgeSize.SM -> Radius.sm; RankBadgeSize.MD -> Radius.md; RankBadgeSize.LG -> Radius.lg }
    val color = Palette.TextTertiary
    val shape = RoundedCornerShape(radius)

    Box(
        modifier = modifier
            .size(box)
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(BorderStroke(1.5.dp, color.copy(alpha = 0.45f)), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "?", color = color, fontWeight = FontWeight.ExtraBold, fontSize = font)
    }
}
