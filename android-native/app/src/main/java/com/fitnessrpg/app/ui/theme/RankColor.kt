package com.fitnessrpg.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.fitnessrpg.app.domain.rank.Rank

/**
 * Accent color per rank (PLAN.txt §7). Rank is NEVER communicated by color alone
 * — always show the letter too (accessibility requirement). These are accents for
 * badges, not full-screen fills. Lives in the UI layer so the domain stays pure.
 */
fun rankColor(rank: Rank): Color = when (rank) {
    Rank.E -> Color(0xFF8B99AA) // gray
    Rank.D -> Color(0xFF4ADE80) // green
    Rank.C -> Color(0xFF38E1D6) // cyan
    Rank.B -> Palette.Primary // blue
    Rank.A -> Palette.Accent // purple
    Rank.S -> Color(0xFFF5C451) // gold
    Rank.S_PLUS -> Color(0xFFFFD76A)
    Rank.SS -> Color(0xFFE9ECFF)
    Rank.SSS -> Color(0xFFFF7CE8)
}
